package com.example.highscores;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;

import java.util.Set;

public class HighscoresService {

    private static final Set<Integer> ALLOWED_MODES = Set.of(5, 10, 20);
    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private final HighscoresRepository highscoresRepository;

    public HighscoresService() {
        this.highscoresRepository = new HighscoresRepository();
    }

    /** Liefert Highscores für mode (5|10|20), limit optional (default 20, max 100). */
    public void getHighscores(int mode, Integer limitParam, Handler<AsyncResult<JsonArray>> resultHandler) {
        if (!ALLOWED_MODES.contains(mode)) {
            resultHandler.handle(Future.failedFuture(new IllegalArgumentException("mode muss 5, 10 oder 20 sein")));
            return;
        }
        int limit = limitParam != null ? limitParam : DEFAULT_LIMIT;
        if (limit < 1) limit = 1;
        if (limit > MAX_LIMIT) limit = MAX_LIMIT;
        String roundLength = "Q" + mode;
        highscoresRepository.fetchByRoundLength(roundLength, limit, resultHandler);
    }
}
