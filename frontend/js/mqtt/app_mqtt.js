/**
 * Quiz-Plattform – MQTT-Client für Echtzeit-Updates
 * Verbindung zum Broker, Abonnements: game/state, countdown, question, evaluation, ended, lobby/status
 */

(function () {
  var mqttClient = null;
  var gameTimerInterval = null;

  function escapeHtml(text) {
    var div = document.createElement("div");
    div.textContent = text || "";
    return div.innerHTML;
  }

  function setGameActive(active) {
    document.querySelectorAll("#mainTabs .tab, #bottomNav .tab").forEach(function (tab) {
      tab.disabled = active;
    });
    document.querySelectorAll(".panel.left, .main-section[data-main-section=\"lobby\"]").forEach(function (el) {
      el.classList.toggle("is-locked", active);
    });
  }

  function handleGameState(state) {
    var gameCountdownOverlay = document.getElementById("gameCountdownOverlay");
    if (state === "LOBBY") {
      setGameActive(false);
      if (gameCountdownOverlay) gameCountdownOverlay.style.display = "none";
      if (window.loadLobbyPlayers) window.loadLobbyPlayers();
      var activeSection = document.querySelector(".main-section.is-active");
      if (activeSection && activeSection.getAttribute("data-main-section") !== "auswertung") {
        if (window.showMainSection) window.showMainSection("lobby");
      }
      return;
    }
    if (state === "COUNTDOWN" || state === "QUESTION") {
      setGameActive(true);
      if (window.showMainSection) window.showMainSection("spiel");
    }
    if (state === "EVALUATION") {
      setGameActive(true);
      if (window.showMainSection) window.showMainSection("auswertung");
    }
    if (state === "ENDED") {
      setGameActive(false);
      if (window.showMainSection) window.showMainSection("highscores");
    }
  }

  function handleCountdown(tick) {
    var gameCountdownOverlay = document.getElementById("gameCountdownOverlay");
    var gameCountdownNumber = document.getElementById("gameCountdownNumber");
    var answerGrid = document.getElementById("answerGrid");

    setGameActive(true);
    if (window.showMainSection) window.showMainSection("spiel");

    if (gameCountdownOverlay) gameCountdownOverlay.style.display = "flex";
    if (gameCountdownNumber) gameCountdownNumber.textContent = tick;
    if (answerGrid) answerGrid.style.display = "none";

    if (tick <= 0 && gameCountdownOverlay) {
      gameCountdownOverlay.style.display = "none";
    }
  }

  function handleQuestion(data) {
    var gameCountdownOverlay = document.getElementById("gameCountdownOverlay");
    var gameQuestionCounterDisplay = document.getElementById("gameQuestionCounterDisplay");
    var gameContent = document.getElementById("gameContent");
    var answerGrid = document.getElementById("answerGrid");
    var gameTimer = document.getElementById("gameTimer");
    var options, timeLeft;

    if (gameCountdownOverlay) gameCountdownOverlay.style.display = "none";
    if (gameQuestionCounterDisplay) gameQuestionCounterDisplay.textContent = data.questionIndex + "/" + data.totalQuestions;

    if (gameContent) {
      gameContent.innerHTML = "<h3>" + escapeHtml(data.text) + "</h3><span class=\"badge\">" + escapeHtml(data.difficulty || "") + "</span>";
    }

    if (answerGrid) {
      options = data.options || {};
      answerGrid.style.display = "grid";
      answerGrid.classList.remove("is-locked");

      answerGrid.querySelectorAll(".answer").forEach(function (button) {
        var letter = button.getAttribute("data-option");
        button.textContent = letter + ": " + (options[letter] || "");
        button.classList.remove("is-selected", "is-correct", "is-wrong");
      });
    }

    timeLeft = data.duration || 30;
    if (gameTimer) gameTimer.textContent = timeLeft + "s";
    if (gameTimerInterval) clearInterval(gameTimerInterval);

    gameTimerInterval = setInterval(function () {
      timeLeft--;
      if (gameTimer) gameTimer.textContent = Math.max(timeLeft, 0) + "s";
      if (timeLeft <= 0) clearInterval(gameTimerInterval);
    }, 1000);
  }

  function handleEvaluation(data) {
    var evalQuestionInfo = document.getElementById("evalQuestionInfo");
    var evalList = document.getElementById("evaluationList");
    var evalRankingsBody = document.getElementById("evalRankingsBody");

    if (gameTimerInterval) clearInterval(gameTimerInterval);

    setGameActive(true);
    if (window.showMainSection) window.showMainSection("auswertung");

    if (evalQuestionInfo) {
      evalQuestionInfo.textContent = "Frage " + data.questionIndex + "/" + data.totalQuestions + " — Richtige Antwort: " + (data.correctOption || "");
    }

    if (evalList) {
      evalList.innerHTML = "";
      (data.questionResults || []).forEach(function (result) {
        var row = document.createElement("div");
        var name = document.createElement("span");
        var tag = document.createElement("span");

        row.className = "result-row";
        name.textContent = result.username;
        tag.className = "tag " + (result.correct ? "ok" : "warn");
        tag.textContent = result.correct ? "+" + formatScoreExact(result.points) + " Pkt" : "Falsch";

        row.appendChild(name);
        row.appendChild(tag);
        evalList.appendChild(row);
      });
    }

    if (evalRankingsBody) {
      evalRankingsBody.innerHTML = "";
      (data.rankings || []).forEach(function (ranking, index) {
        var tr = document.createElement("tr");

        if (index === 0) tr.className = "rank-1";
        if (index === 1) tr.className = "rank-2";
        if (index === 2) tr.className = "rank-3";

        tr.innerHTML = "<td>" + (index + 1) + "</td><td>" + escapeHtml(ranking.username) + "</td><td>" + formatScoreExact(ranking.totalPoints) + "</td>";
        evalRankingsBody.appendChild(tr);
      });
    }
  }

  function handleGameEnded() {
    var gameMode = window.getSelectedCount ? window.getSelectedCount() : 5;

    if (gameTimerInterval) clearInterval(gameTimerInterval);
    setGameActive(false);

    document.querySelectorAll(".score-tab").forEach(function (tab) {
      tab.classList.toggle("is-active", parseInt(tab.getAttribute("data-mode"), 10) === gameMode);
    });

    if (window.showMainSection) window.showMainSection("auswertung");
  }

  function connectFrontendMqtt() {
    var env, prefix, host, port, brokerUrl;

    if (typeof mqtt === "undefined" || !window.__ENV__) return;

    env = window.__ENV__;
    prefix = (env.MQTT_MESSAGE_PREFIX || "group-16/").replace(/\/?$/, "/");
    host = env.MQTT_BROKER_URL || env.MQTT_WS_HOST || window.location.hostname;
    if (host === "mosquitto") host = "localhost";
    if (!host) host = "localhost";
    port = env.MQTT_BROKER_PORT || "9001";
    brokerUrl = "ws://" + host + ":" + port;

    mqttClient = mqtt.connect(brokerUrl, {
      username: env.MQTT_USERNAME || "",
      password: env.MQTT_PASSWORD || "",
      reconnectPeriod: 5000
    });

    mqttClient.on("connect", function () {
      mqttClient.subscribe(prefix + "game/state");
      mqttClient.subscribe(prefix + "game/countdown");
      mqttClient.subscribe(prefix + "game/question");
      mqttClient.subscribe(prefix + "game/evaluation");
      mqttClient.subscribe(prefix + "game/ended");
      mqttClient.subscribe(prefix + "game/lobby/status");
    });

    mqttClient.on("message", function (topic, message) {
      var text = message ? message.toString() : "";
      var data;

      try {
        data = JSON.parse(text);
      } catch (error) {
        return;
      }

      if (topic === prefix + "game/state") handleGameState(data.state);
      if (topic === prefix + "game/countdown") handleCountdown(data.tick);
      if (topic === prefix + "game/question") handleQuestion(data);
      if (topic === prefix + "game/evaluation") handleEvaluation(data);
      if (topic === prefix + "game/ended") handleGameEnded();
      if (topic === prefix + "game/lobby/status" && data.players && window.applyLobbyPlayersData) {
        window.applyLobbyPlayersData(data.players);
      }
    });

    mqttClient.on("error", function (error) {
      console.error("MQTT error:", error);
    });
  }

  document.addEventListener("DOMContentLoaded", function () {
    connectFrontendMqtt();
  });
})();

