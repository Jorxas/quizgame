/**
 * RFID – MFRC522-Leser, Karten-UID auslesen, Login via RFID
 */
#include "hardware/rfid.h"
#include "config.h"

namespace hw::rfid {

static MFRC522 g_rfid(PIN_RFID_CS, PIN_RFID_RST);
static bool g_readerDetected = false;
static uint32_t g_lastDetectCheckMs = 0;
static uint32_t g_lastReadMs = 0;
static String g_lastUid;
static bool g_newScan = false;

/** Initialisiert SPI und MFRC522-RFID-Leser. */
void begin() {

  SPI.begin();

  pinMode(PIN_RFID_CS, OUTPUT);
  g_rfid.PCD_Init();

  g_readerDetected = detectReaderOnce();
}

/** Muss regelmäßig aufgerufen werden: prüft Karte, setzt g_newScan bei neuem UID. */
void service() {
  const uint32_t now = millis();

  // Check if cooldown time passed, return otherwise.
  if (!(now - g_lastDetectCheckMs >= RFID_DETECT_INTERVAL_MS)) return;

  const bool detectedNow = detectReaderOnce();
  if (detectedNow) g_lastDetectCheckMs = now;

  if (!g_readerDetected && detectedNow) {
    g_readerDetected = true;
    g_rfid.PCD_Init();
  }
  if (g_readerDetected && !detectedNow) g_readerDetected = false;

  // If reader not available, return.
  if (!g_readerDetected) return;

  // If no card available, return.
  if (!(g_rfid.PICC_IsNewCardPresent() && g_rfid.PICC_ReadCardSerial())) return;

  g_lastUid = uidToString(g_rfid.uid);
  SAFE_PRINTLN("RFID tag UID: " + g_lastUid);
  g_lastReadMs = now;
  g_newScan = true;

  g_rfid.PICC_HaltA();
  g_rfid.PCD_StopCrypto1();
}

/** Liefert true, wenn der RFID-Leser erkannt wurde. */
bool isReaderDetected() {
  return g_readerDetected;
}

/** Liefert die letzte gelesene Karten-UID. */
const String& lastUid() {
  return g_lastUid;
}

/** Liefert true, wenn ein neuer RFID-Scan noch nicht verarbeitet wurde. */
bool hasNewScan() {
  return g_newScan;
}

/** Markiert den aktuellen Scan als verarbeitet. */
void consumeNewScan() {
  g_newScan = false;
}

/** Prüft einmalig, ob der RFID-Leser antwortet. */
bool detectReaderOnce() {
  byte v = g_rfid.PCD_ReadRegister(MFRC522::VersionReg);
  // If 0x00 or 0xFF the read probably failed.
  return !(v == 0x00 || v == 0xFF);
}

/** Konvertiert UID-Byte-Array zu hexadezimalem String (Großbuchstaben). */
String uidToString(const MFRC522::Uid &uid) {
  String out;
  out.reserve(uid.size * 2);
  for (byte i = 0; i < uid.size; i++) {
    if (uid.uidByte[i] < 0x10) out += "0";
    out += String(uid.uidByte[i], HEX);
  }
  out.toUpperCase();
  return out;
}

} // namespace hw::rfid
