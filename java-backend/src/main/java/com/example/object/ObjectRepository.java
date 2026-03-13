package com.example.object;

import com.example.database.DatabaseClient;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.jdbcclient.JDBCPool;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

public class ObjectRepository {

    private final JDBCPool jdbcPool;

    public ObjectRepository() {
        this.jdbcPool = DatabaseClient.getInstance();
    }

    /** Fügt neues Object in DB ein. */
    public void insertObject(String message, Handler<AsyncResult<Void>> resultHandler) {
        jdbcPool.preparedQuery("INSERT INTO objects (message) VALUES (?)")
                .execute(Tuple.of(message), ar -> {
                    if (ar.succeeded()) {
                        resultHandler.handle(Future.succeededFuture());
                    } else {
                        resultHandler.handle(Future.failedFuture(ar.cause()));
                    }
                });
    }

    /** Liefert alle Objects aus der DB. */
    public void fetchObjects(Handler<AsyncResult<RowSet<io.vertx.sqlclient.Row>>> resultHandler) {
        jdbcPool.query("SELECT * FROM objects")
                .execute(resultHandler);
    }

    /** Aktualisiert Object in DB. */
    public void updateObject(int id, String message, Handler<AsyncResult<Void>> resultHandler) {
        jdbcPool.preparedQuery("UPDATE objects SET message = ? WHERE id = ?")
                .execute(Tuple.of(message, id), ar -> {
                    if (ar.succeeded()) {
                        resultHandler.handle(Future.succeededFuture());
                    } else {
                        resultHandler.handle(Future.failedFuture(ar.cause()));
                    }
                });
    }

    /** Löscht Object aus der DB. */
    public void deleteObject(int id, Handler<AsyncResult<Void>> resultHandler) {
        jdbcPool.preparedQuery("DELETE FROM objects WHERE id = ?")
                .execute(Tuple.of(id), ar -> {
                    if (ar.succeeded()) {
                        resultHandler.handle(Future.succeededFuture());
                    } else {
                        resultHandler.handle(Future.failedFuture(ar.cause()));
                    }
                });
    }
}
