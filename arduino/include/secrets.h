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
static constexpr const char* MQTT_USER     = "test";
static constexpr const char* MQTT_PASS     = "test1234";

// Topics (same prefix as web controller / backend)
static constexpr const char* MQTT_TOPIC_MAC = "test/arduino/mac";
static constexpr const char* MQTT_PREFIX = "group-16/";
