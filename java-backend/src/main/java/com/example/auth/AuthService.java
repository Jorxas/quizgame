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
}
