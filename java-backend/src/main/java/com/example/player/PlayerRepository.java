package com.example.player;

/**
 * Player-Repository – DB-Zugriff für Controller-Bindung (assigned_user_id).
 */
import com.example.database.DatabaseClient;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.jdbcclient.JDBCPool;
import io.vertx.sqlclient.Tuple;

public class PlayerRepository {

    private final JDBCPool jdbcPool;

    public PlayerRepository() {
        this.jdbcPool = DatabaseClient.getInstance();
    }

    /** Bindet Controller an Spieler (assigned_user_id, status ASSIGNED). */
    public void bindControllerToPlayer(String username, String controllerId, String controllerType,
                                       Handler<AsyncResult<Void>> resultHandler) {
        String sql = "UPDATE controllers SET assigned_user_id = (SELECT id FROM users WHERE username = ? LIMIT 1), status = 'ASSIGNED' " +
                "WHERE controller_id = ? AND controller_type = ? AND status = 'FREE'";

        jdbcPool.preparedQuery(sql)
                .execute(Tuple.of(username, controllerId, controllerType))
                .onComplete(ar -> {
                    if (ar.failed()) {
                        resultHandler.handle(Future.failedFuture(ar.cause()));
                        return;
                    }
                    if (ar.result().rowCount() == 0) {
                        resultHandler.handle(Future.failedFuture("Controller nicht gefunden oder nicht verfügbar."));
                        return;
                    }
                    resultHandler.handle(Future.succeededFuture());
                });
    }
}
