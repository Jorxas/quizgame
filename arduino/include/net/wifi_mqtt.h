#pragma once

/**
 * net/wifi_mqtt.h – WiFi/MQTT: ensureConnected, publishPlayerReady, publishPlayerAnswer, rfidLookupUsername
 */
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

// Registers the controller with the backend (group-XX/controller/{mac}/register).
bool publishControllerRegister();

// Call every loop iteration to process MQTT (handle ping, reconnect).
void processMqtt();

// Returns the MAC-Address as string (e.g. "AA:BB:...").
String macAddressString();

// RFID login via MQTT: publish to auth/rfid/lookup, wait for controller/{mac}/rfid/reply.
// Backend does lookup + bind + join. Returns username or empty if not found.
String rfidLookupUsername(const char* uid);

// Legacy: bind+join via HTTP. Not used when MQTT RFID lookup succeeds.
bool rfidBindAndJoin(const char* username);

// Publish ready/not-ready to player/{username}/ready. Call when bound.
bool publishPlayerReady(const char* username, bool ready);

// Game state from MQTT (updated when receiving game/state and game/question).
const char* getGameState();
long getCurrentQuestionId();
// True only when state is QUESTION and we have received the question payload (so answer buttons are valid).
bool isQuestionReadyToAnswer();

// Publish answer to player/{username}/answer. selectedOption = "A"|"B"|"C"|"D". Call when in QUESTION state.
bool publishPlayerAnswer(const char* username, long questionId, const char* selectedOption);

// Set bound username so we only log player/result for this user. Call after RFID login.
void setBoundUsernameForResult(const char* username);

// Set ready state (call when publishing ready from buttons).
void setBoundReady(bool ready);

// Get bound username (set by RFID reply or controller/status from web). Returns empty string if none.
String getBoundUsername();

// Get ready state (from controller/status or set when publishing).
bool getBoundReady();

// Get total score (accumulated from player/result, reset on LOBBY/ENDED).
long getTotalScore();

// Get points for "+X Pkt" flash (0 = not showing). Ages out after 1.5s.
long getPlusXPoints();

} // namespace net::wifi_mqtt
