package com.example.player;

/**
 * Player-Service – Controller-Bindung an Benutzer.
 */
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

public class PlayerService {

    private final PlayerRepository playerRepository;

    public PlayerService() {
        this.playerRepository = new PlayerRepository();
    }

    /** Bindet Controller an Spieler. */
    public void bindControllerToPlayer(String username, String controllerId, String controllerType,
                                       Handler<AsyncResult<Void>> resultHandler) {
        playerRepository.bindControllerToPlayer(username, controllerId, controllerType, resultHandler);
    }
}
