package com.example.auth;

import com.example.database.DatabaseClient;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.jdbcclient.JDBCPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import org.mindrot.jbcrypt.BCrypt;

public class AuthRepository {

    private final JDBCPool jdbcPool;

    public AuthRepository() {
        this.jdbcPool = DatabaseClient.getInstance();
    }

    /** Fügt Benutzer in DB ein (Passwort BCrypt). */
    public void insertUser(String username, String password, Handler<AsyncResult<Void>> resultHandler) {
        String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt());
        String sql = "INSERT INTO users (username, password_hash) VALUES (?, ?)";

        jdbcPool.preparedQuery(sql)
                .execute(Tuple.of(username, passwordHash), ar -> {
                    if (ar.succeeded()) {
                        resultHandler.handle(Future.succeededFuture());
                    } else {
                        resultHandler.handle(Future.failedFuture(ar.cause()));
                    }
                });
    }

    /** Prüft Username/Passwort gegen DB, liefert true falls gültig. */
    public void verifyUser(String username, String password, Handler<AsyncResult<Boolean>> resultHandler) {
        String sql = "SELECT password_hash FROM users WHERE username = ?";
        jdbcPool.preparedQuery(sql)
                .execute(Tuple.of(username), ar -> {
                    if (ar.failed()) {
                        resultHandler.handle(Future.failedFuture(ar.cause()));
                        return;
                    }
                    RowSet<Row> rows = ar.result();
                    if (!rows.iterator().hasNext()) {
                        resultHandler.handle(Future.succeededFuture(false));
                        return;
                    }
                    String storedHash = rows.iterator().next().getString("password_hash");
                    boolean valid = BCrypt.checkpw(password, storedHash);
                    resultHandler.handle(Future.succeededFuture(valid));
                });
    }
}
