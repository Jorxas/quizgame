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

    public void insertUser(String username, String password, String rfidUid, Handler<AsyncResult<Void>> resultHandler) {
        String rfid = (rfidUid != null && !rfidUid.isBlank()) ? rfidUid.trim() : null;
        String hash = BCrypt.hashpw(password, BCrypt.gensalt(10));
        String sql = rfid != null
                ? "INSERT INTO users (username, password_hash, rfid_uid) VALUES (?, ?, ?)"
                : "INSERT INTO users (username, password_hash) VALUES (?, ?)";

        io.vertx.sqlclient.Tuple params = rfid != null ? Tuple.of(username, hash, rfid) : Tuple.of(username, hash);
        jdbcPool.preparedQuery(sql)
                .execute(params, ar -> {
                    if (ar.succeeded()) {
                        resultHandler.handle(Future.succeededFuture());
                    } else {
                        resultHandler.handle(Future.failedFuture(ar.cause()));
                    }
                });
    }

    /** Prüft Username/Passwort gegen DB, liefert true falls gültig. */
    public void verifyUser(String username, String password, Handler<AsyncResult<Boolean>> resultHandler) {
        String sql = "SELECT password_hash FROM users WHERE username = ? LIMIT 1";

        jdbcPool.preparedQuery(sql)
                .execute(Tuple.of(username), ar -> {
                    if (ar.succeeded()) {
                        if (!ar.result().iterator().hasNext()) {
                            resultHandler.handle(Future.succeededFuture(false));
                            return;
                        }
                        Row row = ar.result().iterator().next();
                        String storedHash = row.getString("password_hash");
                        boolean valid = storedHash != null && BCrypt.checkpw(password, storedHash);
                        resultHandler.handle(Future.succeededFuture(valid));
                    } else {
                        resultHandler.handle(Future.failedFuture(ar.cause()));
                    }
                });
    }

    public void getRfidForUser(String username, Handler<AsyncResult<String>> resultHandler) {
        String sql = "SELECT rfid_uid FROM users WHERE username = ? LIMIT 1";
        jdbcPool.preparedQuery(sql)
                .execute(Tuple.of(username), ar -> {
                    if (ar.succeeded() && ar.result().iterator().hasNext()) {
                        Row row = ar.result().iterator().next();
                        resultHandler.handle(Future.succeededFuture(row.getString("rfid_uid")));
                    } else {
                        resultHandler.handle(Future.succeededFuture(null));
                    }
                });
    }

    public void updateRfidForUser(String username, String rfidUid, Handler<AsyncResult<Void>> resultHandler) {
        String rfid = (rfidUid != null && !rfidUid.isBlank()) ? rfidUid.trim().replaceAll("\\s+", "").toUpperCase() : null;
        String sql = "UPDATE users SET rfid_uid = ? WHERE username = ?";
        jdbcPool.preparedQuery(sql)
                .execute(Tuple.of(rfid, username), ar -> {
                    if (ar.succeeded()) {
                        resultHandler.handle(Future.succeededFuture());
                    } else {
                        resultHandler.handle(Future.failedFuture(ar.cause()));
                    }
                });
                    }

    public void findUsernameByRfidUid(String rfidUid, Handler<AsyncResult<String>> resultHandler) {
        String sql = "SELECT username FROM users WHERE rfid_uid = ? LIMIT 1";
        jdbcPool.preparedQuery(sql)
                .execute(Tuple.of(rfidUid), ar -> {
                    if (ar.succeeded() && ar.result().iterator().hasNext()) {
                        Row row = ar.result().iterator().next();
                        resultHandler.handle(Future.succeededFuture(row.getString("username")));
                    } else {
                        resultHandler.handle(Future.succeededFuture(null));
                    }
                });
    }
}

