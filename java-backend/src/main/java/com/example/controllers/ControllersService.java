package com.example.controllers;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class ControllersService {

    private final ControllersRepository controllersRepository;

    public ControllersService() {
        this.controllersRepository = new ControllersRepository();
    }

    /** Liefert verfügbare Controller (Status FREE). */
    public void getAvailableControllers(Handler<AsyncResult<JsonArray>> resultHandler) {
        controllersRepository.fetchAvailableControllers(resultHandler);
    }

    /** Erstellt neuen Web-Controller in der DB. */
    public void createWebController(Handler<AsyncResult<JsonObject>> resultHandler) {
        controllersRepository.insertWebController(resultHandler);
    }

    /** Liefert Player-Status für einen Controller. */
    public void getControllerPlayerStatus(String controllerId, Handler<AsyncResult<JsonObject>> resultHandler) {
        controllersRepository.fetchControllerPlayerStatus(controllerId, resultHandler);
    }
}
