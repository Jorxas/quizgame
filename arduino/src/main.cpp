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
    Serial.println("YELLOW button");
    hw::neopixel::flash(hw::neopixel::strip().Color(120, 120, 0), 20);

    // Send MAC per MQTT as debug print
    (void)net::wifi_mqtt::publishMacAddress();
    return;
  }

  if (hw::buttons::isPressed(PIN_BTN_RED)) {
    Serial.println("RED button");
    hw::neopixel::flash(hw::neopixel::strip().Color(120, 0, 0), 20);
    return;
  }

  if (hw::buttons::isPressed(PIN_BTN_BLUE)) {
    Serial.println("BLUE button");
    hw::neopixel::flash(hw::neopixel::strip().Color(0, 0, 120), 20);
    return;
  }

  if (hw::buttons::isPressed(PIN_BTN_GREEN)) {
    Serial.println("GREEN button");
    hw::neopixel::flash(hw::neopixel::strip().Color(0, 120, 0), 20);
    return;
  }

  // If no button is pressed, turn of the LEDs.
  hw::neopixel::off();
}

void setup() {
  Serial.begin(9600);
  while (!Serial) { delay(10); }

  hw::buttons::begin();
  hw::neopixel::begin();

  hw::oled::begin();
  hw::oled::showThmLogo();

  hw::rfid::begin();

  Serial.println("Setup complete.");

  // Initialize the WiFi and MQTT connections.
  if (net::wifi_mqtt::ensureConnected()) {
    Serial.println("MQTT test connection OK (startup).");
  } else {
    Serial.println("MQTT test connection FAILED (startup).");
  }
}

void loop() {
  // Keep polling for new RFID cards.
  hw::rfid::service();

  // Keep reacting to button presses.
  handleButtons();
}
