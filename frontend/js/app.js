/**
 * Quiz-Plattform – Hauptlogik (Frontend)
 * Steuerung: UI-Zustände (Login/Register/Controller), Tabs, Spielkonfiguration, Lobby-Counter
 */

/** Affiche le score exact sans arrondi (conserve les décimales) */
function formatScoreExact(n) {
  var num = Number(n);
  if (isNaN(num)) return "0";
  return String(num);
}
window.formatScoreExact = formatScoreExact;

/** Zeigt ein Modal-Popup (Erfolg, Fehler, Info) – global über window.showMessage */
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

/** DOMContentLoaded: Event-Handler registrieren, Lobby/Controller-Poll starten */
document.addEventListener("DOMContentLoaded", function () {
  var body = document.body;
  var controllerList = document.getElementById("controllerList");
  var questionCounterDisplay = document.getElementById("questionCounterDisplay");
  var gameQuestionCounterDisplay = document.getElementById("gameQuestionCounterDisplay");
  var configGameBtn = document.getElementById("configGameBtn");

  /** Wechselt zur Registrierungs-Ansicht */
  function showRegister() {
    body.classList.remove("state-initial", "state-controller");
    body.classList.add("state-register");
  }

  /** Wechselt zur Login-Ansicht */
  function showLogin() {
    body.classList.remove("state-register", "state-controller");
    body.classList.add("state-initial");
  }

  /** Erzeugt URL für Web-Controller (Port 81, optional mit ?id=) */
  function getWebControllerUrl(controllerId) {
    var base = window.location.protocol + "//" + window.location.hostname + ":81";
    if (!controllerId) return base + "/controller.html";
    return base + "/controller.html?id=" + encodeURIComponent(controllerId);
  }

  /** Öffnet Web-Controller in neuem Tab, aktualisiert Controller-Liste nach 1,5s */
  function openWebController() {
    window.open(getWebControllerUrl(), "_blank");
    if (window.loadAvailableControllers) {
      setTimeout(function () { window.loadAvailableControllers(); }, 1500);
    }
  }

  /** Wechselt Hauptbereich (Lobby, Spiel, Auswertung, Highscores), aktualisiert Tabs */
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

  function setGameActive(active) {
    document.querySelectorAll("#mainTabs .tab, #bottomNav .tab").forEach(function (tab) {
      tab.disabled = active;
    });

    document.querySelectorAll(".panel.left, .main-section[data-main-section=\"lobby\"]").forEach(function (el) {
      el.classList.toggle("is-locked", active);
    });
  }

  /** Liefert gewählte Fragenanzahl (5/10/20) aus Mode-Pills */
  function getSelectedCount() {
    var activePill = document.querySelector("#modePills .pill.is-active");
    if (!activePill) return 5;
    return parseInt(activePill.getAttribute("data-count"), 10) || 5;
  }

  /** Parst Kategorie-Zähler-Text „(2, 5, 2)“ zu [leicht, mittel, schwer] */
  function parseCategoryCount(text) {
    var match = (text || "").match(/\(?\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*\)?/);
    if (!match) return [0, 0, 0];
    return [
      parseInt(match[1], 10) || 0,
      parseInt(match[2], 10) || 0,
      parseInt(match[3], 10) || 0
    ];
  }

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

  /** Prüft: genug Fragen, Spieler verbunden und alle bereit */
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

  /** Aktiviert/Deaktiviert den „Spiel starten“-Button */
  function updateSpielStartenState() {
    if (!configGameBtn) return;
    configGameBtn.disabled = !canStartGame();
    configGameBtn.style.pointerEvents = configGameBtn.disabled ? "none" : "";
  }

  function refreshLobbyQuestionCounter() {
    var available = getAvailableQuestionCount();
    var selected = getSelectedCount();

    if (questionCounterDisplay) questionCounterDisplay.textContent = available + "/" + selected;
    if (gameQuestionCounterDisplay) gameQuestionCounterDisplay.textContent = "0/" + selected;

    updateSpielStartenState();
  }

  /** Globale Funktionen für app_http.js und app_mqtt.js */
  window.showMainSection = showMainSection;
  window.showLogin = showLogin;
  window.canStartGame = canStartGame;
  window.getAvailableQuestionCount = getAvailableQuestionCount;
  window.getSelectedCount = getSelectedCount;
  window.updateSpielStartenState = updateSpielStartenState;

  /** Event-Handler: Login/Registrierung */
  document.getElementById("goToRegister").addEventListener("click", showRegister);
  document.getElementById("backToLoginFromRegister").addEventListener("click", showLogin);
  document.getElementById("backToLogin").addEventListener("click", showLogin);

  /** Controller-Liste: Auswahl per Klick */
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

  /** Haupt-Tabs: Lobby, Spiel, Auswertung, Web-Controller, Highscores */
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

  /** Highscore-Tabs: 5/10/20 Fragen */
  document.getElementById("highscoreTabs").addEventListener("click", function (event) {
    var button = event.target.closest(".score-tab");
    if (!button) return;

    document.querySelectorAll("#highscoreTabs .score-tab").forEach(function (entry) {
      entry.classList.remove("is-active");
    });

    button.classList.add("is-active");
    if (window.loadHighscores) window.loadHighscores(parseInt(button.getAttribute("data-mode"), 10));
  });

  /** Zurück zur Lobby (von Highscores/Auswertung) */
  document.getElementById("backToLobbyFromHighscores").addEventListener("click", function () {
    showMainSection("lobby");
    if (window.loadLobbyPlayers) window.loadLobbyPlayers();
  });

  document.getElementById("backToLobbyFromAuswertung").addEventListener("click", function () {
    showMainSection("lobby");
    if (window.loadLobbyPlayers) window.loadLobbyPlayers();
  });

  /** Mode-Pills: 5/10/20 Fragen */
  document.getElementById("modePills").addEventListener("click", function (event) {
    var pill = event.target.closest(".pill");
    if (!pill) return;

    document.querySelectorAll("#modePills .pill").forEach(function (entry) {
      entry.classList.remove("is-active");
    });

    pill.classList.add("is-active");
    refreshLobbyQuestionCounter();
  });

  /** Kategorien/Schwierigkeit: Zähler bei Änderung aktualisieren */
  document.getElementById("categoryCheckboxes").addEventListener("change", refreshLobbyQuestionCounter);
  document.getElementById("difficultyCheckboxes").addEventListener("change", refreshLobbyQuestionCounter);

  refreshLobbyQuestionCounter();

  /** Polling: Controller-Liste und Lobby alle 1s */
  if (window.loadAvailableControllers) {
    window.loadAvailableControllers();
    window.setInterval(window.loadAvailableControllers, 1000);
  }

  if (window.loadLobbyPlayers) {
    window.loadLobbyPlayers();
    window.setInterval(window.loadLobbyPlayers, 1000);
  }
});
