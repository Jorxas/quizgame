#pragma once

/**
 * hardware/buttons.h – Tasten A/B/C/D, begin, isPressed, isPressedDebounced
 */
#include <Arduino.h>

namespace hw::buttons {

// Initialize the button pins.
void begin();

// Debounced: returns true once per press, after 50ms stable.
bool isPressedDebounced(uint8_t pin);

} // namespace hw::buttons
