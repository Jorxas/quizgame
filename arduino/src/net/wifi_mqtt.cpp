#include "net/wifi_mqtt.h"
#include "config.h"
#include "secrets.h"
#include "hardware/oled.h"
#include <ArduinoJson.h>

namespace net::wifi_mqtt {

static WiFiClient g_netClient;
static PubSubClient g_client(g_netClient);
static char g_payloadBuf[384];
static uint32_t g_lastRequestStatus = 0;

// Display state (same as web controller)
static char g_controllerId[24] = "";
static char g_playerId[32] = "";
static bool g_ready = false;
static float g_score = 0.0f;
static char g_gameState[16] = "LOBBY";
static char g_subscribedPlayerId[32] = "";
static uint32_t g_lastOledUpdate = 0;

String macAddressString() {
  uint8_t mac[6];
  WiFi.macAddress(mac);

  char buf[18];
  sprintf(buf, "%02X:%02X:%02X:%02X:%02X:%02X",
          mac[0], mac[1], mac[2], mac[3], mac[4], mac[5]);

  return String(buf);
}

bool connectWiFi() {
  if (WiFi.status() == WL_CONNECTED) return true;

  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);

  int retries = 40; // ~20s
  while (WiFi.status() != WL_CONNECTED && retries-- > 0) {
    delay(500);
    if (retries % 8 == 0) { SAFE_PRINTLN("WiFi..."); }
  }

  if (WiFi.status() == WL_CONNECTED) {
    SAFE_PRINTLN("WiFi OK");
    return true;
  }
  SAFE_PRINTLN("WiFi fail");
  return false;
}

static void refreshControllerDisplay() {
  uint32_t now = millis();
  if (now - g_lastOledUpdate < OLED_MIN_UPDATE_INTERVAL_MS) return;
  g_lastOledUpdate = now;
  hw::oled::showControllerInfo(g_controllerId, g_playerId, g_ready, g_score);
}

static void subscribePlayerTopics(const char* playerId) {
  if (!playerId || !playerId[0]) return;
  if (strcmp(playerId, g_subscribedPlayerId) == 0) return;

  // Unsubscribe from old player
  if (g_subscribedPlayerId[0]) {
    String oldStatus = String(MQTT_PREFIX) + "player/" + g_subscribedPlayerId + "/status";
    String oldResult = String(MQTT_PREFIX) + "player/" + g_subscribedPlayerId + "/result";
    g_client.unsubscribe(oldStatus.c_str());
    g_client.unsubscribe(oldResult.c_str());
  }

  strncpy(g_subscribedPlayerId, playerId, sizeof(g_subscribedPlayerId) - 1);
  g_subscribedPlayerId[sizeof(g_subscribedPlayerId) - 1] = '\0';

  String statusTopic = String(MQTT_PREFIX) + "player/" + playerId + "/status";
  String resultTopic = String(MQTT_PREFIX) + "player/" + playerId + "/result";
  g_client.subscribe(statusTopic.c_str());
  g_client.subscribe(resultTopic.c_str());
}

static void mqttCallback(char* topic, byte* payload, unsigned int length) {
  if (length >= sizeof(g_payloadBuf)) length = sizeof(g_payloadBuf) - 1;
  memcpy(g_payloadBuf, payload, length);
  g_payloadBuf[length] = '\0';

  const String mac = macAddressString();
  String statusTopic = String(MQTT_PREFIX) + "controller/" + mac + "/status";

  // controller/{mac}/status -> playerId, ready
  if (String(topic) == statusTopic) {
    StaticJsonDocument<256> doc;
    if (!deserializeJson(doc, g_payloadBuf)) {
      const char* pid = doc["playerId"].as<const char*>();
      bool ready = doc["ready"].as<bool>();
      if (pid) strncpy(g_playerId, pid, sizeof(g_playerId) - 1);
      else g_playerId[0] = '\0';
      g_playerId[sizeof(g_playerId) - 1] = '\0';
      g_ready = ready;
      subscribePlayerTopics(g_playerId[0] ? g_playerId : nullptr);
      refreshControllerDisplay();
    }
    return;
  }

  // player/{id}/status -> ready update
  if (String(topic).startsWith(String(MQTT_PREFIX) + "player/") && String(topic).endsWith("/status")) {
    StaticJsonDocument<128> doc;
    if (!deserializeJson(doc, g_payloadBuf)) {
      if (doc.containsKey("ready")) {
        g_ready = doc["ready"].as<bool>();
      } else {
        const char* st = doc["status"].as<const char*>();
        g_ready = (st && strcmp(st, "READY") == 0);
      }
      refreshControllerDisplay();
    }
    return;
  }

  // player/{id}/result -> correct, points, correctOption
  if (String(topic).startsWith(String(MQTT_PREFIX) + "player/") && String(topic).endsWith("/result")) {
    StaticJsonDocument<256> doc;
    if (!deserializeJson(doc, g_payloadBuf)) {
      bool correct = doc["correct"].as<bool>();
      float pts = doc["points"].as<float>();
      const char* opt = doc["correctOption"].as<const char*>();
      if (correct) g_score += pts;
      hw::oled::showResult(correct, pts, opt ? opt : "");
      // Return to controller info quickly so score/name/ready stay visible.
      delay(900);
      refreshControllerDisplay();
    }
    return;
  }

  // game/state
  if (String(topic) == String(MQTT_PREFIX) + "game/state") {
    StaticJsonDocument<128> doc;
    if (!deserializeJson(doc, g_payloadBuf)) {
      const char* s = doc["state"].as<const char*>();
      if (s) {
        strncpy(g_gameState, s, sizeof(g_gameState) - 1);
        g_gameState[sizeof(g_gameState) - 1] = '\0';
          refreshControllerDisplay();
      }
    }
    return;
  }

  // game/question
  if (String(topic) == String(MQTT_PREFIX) + "game/question") {
    hw::oled::showAnswerPrompt();
    return;
  }

  // game/evaluation
  if (String(topic) == String(MQTT_PREFIX) + "game/evaluation") {
    hw::oled::showEvaluation();
    return;
  }

  // game/ended
  if (String(topic) == String(MQTT_PREFIX) + "game/ended") {
    float finalScore = g_score;
    g_score = 0.0f;
    strcpy(g_gameState, "LOBBY");
    hw::oled::showEnded(finalScore);
    return;
  }

  // controller/{mac}/ping -> reply pong
  String pingTopic = String(MQTT_PREFIX) + "controller/" + mac + "/ping";
  if (String(topic) == pingTopic) {
    String pongTopic = String(MQTT_PREFIX) + "controller/" + mac + "/pong";
    String pongPayload = "{\"action\":\"pong\",\"controllerId\":\"" + mac + "\",\"ts\":" + String(millis()) + "}";
    g_client.publish(pongTopic.c_str(), pongPayload.c_str());
    return;
  }
}

bool connectMqtt() {
  if (!connectWiFi()) return false;

  g_client.setServer(MQTT_HOST, MQTT_PORT);
  g_client.setCallback(mqttCallback);
  if (g_client.connected()) return true;

  SAFE_PRINTLN("MQTT connect...");

  const String clientId = String("uno-") + macAddressString();
  const String mac = macAddressString();

  for (int i = 0; i < 3; i++) {
    if (g_client.connect(clientId.c_str(), MQTT_USER, MQTT_PASS)) {
      SAFE_PRINTLN("MQTT OK");

      strncpy(g_controllerId, mac.c_str(), sizeof(g_controllerId) - 1);
      g_controllerId[sizeof(g_controllerId) - 1] = '\0';

      // Register like web controller
      String regTopic = String(MQTT_PREFIX) + "controller/" + mac + "/register";
      String regPayload = "{\"action\":\"register\",\"controllerId\":\"" + mac + "\"}";
      g_client.publish(regTopic.c_str(), regPayload.c_str());

      // Subscribe to controller topics
      g_client.subscribe((String(MQTT_PREFIX) + "controller/" + mac + "/ping").c_str());
      g_client.subscribe((String(MQTT_PREFIX) + "controller/" + mac + "/status").c_str());

      // Subscribe to game topics
      g_client.subscribe((String(MQTT_PREFIX) + "game/state").c_str());
      g_client.subscribe((String(MQTT_PREFIX) + "game/question").c_str());
      g_client.subscribe((String(MQTT_PREFIX) + "game/evaluation").c_str());
      g_client.subscribe((String(MQTT_PREFIX) + "game/ended").c_str());

      // Request status (backend will publish controller/status)
      String reqTopic = String(MQTT_PREFIX) + "controller/" + mac + "/request-status";
      g_client.publish(reqTopic.c_str(), "{}");

      refreshControllerDisplay();
      return true;
    }

    SAFE_PRINTLN("MQTT retry...");
    delay(2000);
  }
  SAFE_PRINTLN("MQTT fail");
  return false;
}

bool ensureConnected() {
  return connectMqtt();
}

bool publishMacAddress() {
  if (!ensureConnected()) return false;
  const String mac = macAddressString();
  const String payload = String("{\"mac\":\"") + mac + "\"}";
  return g_client.publish(MQTT_TOPIC_MAC, payload.c_str());
}

void loop() {
  if (!g_client.connected()) {
    static uint32_t lastReconnect = 0;
    uint32_t now = millis();
    if (now - lastReconnect > 5000) {
      lastReconnect = now;
      if (connectMqtt()) {
        SAFE_PRINTLN("MQTT recon");
      }
    }
  } else {
    g_client.loop();

    // Request status every 5s (like web controller)
    uint32_t now = millis();
    if (now - g_lastRequestStatus > 5000) {
      g_lastRequestStatus = now;
      const String mac = macAddressString();
      String reqTopic = String(MQTT_PREFIX) + "controller/" + mac + "/request-status";
      g_client.publish(reqTopic.c_str(), "{}");
    }
  }
}

} // namespace net::wifi_mqtt
