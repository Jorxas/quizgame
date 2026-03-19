#include "hardware/oled.h"
#include "config.h"
#include "thm_logo.h"

namespace hw::oled {

static Adafruit_SH1106 g_display(OLED_RESET_PIN);

// Line height for 9pt font
static constexpr int LINE_H = 16;

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

Adafruit_SH1106& display() {
  return g_display;
}

static void truncatePrint(const char* s, int maxLen) {
  if (!s) return;
  for (int i = 0; i < maxLen && s[i]; i++) g_display.print(s[i]);
  if (s[maxLen]) g_display.print("..");
}

void showControllerInfo(const char* controllerId, const char* playerId, bool ready, float score) {
  // Compact mode so all 4 lines are always visible on 128x64
  g_display.setFont(NULL);
  g_display.setTextSize(1);
  g_display.clearDisplay();
  delay(OLED_I2C_DELAY_MS);
  int y = 8;

  // Keep score on first line so it's always visible.
  g_display.setCursor(0, y);
  g_display.print("Score: ");
  g_display.print(score, 1);
  y += 14;

  g_display.setCursor(0, y);
  g_display.print("User: ");
  truncatePrint(playerId && playerId[0] ? playerId : "Nicht verb.", 14);
  y += 14;

  g_display.setCursor(0, y);
  g_display.print("ID: ");
  truncatePrint(controllerId ? controllerId : "-", 14);
  y += 14;

  g_display.setCursor(0, y);
  g_display.print(ready ? "Ready" : "Not ready");

  delay(OLED_I2C_DELAY_MS);
  g_display.display();
  delay(OLED_I2C_DELAY_MS);

  // Restore project default font for other OLED screens.
  g_display.setFont(&FreeSans9pt7b);
}

void showAnswerPrompt() {
  g_display.clearDisplay();
  delay(OLED_I2C_DELAY_MS);
  g_display.setCursor(0, 24);
  g_display.print("Waehle deine");
  g_display.setCursor(0, 42);
  g_display.print("Antwort!");
  delay(OLED_I2C_DELAY_MS);
  g_display.display();
  delay(OLED_I2C_DELAY_MS);
}

void showResult(bool correct, float points, const char* correctOption) {
  g_display.clearDisplay();
  delay(OLED_I2C_DELAY_MS);
  g_display.setCursor(0, 16);
  if (correct) {
    g_display.print("Richtig!");
    g_display.setCursor(0, 36);
    g_display.print("+");
    g_display.print(points, 1);
    g_display.print(" Pkt");
  } else {
    g_display.print("Falsch!");
    g_display.setCursor(0, 36);
    g_display.print("Richtig: ");
    truncatePrint(correctOption ? correctOption : "?", 4);
  }
  delay(OLED_I2C_DELAY_MS);
  g_display.display();
  delay(OLED_I2C_DELAY_MS);
}

void showEvaluation() {
  g_display.clearDisplay();
  delay(OLED_I2C_DELAY_MS);
  g_display.setCursor(0, 24);
  g_display.print("Auswertung");
  g_display.setCursor(0, 42);
  g_display.print("laeuft...");
  delay(OLED_I2C_DELAY_MS);
  g_display.display();
  delay(OLED_I2C_DELAY_MS);
}

void showEnded(float score) {
  g_display.clearDisplay();
  delay(OLED_I2C_DELAY_MS);
  g_display.setCursor(0, 16);
  g_display.print("Spiel beendet!");
  g_display.setCursor(0, 36);
  g_display.print("Score: ");
  g_display.print(score, 1);
  delay(OLED_I2C_DELAY_MS);
  g_display.display();
  delay(OLED_I2C_DELAY_MS);
}

} // namespace hw::oled
