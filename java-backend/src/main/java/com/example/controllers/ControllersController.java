package com.example.controllers;

import com.example.http.HttpController;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class ControllersController implements HttpController {

    private final ControllersService controllersService;

    public ControllersController(Vertx vertx) {
        this.controllersService = new ControllersService();
    }

    /** Registriert die Controller-Routen. */
    @Override
    public void registerRoutes(Router router) {
        router.get("/api/controllers/available").handler(this::handleAvailableControllers);
        router.post("/api/controllers/create-web").handler(this::handleCreateWebController);
        router.get("/api/controllers/:controllerId/player-status").handler(this::handleControllerPlayerStatus);
    }

    /** Liefert Liste verfügbarer Controller (GET /api/controllers/available). */
    private void handleAvailableControllers(RoutingContext ctx) {
        controllersService.getAvailableControllers(ar -> {
            if (ar.succeeded()) {
                JsonArray controllers = ar.result();
                JsonObject response = new JsonObject().put("controllers", controllers);
                ctx.response()
                        .putHeader("content-type", "application/json")
                        .setStatusCode(200)
                        .end(response.encode());
            } else {
                ctx.response().setStatusCode(500).end();
            }
        });
    }

    /** Erstellt neuen Web-Controller (POST /api/controllers/create-web). */
    private void handleCreateWebController(RoutingContext ctx) {
        controllersService.createWebController(ar -> {
            if (ar.succeeded()) {
                JsonObject created = ar.result();
                ctx.response()
                        .putHeader("content-type", "application/json")
                        .setStatusCode(201)
                        .end(created.encode());
            } else {
                ctx.response()
                        .putHeader("content-type", "application/json")
                        .setStatusCode(500)
                        .end(new JsonObject().put("error", "Web-Controller konnte nicht erstellt werden.").encode());
            }
        });
    }

    /** Liefert Player-Status für Controller (GET /api/controllers/:controllerId/player-status). */
    private void handleControllerPlayerStatus(RoutingContext ctx) {
        String controllerId = ctx.pathParam("controllerId");
        if (controllerId == null || controllerId.isBlank()) {
            ctx.response()
                    .putHeader("content-type", "application/json")
                    .setStatusCode(400)
                    .end(new JsonObject().put("error", "controllerId erforderlich.").encode());
            return;
        }

        controllersService.getControllerPlayerStatus(controllerId, ar -> {
            if (ar.succeeded()) {
                ctx.response()
                        .putHeader("content-type", "application/json")
                        .setStatusCode(200)
                        .end(ar.result().encode());
            } else {
                String msg = ar.cause() != null ? ar.cause().getMessage() : "Controller-Status konnte nicht abgerufen werden.";
                int statusCode = "Controller not found".equals(msg) ? 404 : 500;
                String errorText = "Controller not found".equals(msg) ? "Controller nicht gefunden." : msg;
                ctx.response()
                        .putHeader("content-type", "application/json")
                        .setStatusCode(statusCode)
                        .end(new JsonObject().put("error", errorText).encode());
            }
        });
    }
}
