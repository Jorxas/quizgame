package com.example.lobby;

import com.example.database.DatabaseClient;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.jdbcclient.JDBCPool;
import io.vertx.sqlclient.Tuple;

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
}
