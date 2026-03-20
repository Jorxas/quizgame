#pragma once

#include <Arduino.h>
#include <Adafruit_GFX.h>
#include <Fonts/FreeSans9pt7b.h>
#include "Adafruit_SH1106.hpp"

namespace hw::oled {

// Initialize the OLED.
void begin();

// Display the THM-Logo.
void showThmLogo();

// Show "RFID card not recognized" error.
void showRfidError();

// Show welcome message for RFID login.
void showRfidWelcome(const char* username);

// Show player name, ready status, and score.
void showPlayerStatus(const char* username, bool ready, long score);

// Brief "+X Pkt" overlay (call when points earned).
void showPlusXPoints(long points);

// Direct access to the underlying display data.
Adafruit_SH1106& display();

} // namespace hw::oled
