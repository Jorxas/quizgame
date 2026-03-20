/**
 * Buttons – Tasten A/B/C/D, Debounce, Pullup/Active-Low
 */
#include "hardware/buttons.h"
#include "config.h"

namespace hw::buttons {

static int pinToIndex(uint8_t pin) {
  if (pin == PIN_BTN_GREEN) return 0;
  if (pin == PIN_BTN_RED) return 1;
  if (pin == PIN_BTN_YELLOW) return 2;
  if (pin == PIN_BTN_BLUE) return 3;
  return -1;
}

void begin() {
  // Buttons with Pullup -> aktiv LOW
  if (BUTTON_ACTIVE_LOW) {
    pinMode(PIN_BTN_BLUE, INPUT_PULLUP);
    pinMode(PIN_BTN_YELLOW, INPUT_PULLUP);
    pinMode(PIN_BTN_GREEN, INPUT_PULLUP);
    pinMode(PIN_BTN_RED, INPUT_PULLUP);
  // Buttons without Pullup -> aktiv HIGH
  } else {
    pinMode(PIN_BTN_BLUE, INPUT);
    pinMode(PIN_BTN_YELLOW, INPUT);
    pinMode(PIN_BTN_GREEN, INPUT);
    pinMode(PIN_BTN_RED, INPUT);
  }
}

bool isPressedDebounced(uint8_t pin) {
  int idx = pinToIndex(pin);
  if (idx < 0) return false;
  static uint8_t lastState[4] = {0, 0, 0, 0};
  static uint32_t lastChange[4] = {0, 0, 0, 0};
  static uint8_t fired[4] = {0, 0, 0, 0};
  uint8_t raw = digitalRead(pin);
  uint32_t now = millis();
  if (raw != lastState[idx]) {
    lastChange[idx] = now;
    lastState[idx] = raw;
    if (raw == LOW) fired[idx] = 0;
  }
  if (raw == LOW) return false;
  if ((now - lastChange[idx]) < BUTTON_DEBOUNCE_MS) return false;
  if (fired[idx]) return false;
  fired[idx] = 1;
  return true;
}

} // namespace hw::buttons
