-- ====================================================
-- GEN1002 Informatik-Projekt - WiSe25/26 - Czekansky
-- 12-seed-users.sql
-- Demo Users
-- ============================================

SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- ============================================
-- INSERT DEMO USERS
-- Password: test1234 (BCrypt hashed)
-- ============================================

INSERT IGNORE INTO users (id, username, password_hash, display_name) VALUES
(1, 'alice', '$2a$10$luNilKxBbCQztSiOk7J19eryXTa431VP9RopW0CJTSiJEL2npzWfK', 'Alice'),
(2, 'bob', '$2a$10$luNilKxBbCQztSiOk7J19eryXTa431VP9RopW0CJTSiJEL2npzWfK', 'Bob'),
(3, 'charlie', '$2a$10$luNilKxBbCQztSiOk7J19eryXTa431VP9RopW0CJTSiJEL2npzWfK', 'Charlie'),
(4, 'diana', '$2a$10$luNilKxBbCQztSiOk7J19eryXTa431VP9RopW0CJTSiJEL2npzWfK', 'Diana'),
(5, 'bernd', '$2a$10$luNilKxBbCQztSiOk7J19eryXTa431VP9RopW0CJTSiJEL2npzWfK', 'Bernd');

-- Verify insertion
SELECT COUNT(*) as user_count FROM users;
