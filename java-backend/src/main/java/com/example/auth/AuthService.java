package com.example.auth;

/**
 * Auth-Service – Geschäftslogik für Registrierung, Login, RFID-Lookup.
 */
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

public class AuthService {

    private final AuthRepository authRepository;

    public AuthService() {
        this.authRepository = new AuthRepository();
    }

    /** Legt einen neuen Benutzer an. */
    public void register(String username, String password, String rfidUid, Handler<AsyncResult<Void>> resultHandler) {
        authRepository.insertUser(username, password, rfidUid, resultHandler);
    }

    /** Prüft Login und gibt true zurück wenn gültig. */
    public void login(String username, String password, Handler<AsyncResult<Boolean>> resultHandler) {
        authRepository.verifyUser(username, password, resultHandler);
    }

    /** Sucht den Benutzernamen anhand der RFID-Karte. */
    public void lookupByRfid(String rfidUid, Handler<AsyncResult<String>> resultHandler) {
        authRepository.findUsernameByRfidUid(rfidUid, resultHandler);
    }

    /** Liefert die RFID-Karte des angegebenen Benutzers. */
    public void getRfidForUser(String username, Handler<AsyncResult<String>> resultHandler) {
        authRepository.getRfidForUser(username, resultHandler);
    }

    /** Aktualisiert oder entfernt die RFID-Karte des Benutzers. */
    public void updateRfidForUser(String username, String rfidUid, Handler<AsyncResult<Void>> resultHandler) {
        authRepository.updateRfidForUser(username, rfidUid, resultHandler);
    }
}

