#include "net/wifi_mqtt.h"
#include "secrets.h"

namespace net::wifi_mqtt {

static WiFiClient g_netClient;
static PubSubClient g_client(g_netClient);

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

  Serial.print("Connecting to WiFi: ");
  Serial.println(WIFI_SSID);

  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);

  int retries = 40; // ~20s
  while (WiFi.status() != WL_CONNECTED && retries-- > 0) {
    delay(500);
    Serial.print('.');
  }
  Serial.println();

  if (WiFi.status() == WL_CONNECTED) {
    Serial.print("WiFi connected, IP: ");
    Serial.println(WiFi.localIP());
    return true;
  }

  Serial.println("WiFi connection failed!");
  return false;
}

bool connectMqtt() {
  if (!connectWiFi()) return false;

  g_client.setServer(MQTT_HOST, MQTT_PORT);
  if (g_client.connected()) return true;

  Serial.print("Connecting to MQTT broker ");
  Serial.print(MQTT_HOST);
  Serial.print(':');
  Serial.println(MQTT_PORT);

  const String clientId = String("uno-") + macAddressString();

  for (int i = 0; i < 3; i++) {
    if (g_client.connect(clientId.c_str(), MQTT_USER, MQTT_PASS)) {
      Serial.println("MQTT connected!");
      return true;
    }

    Serial.print("MQTT connection failed, rc=");
    Serial.print(g_client.state());
    Serial.println(" -> retrying...");
    delay(2000);
  }

  Serial.println("MQTT connection could not be established.");
  return false;
}

bool ensureConnected() {
  return connectMqtt();
}

bool publishMacAddress() {
  if (!ensureConnected()) {
    Serial.println("Cannot publish MAC address: MQTT not connected.");
    return false;
  }

  const String mac = macAddressString();
  const String payload = String("{\"mac\":\"") + mac + "\"}";

  Serial.print("Publishing MAC via MQTT: ");
  Serial.println(payload);

  const bool ok = g_client.publish(MQTT_TOPIC_MAC, payload.c_str());
  if (ok) {
    Serial.println("MAC address published successfully.");
  } else {
    Serial.println("MQTT publish() failed!");
  }
  return ok;
}

} // namespace net::wifi_mqtt
