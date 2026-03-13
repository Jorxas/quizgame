package com.example.auth;

import com.example.database.DatabaseClient;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.jdbcclient.JDBCPool;
import io.vertx.sqlclient.Tuple;

public class AuthRepository {

    private final JDBCPool jdbcPool;

    public AuthRepository() {
        this.jdbcPool = DatabaseClient.getInstance();
    }

    /** Fügt Benutzer in DB ein (Passwort SHA256). */
    public void insertUser(String username, String password, Handler<AsyncResult<Void>> resultHandler) {
        String sql = "INSERT INTO users (username, password_hash) VALUES (?, SHA2(?, 256))";

        jdbcPool.preparedQuery(sql)
                .execute(Tuple.of(username, password), ar -> {
                    if (ar.succeeded()) {
                        resultHandler.handle(Future.succeededFuture());
                    } else {
                        resultHandler.handle(Future.failedFuture(ar.cause()));
                    }
                });
    }
}
