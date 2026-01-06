// Get controller id from query parameter 'id'
const controllerIdElement = document.getElementById('controllerId');
const params = new URLSearchParams(window.location.search);
let controllerId = params.get('id');
let mqttMessagePrefix = window.__ENV__.MQTT_MESSAGE_PREFIX || 'game/';


// Generate controller id and append to URL if not given
if (!controllerId) {
    controllerId = 'web-' + Math.random().toString(36).substring(2, 8);
    params.set('id', controllerId);
    window.history.replaceState({}, '', `${window.location.pathname}?${params.toString()}`);
}
controllerIdElement.innerText = controllerId;

// Connect to Mosquitto server, subscribe and receive messages on topic 'game/#'
// https://github.com/mqttjs/MQTT.js
const client = mqtt.connect("ws://" + window.__ENV__.MQTT_BROKER_URL + ":" + window.__ENV__.MQTT_BROKER_PORT , {
    username: window.__ENV__.MQTT_USERNAME,
    password: window.__ENV__.MQTT_PASSWORD,
});

client.on('connect', () => {
    console.log('✅ Connected to MQTT broker');
    client.subscribe(mqttMessagePrefix + 'game/#', err => {
        if (err) console.error('❌ Subscribe Fehler:', err);
    });
});

client.on('message', (topic, message) => {
    const msg = message.toString();
    console.log(`📥 Message received [${topic}]:`, msg);
});

function publishMessage(message) {
    client.publish(mqttMessagePrefix + 'game/start', message);
}

function updateLCD(textLine1, textLine2, textLine3) {
    document.querySelectorAll('.lcd-line')[0].textContent = textLine1;
    document.querySelectorAll('.lcd-line')[1].textContent = textLine2;
    document.querySelectorAll('.lcd-line')[2].textContent = textLine3;
}

updateLCD('Welcome', 'to the', 'Game');

// Add click event listener to color buttons.
document.querySelectorAll('.color-button').forEach(button => {
    button.addEventListener('click', () => {
        const color = button.getAttribute('data-color');
        publishMessage(`ID:${controllerId} ${color} pressed!`);

        // audio feedback
        const tone = parseInt(button.getAttribute('data-tone'), 10);
        playTone(tone);

        // visual feedback
        button.classList.add('active');
        button.addEventListener('animationend', () => {
            button.classList.remove('active');
        }, {once: true});
    });
});

// Add keymapping support for buttons
const keyMap = {
    q: "green",
    w: "red",
    a: "yellow",
    s: "blue"
};

document.addEventListener("keydown", (event) => {
    const color = keyMap[event.key.toLowerCase()];
    if (color) {
        document.querySelector(`.color-button[data-color = "${color}"]`)?.click();
    }
});

function playTone(frequency, duration = 300) {
    const context = new (window.AudioContext || window.webkitAudioContext)();
    const oscillator = context.createOscillator();
    const gainNode = context.createGain();

    oscillator.type = 'sine';
    oscillator.frequency.value = frequency;
    oscillator.connect(gainNode);
    gainNode.connect(context.destination);

    oscillator.start();
    setTimeout(() => {
        oscillator.stop();
        context.close();
    }, duration);
}

async function playColorSequence(colors) {
    for (const color of colors) {
        const button = document.querySelector(`.color-button[data-color = "${color}"]`);
        if (button) {
            button.classList.add("active");
            const tone = parseInt(button.getAttribute("data-tone"), 10);
            playTone(tone);
            await new Promise(resolve => setTimeout(resolve, 400));
            button.classList.remove("active");
            await new Promise(resolve => setTimeout(resolve, 100));
        }
    }
}

// Todo: Demo maybe not for students remove code later!
const demoButton = document.createElement("button");
demoButton.textContent = "Start Singleplayer Demo";
demoButton.id = 'demo-button';
document.body.appendChild(demoButton);
demoButton.addEventListener('click', startDemo);

const demoGame = {
    currentSequence: [],
    expectedIndex: 0,
    playtime: 0,
    round: 0,
    isRunning: false,
    currentIntervalId: null
};

async function startDemo() {
    demoButton.disabled = true;
    demoGame.isRunning = true;
    demoGame.currentSequence = [getRandomColor()];
    demoGame.playtime = 0;
    demoGame.round = 1;
    demoGame.expectedIndex = 0;

    await playColorSequence(demoGame.currentSequence);
    demoGame.currentIntervalId = setInterval(() => updateLCD(`Demo ${demoGame.round}`, `Playtime: ${++demoGame.playtime}ms`), 1);
}

const colors = ["green", "red", "yellow", "blue"];

function getRandomColor() {
    return colors[Math.floor(Math.random() * colors.length)];
}

document.querySelectorAll('.color-button').forEach(button => {
    button.addEventListener('click', async (event) => {
        if (!demoGame.isRunning) return;

        const color = event.target.getAttribute('data-color');
        // Wrong color pressed? => End demo
        if (color !== demoGame.currentSequence[demoGame.expectedIndex]) {
            clearInterval(demoGame.currentIntervalId);
            updateLCD(`Demo ${demoGame.round}`, `Playtime: ${demoGame.playtime}ms`, 'Ended');
            demoButton.disabled = false;
            demoGame.isRunning = false;
        } else {
            demoGame.expectedIndex++;
            // End of sequence? => Start next sequence with one more color
            if (demoGame.expectedIndex === demoGame.currentSequence.length) {
                clearInterval(demoGame.currentIntervalId);
                await new Promise(resolve => setTimeout(resolve, 1000));

                demoGame.currentSequence.push(getRandomColor());
                demoGame.expectedIndex = 0;
                demoGame.round++;

                updateLCD(`Demo ${demoGame.round}`, `Playtime: ${++demoGame.playtime}ms`)
                await playColorSequence(demoGame.currentSequence);
                demoGame.currentIntervalId = setInterval(() => updateLCD(`Demo ${demoGame.round}`, `Playtime: ${demoGame.playtime++}ms`), 1);
            }
        }
    });
});
