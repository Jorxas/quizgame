/**
 * Hardware-Controller – Hauptprogramm
 * Buttons (A–D, Ready/Not-Ready), RFID-Login, MQTT-Kommunikation, OLED-Anzeige
 */
#include <Arduino.h>
#include <string.h>

#include "config.h"

#include "hardware/buttons.h"
#include "hardware/neopixel.h"
#include "hardware/oled.h"
#include "hardware/rfid.h"
#include "net/wifi_mqtt.h"

// Ready state (synced from button presses)
static bool g_ready = false;
// Last question we sent an answer for (avoid double-send per question)
static long g_lastAnsweredQuestionId = 0;

/** Liest Tasten, sendet Antworten (A-D) oder Ready/Not-ready per MQTT. */
static void handleButtons() {
  String boundUsername = net::wifi_mqtt::getBoundUsername();
  long currentQ = net::wifi_mqtt::getCurrentQuestionId();
  const bool inQuestion = net::wifi_mqtt::isQuestionReadyToAnswer() && currentQ != g_lastAnsweredQuestionId;

  // Read each PHYSICAL button ONCE per loop (A=Blue, B=Green, C=Yellow, D=Red)
  bool btnBlue = hw::buttons::isPressedDebounced(PIN_BTN_BLUE);    // ANSWER_A
  bool btnGreen = hw::buttons::isPressedDebounced(PIN_BTN_GREEN);  // ANSWER_B
  bool btnYellow = hw::buttons::isPressedDebounced(PIN_BTN_YELLOW);// ANSWER_C
  bool btnRed = hw::buttons::isPressedDebounced(PIN_BTN_RED);      // ANSWER_D

  // In QUESTION-Phase: Blau=A, Grün=B, Gelb=C, Rot=D (eine Antwort pro Frage)
  if (boundUsername.length() > 0 && inQuestion) {
    if (btnBlue) {
      if (net::wifi_mqtt::publishPlayerAnswer(boundUsername.c_str(), currentQ, "A")) {
        g_lastAnsweredQuestionId = currentQ;
        SAFE_PRINTLN("[Answer] " + boundUsername + " -> A (q=" + String((long)currentQ) + ")");
        hw::neopixel::flash(hw::neopixel::strip().Color(0, 0, 120), 80);
      } else SAFE_PRINTLN("[Answer] MQTT publish failed");
      return;
    }
    if (btnGreen) {
      if (net::wifi_mqtt::publishPlayerAnswer(boundUsername.c_str(), currentQ, "B")) {
        g_lastAnsweredQuestionId = currentQ;
        SAFE_PRINTLN("[Answer] " + boundUsername + " -> B (q=" + String((long)currentQ) + ")");
        hw::neopixel::flash(hw::neopixel::strip().Color(0, 120, 0), 80);
      } else SAFE_PRINTLN("[Answer] MQTT publish failed");
      return;
    }
    if (btnYellow) {
      if (net::wifi_mqtt::publishPlayerAnswer(boundUsername.c_str(), currentQ, "C")) {
        g_lastAnsweredQuestionId = currentQ;
        SAFE_PRINTLN("[Answer] " + boundUsername + " -> C (q=" + String((long)currentQ) + ")");
        hw::neopixel::flash(hw::neopixel::strip().Color(120, 120, 0), 80);
      } else SAFE_PRINTLN("[Answer] MQTT publish failed");
      return;
    }
    if (btnRed) {
      if (net::wifi_mqtt::publishPlayerAnswer(boundUsername.c_str(), currentQ, "D")) {
        g_lastAnsweredQuestionId = currentQ;
        SAFE_PRINTLN("[Answer] " + boundUsername + " -> D (q=" + String((long)currentQ) + ")");
        hw::neopixel::flash(hw::neopixel::strip().Color(120, 0, 0), 80);
      } else SAFE_PRINTLN("[Answer] MQTT publish failed");
      return;
    }
  }

  // When bound and not in QUESTION: GREEN = Ready, RED = Not ready
  if (boundUsername.length() > 0) {
    if (btnGreen) {
      g_ready = true;
      SAFE_PRINTLN("[Ready] GREEN -> Ready sent for " + boundUsername);
      hw::neopixel::flash(hw::neopixel::strip().Color(0, 120, 0), 50);
      if (net::wifi_mqtt::publishPlayerReady(boundUsername.c_str(), true)) {
        net::wifi_mqtt::setBoundReady(true);
        hw::oled::showPlayerStatus(boundUsername.c_str(), true, net::wifi_mqtt::getTotalScore());
      } else {
        SAFE_PRINTLN("[Ready] MQTT publish failed");
      }
      return;
    }
    if (btnRed) {
      g_ready = false;
      SAFE_PRINTLN("[Ready] RED -> Not ready sent for " + boundUsername);
      hw::neopixel::flash(hw::neopixel::strip().Color(120, 0, 0), 50);
      if (net::wifi_mqtt::publishPlayerReady(boundUsername.c_str(), false)) {
        hw::oled::showPlayerStatus(boundUsername.c_str(), false, net::wifi_mqtt::getTotalScore());
      } else {
        SAFE_PRINTLN("[Ready] MQTT publish failed");
      }
      return;
    }
  }

  if (btnYellow) {
    hw::neopixel::flash(hw::neopixel::strip().Color(120, 120, 0), 20);
    (void)net::wifi_mqtt::publishMacAddress();
    return;
  }
  if (btnRed) {
    if (boundUsername.length() == 0) {
      hw::neopixel::flash(hw::neopixel::strip().Color(120, 0, 0), 20);
    }
    return;
  }
  if (btnBlue) {
    hw::neopixel::flash(hw::neopixel::strip().Color(0, 0, 120), 20);
    return;
  }
  if (btnGreen) {
    hw::neopixel::flash(hw::neopixel::strip().Color(0, 120, 0), 20);
    return;
  }

  // If no button is pressed, turn of the LEDs.
  hw::neopixel::off();
}

/** Arduino-Init: Serial, Buttons, NeoPixel, OLED, RFID, MQTT-Register. */
void setup() {
  Serial.begin(9600);
  for (uint32_t t = millis(); !Serial && (millis() - t < 2000);) delay(10);

  hw::buttons::begin();
  hw::neopixel::begin();

  hw::oled::begin();
  SAFE_PRINTLN("[Start] OLED init done");
  hw::oled::showThmLogo();

  hw::rfid::begin();

  if (net::wifi_mqtt::ensureConnected()) {
    net::wifi_mqtt::publishControllerRegister();
  }
}

/** Verarbeitet neuen RFID-Scan, ruft Lookup auf, zeigt Willkommen oder Fehler. */
static void handleRfidScan() {
  if (!hw::rfid::hasNewScan()) return;
  const String uid = hw::rfid::lastUid();
  hw::rfid::consumeNewScan();
  SAFE_PRINTLN("RFID lookup: " + uid + " ...");
  String username = net::wifi_mqtt::rfidLookupUsername(uid.c_str());
  if (username.length() > 0) {
    SAFE_PRINTLN("  -> found: " + username);
    g_ready = false;
    net::wifi_mqtt::setBoundUsernameForResult(username.c_str());
    hw::oled::showRfidWelcome(username.c_str());
    delay(800);
    net::wifi_mqtt::setBoundReady(false);
    hw::oled::showPlayerStatus(username.c_str(), false, 0);
  } else {
    SAFE_PRINTLN("  -> not found");
    hw::oled::showRfidError();
  }
}

/** Aktualisiert OLED-Anzeige (Spielername, Status, Score oder +X Pkt). */
static void refreshOled() {
  String bound = net::wifi_mqtt::getBoundUsername();
  if (bound.length() == 0) return;
  long plusX = net::wifi_mqtt::getPlusXPoints();
  if (plusX > 0) {
    hw::oled::showPlusXPoints(plusX);
    return;
  }
  hw::oled::showPlayerStatus(bound.c_str(), net::wifi_mqtt::getBoundReady(), net::wifi_mqtt::getTotalScore());
}

/** Hauptschleife: MQTT, RFID, Buttons, OLED-Refresh. */
void loop() {
  net::wifi_mqtt::processMqtt();
  hw::rfid::service();
  handleRfidScan();
  handleButtons();
  // Refresh OLED when bound (shows name, status, score; or +X Pkt when points earned)
  static uint32_t lastOledRefresh = 0;
  if (millis() - lastOledRefresh >= 300) {
    lastOledRefresh = millis();
    refreshOled();
  }
}
