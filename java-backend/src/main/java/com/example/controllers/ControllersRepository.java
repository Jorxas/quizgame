package com.example.controllers;

import com.example.database.DatabaseClient;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.jdbcclient.JDBCPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

import java.util.UUID;

public class ControllersRepository {

    private final JDBCPool jdbcPool;

    public ControllersRepository() {
        this.jdbcPool = DatabaseClient.getInstance();
    }

    /** Liefert alle Controller mit Status FREE. */
    public void fetchAvailableControllers(Handler<AsyncResult<JsonArray>> resultHandler) {
        String sql = "SELECT c.controller_id, c.controller_type, c.status " +
                "FROM controllers c " +
                "WHERE c.status = 'FREE'";

        jdbcPool.preparedQuery(sql)
                .execute(Tuple.tuple(), ar -> {
                    if (ar.succeeded()) {
                        RowSet<Row> rows = ar.result();
                        JsonArray controllers = new JsonArray();
                        for (Row row : rows) {
                            String controllerId = row.getString("controller_id");
                            String type = row.getString("controller_type");
                            JsonObject json = new JsonObject()
                                    .put("controllerId", controllerId)
                                    .put("type", type)
                                    .put("status", "FREE");
                            controllers.add(json);
                        }
                        resultHandler.handle(Future.succeededFuture(controllers));
                    } else {
                        resultHandler.handle(Future.failedFuture(ar.cause()));
                    }
                });
    }

    /** Erstellt neuen Web-Controller mit eindeutiger ID (WEB-xxx), Status FREE. */
    public void insertWebController(Handler<AsyncResult<JsonObject>> resultHandler) {
        String controllerId = "WEB-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String sql = "INSERT INTO controllers (controller_id, controller_type, status) VALUES (?, 'WEB', 'FREE')";

        jdbcPool.preparedQuery(sql)
                .execute(Tuple.of(controllerId), ar -> {
                    if (ar.succeeded()) {
                        JsonObject created = new JsonObject()
                                .put("controllerId", controllerId)
                                .put("type", "WEB")
                                .put("status", "FREE");
                        resultHandler.handle(Future.succeededFuture(created));
                    } else {
                        resultHandler.handle(Future.failedFuture(ar.cause()));
                    }
                });
    }

    /** Liefert Player-Status für einen Controller (username, ready). */
    public void fetchControllerPlayerStatus(String controllerId, Handler<AsyncResult<JsonObject>> resultHandler) {
        String sql = "SELECT u.username AS player_id, " +
                "COALESCE((SELECT gsp.is_ready " +
                "FROM game_session_players gsp " +
                "JOIN game_sessions gs ON gs.id = gsp.game_session_id " +
                "WHERE gsp.controller_id = c.id AND gs.state = 'LOBBY' AND gs.id = (SELECT MAX(id) FROM game_sessions WHERE state = 'LOBBY') " +
                "LIMIT 1), 0) AS is_ready " +
                "FROM controllers c " +
                "LEFT JOIN users u ON u.id = c.assigned_user_id " +
                "WHERE c.controller_id = ? " +
                "LIMIT 1";

        jdbcPool.preparedQuery(sql)
                .execute(Tuple.of(controllerId), ar -> {
                    if (ar.failed()) {
                        resultHandler.handle(Future.failedFuture(ar.cause()));
                        return;
                    }
                    try {
                        RowSet<Row> rows = ar.result();
                        if (!rows.iterator().hasNext()) {
                            resultHandler.handle(Future.failedFuture("Controller not found"));
                            return;
                        }
                        Row row = rows.iterator().next();
                        String playerId = row.getString("player_id");
                        Integer isReadyVal = row.getInteger("is_ready");
                        boolean ready = isReadyVal != null && isReadyVal == 1;
                        JsonObject status = new JsonObject()
                                .put("controllerId", controllerId)
                                .put("playerId", playerId)
                                .put("username", playerId)
                                .put("ready", ready);
                        resultHandler.handle(Future.succeededFuture(status));
                    } catch (Exception e) {
                        resultHandler.handle(Future.failedFuture(e));
                    }
                });
    }

    /**
     * Creates a controller if it does not exist (ID sent by web-controller via MQTT register).
     * Uses INSERT ... ON DUPLICATE KEY UPDATE to set last_seen_at.
     */
    public void createControllerIfNotExists(String controllerId, Handler<AsyncResult<Void>> resultHandler) {
        String sql = "INSERT INTO controllers (controller_id, controller_type, status) VALUES (?, 'WEB', 'FREE') " +
                "ON DUPLICATE KEY UPDATE last_seen_at = CURRENT_TIMESTAMP";
        jdbcPool.preparedQuery(sql)
                .execute(Tuple.of(controllerId), ar -> {
                    if (ar.succeeded()) {
                        resultHandler.handle(Future.succeededFuture());
                    } else {
                        resultHandler.handle(Future.failedFuture(ar.cause()));
                    }
                });
    }

    /** Updates last_seen_at for the given controller_id (e.g. on MQTT register or pong). */
    public void updateLastSeen(String controllerId, Handler<AsyncResult<Void>> resultHandler) {
        String sql = "UPDATE controllers SET last_seen_at = CURRENT_TIMESTAMP WHERE controller_id = ?";
        jdbcPool.preparedQuery(sql)
                .execute(Tuple.of(controllerId), ar -> {
                    if (ar.succeeded()) {
                        resultHandler.handle(Future.succeededFuture());
                    } else {
                        resultHandler.handle(Future.failedFuture(ar.cause()));
                    }
                });
    }

    /** Sets status (e.g. OFFLINE) for the given controller_id. */
    public void updateStatus(String controllerId, String status, Handler<AsyncResult<Void>> resultHandler) {
        String sql = "UPDATE controllers SET status = ? WHERE controller_id = ?";
        jdbcPool.preparedQuery(sql)
                .execute(Tuple.of(status, controllerId), ar -> {
                    if (ar.succeeded()) {
                        resultHandler.handle(Future.succeededFuture());
                    } else {
                        resultHandler.handle(Future.failedFuture(ar.cause()));
                    }
                });
    }

    /** Hebt die Spielerzuordnung auf und setzt Controller auf FREE (z.B. bei Lobby-Austritt). */
    public void unbindControllerForUser(String username, Handler<AsyncResult<Void>> resultHandler) {
        String sql = "UPDATE controllers SET status = 'FREE', assigned_user_id = NULL WHERE assigned_user_id = (SELECT id FROM users WHERE username = ? LIMIT 1)";
        jdbcPool.preparedQuery(sql).execute(Tuple.of(username), ar -> {
            if (ar.succeeded()) resultHandler.handle(Future.succeededFuture());
            else resultHandler.handle(Future.failedFuture(ar.cause()));
        });
    }

    /** Setzt Controller auf OFFLINE und hebt die Spielerzuordnung auf. */
    public void disconnectController(String controllerId, Handler<AsyncResult<Void>> resultHandler) {
        String sql = "UPDATE controllers SET status = 'OFFLINE', assigned_user_id = NULL WHERE controller_id = ?";
        jdbcPool.preparedQuery(sql)
                .execute(Tuple.of(controllerId), ar -> {
                    if (ar.succeeded()) {
                        resultHandler.handle(Future.succeededFuture());
                    } else {
                        resultHandler.handle(Future.failedFuture(ar.cause()));
                    }
                });
    }
}
