-- ====================================================
-- GEN1002 Informatik-Projekt - WiSe25/26 - Czekansky
-- 10-seed-questions-embedded.sql
-- Embedded Systems (6 Fragen: 2x EASY, 2x MEDIUM, 2x HARD)
-- ============================================

SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- ============================================
-- CATEGORY 9: EMBEDDED SYSTEMS (ID 9)
-- ============================================

-- EASY Level (2 Fragen)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(49, 9, 'EASY', 'Wofür steht GPIO?', 'D', 1),
(50, 9, 'EASY', 'Welche Komponente wird verwendet, um digitale Signale zu erzeugen oder zu lesen?', 'C', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q49 (GPIO)
(193, 49, 'A', 'General Purpose Graph Output'),
(194, 49, 'B', 'Global Peripheral I/O'),
(195, 49, 'C', 'Generic Processing Gate Output'),
(196, 49, 'D', 'General Purpose Input/Output'),
-- Q50 (GPIO Pin)
(197, 50, 'A', 'Ein Kondensator'),
(198, 50, 'B', 'Ein Widerstand'),
(199, 50, 'C', 'Ein GPIO-Pin'),
(200, 50, 'D', 'Ein Crystal Oszillator');

-- MEDIUM Level (2 Fragen)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(51, 9, 'MEDIUM', 'Was ist PWM (Pulse Width Modulation)?', 'B', 1),
(52, 9, 'MEDIUM', 'Welcher Standard wird häufig für Kommunikation zwischen Mikrocontrollern und Sensoren genutzt?', 'A', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q51 (PWM)
(201, 51, 'A', 'Permanent Web Memory'),
(202, 51, 'B', 'Pulsweitenmodulation - Variation der Impulsbreite für Analog-ähnliche Ausgaben (z.B. LED-Helligkeit)'),
(203, 51, 'C', 'Private WiFi Mode'),
(204, 51, 'D', 'Parallel Wire Multiplexing'),
-- Q52 (I2C/SPI)
(205, 52, 'A', 'I2C oder SPI'),
(206, 52, 'B', 'UART'),
(207, 52, 'C', 'CAN-Bus'),
(208, 52, 'D', 'Ethernet');

-- HARD Level (2 Fragen)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(53, 9, 'HARD', 'Warum nutzt man Interrupts in Embedded Systems?', 'C', 1),
(54, 9, 'HARD', 'Was ist ein Watchdog-Timer?', 'B', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q53 (Interrupts)
(209, 53, 'A', 'Um Code langsamer zu machen'),
(210, 53, 'B', 'Um mehr RAM zu reservieren'),
(211, 53, 'C', 'Um auf externe Ereignisse sofort zu reagieren statt ständig zu pollen - effizient und responsiv'),
(212, 53, 'D', 'Um SQL-Abfragen zu beschleunigen'),
-- Q54 (Watchdog)
(213, 54, 'A', 'Ein Sensor zur Temperaturüberwachung'),
(214, 54, 'B', 'Ein Timer, der das System zurücksetzt, wenn es hängen bleibt oder nicht reagiert'),
(215, 54, 'C', 'Ein GPIO-Pin mit spezieller Funktion'),
(216, 54, 'D', 'Eine Debug-Komponente für Fehleranalyse');

-- Verify insertion
SELECT COUNT(*) as embedded_questions FROM questions WHERE category_id = 9;
SELECT difficulty, COUNT(*) as count FROM questions WHERE category_id = 9 GROUP BY difficulty;
