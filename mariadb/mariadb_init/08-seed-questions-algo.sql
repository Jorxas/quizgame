-- ====================================================
-- GEN1002 Informatik-Projekt - WiSe25/26 - Czekansky
-- 08-seed-questions-algo.sql
-- Algorithmen (6 Fragen: 2x EASY, 2x MEDIUM, 2x HARD)
-- ============================================

SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- ============================================
-- CATEGORY 7: ALGORITHMEN (ID 7)
-- ============================================

-- EASY Level (2 Fragen)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(37, 7, 'EASY', 'Welche Datenstruktur arbeitet nach dem FIFO-Prinzip (First In, First Out)?', 'C', 1),
(38, 7, 'EASY', 'Welche Datenstruktur arbeitet nach dem LIFO-Prinzip (Last In, First Out)?', 'A', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q37 (FIFO)
(145, 37, 'A', 'Stack'),
(146, 37, 'B', 'Heap'),
(147, 37, 'C', 'Queue'),
(148, 37, 'D', 'Tree'),
-- Q38 (LIFO)
(149, 38, 'A', 'Stack'),
(150, 38, 'B', 'Queue'),
(151, 38, 'C', 'Linked List'),
(152, 38, 'D', 'Hash Table');

-- MEDIUM Level (2 Fragen)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(39, 7, 'MEDIUM', 'Welche Laufzeit hat die binäre Suche im Worst Case?', 'A', 1),
(40, 7, 'MEDIUM', 'Welcher Sortier-Algorithmus hat im Durchschnitt O(n log n) Laufzeit?', 'B', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q39 (Binary Search)
(153, 39, 'A', 'O(log n)'),
(154, 39, 'B', 'O(n)'),
(155, 39, 'C', 'O(n log n)'),
(156, 39, 'D', 'O(1)'),
-- Q40 (Sorting)
(157, 40, 'A', 'Bubble Sort'),
(158, 40, 'B', 'Merge Sort oder Quick Sort'),
(159, 40, 'C', 'Insertion Sort'),
(160, 40, 'D', 'Selection Sort');

-- HARD Level (2 Fragen)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(41, 7, 'HARD', 'Welche Voraussetzung muss erfüllt sein, damit der Dijkstra-Algorithmus funktioniert?', 'D', 1),
(42, 7, 'HARD', 'Welches Problem beschreibt das „Traveling Salesman Problem" (TSP)?', 'C', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q41 (Dijkstra)
(161, 41, 'A', 'Der Graph muss vollständig sein'),
(162, 41, 'B', 'Es dürfen nur negative Kanten existieren'),
(163, 41, 'C', 'Es dürfen nur ungerichtete Kanten existieren'),
(164, 41, 'D', 'Keine negativen Kantengewichte'),
-- Q42 (TSP)
(165, 42, 'A', 'Das Sortieren von Passwörtern'),
(166, 42, 'B', 'Die schnellste Route durch eine Datenbank'),
(167, 42, 'C', 'Die kürzeste Rundreise, die alle Orte genau einmal besucht'),
(168, 42, 'D', 'Die Komprimierung von Text-Dateien');

-- Verify insertion
SELECT COUNT(*) as algo_questions FROM questions WHERE category_id = 7;
SELECT difficulty, COUNT(*) as count FROM questions WHERE category_id = 7 GROUP BY difficulty;
