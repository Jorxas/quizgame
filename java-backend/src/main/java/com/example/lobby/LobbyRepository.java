package com.example.lobby;

/**
 * Lobby-Repository – DB-Zugriff für game_session_players, Lobby-Status.
 */
import com.example.database.DatabaseClient;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.jdbcclient.JDBCPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class LobbyRepository {

    private final JDBCPool jdbcPool;

    public LobbyRepository() {
        this.jdbcPool = DatabaseClient.getInstance();
    }

    /** Prüft, ob der Benutzer bereits in der aktuellen Lobby-Session ist. */
    public void isUserInCurrentLobby(String username, Handler<AsyncResult<Boolean>> resultHandler) {
        String sql = "SELECT 1 FROM game_session_players gsp " +
                "JOIN game_sessions gs ON gs.id = gsp.game_session_id " +
                "JOIN users u ON u.id = gsp.user_id " +
                "WHERE gs.state = 'LOBBY' AND gs.id = (SELECT MAX(id) FROM game_sessions WHERE state = 'LOBBY') " +
                "AND u.username = ? LIMIT 1";
        jdbcPool.preparedQuery(sql).execute(Tuple.of(username), ar -> {
            if (ar.failed()) {
                resultHandler.handle(Future.failedFuture(ar.cause()));
                return;
            }
            resultHandler.handle(Future.succeededFuture(ar.result().iterator().hasNext()));
        });
    }

    /** Erstellt neue LOBBY-Session. Beendet zuvor alle laufenden Sessions (LOBBY, COUNTDOWN, QUESTION, EVALUATION). */
    public void createNewLobbySession(Handler<AsyncResult<Void>> resultHandler) {
        jdbcPool.preparedQuery("UPDATE game_sessions SET state = 'ENDED' WHERE state != 'ENDED'")
                .execute(Tuple.tuple(), updateAr -> {
                    if (updateAr.failed()) {
                        resultHandler.handle(Future.failedFuture(updateAr.cause()));
                        return;
                    }
                    jdbcPool.preparedQuery("INSERT INTO game_sessions (round_length, state) VALUES ('Q5', 'LOBBY')")
                            .execute(Tuple.tuple(), insAr -> {
                                resultHandler.handle(insAr.succeeded() ? Future.succeededFuture() : Future.failedFuture(insAr.cause()));
                            });
                });
    }

    /** Liefert Spieler der aktuellen Lobby mit ready-Status, Controller-ID und Controller-Status. */
    public void fetchPlayersWithStatus(Handler<AsyncResult<JsonArray>> resultHandler) {
        String sql = "SELECT u.username, gsp.is_ready, c.controller_id, c.status AS controller_status " +
                "FROM game_session_players gsp " +
                "JOIN game_sessions gs ON gs.id = gsp.game_session_id " +
                "JOIN users u ON u.id = gsp.user_id " +
                "LEFT JOIN controllers c ON c.id = gsp.controller_id " +
                "WHERE gs.state = 'LOBBY' AND gs.id = (SELECT MAX(id) FROM game_sessions WHERE state = 'LOBBY')";

        jdbcPool.preparedQuery(sql)
                .execute(Tuple.tuple(), ar -> {
                    if (ar.succeeded()) {
                        RowSet<Row> rows = ar.result();
                        JsonArray players = new JsonArray();
                        for (Row row : rows) {
                            String username = row.getString("username");
                            Boolean isReady = row.getBoolean("is_ready");
                            boolean ready = Boolean.TRUE.equals(isReady);
                            String controllerId = row.getString("controller_id");
                            String controllerStatus = row.getString("controller_status");
                            JsonObject playerJson = new JsonObject()
                                    .put("username", username)
                                    .put("ready", ready);
                            if (controllerId != null && !controllerId.isBlank()) {
                                playerJson.put("controllerId", controllerId);
                            }
                            if (controllerStatus != null && !controllerStatus.isBlank()) {
                                playerJson.put("controllerStatus", controllerStatus);
                            }
                            players.add(playerJson);
                        }
                        resultHandler.handle(Future.succeededFuture(players));
                    } else {
                        resultHandler.handle(Future.failedFuture(ar.cause()));
                    }
                });
    }

    /** Fügt Spieler zur aktuellen Lobby hinzu (Benutzer + Controller erforderlich). */
    public void addPlayerToLobby(String username, Handler<AsyncResult<Void>> resultHandler) {
        String sqlLobby = "SELECT id FROM game_sessions WHERE state = 'LOBBY' ORDER BY id DESC LIMIT 1";
        jdbcPool.preparedQuery(sqlLobby)
                .execute(Tuple.tuple(), lobbyAr -> {
                    if (lobbyAr.failed()) {
                        resultHandler.handle(Future.failedFuture(lobbyAr.cause()));
                        return;
                    }
                    var lobbyRows = lobbyAr.result().iterator();
                    if (!lobbyRows.hasNext()) {
                        resultHandler.handle(Future.failedFuture("Keine Lobby-Session vorhanden"));
                        return;
                    }
                    Long sessionId = lobbyRows.next().getLong("id");

                    jdbcPool.preparedQuery("SELECT COUNT(*) AS cnt FROM game_session_players WHERE game_session_id = ?")
                            .execute(Tuple.of(sessionId), countAr -> {
                                if (countAr.failed()) {
                                    resultHandler.handle(Future.failedFuture(countAr.cause()));
                                    return;
                                }
                                int playerCount = countAr.result().iterator().hasNext()
                                        ? countAr.result().iterator().next().getInteger("cnt")
                                        : 0;
                                if (playerCount >= 99) {
                                    resultHandler.handle(Future.failedFuture("Lobby ist voll (max. 99 Spieler)."));
                                    return;
                                }
                                doAddPlayerToLobby(sessionId, username, resultHandler);
                            });
                });
    }

    /** Fügt Spieler intern zur Lobby hinzu (Benutzer- und Controller-ID erforderlich). */
    private void doAddPlayerToLobby(Long sessionId, String username, Handler<AsyncResult<Void>> resultHandler) {
        jdbcPool.preparedQuery("SELECT id FROM users WHERE username = ? LIMIT 1")
                .execute(Tuple.of(username), userAr -> {
                    if (userAr.failed()) {
                        resultHandler.handle(Future.failedFuture(userAr.cause()));
                        return;
                    }
                    var userRows = userAr.result().iterator();
                    if (!userRows.hasNext()) {
                        resultHandler.handle(Future.failedFuture("Benutzer nicht gefunden"));
                        return;
                    }
                    Long userId = userRows.next().getLong("id");

                    jdbcPool.preparedQuery("SELECT id FROM controllers WHERE assigned_user_id = ? AND status = 'ASSIGNED' LIMIT 1")
                            .execute(Tuple.of(userId), controllerAr -> {
                                if (controllerAr.failed()) {
                                    resultHandler.handle(Future.failedFuture(controllerAr.cause()));
                                    return;
                                }
                                var controllerRows = controllerAr.result().iterator();
                                if (!controllerRows.hasNext()) {
                                    resultHandler.handle(Future.failedFuture("Kein Controller mit diesem Benutzer verbunden"));
                                    return;
                                }
                                Long controllerId = controllerRows.next().getLong("id");

                                jdbcPool.preparedQuery("SELECT 1 FROM game_session_players WHERE game_session_id = ? AND user_id = ? LIMIT 1")
                                        .execute(Tuple.of(sessionId, userId), checkAr -> {
                                            if (checkAr.failed()) {
                                                resultHandler.handle(Future.failedFuture(checkAr.cause()));
                                                return;
                                            }
                                            if (checkAr.result().iterator().hasNext()) {
                                                resultHandler.handle(Future.failedFuture("Du bist bereits in der Lobby."));
                                                return;
                                            }

                                            jdbcPool.preparedQuery("SELECT COUNT(*) AS cnt FROM game_session_players WHERE game_session_id = ?")
                                                    .execute(Tuple.of(sessionId), finalCountAr -> {
                                                        if (finalCountAr.failed()) {
                                                            resultHandler.handle(Future.failedFuture(finalCountAr.cause()));
                                                            return;
                                                        }
                                                        int cnt = finalCountAr.result().iterator().hasNext()
                                                                ? finalCountAr.result().iterator().next().getInteger("cnt") : 0;
                                                        if (cnt >= 99) {
                                                            resultHandler.handle(Future.failedFuture("Lobby ist voll (max. 99 Spieler)."));
                                                            return;
                                                        }
                                                        String sql = "INSERT INTO game_session_players (game_session_id, user_id, controller_id, is_ready) VALUES (?, ?, ?, 0)";
                                                        jdbcPool.preparedQuery(sql)
                                                                .execute(Tuple.of(sessionId, userId, controllerId), insertAr -> {
                                                                    if (insertAr.succeeded()) {
                                                                        resultHandler.handle(Future.succeededFuture());
                                                                    } else {
                                                                        resultHandler.handle(Future.failedFuture(insertAr.cause()));
                                                                    }
                                                                });
                                                    });
                                        });
                            });
                });
    }

    /** Entfernt Spieler aus der aktuellen Lobby. */
    public void removePlayerFromLobby(String username, Handler<AsyncResult<Void>> resultHandler) {
        String sql = "DELETE gsp FROM game_session_players gsp " +
                "JOIN game_sessions gs ON gs.id = gsp.game_session_id " +
                "JOIN users u ON u.id = gsp.user_id " +
                "WHERE gs.state IN ('LOBBY', 'COUNTDOWN') " +
                "AND gs.id = (SELECT id FROM game_sessions WHERE state IN ('LOBBY', 'COUNTDOWN') ORDER BY id DESC LIMIT 1) " +
                "AND u.username = ?";

        jdbcPool.preparedQuery(sql)
                .execute(Tuple.of(username), ar -> {
                    if (ar.failed()) {
                        resultHandler.handle(Future.failedFuture(ar.cause()));
                        return;
                    }
                    if (ar.result().rowCount() == 0) {
                        resultHandler.handle(Future.failedFuture("Spieler nicht in der aktuellen Lobby gefunden."));
                        return;
                    }
                    resultHandler.handle(Future.succeededFuture());
                });
    }

    /** Aktualisiert ready-Status eines Spielers. */
    public void updatePlayerReady(String playerId, boolean ready, Handler<AsyncResult<Void>> resultHandler) {
        String sql = "UPDATE game_session_players gsp " +
                "JOIN game_sessions gs ON gs.id = gsp.game_session_id " +
                "JOIN users u ON u.id = gsp.user_id " +
                "SET gsp.is_ready = ? " +
                "WHERE gs.state IN ('LOBBY', 'COUNTDOWN') AND gs.id = (SELECT id FROM game_sessions WHERE state IN ('LOBBY', 'COUNTDOWN') ORDER BY id DESC LIMIT 1) AND u.username = ?";

        jdbcPool.preparedQuery(sql)
                .execute(Tuple.of(ready, playerId), ar -> {
                    if (ar.failed()) {
                        resultHandler.handle(Future.failedFuture(ar.cause()));
                        return;
                    }
                    if (ar.result().rowCount() == 0) {
                        resultHandler.handle(Future.failedFuture("Spieler nicht in der Lobby gefunden."));
                        return;
                    }
                    resultHandler.handle(Future.succeededFuture());
                });
    }

    /** Liefert Status eines Spielers in der Lobby. */
    public void fetchPlayerStatus(String playerId, Handler<AsyncResult<JsonObject>> resultHandler) {
        String sql = "SELECT u.username, gsp.is_ready " +
                "FROM game_session_players gsp " +
                "JOIN game_sessions gs ON gs.id = gsp.game_session_id " +
                "JOIN users u ON u.id = gsp.user_id " +
                "WHERE gs.state = 'LOBBY' AND gs.id = (SELECT MAX(id) FROM game_sessions WHERE state = 'LOBBY') AND u.username = ? " +
                "LIMIT 1";

        jdbcPool.preparedQuery(sql)
                .execute(Tuple.of(playerId), ar -> {
                    if (ar.failed()) {
                        resultHandler.handle(Future.failedFuture(ar.cause()));
                        return;
                    }
                    RowSet<Row> rows = ar.result();
                    if (!rows.iterator().hasNext()) {
                        resultHandler.handle(Future.failedFuture("Spieler nicht in der Lobby gefunden."));
                        return;
                    }
                    Row row = rows.iterator().next();
                    JsonObject status = new JsonObject()
                            .put("playerId", row.getString("username"))
                            .put("ready", Boolean.TRUE.equals(row.getBoolean("is_ready")));
                    resultHandler.handle(Future.succeededFuture(status));
                });
    }
}
