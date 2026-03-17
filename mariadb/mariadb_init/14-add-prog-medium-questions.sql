-- ====================================================
-- 3 zusätzliche MEDIUM-Fragen für Programmierung (zum Testen)
-- Vorher: 2 MEDIUM → Jetzt: 5 MEDIUM
-- ====================================================

SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- CATEGORY 1: PROGRAMMIERUNG - 3 neue MEDIUM-Fragen (IDs 61, 62, 63)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(61, 1, 'MEDIUM', 'Was ist eine Schleife (Loop) in der Programmierung?', 'B', 1),
(62, 1, 'MEDIUM', 'Welche Aussage über Rekursion ist korrekt?', 'A', 1),
(63, 1, 'MEDIUM', 'Was versteht man unter „Refactoring"?', 'C', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q61 (Schleife)
(241, 61, 'A', 'Eine Datenstruktur zum Speichern von Objekten'),
(242, 61, 'B', 'Ein Kontrollflusskonstrukt, das Code wiederholt ausführt, bis eine Bedingung erfüllt ist'),
(243, 61, 'C', 'Ein Compiler-Optimierungsverfahren'),
(244, 61, 'D', 'Eine Methode zur Fehlersuche'),
-- Q62 (Rekursion)
(245, 62, 'A', 'Eine Funktion, die sich selbst aufruft und einen Abbruchfall haben muss'),
(246, 62, 'B', 'Ein paralleler Programmablauf über mehrere Threads'),
(247, 62, 'C', 'Eine Schleife mit unbegrenzter Anzahl von Durchläufen'),
(248, 62, 'D', 'Ein Design-Pattern für Datenbankabfragen'),
-- Q63 (Refactoring)
(249, 63, 'A', 'Das Hinzufügen neuer Features zum Code'),
(250, 63, 'B', 'Das Beheben von Compiler-Fehlern'),
(251, 63, 'C', 'Die Verbesserung der Code-Struktur ohne Änderung des externen Verhaltens'),
(252, 63, 'D', 'Das Schreiben von Unit-Tests');
