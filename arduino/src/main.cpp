#include <Arduino.h>

#include "config.h"

#include "hardware/buttons.h"
#include "hardware/neopixel.h"
#include "hardware/oled.h"
#include "hardware/rfid.h"
#include "net/wifi_mqtt.h"

// Main program: Initialises hardware and reacts to button presses.

static void handleButtons() {
  if (hw::buttons::isPressed(PIN_BTN_YELLOW)) {
    hw::neopixel::flash(hw::neopixel::strip().Color(120, 120, 0), 20);
    (void)net::wifi_mqtt::publishMacAddress();
    return;
  }

  if (hw::buttons::isPressed(PIN_BTN_RED)) {
    hw::neopixel::flash(hw::neopixel::strip().Color(120, 0, 0), 20);
    return;
  }

  if (hw::buttons::isPressed(PIN_BTN_BLUE)) {
    hw::neopixel::flash(hw::neopixel::strip().Color(0, 0, 120), 20);
    return;
  }

  if (hw::buttons::isPressed(PIN_BTN_GREEN)) {
    hw::neopixel::flash(hw::neopixel::strip().Color(0, 120, 0), 20);
    return;
  }

  // If no button is pressed, turn of the LEDs.
  hw::neopixel::off();
}

void setup() {
  Serial.begin(9600);
  for (uint32_t t = millis(); !Serial && (millis() - t < 2000);) delay(10);

  hw::buttons::begin();
  hw::neopixel::begin();

  hw::oled::begin();
  hw::oled::showThmLogo();

  hw::rfid::begin();

  if (net::wifi_mqtt::ensureConnected()) {
    SAFE_PRINTLN("Ready");
  } else {
    SAFE_PRINTLN("MQTT fail");
  }
}

void loop() {
  // Process MQTT (controller status, game events -> OLED display).
  net::wifi_mqtt::loop();

  // Keep polling for new RFID cards.
  hw::rfid::service();

  // Keep reacting to button presses.
  handleButtons();
}
