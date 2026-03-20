/**
 * NeoPixel – LED-Strip, Farben, Flash-Feedback bei Tastendruck
 */
#include "hardware/neopixel.h"
#include "config.h"

namespace hw::neopixel {

static Adafruit_NeoPixel g_strip(NEOPIXEL_LED_COUNT, PIN_NEOPIXEL, NEO_RGB + NEO_KHZ800);

/** Initialisiert den NeoPixel-Strip und setzt Helligkeit. */
void begin() {
  g_strip.begin();
  g_strip.setBrightness(200);
  g_strip.show();
}

/** Setzt alle LEDs auf die angegebene Farbe. */
void setAll(uint32_t color) {
  for (uint16_t i = 0; i < g_strip.numPixels(); i++) {
    g_strip.setPixelColor(i, color);
  }
  g_strip.show();
}

/** Schaltet alle LEDs aus. */
void off() {
  setAll(g_strip.Color(0, 0, 0));
}

/** Zeigt kurzen Farbblitz (z.B. bei Tastendruck). */
void flash(uint32_t color, uint16_t waitMs) {
  setAll(color);
  delay(waitMs);
}

/** Liefert die NeoPixel-Strip-Instanz. */
Adafruit_NeoPixel& strip() {
  return g_strip;
}

} // namespace hw::neopixel
