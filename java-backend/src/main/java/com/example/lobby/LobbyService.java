package com.example.lobby;

import com.example.controllers.ControllersRepository;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class LobbyService {

    private final LobbyRepository lobbyRepository;
    private final ControllersRepository controllersRepository;

    public LobbyService() {
        this.lobbyRepository = new LobbyRepository();
        this.controllersRepository = new ControllersRepository();
    }

    /** Prüft, ob Benutzer bereits in der aktuellen Lobby ist. */
    public void isUserInCurrentLobby(String username, Handler<AsyncResult<Boolean>> resultHandler) {
        lobbyRepository.isUserInCurrentLobby(username, resultHandler);
    }

    /** Erstellt neue Lobby-Session. */
    public void createLobbySession(Handler<AsyncResult<Void>> resultHandler) {
        lobbyRepository.createNewLobbySession(resultHandler);
    }

    /** Liefert Spieler-Liste der aktuellen Lobby. */
    public void getLobbyStatus(Handler<AsyncResult<JsonArray>> resultHandler) {
        lobbyRepository.fetchPlayersWithStatus(resultHandler);
    }

    /** Fügt Spieler zur Lobby hinzu. */
    public void addPlayerToLobby(String username, Handler<AsyncResult<Void>> resultHandler) {
        lobbyRepository.addPlayerToLobby(username, resultHandler);
    }

    public void removePlayerFromLobby(String username, Handler<AsyncResult<Void>> resultHandler) {
        controllersRepository.unbindControllerForUser(username, unused -> {
            lobbyRepository.removePlayerFromLobby(username, resultHandler);
        });
    }

    public void updatePlayerReady(String playerId, boolean ready, Handler<AsyncResult<Void>> resultHandler) {
        lobbyRepository.updatePlayerReady(playerId, ready, resultHandler);
    }

    /** Liefert Status eines Spielers in der Lobby. */
    public void getPlayerStatus(String playerId, Handler<AsyncResult<JsonObject>> resultHandler) {
        lobbyRepository.fetchPlayerStatus(playerId, resultHandler);
    }
}
