#pragma once

#include <Arduino.h>
#include <PubSubClient.h>
#include <WiFiS3.h>

namespace net::wifi_mqtt {

// Connects to WiFi and MQTT.
bool ensureConnected();

// Connects to WiFi only.
bool connectWiFi();

// Connects to MQTT only.
bool connectMqtt();

// Publishes the MAC-Address through MQTT.
bool publishMacAddress();

// Returns the MAC-Address as string (e.g. "AA:BB:...").
String macAddressString();

// Call from main loop(): processes MQTT messages and reconnects if needed.
void loop();

} // namespace net::wifi_mqtt
