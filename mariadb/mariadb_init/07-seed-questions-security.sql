-- ====================================================
-- GEN1002 Informatik-Projekt - WiSe25/26 - Czekansky
-- 07-seed-questions-security.sql
-- IT-Sicherheit (6 Fragen: 2x EASY, 2x MEDIUM, 2x HARD)
-- ============================================

SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- ============================================
-- CATEGORY 6: IT-SICHERHEIT (ID 6)
-- ============================================

-- EASY Level (2 Fragen)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(31, 6, 'EASY', 'Was ist Phishing?', 'A', 1),
(32, 6, 'EASY', 'Welche ist eine sichere Passwort-Praktik?', 'B', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q31 (Phishing)
(121, 31, 'A', 'Betrug, um an Zugangsdaten über gefälschte Nachrichten/Seiten zu kommen'),
(122, 31, 'B', 'Ein legales Hacking-Zertifikat'),
(123, 31, 'C', 'Ein Kompressions-Algorithmus'),
(124, 31, 'D', 'Ein Firewall-Rule-Set'),
-- Q32 (Passwort)
(125, 32, 'A', 'Dasselbe Passwort überall nutzen'),
(126, 32, 'B', 'Ein starkes, eindeutiges Passwort mit Groß-, Kleinbuchstaben, Zahlen, Sonderzeichen'),
(127, 32, 'C', 'Ein Passwort mit Geburtsdatum aufschreiben'),
(128, 32, 'D', 'Passwörter in Browser speichern');

-- MEDIUM Level (2 Fragen)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(33, 6, 'MEDIUM', 'Wozu dient ein Salt beim Passwort-Hashing?', 'D', 1),
(34, 6, 'MEDIUM', 'Was ist eine Brute-Force-Attacke?', 'C', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q33 (Salt)
(129, 33, 'A', 'Ersetzt das Passwort durch Klartext'),
(130, 33, 'B', 'Verkürzt Hashes für schnellere Logins'),
(131, 33, 'C', 'Entfernt Sonderzeichen aus Passwörtern'),
(132, 33, 'D', 'Verhindert gleiche Hashes bei gleichen Passwörtern (Rainbow Tables)'),
-- Q34 (Brute-Force)
(133, 34, 'A', 'Ein Angriff auf MQTT über WebSockets'),
(134, 34, 'B', 'Das Manipulieren von DNS-Einträgen'),
(135, 34, 'C', 'Das systematische Durchprobieren aller möglichen Passwörter/Schlüssel'),
(136, 34, 'D', 'Ein Angriff durch defekte RAM-Bausteine');

-- HARD Level (2 Fragen)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(35, 6, 'HARD', 'Was ist SQL-Injection?', 'B', 1),
(36, 6, 'HARD', 'Welcher Sicherheits-Standard gilt für Webseiten mit sensiblen Daten?', 'A', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q35 (SQL-Injection)
(137, 35, 'A', 'Ein Angriff auf MQTT durch fehlende Authentifizierung'),
(138, 35, 'B', 'Einschleusen von SQL-Code über Benutzereingaben, um Queries zu manipulieren'),
(139, 35, 'C', 'Ein Angriff durch defekte Datenbankzertifikate'),
(140, 35, 'D', 'Ein Angriff auf Bluetooth-Pairing'),
-- Q36 (HTTPS)
(141, 36, 'A', 'HTTPS mit gültigem TLS/SSL-Zertifikat und Verschlüsselung'),
(142, 36, 'B', 'HTTP mit starken Passwörtern'),
(143, 36, 'C', 'FTP mit 2FA (Two-Factor Authentication)'),
(144, 36, 'D', 'TELNET mit Firewall-Protection');

-- Verify insertion
SELECT COUNT(*) as security_questions FROM questions WHERE category_id = 6;
SELECT difficulty, COUNT(*) as count FROM questions WHERE category_id = 6 GROUP BY difficulty;
