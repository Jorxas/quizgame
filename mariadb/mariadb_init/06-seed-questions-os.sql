-- ====================================================
-- GEN1002 Informatik-Projekt - WiSe25/26 - Czekansky
-- 06-seed-questions-os.sql
-- Betriebssysteme (6 Fragen: 2x EASY, 2x MEDIUM, 2x HARD)
-- ============================================

SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- ============================================
-- CATEGORY 5: BETRIEBSSYSTEME (ID 5)
-- ============================================

-- EASY Level (2 Fragen)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(25, 5, 'EASY', 'Was ist ein Prozess in einem Betriebssystem?', 'B', 1),
(26, 5, 'EASY', 'Welcher der folgenden ist ein modernes Desktop-Betriebssystem?', 'D', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q25 (Prozess)
(97, 25, 'A', 'Eine Datei im Dateisystem'),
(98, 25, 'B', 'Ein Programm in Ausführung mit eigenem Adressraum und Ressourcen'),
(99, 25, 'C', 'Ein Netzwerkpaket'),
(100, 25, 'D', 'Ein CPU-Register'),
-- Q26 (OS)
(101, 26, 'A', 'MS-DOS'),
(102, 26, 'B', 'Windows 3.1'),
(103, 26, 'C', 'AmigaOS'),
(104, 26, 'D', 'Linux oder Windows 10/11');

-- MEDIUM Level (2 Fragen)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(27, 5, 'MEDIUM', 'Wofür wird ein Mutex (Mutual Exclusion Lock) typischerweise verwendet?', 'A', 1),
(28, 5, 'MEDIUM', 'Was ist Scheduling im Kontext eines Betriebssystems?', 'C', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q27 (Mutex)
(105, 27, 'A', 'Schutz kritischer Abschnitte (gegenseitiger Ausschluss zwischen Threads)'),
(106, 27, 'B', 'Schnelleres Kompilieren von Programmen'),
(107, 27, 'C', 'DNS-Auflösung von Domainnamen'),
(108, 27, 'D', 'Komprimierung von Dateien'),
-- Q28 (Scheduling)
(109, 28, 'A', 'Das Ändern von Datei-Ownerships'),
(110, 28, 'B', 'Das Erstellen von Backups'),
(111, 28, 'C', 'Die Verwaltung und Vergabe von CPU-Zeit an Prozesse/Threads'),
(112, 28, 'D', 'Die Synchronisation von Netzwerk-Uhren');

-- HARD Level (2 Fragen)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(29, 5, 'HARD', 'Was ist ein Deadlock in der Prozess-Synchronisation?', 'C', 1),
(30, 5, 'HARD', 'Was ist die virtuelle Speicherverwaltung?', 'B', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q29 (Deadlock)
(113, 29, 'A', 'Ein Speicherleck, das zu RAM-Erschöpfung führt'),
(114, 29, 'B', 'Ein schneller Kontextwechsel zwischen Prozessen'),
(115, 29, 'C', 'Wechselseitiges Warten blockierter Prozesse/Threads auf Ressourcen - ein Stillstand'),
(116, 29, 'D', 'Ein Timeout bei der Ping-Kommunikation'),
-- Q30 (Virtual Memory)
(117, 30, 'A', 'Auslagerung von Prozessen auf andere Rechner'),
(118, 30, 'B', 'Die Abstraktion des physischen Speichers durch Paging/Segmentierung - ermöglicht größere Adressräume'),
(119, 30, 'C', 'Das Verschlüsseln des RAM-Inhalts'),
(120, 30, 'D', 'Ein Datenbankfeature zur Speicheroptimierung');

-- Verify insertion
SELECT COUNT(*) as os_questions FROM questions WHERE category_id = 5;
SELECT difficulty, COUNT(*) as count FROM questions WHERE category_id = 5 GROUP BY difficulty;
