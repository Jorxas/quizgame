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

} // namespace net::wifi_mqtt
