#pragma once

/**
 * config.h – Pin-Belegung, Hardware-Parameter, Debounce, OLED
 */
#include <Arduino.h>

// =======================
// Pin mapping
// =======================

// NeoPixel
static constexpr uint8_t PIN_NEOPIXEL = D2;
static constexpr uint16_t NEOPIXEL_LED_COUNT = 4;

// Buttons (physical pins)
static constexpr uint8_t PIN_BTN_GREEN  = D4;
static constexpr uint8_t PIN_BTN_RED    = D5;
static constexpr uint8_t PIN_BTN_YELLOW = D6;
static constexpr uint8_t PIN_BTN_BLUE   = D7;

// Antwort-Tasten: A=Blau, B=Grün, C=Gelb, D=Rot
static constexpr uint8_t PIN_BTN_ANSWER_A = D7;  // BLUE
static constexpr uint8_t PIN_BTN_ANSWER_B = D4;  // GREEN
static constexpr uint8_t PIN_BTN_ANSWER_C = D6;  // YELLOW
static constexpr uint8_t PIN_BTN_ANSWER_D = D5;  // RED

// OLED (I2C)
static constexpr uint8_t PIN_OLED_SDA = A4;
static constexpr uint8_t PIN_OLED_SCL = A5;

// RFID (RC522, SPI)
// SCK=D13, SS/SDA=D10, MOSI=D11, MISO=D12, RST=D9
static constexpr uint8_t PIN_RFID_RST  = D9;
static constexpr uint8_t PIN_RFID_CS   = D10;
static constexpr uint8_t PIN_RFID_MOSI = D11;
static constexpr uint8_t PIN_RFID_MISO = D12;
static constexpr uint8_t PIN_RFID_SCK  = D13;

// =======================
// Hardware behavior
// =======================

static constexpr bool BUTTON_ACTIVE_LOW = true;      // Button against GND -> INPUT_PULLUP
static constexpr uint32_t BUTTON_DEBOUNCE_MS = 50;
static constexpr uint32_t MQTT_RECONNECT_INTERVAL_MS = 5000;
static constexpr uint32_t RFID_READ_INTERVAL_MS = 5000;
static constexpr uint32_t RFID_DETECT_INTERVAL_MS = 500; // Check every 0.5s

// =======================
// OLED config
// =======================

static constexpr uint8_t OLED_I2C_ADDRESS = 0x3C;
static constexpr int OLED_RESET_PIN = -1;

// Screen size (SH1106/SSD1306 compatible)
static constexpr uint16_t SCREEN_WIDTH  = 128;
static constexpr uint16_t SCREEN_HEIGHT = 64;

// =======================
// MICRO-Lab safety (UART/OLED)
// =======================
// Delay after Serial.println to avoid UART deadlock (MCU unflashable).
// Delay between OLED I2C ops to prevent display driver hang.
static constexpr uint16_t SERIAL_FLUSH_DELAY_MS = 10;
static constexpr uint8_t  OLED_I2C_DELAY_MS = 3;

#define SAFE_PRINTLN(x) do { Serial.println(x); delay(SERIAL_FLUSH_DELAY_MS); } while(0)
