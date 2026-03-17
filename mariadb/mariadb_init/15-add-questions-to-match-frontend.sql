-- ====================================================
-- Questions ajoutées pour correspondre aux compteurs du frontend
-- Frontend: (leicht, mittel, schwer) = (EASY, MEDIUM, HARD)
-- ====================================================

SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- IDs: questions 64+ (après 63), options 253+

-- ========== DATENBANKEN (2): (2, 7, 1) - actuellement (2, 2, 2) → +5 MEDIUM
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active) VALUES
(64, 2, 'MEDIUM', 'Was ist ein Primärschlüssel (Primary Key)?', 'A', 1),
(65, 2, 'MEDIUM', 'Welche SQL-Klausel filtert Zeilen nach einer Bedingung?', 'B', 1),
(66, 2, 'MEDIUM', 'Was bewirkt der JOIN in SQL?', 'C', 1),
(67, 2, 'MEDIUM', 'Was ist ein Index in einer Datenbank?', 'D', 1),
(68, 2, 'MEDIUM', 'Welche Aussage zur Transaktion (ACID) ist richtig?', 'A', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
(253, 64, 'A', 'Ein eindeutiger Bezeichner für einen Datensatz in einer Tabelle'),
(254, 64, 'B', 'Ein Fremdschlüssel aus einer anderen Tabelle'),
(255, 64, 'C', 'Ein temporärer Speicher für Abfragen'),
(256, 64, 'D', 'Ein Backup der Tabelle'),
(257, 65, 'A', 'GROUP BY'),
(258, 65, 'B', 'WHERE'),
(259, 65, 'C', 'ORDER BY'),
(260, 65, 'D', 'SELECT'),
(261, 66, 'A', 'Löscht Duplikate'),
(262, 66, 'B', 'Sortiert die Ergebnisse'),
(263, 66, 'C', 'Kombiniert Zeilen aus mehreren Tabellen anhand einer Bedingung'),
(264, 66, 'D', 'Erstellt eine neue Tabelle'),
(265, 67, 'A', 'Eine Sicherungskopie'),
(266, 67, 'B', 'Ein Logbuch für Änderungen'),
(267, 67, 'C', 'Ein Stored Procedure'),
(268, 67, 'D', 'Eine Datenstruktur zur Beschleunigung von Suchen'),
(269, 68, 'A', 'Atomarität: Entweder die gesamte Transaktion wird ausgeführt oder keine'),
(270, 68, 'B', 'Transaktionen können nicht rückgängig gemacht werden'),
(271, 68, 'C', 'ACID gilt nur für NoSQL-Datenbanken'),
(272, 68, 'D', 'Transaktionen blockieren niemals andere Nutzer');

-- ========== WEB-TECHNOLOGIEN (3): (5, 1, 1) - actuellement (2, 2, 2) → +3 EASY
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active) VALUES
(69, 3, 'EASY', 'Welche HTTP-Methode wird typischerweise zum Abrufen von Daten verwendet?', 'A', 1),
(70, 3, 'EASY', 'Wofür steht HTML?', 'B', 1),
(71, 3, 'EASY', 'Was ist eine API (Application Programming Interface)?', 'C', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
(273, 69, 'A', 'GET'),
(274, 69, 'B', 'POST'),
(275, 69, 'C', 'DELETE'),
(276, 69, 'D', 'PUT'),
(277, 70, 'A', 'Hyper Transfer Markup Language'),
(278, 70, 'B', 'Hypertext Markup Language'),
(279, 70, 'C', 'High Technology Markup Layout'),
(280, 70, 'D', 'Home Tool Markup Language'),
(281, 71, 'A', 'Eine grafische Benutzeroberfläche'),
(282, 71, 'B', 'Eine Programmiersprache'),
(283, 71, 'C', 'Eine Schnittstelle, über die Programme miteinander kommunizieren können'),
(284, 71, 'D', 'Ein Datenbankformat');

-- ========== NETZWERKE (4): (8, 9, 2) - actuellement (2, 2, 2) → +6 EASY, +7 MEDIUM
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active) VALUES
(72, 4, 'EASY', 'Welches Protokoll nutzt typischerweise Port 443?', 'B', 1),
(73, 4, 'EASY', 'Wofür steht DNS?', 'A', 1),
(74, 4, 'EASY', 'Was ist ein Router?', 'C', 1),
(75, 4, 'EASY', 'Welches Gerät verbindet Geräte in einem lokalen Netzwerk?', 'D', 1),
(76, 4, 'EASY', 'Was bedeutet LAN?', 'A', 1),
(77, 4, 'EASY', 'Welches Protokoll dient zur E-Mail-Übertragung?', 'B', 1),
(78, 4, 'MEDIUM', 'Was ist ein Subnetz (Subnet)?', 'A', 1),
(79, 4, 'MEDIUM', 'Was beschreibt die IPv4-Adresse 127.0.0.1?', 'B', 1),
(80, 4, 'MEDIUM', 'Was ist ein Firewall?', 'C', 1),
(81, 4, 'MEDIUM', 'Welches Protokoll arbeitet auf dem Transport-Layer?', 'D', 1),
(82, 4, 'MEDIUM', 'Was ist DHCP?', 'A', 1),
(83, 4, 'MEDIUM', 'Wofür steht UDP?', 'B', 1),
(84, 4, 'MEDIUM', 'Was ist eine MAC-Adresse?', 'C', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
(285, 72, 'A', 'HTTP'),
(286, 72, 'B', 'HTTPS'),
(287, 72, 'C', 'FTP'),
(288, 72, 'D', 'SMTP'),
(289, 73, 'A', 'Domain Name System - übersetzt Domainnamen in IP-Adressen'),
(290, 73, 'B', 'Data Network Service'),
(291, 73, 'C', 'Dynamic Network Security'),
(292, 73, 'D', 'Digital Number System'),
(293, 74, 'A', 'Ein Speichergerät'),
(294, 74, 'B', 'Eine Programmiersprache'),
(295, 74, 'C', 'Ein Gerät, das Netzwerkpakete zwischen Netzwerken weiterleitet'),
(296, 74, 'D', 'Ein Antivirenprogramm'),
(297, 75, 'A', 'Ein Monitor'),
(298, 75, 'B', 'Eine Tastatur'),
(299, 75, 'C', 'Ein Drucker'),
(300, 75, 'D', 'Ein Switch oder ein WLAN-Access-Point'),
(301, 76, 'A', 'Local Area Network - ein lokales Netzwerk'),
(302, 76, 'B', 'Large Access Network'),
(303, 76, 'C', 'Logical Address Number'),
(304, 76, 'D', 'Linked Application Node'),
(305, 77, 'A', 'HTTP'),
(306, 77, 'B', 'SMTP oder IMAP'),
(307, 77, 'C', 'FTP'),
(308, 77, 'D', 'SSH'),
(309, 78, 'A', 'Ein logisch geteilter Teil eines Netzwerks (z.B. durch Subnetzmasken)'),
(310, 78, 'B', 'Ein Sicherheitszertifikat'),
(311, 78, 'C', 'Ein Router-Modell'),
(312, 78, 'D', 'Ein Protokoll für E-Mails'),
(313, 79, 'A', 'Eine öffentliche IP im Internet'),
(314, 79, 'B', 'Localhost - die eigene Maschine'),
(315, 79, 'C', 'Eine Broadcast-Adresse'),
(316, 79, 'D', 'Eine Multicast-Adresse'),
(317, 80, 'A', 'Ein Antivirenprogramm'),
(318, 80, 'B', 'Ein Backup-Tool'),
(319, 80, 'C', 'Ein System, das Netzwerkverkehr filtert und blockiert/zulässt'),
(320, 80, 'D', 'Ein Datenbank-Server'),
(321, 81, 'A', 'HTTP'),
(322, 81, 'B', 'IP'),
(323, 81, 'C', 'Ethernet'),
(324, 81, 'D', 'TCP oder UDP'),
(325, 82, 'A', 'Dynamic Host Configuration Protocol - vergibt automatisch IP-Adressen'),
(326, 82, 'B', 'Data Hyper Control Protocol'),
(327, 82, 'C', 'Domain Host Configuration'),
(328, 82, 'D', 'Digital Handshake Control'),
(329, 83, 'A', 'Unified Data Protocol'),
(330, 83, 'B', 'User Datagram Protocol - verbindungsloses Transportprotokoll'),
(331, 83, 'C', 'Universal Digital Port'),
(332, 83, 'D', 'Unified Data Packet'),
(333, 84, 'A', 'Die IP-Adresse eines Routers'),
(334, 84, 'B', 'Eine Software-Versionsnummer'),
(335, 84, 'C', 'Eine hardwarenahe Adresse einer Netzwerkkarte (z.B. 00:1A:2B:3C:4D:5E)'),
(336, 84, 'D', 'Eine Domain-Adresse');

-- ========== BETRIEBSSYSTEME (5): (3, 6, 4) - actuellement (2, 2, 2) → +1 EASY, +4 MEDIUM, +2 HARD
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active) VALUES
(85, 5, 'EASY', 'Was ist ein Thread?', 'B', 1),
(86, 5, 'MEDIUM', 'Was ist ein Kontextwechsel (Context Switch)?', 'A', 1),
(87, 5, 'MEDIUM', 'Was ist ein Speicher-Leak (Memory Leak)?', 'C', 1),
(88, 5, 'MEDIUM', 'Was ist ein Semaphor?', 'D', 1),
(89, 5, 'MEDIUM', 'Was ist Swapping?', 'A', 1),
(90, 5, 'HARD', 'Was ist Priority Inversion?', 'B', 1),
(91, 5, 'HARD', 'Was ist ein Producer-Consumer-Problem?', 'C', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
(337, 85, 'A', 'Eine Datei auf der Festplatte'),
(338, 85, 'B', 'Eine leichte Ausführungseinheit innerhalb eines Prozesses - teilt sich Speicher'),
(339, 85, 'C', 'Ein Netzwerkprotokoll'),
(340, 85, 'D', 'Ein Compiler'),
(341, 86, 'A', 'Das Speichern und Wiederherstellen des Prozesszustands beim Wechsel der CPU'),
(342, 86, 'B', 'Ein Netzwerk-Switch'),
(343, 86, 'C', 'Ein Backup-Verfahren'),
(344, 86, 'D', 'Eine Verschlüsselungsmethode'),
(345, 87, 'A', 'Ein defekter RAM-Baustein'),
(346, 87, 'B', 'Eine schnelle Speicherzugriffsmethode'),
(347, 87, 'C', 'Nicht mehr freigegebener Speicher - führt zu Speicherverlust über die Zeit'),
(348, 87, 'D', 'Ein Cache-Algorithmus'),
(349, 88, 'A', 'Ein Bildformat'),
(350, 88, 'B', 'Ein Netzwerkprotokoll'),
(351, 88, 'C', 'Eine Programmiersprache'),
(352, 88, 'D', 'Ein Synchronisationsmechanismus zur Steuerung des Zugriffs auf Ressourcen'),
(353, 89, 'A', 'Auslagern von Prozessen vom RAM auf die Festplatte und zurück'),
(354, 89, 'B', 'Ein Backup-Verfahren'),
(355, 89, 'C', 'Netzwerk-Routing'),
(356, 89, 'D', 'Verschlüsselung von Dateien'),
(357, 90, 'A', 'Ein Sortieralgorithmus'),
(358, 90, 'B', 'Ein Problem, bei dem ein niederpriorer Thread einen höherprioreren blockiert'),
(359, 90, 'C', 'Ein Netzwerkfehler'),
(360, 90, 'D', 'Eine Speicheroptimierung'),
(361, 91, 'A', 'Ein Compiler-Fehler'),
(362, 91, 'B', 'Ein Netzwerk-Protokoll'),
(363, 91, 'C', 'Klassisches Synchronisationsproblem: Ein Produzent erzeugt Daten, ein Konsument verbraucht sie'),
(364, 91, 'D', 'Ein Datenbank-Design-Pattern');

-- ========== IT-SICHERHEIT (6): (1, 6, 6) - actuellement (2, 2, 2) → +4 MEDIUM, +4 HARD
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active) VALUES
(92, 6, 'MEDIUM', 'Was ist Zwei-Faktor-Authentifizierung (2FA)?', 'A', 1),
(93, 6, 'MEDIUM', 'Was ist ein Man-in-the-Middle-Angriff?', 'B', 1),
(94, 6, 'MEDIUM', 'Was ist ein Zero-Day-Exploit?', 'C', 1),
(95, 6, 'MEDIUM', 'Was ist Verschlüsselung (Encryption)?', 'D', 1),
(96, 6, 'HARD', 'Was beschreibt die Kryptographie mit asymmetrischen Schlüsseln?', 'A', 1),
(97, 6, 'HARD', 'Was ist ein DDoS-Angriff?', 'B', 1),
(98, 6, 'HARD', 'Was ist ein Keylogger?', 'C', 1),
(99, 6, 'HARD', 'Was ist XSS (Cross-Site Scripting)?', 'D', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
(365, 92, 'A', 'Authentifizierung mit zwei verschiedenen Faktoren (z.B. Passwort + SMS-Code)'),
(366, 92, 'B', 'Zwei Passwörter nacheinander'),
(367, 92, 'C', 'Zwei Benutzerkonten'),
(368, 92, 'D', 'Zwei Firewalls'),
(369, 93, 'A', 'Ein Angriff durch defekte Hardware'),
(370, 93, 'B', 'Ein Angreifer leitet/schnüffelt die Kommunikation zwischen zwei Parteien ab'),
(371, 93, 'C', 'Ein Angriff durch E-Mails'),
(372, 93, 'D', 'Ein Backup-Verfahren'),
(373, 94, 'A', 'Ein Exploit an Tag 0 des Projekts'),
(374, 94, 'B', 'Ein Angriff ohne Schadsoftware'),
(375, 94, 'C', 'Eine Schwachstelle, die noch unbekannt ist - kein Patch verfügbar'),
(376, 94, 'D', 'Ein Sicherheitszertifikat'),
(377, 95, 'A', 'Das Löschen von Daten'),
(378, 95, 'B', 'Das Komprimieren von Dateien'),
(379, 95, 'C', 'Das Sichern auf eine zweite Festplatte'),
(380, 95, 'D', 'Umwandlung von Daten in eine unleserliche Form ohne Schlüssel'),
(381, 96, 'A', 'Öffentlicher und privater Schlüssel - Verschlüsseln mit dem einen, Entschlüsseln mit dem anderen'),
(382, 96, 'B', 'Nur ein Schlüssel für alles'),
(383, 96, 'C', 'Keine Verschlüsselung'),
(384, 96, 'D', 'Ein Passwort pro Nutzer'),
(385, 97, 'A', 'Ein Angriff durch eine einzelne Person'),
(386, 97, 'B', 'Distributed Denial of Service - viele Quellen überlasten einen Dienst'),
(387, 97, 'C', 'Ein Datenbank-Angriff'),
(388, 97, 'D', 'Ein Virus'),
(389, 98, 'A', 'Ein Backup-Programm'),
(390, 98, 'B', 'Ein Verschlüsselungs-Tool'),
(391, 98, 'C', 'Schadsoftware, die Tastatureingaben mitschneidet'),
(392, 98, 'D', 'Ein Firewall-Feature'),
(393, 99, 'A', 'Ein SQL-Fehler'),
(394, 99, 'B', 'Ein Netzwerk-Protokoll'),
(395, 99, 'C', 'Ein Datenbank-Design'),
(396, 99, 'D', 'Einschleusen von Skripten in Webseiten, die im Browser anderer Nutzer ausgeführt werden');

-- ========== ALGORITHMEN (7): (8, 2, 4) - actuellement (2, 2, 2) → +6 EASY, +2 HARD
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active) VALUES
(100, 7, 'EASY', 'Was ist eine Variable in der Programmierung?', 'A', 1),
(101, 7, 'EASY', 'Welche Laufzeit hat ein einfacher Zugriff auf ein Array-Element?', 'B', 1),
(102, 7, 'EASY', 'Was ist eine bedingte Anweisung (if/else)?', 'C', 1),
(103, 7, 'EASY', 'Was beschreibt eine Schleife (for/while)?', 'D', 1),
(104, 7, 'EASY', 'Welche Datenstruktur verwendet Schlüssel-Wert-Paare?', 'A', 1),
(105, 7, 'EASY', 'Was ist die Laufzeit O(1)?', 'B', 1),
(106, 7, 'HARD', 'Was ist die Zeitkomplexität von Merge Sort im Worst Case?', 'C', 1),
(107, 7, 'HARD', 'Was beschreibt ein NP-vollständiges Problem?', 'D', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
(397, 100, 'A', 'Ein benannter Speicherplatz für einen Wert'),
(398, 100, 'B', 'Eine Funktion'),
(399, 100, 'C', 'Eine Schleife'),
(400, 100, 'D', 'Ein Compiler'),
(401, 101, 'A', 'O(n)'),
(402, 101, 'B', 'O(1) - konstanter Zugriff per Index'),
(403, 101, 'C', 'O(log n)'),
(404, 101, 'D', 'O(n²)'),
(405, 102, 'A', 'Eine Schleife'),
(406, 102, 'B', 'Eine Funktion'),
(407, 102, 'C', 'Ausführung von Code abhängig von einer Bedingung'),
(408, 102, 'D', 'Ein Array'),
(409, 103, 'A', 'Eine einmalige Ausführung'),
(410, 103, 'B', 'Eine Bedingungsprüfung'),
(411, 103, 'C', 'Eine Funktion'),
(412, 103, 'D', 'Wiederholte Ausführung von Code'),
(413, 104, 'A', 'Hash-Map / Dictionary'),
(414, 104, 'B', 'Array'),
(415, 104, 'C', 'Stack'),
(416, 104, 'D', 'Queue'),
(417, 105, 'A', 'Linear zur Eingabegröße'),
(418, 105, 'B', 'Konstant - unabhängig von der Eingabegröße'),
(419, 105, 'C', 'Quadratisch'),
(420, 105, 'D', 'Logarithmisch'),
(421, 106, 'A', 'O(n)'),
(422, 106, 'B', 'O(n²)'),
(423, 106, 'C', 'O(n log n)'),
(424, 106, 'D', 'O(1)'),
(425, 107, 'A', 'Ein Problem mit O(1) Lösung'),
(426, 107, 'B', 'Ein Problem, das nicht lösbar ist'),
(427, 107, 'C', 'Ein Problem mit einfacher Schleife'),
(428, 107, 'D', 'Ein Problem, für das kein effizienter Algorithmus bekannt ist - typischerweise NP-schwer');

-- ========== SOFTWARE ENGINEERING (8): (2, 4, 3) - actuellement (2, 2, 2) → +2 MEDIUM, +1 HARD
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active) VALUES
(108, 8, 'MEDIUM', 'Was ist ein Unit-Test?', 'A', 1),
(109, 8, 'MEDIUM', 'Was beschreibt Code Review?', 'B', 1),
(110, 8, 'HARD', 'Was ist ein Dependency Injection Container?', 'C', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
(429, 108, 'A', 'Ein automatisierter Test für eine kleine Einheit (z.B. eine Funktion)'),
(430, 108, 'B', 'Ein manueller Benutzertest'),
(431, 108, 'C', 'Ein Datenbanktest'),
(432, 108, 'D', 'Ein Netzwerk-Test'),
(433, 109, 'A', 'Das Schreiben von Code'),
(434, 109, 'B', 'Überprüfung von Code durch andere Entwickler vor dem Merge'),
(435, 109, 'C', 'Das Löschen von Code'),
(436, 109, 'D', 'Automatische Formatierung'),
(437, 110, 'A', 'Ein Backup-System'),
(438, 110, 'B', 'Ein Compiler'),
(439, 110, 'C', 'Ein Framework, das Abhängigkeiten verwaltet und injiziert - lose Kopplung'),
(440, 110, 'D', 'Ein Datenbank-Manager');

-- ========== EMBEDDED SYSTEMS (9): (1, 5, 5) - actuellement (2, 2, 2) → +3 MEDIUM, +3 HARD
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active) VALUES
(111, 9, 'MEDIUM', 'Was ist UART?', 'A', 1),
(112, 9, 'MEDIUM', 'Was ist ein Timer/Counter in einem Mikrocontroller?', 'B', 1),
(113, 9, 'MEDIUM', 'Was bedeutet Echtzeit (Real-Time) bei Embedded Systems?', 'C', 1),
(114, 9, 'HARD', 'Was ist DMA (Direct Memory Access)?', 'D', 1),
(115, 9, 'HARD', 'Was beschreibt den RISC-Ansatz bei Prozessoren?', 'A', 1),
(116, 9, 'HARD', 'Was ist ein Bootloader?', 'B', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
(441, 111, 'A', 'Universelles serielles Kommunikationsprotokoll (z.B. für Debug-Ausgabe)'),
(442, 111, 'B', 'Ein Sensortyp'),
(443, 111, 'C', 'Ein Speichertyp'),
(444, 111, 'D', 'Ein Netzwerkprotokoll'),
(445, 112, 'A', 'Ein Backup-Speicher'),
(446, 112, 'B', 'Hardware zur Zeitmessung oder Zählung von Ereignissen'),
(447, 112, 'C', 'Ein Display-Controller'),
(448, 112, 'D', 'Ein Audio-Codec'),
(449, 113, 'A', 'Sehr schnelle Ausführung'),
(450, 113, 'B', 'Minimaler Stromverbrauch'),
(451, 113, 'C', 'Garantierte Reaktionszeit innerhalb eines Zeitlimits'),
(452, 113, 'D', 'Großer Speicher'),
(453, 114, 'A', 'Direct Module Access'),
(454, 114, 'B', 'Data Management API'),
(455, 114, 'C', 'Digital Memory Array'),
(456, 114, 'D', 'Zugriff auf Speicher ohne CPU - entlastet die CPU bei Datenübertragungen'),
(457, 115, 'A', 'Reduced Instruction Set - wenige, einfache Befehle für effiziente Ausführung'),
(458, 115, 'B', 'Random Instruction Set Code'),
(459, 115, 'C', 'Rapid Integration Storage'),
(460, 115, 'D', 'Real-Time Input System'),
(461, 116, 'A', 'Ein Betriebssystem'),
(462, 116, 'B', 'Programm, das beim Start lädt und die eigentliche Anwendung startet (z.B. Firmware-Update)'),
(463, 116, 'C', 'Ein Compiler'),
(464, 116, 'D', 'Ein Debugger');

-- ========== LINUX & TOOLS (10): (9, 3, 2) - actuellement (2, 2, 2) → +7 EASY, +1 MEDIUM
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active) VALUES
(117, 10, 'EASY', 'Welcher Befehl löscht eine Datei?', 'B', 1),
(118, 10, 'EASY', 'Welcher Befehl wechselt das Verzeichnis?', 'C', 1),
(119, 10, 'EASY', 'Welcher Befehl zeigt den Inhalt einer Datei an?', 'D', 1),
(120, 10, 'EASY', 'Was bedeutet der Befehl „mkdir ordner"?', 'A', 1),
(121, 10, 'EASY', 'Welcher Befehl kopiert eine Datei?', 'B', 1),
(122, 10, 'EASY', 'Welcher Befehl verschiebt oder benennt eine Datei um?', 'C', 1),
(123, 10, 'EASY', 'Was macht der Befehl „grep suchtext datei"?', 'D', 1),
(124, 10, 'MEDIUM', 'Was bewirkt der Befehl „sudo"?', 'A', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
(465, 117, 'A', 'ls'),
(466, 117, 'B', 'rm'),
(467, 117, 'C', 'cd'),
(468, 117, 'D', 'cat'),
(469, 118, 'A', 'ls'),
(470, 118, 'B', 'pwd'),
(471, 118, 'C', 'cd'),
(472, 118, 'D', 'rm'),
(473, 119, 'A', 'ls'),
(474, 119, 'B', 'cd'),
(475, 119, 'C', 'pwd'),
(476, 119, 'D', 'cat'),
(477, 120, 'A', 'Erstellt ein neues Verzeichnis namens „ordner"'),
(478, 120, 'B', 'Löscht das Verzeichnis'),
(479, 120, 'C', 'Wechselt ins Verzeichnis'),
(480, 120, 'D', 'Kopiert das Verzeichnis'),
(481, 121, 'A', 'mv'),
(482, 121, 'B', 'cp'),
(483, 121, 'C', 'ls'),
(484, 121, 'D', 'rm'),
(485, 122, 'A', 'cp'),
(486, 122, 'B', 'ls'),
(487, 122, 'C', 'mv'),
(488, 122, 'D', 'cat'),
(489, 123, 'A', 'Erstellt eine Datei'),
(490, 123, 'B', 'Löscht Zeilen'),
(491, 123, 'C', 'Sortiert die Datei'),
(492, 123, 'D', 'Sucht nach „suchtext" in der Datei und zeigt passende Zeilen'),
(493, 124, 'A', 'Führt einen Befehl mit Administrator-Rechten (root) aus'),
(494, 124, 'B', 'Startet einen Server'),
(495, 124, 'C', 'Installiert ein Programm'),
(496, 124, 'D', 'Erstellt einen Benutzer');
