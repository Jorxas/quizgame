package com.example.lobby;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

public class LobbyService {

    private final LobbyRepository lobbyRepository;

    public LobbyService() {
        this.lobbyRepository = new LobbyRepository();
    }

    /** Prüft, ob Benutzer bereits in der aktuellen Lobby ist. */
    public void isUserInCurrentLobby(String username, Handler<AsyncResult<Boolean>> resultHandler) {
        lobbyRepository.isUserInCurrentLobby(username, resultHandler);
    }
}
