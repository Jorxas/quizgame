-- ====================================================
-- GEN1002 Informatik-Projekt - WiSe25/26 - Czekansky
-- 02-seed-questions-prog.sql
-- Programmierung (6 Fragen: 2x EASY, 2x MEDIUM, 2x HARD)
-- ============================================

SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- ============================================
-- CATEGORY 1: PROGRAMMIERUNG (ID 1)
-- ============================================

-- EASY Level (2 Fragen)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(1, 1, 'EASY', 'Wofür steht die Abkürzung IDE?', 'B', 1),
(2, 1, 'EASY', 'Welche der folgenden ist eine Programmiersprache?', 'C', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q1 (IDE)
(1, 1, 'A', 'Internet Data Exchange'),
(2, 1, 'B', 'Integrated Development Environment'),
(3, 1, 'C', 'Internal Debug Engine'),
(4, 1, 'D', 'Interface Description Editor'),
-- Q2 (Programmiersprache)
(5, 2, 'A', 'HTML'),
(6, 2, 'B', 'CSS'),
(7, 2, 'C', 'Python'),
(8, 2, 'D', 'JSON');

-- MEDIUM Level (2 Fragen)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(3, 1, 'MEDIUM', 'Was macht der git-Befehl „git commit"?', 'C', 1),
(4, 1, 'MEDIUM', 'Welches Paradigma beschreibt die objektorientierte Programmierung (OOP)?', 'A', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q3 (git commit)
(9, 3, 'A', 'Erstellt automatisch einen Merge Request'),
(10, 3, 'B', 'Löscht nicht getrackte Dateien'),
(11, 3, 'C', 'Speichert Änderungen als Snapshot in der Repository-Historie'),
(12, 3, 'D', 'Schreibt Änderungen direkt ins Remote-Repository'),
-- Q4 (OOP)
(13, 4, 'A', 'Ein Konzept, das Daten und Funktionen in „Objekten" kapselt'),
(14, 4, 'B', 'Ein funktionales Paradigma, das nur reine Funktionen nutzt'),
(15, 4, 'C', 'Eine Methode zur manuellen Speicherverwaltung'),
(16, 4, 'D', 'Ein Debugging-Verfahren für schnellere Kompilierung');

-- HARD Level (2 Fragen)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(5, 1, 'HARD', 'Was beschreibt die Big-O-Notation primär?', 'A', 1),
(6, 1, 'HARD', 'Welches Principle beschreibt SOLID in der Softwareentwicklung richtig?', 'B', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q5 (Big-O)
(17, 5, 'A', 'Die asymptotische Wachstumsrate des Aufwands in Abhängigkeit von der Eingabegröße n'),
(18, 5, 'B', 'Die exakte Laufzeit eines Algorithmus in Millisekunden'),
(19, 5, 'C', 'Den durchschnittlichen CPU-Takt eines Systems'),
(20, 5, 'D', 'Die Anzahl der möglichen Bugs im Code'),
-- Q6 (SOLID)
(21, 6, 'A', 'Nur Single Inheritance nutzen'),
(22, 6, 'B', 'Single Responsibility, Open/Closed, Liskov Substitution, Interface Segregation, Dependency Inversion'),
(23, 6, 'C', 'Strukturelles Logging und Objekt Debugging'),
(24, 6, 'D', 'Statische Variable und Override-Limitierungen');

-- Verify insertion
SELECT COUNT(*) as prog_questions FROM questions WHERE category_id = 1;
SELECT difficulty, COUNT(*) as count FROM questions WHERE category_id = 1 GROUP BY difficulty;
