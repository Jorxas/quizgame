package com.example.controllers;

/**
 * Controllers-Service – Verwaltung verfügbarer Controller, Disconnect.
 */
import com.example.lobby.LobbyService;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class ControllersService {

    private final ControllersRepository controllersRepository;
    private final LobbyService lobbyService;
    private final Vertx vertx;

    public ControllersService() {
        this.controllersRepository = new ControllersRepository();
        this.lobbyService = new LobbyService();
        this.vertx = null;
    }

    public ControllersService(Vertx vertx) {
        this.controllersRepository = new ControllersRepository();
        this.lobbyService = new LobbyService();
        this.vertx = vertx;
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

    /** Meldet Controller ab (OFFLINE, unbind), entfernt Spieler aus Lobby, benachrichtigt EventBus. */
    public void disconnectController(String controllerId, Handler<AsyncResult<Void>> resultHandler) {
        controllersRepository.fetchControllerPlayerStatus(controllerId, statusAr -> {
            if (statusAr.failed()) {
                controllersRepository.disconnectController(controllerId, ar -> {
                    if (vertx != null) vertx.eventBus().publish("lobby.updated", "");
                    resultHandler.handle(ar);
                });
                return;
            }
            String username = statusAr.result().getString("username");
            controllersRepository.disconnectController(controllerId, disconnectAr -> {
                if (disconnectAr.failed()) {
                    resultHandler.handle(disconnectAr);
                    return;
                }
                if (username != null && !username.isBlank()) {
                    lobbyService.removePlayerFromLobby(username, removeAr -> {
                        if (vertx != null) vertx.eventBus().publish("lobby.updated", "");
                        resultHandler.handle(Future.succeededFuture());
                    });
                } else {
                    if (vertx != null) vertx.eventBus().publish("lobby.updated", "");
                    resultHandler.handle(Future.succeededFuture());
                }
            });
        });
    }
}
