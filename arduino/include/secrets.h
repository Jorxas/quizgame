#pragma once

// =======================
// WiFi + MQTT credentials
// =======================
// ATTENTION: Credentials should NOT be published in Git-Repositories.
// For ths module the are necessary, but should not be published elsewhere.
// ACHTUNG: Zugangsdaten gehören normalerweise NICHT ins Repository.
// Für den Kurs sind sie hier hinterlegt, sollten aber nicht an dritte weitergegeben werden.

static constexpr const char* WIFI_SSID     = "MQTT-Broker";
static constexpr const char* WIFI_PASSWORD = "Cannej-poscys-cyfqy9";

static constexpr const char* MQTT_HOST     = "iti-mqtt.mni.thm.de";
static constexpr uint16_t    MQTT_PORT     = 1883;
static constexpr const char* MQTT_USER     = "group-16";
static constexpr const char* MQTT_PASS     = "r%^e{[FxW8Lp";

// Topics (prefix must match backend MQTT_MESSAGE_PREFIX)
static constexpr const char* MQTT_TOPIC_PREFIX = "group-16/";
static constexpr const char* MQTT_TOPIC_MAC = "test/arduino/mac";

// Backend for RFID login (IP of machine running docker, same network as Arduino)
static constexpr const char* BACKEND_HOST = "192.168.1.1";
static constexpr uint16_t BACKEND_PORT = 8080;
