-- ====================================================
-- GEN1002 Informatik-Projekt - WiSe25/26 - Czekansky
-- 05-seed-questions-network.sql
-- Netzwerke (6 Fragen: 2x EASY, 2x MEDIUM, 2x HARD)
-- ============================================

SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- ============================================
-- CATEGORY 4: NETZWERKE (ID 4)
-- ============================================

-- EASY Level (2 Fragen)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(19, 4, 'EASY', 'Welches Protokoll nutzt typischerweise Port 80?', 'C', 1),
(20, 4, 'EASY', 'Was ist eine IP-Adresse?', 'A', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q19 (Port 80)
(73, 19, 'A', 'HTTPS'),
(74, 19, 'B', 'FTP'),
(75, 19, 'C', 'HTTP'),
(76, 19, 'D', 'SSH'),
-- Q20 (IP-Adresse)
(77, 20, 'A', 'Eine eindeutige numerische Adresse zum Identifizieren von Geräten in einem Netzwerk'),
(78, 20, 'B', 'Ein Passwort für Netzwerk-Authentifizierung'),
(79, 20, 'C', 'Ein Routing-Protokoll'),
(80, 20, 'D', 'Eine Datenbankverbindungs-Zeichenkette');

-- MEDIUM Level (2 Fragen)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(21, 4, 'MEDIUM', 'Was ist NAT (Network Address Translation)?', 'A', 1),
(22, 4, 'MEDIUM', 'Welches OSI-Modell-Layer arbeitet mit IP-Adressen?', 'B', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q21 (NAT)
(81, 21, 'A', 'Übersetzt private IPs zu einer öffentlichen IP (und Ports) - ermöglicht mehreren Geräten einen Internetzugang'),
(82, 21, 'B', 'Verschlüsselt Datenpakete auf Layer 2 automatisch'),
(83, 21, 'C', 'Synchronisiert Uhren im Netzwerk'),
(84, 21, 'D', 'Verhindert Paketverlust durch automatische Retransmission'),
-- Q22 (OSI Layer 3)
(85, 22, 'A', 'Layer 2 (Data Link)'),
(86, 22, 'B', 'Layer 3 (Network)'),
(87, 22, 'C', 'Layer 4 (Transport)'),
(88, 22, 'D', 'Layer 7 (Application)');

-- HARD Level (2 Fragen)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(23, 4, 'HARD', 'Wozu dient TLS/SSL primär?', 'D', 1),
(24, 4, 'HARD', 'Was beschreibt das TCP-Handshake-Verfahren (3-Way Handshake)?', 'C', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q23 (TLS/SSL)
(89, 23, 'A', 'Komprimierung von Bildern in HTTP'),
(90, 23, 'B', 'Routing über mehrere Hops in großen Netzwerken'),
(91, 23, 'C', 'Load Balancing auf Layer 4 (Transport)'),
(92, 23, 'D', 'Verschlüsselung und Integrität bei der Verbindung'),
-- Q24 (TCP Handshake)
(93, 24, 'A', 'Ein Verfahren zum Komprimieren von TCP-Paketen'),
(94, 24, 'B', 'Das Beenden einer TCP-Verbindung nach Datenaustausch'),
(95, 24, 'C', 'Ein Verfahren zum Aufbau einer zuverlässigen Verbindung: SYN, SYN-ACK, ACK'),
(96, 24, 'D', 'Ein DNS-Auflösungsalgorithmus');

-- Verify insertion
SELECT COUNT(*) as network_questions FROM questions WHERE category_id = 4;
SELECT difficulty, COUNT(*) as count FROM questions WHERE category_id = 4 GROUP BY difficulty;
