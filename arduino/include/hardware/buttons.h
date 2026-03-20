#pragma once

#include <Arduino.h>

namespace hw::buttons {

// Initialize the button pins.
void begin();

// Returns true if button is pressed (raw).
bool isPressed(uint8_t pin);

// Debounced: returns true once per press, after 50ms stable.
bool isPressedDebounced(uint8_t pin);

} // namespace hw::buttons
