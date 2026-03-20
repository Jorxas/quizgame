/**
 * OLED – SH1106-Display: THM-Logo, RFID-Willkommen/Fehler, Spieler-Status, Punkte
 */
#include "hardware/oled.h"
#include "config.h"
#include "thm_logo.h"

namespace hw::oled {

static Adafruit_SH1106 g_display(OLED_RESET_PIN);

void begin() {
  g_display.begin(0x02, OLED_I2C_ADDRESS, OLED_RESET_PIN);
  delay(OLED_I2C_DELAY_MS);
  g_display.clearDisplay();
  delay(OLED_I2C_DELAY_MS);
  g_display.setRotation(0);
  g_display.display();
  delay(OLED_I2C_DELAY_MS);

  g_display.setTextColor(WHITE);
  g_display.setFont(&FreeSans9pt7b);
}

void showThmLogo() {
  g_display.clearDisplay();
  delay(OLED_I2C_DELAY_MS);

  delay(1000);

  g_display.drawBitmap(
      (g_display.width() - THM_LOGO_WIDTH) / 2,
      (g_display.height() - THM_LOGO_HEIGHT) / 2,
      THM_LOGO_BITMAP, THM_LOGO_WIDTH, THM_LOGO_HEIGHT, 1);

  g_display.setCursor(0, 60);
  g_display.display();
  delay(OLED_I2C_DELAY_MS);
}

void showRfidError() {
  g_display.clearDisplay();
  delay(OLED_I2C_DELAY_MS);
  g_display.setCursor(0, 20);
  g_display.print("RFID card");
  g_display.setCursor(0, 40);
  g_display.print("not recognized");
  delay(OLED_I2C_DELAY_MS);
  g_display.display();
  delay(OLED_I2C_DELAY_MS);
}

void showRfidWelcome(const char* username) {
  g_display.clearDisplay();
  delay(OLED_I2C_DELAY_MS);
  g_display.setCursor(0, 20);
  g_display.print("Welcome");
  g_display.setCursor(0, 40);
  g_display.print(username ? username : "");
  delay(OLED_I2C_DELAY_MS);
  g_display.display();
  delay(OLED_I2C_DELAY_MS);
}

void showPlayerStatus(const char* username, bool ready, long score) {
  g_display.clearDisplay();
  delay(OLED_I2C_DELAY_MS);
  g_display.setCursor(0, 14);
  g_display.print(username ? username : "");
  g_display.setCursor(0, 32);
  g_display.print(ready ? "Ready" : "Not ready");
  g_display.setCursor(0, 50);
  g_display.print("Score: ");
  g_display.print(score);
  delay(OLED_I2C_DELAY_MS);
  g_display.display();
  delay(OLED_I2C_DELAY_MS);
}

void showPlusXPoints(long points) {
  g_display.clearDisplay();
  delay(OLED_I2C_DELAY_MS);
  g_display.setCursor(20, 28);
  g_display.print("+");
  g_display.print(points);
  g_display.print(" Pkt");
  delay(OLED_I2C_DELAY_MS);
  g_display.display();
  delay(OLED_I2C_DELAY_MS);
}

Adafruit_SH1106& display() {
  return g_display;
}

} // namespace hw::oled
