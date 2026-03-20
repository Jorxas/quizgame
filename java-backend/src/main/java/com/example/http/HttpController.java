package com.example.http;

/**
 * HttpController – Interface für REST-Controller (registerRoutes).
 */
import io.vertx.ext.web.Router;

public interface HttpController {
    public void registerRoutes(Router router);
}
