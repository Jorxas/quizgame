-- Met Diane (diana) en état "non prêt" dans le lobby
UPDATE game_session_players gsp
JOIN users u ON u.id = gsp.user_id
JOIN game_sessions gs ON gs.id = gsp.game_session_id
SET gsp.is_ready = 0
WHERE u.username = 'diana' AND gs.state = 'LOBBY';
