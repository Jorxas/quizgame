-- RFID login demo: add rfid_uid to alice for testing.
-- After scanning your card, get UID from Serial, then run in phpMyAdmin:
--   UPDATE users SET rfid_uid = 'YOUR_UID' WHERE username = 'alice';
-- Example (4-byte UID): 04A1B2C3
UPDATE users SET rfid_uid = '53EDA50D' WHERE username = 'alice' LIMIT 1;