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

// Direct access to the underlying display data.
Adafruit_SH1106& display();

// Display controller info (same as web controller's player-info).
// controllerId: e.g. MAC "AA:BB:CC:DD:EE:FF"
// playerId: username or empty = "Nicht verbunden"
// ready: true = Ready, false = Not ready
// score: current score (e.g. 0)
void showControllerInfo(const char* controllerId, const char* playerId, bool ready, float score);

// Show "Waehle deine Antwort!" (during QUESTION).
void showAnswerPrompt();

// Show result: correct + points, or wrong + correctOption.
void showResult(bool correct, float points, const char* correctOption);

// Show "Auswertung laeuft..." (during EVALUATION).
void showEvaluation();

// Show "Spiel beendet! Score: X" (ENDED).
void showEnded(float score);

} // namespace hw::oled
