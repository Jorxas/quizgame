package com.example.object;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;

public class ObjectService {

    private final ObjectRepository objectRepository;

    public ObjectService() {
        this.objectRepository = new ObjectRepository();
    }

    /** Erstellt neues Object. */
    public void createObject(String message, Handler<AsyncResult<Void>> resultHandler) {
        objectRepository.insertObject(message, resultHandler);
    }

    /** Liefert alle Objects. */
    public void readObjects(Handler<AsyncResult<JsonArray>> resultHandler) {
        objectRepository.fetchObjects(ar -> {
            if (ar.succeeded()) {
                RowSet<Row> resultSet = ar.result();
                JsonArray jsonArray = new JsonArray();
                resultSet.forEach(row -> {
                    JsonObject jsonObject = new JsonObject()
                            .put("id", row.getInteger("id"))
                            .put("message", row.getString("message"))
                            .put("created_at", row.getTemporal("created_at").toString());
                    jsonArray.add(jsonObject);
                });
                resultHandler.handle(Future.succeededFuture(jsonArray));
            } else {
                resultHandler.handle(Future.failedFuture(ar.cause()));
            }
        });
    }

    /** Aktualisiert Object. */
    public void updateObject(int id, String message, Handler<AsyncResult<Void>> resultHandler) {
        objectRepository.updateObject(id, message, resultHandler);
    }

    /** Löscht Object. */
    public void deleteObject(int id, Handler<AsyncResult<Void>> resultHandler) {
        objectRepository.deleteObject(id, resultHandler);
    }
}
