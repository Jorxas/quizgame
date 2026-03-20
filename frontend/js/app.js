/** Zeigt ein modales Nachrichtenfenster (Info/Fehler) mit OK-Button. */
function showMessage(text, type) {
  var overlay = document.createElement("div");
  var box = document.createElement("div");
  var textNode = document.createElement("p");
  var button = document.createElement("button");

  overlay.className = "app-message-overlay";
  box.className = "app-message app-message--" + (type || "info");
  textNode.textContent = text || "";
  textNode.style.margin = "0";
  button.className = "btn primary app-message-ok";
  button.textContent = "OK";

  button.addEventListener("click", function () {
    overlay.remove();
  });

  overlay.addEventListener("click", function (event) {
    if (event.target === overlay) overlay.remove();
  });

  box.appendChild(textNode);
  box.appendChild(button);
  overlay.appendChild(box);
  document.body.appendChild(overlay);
}

window.showMessage = showMessage;

document.addEventListener("DOMContentLoaded", function () {
  var body = document.body;
  var controllerList = document.getElementById("controllerList");
  var questionCounterDisplay = document.getElementById("questionCounterDisplay");
  var gameQuestionCounterDisplay = document.getElementById("gameQuestionCounterDisplay");
  var configGameBtn = document.getElementById("configGameBtn");
  var gameCountdownOverlay = document.getElementById("gameCountdownOverlay");
  var gameCountdownNumber = document.getElementById("gameCountdownNumber");
  var gameContent = document.getElementById("gameContent");
  var answerGrid = document.getElementById("answerGrid");
  var gameTimer = document.getElementById("gameTimer");
  var evalRankingsBody = document.getElementById("evalRankingsBody");
  var evalQuestionInfo = document.getElementById("evalQuestionInfo");

  /** Wechselt zur Registrierungsansicht. */
  function showRegister() {
    body.classList.remove("state-initial", "state-controller");
    body.classList.add("state-register");
  }

  /** Wechselt zurück zur Login-Ansicht. */
  function showLogin() {
    body.classList.remove("state-register", "state-controller");
    body.classList.add("state-initial");
  }

  /** Erstellt die URL zum Web-Controller (optional mit Controller-ID). */
  function getWebControllerUrl(controllerId) {
    var base = window.location.protocol + "//" + window.location.hostname + ":81";
    if (!controllerId) return base + "/controller.html";
    return base + "/controller.html?id=" + encodeURIComponent(controllerId);
  }

  /** Öffnet den Web-Controller in neuem Tab und aktualisiert die Controller-Liste. */
  function openWebController() {
    window.open(getWebControllerUrl(), "_blank");
    if (window.loadAvailableControllers) {
      setTimeout(function () { window.loadAvailableControllers(); }, 1500);
    }
  }

  /** Zeigt die angegebene Hauptsektion (Lobby, Spiel, Auswertung, Highscores) an. */
  function showMainSection(sectionId) {
    document.querySelectorAll(".main-section").forEach(function (section) {
      section.classList.remove("is-active");
    });

    document.querySelectorAll("#mainTabs .tab, #bottomNav .tab").forEach(function (tab) {
      tab.classList.remove("is-active");
    });

    document.querySelectorAll(".main-section[data-main-section=\"" + sectionId + "\"]").forEach(function (section) {
      section.classList.add("is-active");
    });

    document.querySelectorAll(".tab[data-section=\"" + sectionId + "\"]").forEach(function (tab) {
      tab.classList.add("is-active");
    });

    if (sectionId === "highscores" && window.loadHighscores) {
      var activeMode = document.querySelector(".score-tab.is-active");
      window.loadHighscores(activeMode ? parseInt(activeMode.getAttribute("data-mode"), 10) : 5);
    }
  }

  /** Liefert die ausgewählte Fragenanzahl (Modus) aus den Pills. */
  function getSelectedCount() {
    var activePill = document.querySelector("#modePills .pill.is-active");
    if (!activePill) return 5;
    return parseInt(activePill.getAttribute("data-count"), 10) || 5;
  }

  /** Parst die Kategorie-Zahlen (leicht, mittel, schwer) aus dem Anzeigetext. */
  function parseCategoryCount(text) {
    var match = (text || "").match(/\(?\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*\)?/);
    if (!match) return [0, 0, 0];
    return [
      parseInt(match[1], 10) || 0,
      parseInt(match[2], 10) || 0,
      parseInt(match[3], 10) || 0
    ];
  }

  /** Berechnet die verfügbare Fragenanzahl anhand ausgewählter Kategorien und Schwierigkeiten. */
  function getAvailableQuestionCount() {
    var total = 0;
    var difficultyIndex = { leicht: 0, mittel: 1, schwer: 2 };
    var selectedDifficulties = [];

    document.querySelectorAll("#difficultyCheckboxes input[name=difficulty]:checked").forEach(function (checkbox) {
      if (difficultyIndex[checkbox.value] !== undefined) {
        selectedDifficulties.push(difficultyIndex[checkbox.value]);
      }
    });

    document.querySelectorAll("#categoryCheckboxes label").forEach(function (label) {
      var checkbox = label.querySelector("input[name=category]");
      var countElement = label.querySelector(".category-count");
      var counts = parseCategoryCount(countElement ? countElement.textContent : "");

      if (!checkbox || !checkbox.checked) return;
      selectedDifficulties.forEach(function (index) {
        total += counts[index] || 0;
      });
    });

    return total;
  }

  /** Prüft, ob das Spiel gestartet werden kann (Fragen passen, alle Spieler bereit). */
  function canStartGame() {
    if (getAvailableQuestionCount() !== getSelectedCount() || getSelectedCount() <= 0) return false;
    var players = window.lobbyPlayers || [];
    var connected = players.filter(function (p) {
      var ctrlId = p.controllerId || p.controller_id;
      var status = (p.controllerStatus || p.controller_status || "").toString().toUpperCase();
      return ctrlId && status !== "OFFLINE";
    });
    if (connected.length === 0) return false;
    return connected.every(function (p) { return !!p.ready; });
  }

  /** Aktualisiert den Zustand des Spiel-starten-Buttons (aktiv/disabled). */
  function updateSpielStartenState() {
    if (!configGameBtn) return;
    configGameBtn.disabled = !canStartGame();
    configGameBtn.style.pointerEvents = configGameBtn.disabled ? "none" : "";
  }

  /** Aktualisiert die Lobby-Fragenzähler und den Spiel-starten-Button. */
  function refreshLobbyQuestionCounter() {
    var available = getAvailableQuestionCount();
    var selected = getSelectedCount();

    if (questionCounterDisplay) questionCounterDisplay.textContent = available + "/" + selected;
    if (gameQuestionCounterDisplay) gameQuestionCounterDisplay.textContent = "0/" + selected;

    updateSpielStartenState();
  }

  window.showMainSection = showMainSection;
  window.showLogin = showLogin;
  window.canStartGame = canStartGame;
  window.getAvailableQuestionCount = getAvailableQuestionCount;
  window.getSelectedCount = getSelectedCount;
  window.updateSpielStartenState = updateSpielStartenState;

  document.getElementById("goToRegister").addEventListener("click", showRegister);
  document.getElementById("backToLoginFromRegister").addEventListener("click", showLogin);
  document.getElementById("backToLogin").addEventListener("click", showLogin);

  if (controllerList) {
    controllerList.addEventListener("click", function (event) {
      var card = event.target.closest(".controller-card.is-available");
      if (!card) return;

      controllerList.querySelectorAll(".controller-card").forEach(function (entry) {
        entry.classList.remove("is-selected");
      });

      card.classList.add("is-selected");
    });
  }

  document.querySelectorAll("#mainTabs .tab, #bottomNav .tab").forEach(function (tab) {
    tab.addEventListener("click", function () {
      var section = tab.getAttribute("data-section");

      if (!section || tab.disabled) return;
      if (section === "web-controller") {
        openWebController();
        return;
      }

      showMainSection(section);
    });
  });

  document.getElementById("highscoreTabs").addEventListener("click", function (event) {
    var button = event.target.closest(".score-tab");
    if (!button) return;

    document.querySelectorAll("#highscoreTabs .score-tab").forEach(function (entry) {
      entry.classList.remove("is-active");
    });

    button.classList.add("is-active");
    if (window.loadHighscores) window.loadHighscores(parseInt(button.getAttribute("data-mode"), 10));
  });

  document.getElementById("backToLobbyFromHighscores").addEventListener("click", function () {
    showMainSection("lobby");
    if (window.loadLobbyPlayers) window.loadLobbyPlayers();
  });

  document.getElementById("backToLobbyFromAuswertung").addEventListener("click", function () {
    showMainSection("lobby");
    if (window.loadLobbyPlayers) window.loadLobbyPlayers();
  });

  document.getElementById("modePills").addEventListener("click", function (event) {
    var pill = event.target.closest(".pill");
    if (!pill) return;

    document.querySelectorAll("#modePills .pill").forEach(function (entry) {
      entry.classList.remove("is-active");
    });

    pill.classList.add("is-active");
    refreshLobbyQuestionCounter();
  });

  document.getElementById("categoryCheckboxes").addEventListener("change", refreshLobbyQuestionCounter);
  document.getElementById("difficultyCheckboxes").addEventListener("change", refreshLobbyQuestionCounter);

  refreshLobbyQuestionCounter();

  if (window.loadAvailableControllers) {
    window.loadAvailableControllers();
    window.setInterval(window.loadAvailableControllers, 1000);
  }

  if (window.loadLobbyPlayers) {
    window.loadLobbyPlayers();
    window.setInterval(window.loadLobbyPlayers, 1000);
  }
});
