package com.example.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;

public class GameStateManager {

    private static final Logger logger = LoggerFactory.getLogger(GameStateManager.class);

    private final List<String> activePlayers = Collections.synchronizedList(new ArrayList<>());
    private final List<String> activeControllers = Collections.synchronizedList(new ArrayList<>());
    private final List<Integer> currentSequence = Collections.synchronizedList(new ArrayList<>());

    private static GameStateManager instance;
    private final EventBus eventBus;

    private GameStateManager(Vertx vertx) {
        this.eventBus = vertx.eventBus();
    }

    public static synchronized GameStateManager getInstance(Vertx vertx) {
        if (instance == null) {
            instance = new GameStateManager(vertx);
        }
        return instance;
    }

}
