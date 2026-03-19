package com.example.auth;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

public class AuthService {

    private final AuthRepository authRepository;

    public AuthService() {
        this.authRepository = new AuthRepository();
    }

    /** Legt einen neuen Benutzer an. */
    public void register(String username, String password, Handler<AsyncResult<Void>> resultHandler) {
        authRepository.insertUser(username, password, resultHandler);
    }

    /** Prüft Login und gibt true zurück wenn gültig. */
    public void login(String username, String password, Handler<AsyncResult<Boolean>> resultHandler) {
        authRepository.verifyUser(username, password, resultHandler);
    }

    public void lookupByRfid(String rfidUid, Handler<AsyncResult<String>> resultHandler) {
        authRepository.findUsernameByRfidUid(rfidUid, resultHandler);
    }

    public void getRfidForUser(String username, Handler<AsyncResult<String>> resultHandler) {
        authRepository.getRfidForUser(username, resultHandler);
    }

    public void updateRfidForUser(String username, String rfidUid, Handler<AsyncResult<Void>> resultHandler) {
        authRepository.updateRfidForUser(username, rfidUid, resultHandler);
    }
}

