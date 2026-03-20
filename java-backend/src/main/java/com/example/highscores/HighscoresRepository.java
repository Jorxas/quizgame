package com.example.highscores;

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

public class HighscoresRepository {

    private final JDBCPool jdbcPool;

    public HighscoresRepository() {
        this.jdbcPool = DatabaseClient.getInstance();
    }

    /** Liefert Highscores für Rundenlänge (Q5, Q10, Q20), sortiert nach Punkten. */
    public void fetchByRoundLength(String roundLength, int limit, Handler<AsyncResult<JsonArray>> resultHandler) {
        String sql = "SELECT u.username, h.total_points AS score, h.created_at " +
                "FROM highscores h JOIN users u ON u.id = h.user_id " +
                "WHERE h.round_length = ? " +
                "ORDER BY h.total_points DESC, h.created_at ASC LIMIT ?";
        jdbcPool.preparedQuery(sql).execute(Tuple.of(roundLength, limit), ar -> {
            if (ar.succeeded()) {
                RowSet<Row> rows = ar.result();
                JsonArray list = new JsonArray();
                for (Row row : rows) {
                    list.add(new JsonObject()
                            .put("username", row.getString("username"))
                            .put("score", row.getValue("score") != null ? ((Number) row.getValue("score")).doubleValue() : 0.0)
                            .put("created_at", row.getTemporal("created_at") != null ? row.getTemporal("created_at").toString() : null));
                }
                resultHandler.handle(Future.succeededFuture(list));
            } else {
                resultHandler.handle(Future.failedFuture(ar.cause()));
            }
        });
    }
}
