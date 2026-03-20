/**
 * WiFi + MQTT – Verbindung zum Broker, Register, Ready, Antworten, RFID-Lookup
 */
#include "net/wifi_mqtt.h"
#include "config.h"
#include "secrets.h"
#include <string.h>

namespace net::wifi_mqtt {

static WiFiClient g_netClient;
static PubSubClient g_client(g_netClient);

// RFID MQTT reply: set when we receive controller/{mac}/rfid/reply
static String g_rfidReplyUsername;
static bool g_rfidReplyReceived = false;

// Game state from MQTT (for answer buttons)
static String g_gameState;
static long g_currentQuestionId = 0;
// True after game/state QUESTION until game/question arrives (backend sends state before question)
static bool g_waitingForQuestion = false;
static uint32_t g_questionStateTimestamp = 0;

// Bound username for filtering player/result (set from main after RFID login)
static String g_boundUsernameForResult;
// Ready status from controller/status (or set when we publish ready)
static bool g_boundReady = false;
// Total score (accumulated from player/result, reset on LOBBY/ENDED)
static long g_totalScore = 0;
// Brief "+X Pkt" display: points to show, 0 = don't show
static long g_plusXPoints = 0;
static uint32_t g_plusXShowUntil = 0;

// Helper: find value of key "key":"value" or "key":123 in buf
/** Liest String-Wert eines JSON-Schlüssels aus dem Buffer. */
static String extractJsonString(const char* buf, const char* key) {
  String k = String("\"") + key + "\":\"";
  const char* p = strstr(buf, k.c_str());
  if (!p) return "";
  p += k.length();
  const char* end = strchr(p, '"');
  if (!end || end <= p) return "";
  return String(p).substring(0, (size_t)(end - p));
}
/** Liest Long-Wert eines JSON-Schlüssels aus dem Buffer. */
static long extractJsonLong(const char* buf, const char* key) {
  String k = String("\"") + key + "\":";
  const char* p = strstr(buf, k.c_str());
  if (!p) return -1;
  p += k.length();
  return atol(p);
}
/** Liest Boolean-Wert eines JSON-Schlüssels aus dem Buffer. */
static bool extractJsonBool(const char* buf, const char* key) {
  String k = String("\"") + key + "\":";
  const char* p = strstr(buf, k.c_str());
  if (!p) return false;
  p += k.length();
  return (strncmp(p, "true", 4) == 0);
}

/** MQTT-Callback: verarbeitet game/state, game/question, player/result, controller/status, rfid/reply, ping. */
static void onMqttMessage(char* topic, uint8_t* payload, unsigned int len) {
  size_t L = strlen(topic);
  if (len > 0 && len < 512) {
    char buf[512];
    memcpy(buf, payload, len);
    buf[len] = '\0';
    // Game state (internal only: enable answer buttons when QUESTION)
    if (strstr(topic, "game/state")) {
      String state = extractJsonString(buf, "state");
      if (state.length() > 0) {
        g_gameState = state;
        if (state == "LOBBY") g_totalScore = 0;  // reset for new round
        if (state != "QUESTION") {
          g_waitingForQuestion = true;
        } else {
          g_questionStateTimestamp = millis();
        }
      }
      return;
    }
    // Question: only need questionId to send answers (no display like web controller)
    if (strstr(topic, "game/question")) {
      long qId = extractJsonLong(buf, "questionId");
      g_currentQuestionId = qId;
      g_waitingForQuestion = false;
      return;
    }
    // Game ended
    if (strstr(topic, "game/ended")) {
      g_gameState = "ENDED";
      g_totalScore = 0;  // reset for next game
      return;
    }
    // Per-answer result for our player (player/{username}/result)
    if (strstr(topic, "/result")) {
      const char* prefix = "player/";
      const char* p = strstr(topic, prefix);
      if (p && g_boundUsernameForResult.length() > 0) {
        p += strlen(prefix);
        const char* end = strchr(p, '/');
        if (end && end > p) {
          String topicUser = String(p).substring(0, (size_t)(end - p));
          if (topicUser == g_boundUsernameForResult) {
            bool correct = (strstr(buf, "\"correct\":true") != nullptr);
            long points = extractJsonLong(buf, "points");
            g_totalScore += points;
            if (points > 0) {
              g_plusXPoints = points;
              g_plusXShowUntil = millis() + 1500;  // show "+X Pkt" for 1.5s
            }
            SAFE_PRINTLN("[Result] " + (correct ? String("Correct") : String("Wrong")) + ", +" + String((long)points) + " Pkt, total: " + String((long)g_totalScore));
          }
        }
      }
      return;
    }
  }

  // Handle controller/status (playerId assigned from web; update bound user + ready)
  if (L >= 7 && strcmp(topic + L - 7, "/status") == 0 && strstr(topic, "controller/") != nullptr) {
    if (len > 0 && len < 256) {
      char buf[256];
      memcpy(buf, payload, len);
      buf[len] = '\0';
      String playerId = extractJsonString(buf, "playerId");
      bool ready = extractJsonBool(buf, "ready");
      bool changed = (playerId != g_boundUsernameForResult || ready != g_boundReady);
      g_boundUsernameForResult = playerId;
      g_boundReady = ready;
      if (changed && playerId.length() > 0) {
        SAFE_PRINTLN("[Status] bound: " + playerId + ", ready=" + String(ready ? "true" : "false"));
      }
    }
    return;
  }

  // Handle RFID lookup reply
  if (L >= 12 && strcmp(topic + L - 11, "/rfid/reply") == 0) {
    g_rfidReplyUsername = "";
    if (len > 0 && len < 256) {
      char buf[256];
      memcpy(buf, payload, len);
      buf[len] = '\0';
      const char* key = "\"username\":\"";
      const char* p = strstr(buf, key);
      if (p) {
        p += strlen(key);
        const char* end = strchr(p, '"');
        if (end && end > p) {
          g_rfidReplyUsername = String(p).substring(0, (size_t)(end - p));
        }
      }
    }
    g_rfidReplyReceived = true;
    return;
  }
  // Handle ping -> pong
  if (L < 5 || strcmp(topic + L - 4, "ping") != 0) return;
  char pongTopic[64];
  strncpy(pongTopic, topic, 63);
  pongTopic[63] = '\0';
  pongTopic[L - 3] = 'o';  // ping -> pong
  g_client.publish(pongTopic, "{}");
}

/** Liefert die MAC-Adresse als String (XX:XX:XX:XX:XX:XX). */
String macAddressString() {
  uint8_t mac[6];
  WiFi.macAddress(mac);

  char buf[18];
  sprintf(buf, "%02X:%02X:%02X:%02X:%02X:%02X",
          mac[0], mac[1], mac[2], mac[3], mac[4], mac[5]);

  return String(buf);
}

/** Verbindet mit dem konfigurierten WLAN. */
bool connectWiFi() {
  if (WiFi.status() == WL_CONNECTED) return true;

  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);

  int retries = 40; // ~20s
  while (WiFi.status() != WL_CONNECTED && retries-- > 0) delay(500);

  if (WiFi.status() == WL_CONNECTED) return true;
  return false;
}

/** Verbindet mit MQTT-Broker und abonniert alle benötigten Topics. */
bool connectMqtt() {
  if (!connectWiFi()) return false;

  g_client.setBufferSize(1024);
  g_client.setServer(MQTT_HOST, MQTT_PORT);
  if (g_client.connected()) return true;

  const String clientId = String("uno-") + macAddressString();

  for (int i = 0; i < 3; i++) {
    if (g_client.connect(clientId.c_str(), MQTT_USER, MQTT_PASS)) {
      g_client.setCallback(onMqttMessage);
      String mac = macAddressString();
      g_client.subscribe((String(MQTT_TOPIC_PREFIX) + "controller/" + mac + "/ping").c_str());
      g_client.subscribe((String(MQTT_TOPIC_PREFIX) + "controller/" + mac + "/status").c_str());
      g_client.subscribe((String(MQTT_TOPIC_PREFIX) + "controller/" + mac + "/rfid/reply").c_str());
      g_client.subscribe((String(MQTT_TOPIC_PREFIX) + "game/state").c_str());
      g_client.subscribe((String(MQTT_TOPIC_PREFIX) + "game/question").c_str());
      g_client.subscribe((String(MQTT_TOPIC_PREFIX) + "game/ended").c_str());
      g_client.subscribe((String(MQTT_TOPIC_PREFIX) + "player/+/result").c_str());
      return true;
    }

    delay(2000);
  }
  return false;
}

/** Stellt sicher, dass WiFi und MQTT verbunden sind. */
bool ensureConnected() {
  return connectMqtt();
}

/** Sendet die MAC-Adresse an das Mac-Topic (für Controller-Registrierung). */
bool publishMacAddress() {
  if (!ensureConnected()) return false;
  const String mac = macAddressString();
  const String payload = String("{\"mac\":\"") + mac + "\"}";
  return g_client.publish(MQTT_TOPIC_MAC, payload.c_str());
}

/** Registriert den Hardware-Controller beim Backend per MQTT. */
bool publishControllerRegister() {
  if (!ensureConnected()) return false;

  const String mac = macAddressString();
  const String topic = String(MQTT_TOPIC_PREFIX) + "controller/" + mac + "/register";
  static const char payload[] = "{\"controllerType\":\"HARDWARE\"}";

  const bool ok = g_client.publish(topic.c_str(), payload);
  return ok;
}

/** Ruft Benutzernamen für RFID-UID per MQTT ab (blockierend, Timeout 5s). */
String rfidLookupUsername(const char* uid) {
  if (!ensureConnected()) return "";
  g_rfidReplyReceived = false;
  g_rfidReplyUsername = "";
  String mac = macAddressString();
  String payload = String("{\"uid\":\"") + uid + "\",\"mac\":\"" + mac + "\"}";
  bool ok = g_client.publish((String(MQTT_TOPIC_PREFIX) + "auth/rfid/lookup").c_str(), payload.c_str());
  if (!ok) return "";
  uint32_t start = millis();
  while (!g_rfidReplyReceived && (millis() - start < 5000)) {
    g_client.loop();
    delay(10);
  }
  return g_rfidReplyUsername;
}

/** Bindet Controller an Spieler und tritt der Lobby bei (HTTP POST). */
bool rfidBindAndJoin(const char* username) {
  if (!connectWiFi()) return false;
  WiFiClient client;
  if (!client.connect(BACKEND_HOST, BACKEND_PORT)) return false;
  String mac = macAddressString();
  String body = String("{\"username\":\"") + username + "\",\"controllerId\":\"" + mac + "\",\"controllerType\":\"HARDWARE\"}";
  client.println("POST /api/players/bind HTTP/1.0");
  client.print("Host: ");
  client.println(BACKEND_HOST);
  client.print("Content-Length: ");
  client.println(body.length());
  client.println("Content-Type: application/json");
  client.println();
  client.print(body);
  uint32_t t = millis();
  while (!client.available() && millis() - t < 3000) delay(10);
  int status = 0;
  while (client.available()) {
    String line = client.readStringUntil('\n');
    if (line.startsWith("HTTP/1")) {
      int sp = line.indexOf(' ');
      if (sp >= 0) status = line.substring(sp + 1, sp + 4).toInt();
    }
    if (line == "\r") break;
  }
  client.stop();
  if (status != 200) return false;
  if (!client.connect(BACKEND_HOST, BACKEND_PORT)) return false;
  body = String("{\"username\":\"") + username + "\"}";
  client.println("POST /api/lobby/join HTTP/1.0");
  client.print("Host: ");
  client.println(BACKEND_HOST);
  client.print("Content-Length: ");
  client.println(body.length());
  client.println("Content-Type: application/json");
  client.println();
  client.print(body);
  t = millis();
  while (!client.available() && millis() - t < 3000) delay(10);
  status = 0;
  while (client.available()) {
    String line = client.readStringUntil('\n');
    if (line.startsWith("HTTP/1")) {
      int sp = line.indexOf(' ');
      if (sp >= 0) status = line.substring(sp + 1, sp + 4).toInt();
    }
    if (line == "\r") break;
  }
  client.stop();
  return (status == 200);
}

/** Sendet Ready/Not-ready-Status des Spielers per MQTT. */
bool publishPlayerReady(const char* username, bool ready) {
  if (!username || !username[0] || !ensureConnected()) return false;
  String topic = String(MQTT_TOPIC_PREFIX) + "player/" + String(username) + "/ready";
  String payload = String("{\"ready\":") + (ready ? "true" : "false") + ",\"action\":\"" + (ready ? "ready" : "not-ready") + "\"}";
  return g_client.publish(topic.c_str(), payload.c_str());
}

/** Liefert den aktuellen Spielzustand (LOBBY, QUESTION, ENDED). */
const char* getGameState() {
  return g_gameState.c_str();
}

/** Liefert die ID der aktuellen Frage. */
long getCurrentQuestionId() {
  return g_currentQuestionId;
}

/** Prüft, ob Antworten für die aktuelle Frage gesendet werden dürfen. */
bool isQuestionReadyToAnswer() {
  if (g_gameState != "QUESTION" || g_currentQuestionId <= 0) return false;
  if (!g_waitingForQuestion) return true;
  // Fallback: if game/question was dropped (buffer too small), allow after 2s
  if (g_questionStateTimestamp > 0 && (millis() - g_questionStateTimestamp > 2000)) {
    g_waitingForQuestion = false;
    SAFE_PRINTLN("[Game] Timeout waiting for question payload, allowing answers");
    return true;
  }
  return false;
}

/** Sendet die Spieler-Antwort (A/B/C/D) per MQTT. */
bool publishPlayerAnswer(const char* username, long questionId, const char* selectedOption) {
  if (!username || !username[0] || questionId <= 0 || !selectedOption || !ensureConnected()) return false;
  String topic = String(MQTT_TOPIC_PREFIX) + "player/" + String(username) + "/answer";
  String payload = String("{\"questionId\":") + String((long)questionId) + ",\"selectedOption\":\"" + String(selectedOption) + "\"}";
  return g_client.publish(topic.c_str(), payload.c_str());
}

/** Setzt den Benutzernamen für die Filterung von player/result. */
void setBoundUsernameForResult(const char* username) {
  g_boundUsernameForResult = username ? String(username) : "";
}

/** Setzt den lokalen Ready-Status. */
void setBoundReady(bool ready) {
  g_boundReady = ready;
}

/** Liefert den gebundenen Benutzernamen. */
String getBoundUsername() {
  return g_boundUsernameForResult;
}

/** Liefert den Ready-Status des gebundenen Spielers. */
bool getBoundReady() {
  return g_boundReady;
}

/** Liefert die kumulierte Punktzahl des Spielers. */
long getTotalScore() {
  return g_totalScore;
}

/** Liefert die Punkte für „+X Pkt“-Anzeige (0 wenn abgelaufen). */
long getPlusXPoints() {
  if (g_plusXPoints > 0 && millis() < g_plusXShowUntil) return g_plusXPoints;
  g_plusXPoints = 0;
  return 0;
}

/** Fordert den Controller-Status vom Backend an. */
bool publishRequestStatus() {
  if (!ensureConnected()) return false;
  const String mac = macAddressString();
  const String topic = String(MQTT_TOPIC_PREFIX) + "controller/" + mac + "/request-status";
  return g_client.publish(topic.c_str(), "{}");
}

static const uint32_t REQUEST_STATUS_INTERVAL_MS = 5000;

/** Muss regelmäßig aufgerufen werden: MQTT-Loop, Reconnect, periodischer Status-Request. */
void processMqtt() {
  if (!g_client.connected()) {
    static uint32_t lastAttempt = 0;
    uint32_t now = millis();
    if (now - lastAttempt >= MQTT_RECONNECT_INTERVAL_MS) {
      lastAttempt = now;
      if (connectMqtt()) publishControllerRegister();
    }
    return;
  }
  g_client.loop();
  // Request controller status periodically (backend replies with playerId when bound from web)
  static uint32_t lastRequestStatus = 0;
  uint32_t now = millis();
  if (now - lastRequestStatus >= REQUEST_STATUS_INTERVAL_MS) {
    lastRequestStatus = now;
    publishRequestStatus();
  }
}

} // namespace net::wifi_mqtt
