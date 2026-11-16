import * as THREE from 'three';
import { OrbitControls } from 'three/addons/controls/OrbitControls.js';
import { GLTFLoader } from 'three/addons/loaders/GLTFLoader.js';
import { DRACOLoader } from 'three/addons/loaders/DRACOLoader.js';

// ==================== 1. CATÁLOGO DE 14 SITUACIONES (IGUAL A APK) ====================
const SITUATIONS = [
  {
    name: 'Órbita nominal',
    zones: [],
    effectNote: 'Indicadores simulados estables · sin zonas marcadas.'
  },
  {
    name: 'Actividad extravehicular (EVA)',
    zones: ['heart', 'bone_left_femur', 'bone_right_femur'],
    zoneLabels: ['corazón y vasos (referencia visual)', 'extremidades (referencia visual del esqueleto)'],
    effectNote: 'Mayor demanda cardiovascular y carga articular durante caminata espacial.'
  },
  {
    name: 'Microgravedad prolongada',
    zones: ['bone_spine', 'bone_left_femur', 'bone_right_femur', 'body'],
    zoneLabels: ['esqueleto (columna y fémur)', 'masa muscular (referencia visual)'],
    effectNote: 'Desacondicionamiento óseo y pérdida de masa muscular por falta de carga gravitacional.'
  },
  {
    name: 'Evento o tormenta solar',
    zones: ['heart', 'bone_spine', 'bone_left_femur', 'bone_right_femur'],
    zoneLabels: ['esqueleto (referencia visual a tejidos formadores de sangre)', 'corazón y vasos (referencia visual)'],
    effectNote: 'Incremento de flujo de partículas energéticas; requiere monitoreo de dosimetría.'
  },
  {
    name: 'Exposición a radiación espacial',
    zones: ['heart', 'bone_spine', 'bone_left_femur', 'bone_right_femur'],
    zoneLabels: ['esqueleto (referencia visual a tejidos formadores de sangre)', 'corazón y vasos (referencia visual)', 'cerebro/SNC (referencia visual)'],
    effectNote: 'Exposición acumulada a radiación cósmica galáctica y partículas solares.'
  },
  {
    name: 'Aumento de CO₂ en cabina',
    zones: ['heart', 'bone_spine', 'bone_left_femur', 'bone_right_femur'],
    zoneLabels: ['pulmones', 'esqueleto (referencia visual a tejidos formadores de sangre)', 'extremidades (referencia visual del esqueleto)'],
    effectNote: 'Elevación leve de presión parcial de CO₂; compensación respiratoria y cardiovascular.'
  },
  {
    name: 'Descanso insuficiente',
    zones: [],
    effectNote: 'Sueño y estrés se modifican como datos de demostración, no como mediciones.'
  },
  {
    name: 'Tormenta solar',
    zones: ['heart', 'bone_spine'],
    zoneLabels: ['esqueleto (referencia visual a tejidos formadores de sangre)', 'corazón y vasos (referencia visual)'],
    effectNote: 'Alerta geomagnética activa; se sugiere resguardo en zona blindada.'
  },
  {
    name: 'Fuga de CO₂ en cabina',
    zones: ['heart', 'bone_spine', 'bone_left_femur', 'bone_right_femur'],
    zoneLabels: ['pulmones', 'esqueleto (referencia visual a tejidos formadores de sangre)', 'extremidades (referencia visual del esqueleto)'],
    effectNote: 'Variación crítica de atmósfera interna; revisión inmediata de depuradores.'
  },
  {
    name: 'Fatiga del tripulante',
    zones: ['body'],
    zoneLabels: ['masa muscular (referencia visual)'],
    effectNote: 'Fatiga acumulada tras jornadas extendidas en órbita.'
  },
  {
    name: 'Erupciones solares y clima espacial',
    zones: ['heart', 'bone_spine'],
    zoneLabels: ['esqueleto (referencia visual a tejidos formadores de sangre)', 'corazón y vasos (referencia visual)'],
    effectNote: 'Actividad solar clase M/X registrada en los últimos 30 días.'
  },
  {
    name: 'Biología del vuelo espacial',
    zones: ['bone_spine', 'bone_left_femur', 'bone_right_femur'],
    zoneLabels: ['esqueleto (referencia visual a tejidos formadores de sangre)'],
    effectNote: 'Adaptación fisiológica general al entorno de caída libre.'
  },
  {
    name: 'Dosimetría de radiación',
    zones: ['heart', 'bone_spine', 'bone_left_femur', 'bone_right_femur'],
    zoneLabels: ['esqueleto (referencia visual a tejidos formadores de sangre)', 'corazón y vasos (referencia visual)', 'cerebro/SNC (referencia visual)'],
    effectNote: 'La dosimetría requiere lecturas y periodo de exposición; el dato DosTel disponible es histórico.'
  },
  {
    name: 'Telemetría de cabina ISS',
    zones: [],
    effectNote: 'Parámetros ambientales nominales dentro de rango operativo.'
  }
];

// ==================== 2. UTILIDADES UI Y TOAST ====================
let toastTimer = null;
function showToast(msg) {
  const el = document.getElementById('toast-banner');
  el.textContent = msg;
  el.classList.remove('hidden');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => el.classList.add('hidden'), 2600);
}

// ==================== 3. NAVEGACIÓN ENTRE PESTAÑAS ====================
const navButtons = document.querySelectorAll('.nav-btn');
const tabViews = document.querySelectorAll('.tab-view');

function switchTab(targetId) {
  navButtons.forEach((b) => b.classList.toggle('active', b.dataset.target === targetId));
  tabViews.forEach((v) => v.classList.toggle('active', v.id === targetId));

  if (targetId === 'view-inicio') {
    const homeWrapper = document.querySelector('#view-inicio .canvas-wrapper');
    const container = document.getElementById('canvas-container');
    if (homeWrapper && container && container.parentElement !== homeWrapper) {
      homeWrapper.appendChild(container);
    }
    resizeViewer();
  }
}

navButtons.forEach((btn) => {
  btn.addEventListener('click', () => switchTab(btn.dataset.target));
});

// ==================== 4. VISOR 3D THREE.JS ====================
const container = document.getElementById('canvas-container');
const statusEl = document.getElementById('connection-status');

const scene = new THREE.Scene();
const camera = new THREE.PerspectiveCamera(36, container.clientWidth / container.clientHeight, 0.05, 100);
camera.position.set(0, 0.95, 1.85);

const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });
renderer.setSize(container.clientWidth, container.clientHeight);
renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
container.appendChild(renderer.domElement);

const controls = new OrbitControls(camera, renderer.domElement);
controls.enableDamping = true;
controls.target.set(0, 0.92, 0);

scene.add(new THREE.AmbientLight(0xaad4ff, 1.4));
const dirLight = new THREE.DirectionalLight(0xffffff, 2.0);
dirLight.position.set(2, 4, 3);
scene.add(dirLight);
const rimLight = new THREE.DirectionalLight(0x00e699, 1.2);
rimLight.position.set(-2, 2, -3);
scene.add(rimLight);

const nodes = {
  body: [],
  heart: [],
  bone_spine: [],
  bone_left_femur: [],
  bone_right_femur: [],
  other: []
};

let heartPivot = null;
let currentBpm = 76;

function assignNodeMaterials(root) {
  root.traverse((obj) => {
    if (!obj.isMesh) return;

    let current = obj;
    let matchedKey = 'other';
    while (current) {
      const name = (current.name || '').toLowerCase();
      if (name.includes('heart')) { matchedKey = 'heart'; break; }
      if (name === 'bone_spine') { matchedKey = 'bone_spine'; break; }
      if (name === 'bone_left_femur') { matchedKey = 'bone_left_femur'; break; }
      if (name === 'bone_right_femur') { matchedKey = 'bone_right_femur'; break; }
      if (name === 'body') { matchedKey = 'body'; break; }
      current = current.parent;
    }

    if (matchedKey === 'body') {
      obj.renderOrder = 1;
      obj.material = new THREE.MeshStandardMaterial({
        color: 0x00e699,
        transparent: true,
        opacity: 0.14,
        roughness: 0.25,
        depthWrite: false
      });
    } else if (matchedKey === 'heart') {
      obj.renderOrder = 3;
      obj.material = new THREE.MeshStandardMaterial({
        color: 0x00e699,
        emissive: 0x00e699,
        emissiveIntensity: 0.95,
        roughness: 0.3,
        depthTest: false
      });
    } else {
      obj.renderOrder = 2;
      obj.material = new THREE.MeshStandardMaterial({
        color: 0x00e699,
        emissive: 0x003322,
        emissiveIntensity: 0.45,
        roughness: 0.35
      });
    }

    nodes[matchedKey].push(obj);
  });
}

function applyZoneHighlights(alertZones) {
  const GREEN = 0x00e699;
  const RED = 0xff3b30;

  ['body', 'heart', 'bone_spine', 'bone_left_femur', 'bone_right_femur', 'other'].forEach((key) => {
    const isAlert = alertZones.includes(key);
    const hex = isAlert ? RED : GREEN;
    nodes[key].forEach((mesh) => {
      mesh.material.color.setHex(hex);
      if (mesh.material.emissive) {
        mesh.material.emissive.setHex(isAlert ? RED : (key === 'heart' ? GREEN : 0x003322));
      }
    });
  });
}

const dracoLoader = new DRACOLoader();
dracoLoader.setDecoderPath('https://www.gstatic.com/draco/versioned/decoders/1.5.7/');

const gltfLoader = new GLTFLoader();
gltfLoader.setDRACOLoader(dracoLoader);

gltfLoader.load(
  'assets/modelo_zerog_pulse.glb',
  (gltf) => {
    const model = gltf.scene;
    assignNodeMaterials(model);

    nodes.heart.forEach((m) => (m.visible = false));

    const bodyBox = new THREE.Box3().setFromObject(model);
    const bodySize = bodyBox.getSize(new THREE.Vector3());
    const bodyCenter = bodyBox.getCenter(new THREE.Vector3());
    const maxDim = Math.max(bodySize.x, bodySize.y, bodySize.z);
    const scale = 1.9 / maxDim;

    model.scale.setScalar(scale);
    model.position.sub(bodyCenter.multiplyScalar(scale));
    model.position.y += 0.9;
    scene.add(model);
    model.updateMatrixWorld(true);

    const fullBox = new THREE.Box3().setFromObject(model);
    const fullCenter = fullBox.getCenter(new THREE.Vector3());
    const fullSize = fullBox.getSize(new THREE.Vector3());

    const chestAnchor = new THREE.Vector3(
      fullCenter.x + 0.018,
      fullBox.min.y + fullSize.y * 0.72,
      fullCenter.z + 0.025
    );
    controls.target.set(fullCenter.x, fullBox.min.y + fullSize.y * 0.5, fullCenter.z);

    if (nodes.heart.length > 0) {
      heartPivot = new THREE.Group();
      heartPivot.position.copy(chestAnchor);
      scene.add(heartPivot);

      nodes.heart.forEach((mesh) => {
        mesh.visible = true;
        heartPivot.attach(mesh);
      });
      heartPivot.updateMatrixWorld(true);

      const hBox = new THREE.Box3().setFromObject(heartPivot);
      const hSize = hBox.getSize(new THREE.Vector3());
      const hCenter = hBox.getCenter(new THREE.Vector3());
      const maxHeartDim = Math.max(hSize.x, hSize.y, hSize.z);

      const offset = hCenter.clone().sub(chestAnchor);
      nodes.heart.forEach((mesh) => {
        mesh.position.sub(offset);
      });

      const heartScale = 0.085 / maxHeartDim;
      heartPivot.userData.baseScale = heartScale;
      heartPivot.scale.setScalar(heartScale);
    }

    statusEl.textContent = 'Modelo listo · En línea';
    const history = loadHistory();
    if (history.length > 0 && history[0].alertZones) {
      applyZoneHighlights(history[0].alertZones);
    }
  },
  undefined,
  () => {
    statusEl.textContent = 'Error cargando modelo 3D';
  }
);

// ==================== 5. MODALES DE ESCANEO (ETAPA -> SITUACIÓN) ====================
const modalStage = document.getElementById('modal-stage');
const modalSituation = document.getElementById('modal-situation');
const situationListEl = document.getElementById('situation-options-list');

let pendingStageKey = 'before';
let pendingStageTitle = 'Escaneo inicial · antes del viaje';
let activeStageDetail = 'before';

SITUATIONS.forEach((sit) => {
  const btn = document.createElement('button');
  btn.type = 'button';
  btn.className = 'modal-option-btn';
  btn.textContent = sit.name;
  btn.addEventListener('click', () => {
    modalSituation.classList.add('hidden');
    executeScan(pendingStageKey, pendingStageTitle, sit);
  });
  situationListEl.appendChild(btn);
});

document.getElementById('btn-open-scan').addEventListener('click', () => {
  modalStage.classList.remove('hidden');
});

document.getElementById('btn-cancel-stage').addEventListener('click', () => {
  modalStage.classList.add('hidden');
});

document.getElementById('btn-cancel-situation').addEventListener('click', () => {
  modalSituation.classList.add('hidden');
});

document.querySelectorAll('[data-stage-choice]').forEach((btn) => {
  btn.addEventListener('click', () => {
    pendingStageKey = btn.dataset.stageChoice;
    pendingStageTitle = btn.dataset.label;
    modalStage.classList.add('hidden');
    modalSituation.classList.remove('hidden');
  });
});

// ==================== 6. HISTORIAL Y ETAPAS ====================
const HISTORY_KEY = 'astrovital_scans_v2';

function loadHistory() {
  try {
    return JSON.parse(localStorage.getItem(HISTORY_KEY) || '[]');
  } catch {
    return [];
  }
}

function saveHistory(list) {
  localStorage.setItem(HISTORY_KEY, JSON.stringify(list));
}

function executeScan(stageKey, stageTitle, situationObj) {
  const now = new Date();
  const datePart = now.toLocaleDateString('es-GT');
  const timePart = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  const stageNameMap = {
    before: 'Antes del viaje',
    during: 'Durante la misión',
    after: 'Después del viaje'
  };

  const bpm = situationObj.name === 'Órbita nominal' ? 67 : Math.floor(76 + Math.random() * 14);
  const spo2 = situationObj.name.includes('CO₂') ? 95 : 97 + Math.floor(Math.random() * 2);
  const temp = (36.5 + Math.random() * 0.4).toFixed(2);
  const sleep = (4.3 + Math.random() * 3.4).toFixed(1);
  const stress = situationObj.zones.length > 0 ? 48 : 36;

  currentBpm = bpm;
  applyZoneHighlights(situationObj.zones);

  const record = {
    id: Date.now(),
    stageKey,
    stageLabel: stageNameMap[stageKey],
    title: stageTitle,
    timestamp: `${datePart} · ${timePart}`,
    situation: situationObj.name,
    alertZones: situationObj.zones,
    zoneLabels: situationObj.zoneLabels || [],
    effectNote: situationObj.effectNote,
    bpm,
    spo2,
    temp,
    sleep,
    stress
  };

  const history = loadHistory();
  history.unshift(record);
  saveHistory(history);

  updateHomeStageSummaries();
  renderHistory();
  openStageDetail(stageKey);
  showToast('Escaneo guardado en su etapa y en Historial');
}

function updateHomeStageSummaries() {
  const history = loadHistory();
  ['before', 'during', 'after'].forEach((key) => {
    const latest = history.find((item) => item.stageKey === key);
    const el = document.getElementById(`stage-summary-${key}`);
    if (el) {
      el.textContent = latest
        ? `Último registro: ${latest.timestamp} · toca para ver ·`
        : 'Aún no hay escaneos · toca para abrir ·';
    }
  });

  const redNoteEl = document.getElementById('latest-scan-red-note');
  const latestWithAlerts = history.find((item) => item.zoneLabels && item.zoneLabels.length > 0);
  if (redNoteEl) {
    redNoteEl.textContent = latestWithAlerts
      ? `${latestWithAlerts.timestamp} · Zonas señaladas: ${latestWithAlerts.zoneLabels.join(', ')}`
      : '';
  }
}

function renderHistory() {
  const listEl = document.getElementById('history-list');
  const history = loadHistory();

  if (history.length === 0) {
    listEl.innerHTML = `<div class="history-card"><p class="card-sub">No hay escaneos guardados aún. Usa "Escanear y guardar resultados" en Inicio.</p></div>`;
    return;
  }

  listEl.innerHTML = history
    .map((item) => {
      const hasAlerts = item.zoneLabels && item.zoneLabels.length > 0;
      const statusHtml = hasAlerts
        ? `<div class="history-status-alert">● Zonas marcadas: ${item.zoneLabels.join(', ')}</div>`
        : `<div class="history-status-ok">● Estado estable · sin zonas marcadas</div>`;

      return `
        <article class="history-card">
          <h3>${item.title}</h3>
          <div class="history-meta">${item.timestamp} · ${item.stageLabel}</div>
          <div class="history-situation">Situación: ${item.situation}</div>
          ${statusHtml}
          <div class="history-telemetry">FC ${item.bpm} lpm · SpO₂ ${item.spo2}% · Temp. ${item.temp} °C</div>
        </article>
      `;
    })
    .join('');
}

document.getElementById('btn-clear-history').addEventListener('click', () => {
  saveHistory([]);
  applyZoneHighlights([]);
  updateHomeStageSummaries();
  renderHistory();
  showToast('Historial limpiado');
});

// ==================== 7. SUBVISTA DETALLE DE ETAPA ====================
function openStageDetail(stageKey) {
  activeStageDetail = stageKey;
  const titles = {
    before: 'Antes del viaje',
    during: 'Durante la misión',
    after: 'Después del viaje'
  };
  document.getElementById('stage-detail-title').textContent = titles[stageKey];

  const history = loadHistory().filter((item) => item.stageKey === stageKey);
  const latestCard = document.getElementById('stage-latest-card');
  const recordsList = document.getElementById('stage-records-list');

  if (history.length > 0) {
    const latest = history[0];
    applyZoneHighlights(latest.alertZones || []);
    const hasAlerts = latest.zoneLabels && latest.zoneLabels.length > 0;
    latestCard.innerHTML = `
      <h2>${latest.title}</h2>
      <div class="history-meta">${latest.timestamp} · registro local</div>
      <div class="history-situation"><strong>Situación evaluada:</strong> ${latest.situation} · simulación</div>
      <p class="card-sub">${latest.effectNote}</p>
      ${hasAlerts
        ? `<div class="history-status-alert">● Zonas destacadas por simulación: ${latest.zoneLabels.join(', ')}</div>`
        : `<div class="history-status-ok">● Indicadores simulados estables · sin zonas marcadas</div>`
      }
      <div class="history-telemetry">Frecuencia cardíaca: ${latest.bpm} lpm · Oxígeno: ${latest.spo2}% · Temperatura: ${latest.temp} °C</div>
      <div class="history-telemetry">Sueño (dato demo): ${latest.sleep} h · Estrés (dato demo): ${latest.stress}/100</div>
    `;

    recordsList.innerHTML = history
      .map(
        (r) => `
        <div class="history-card">
          <h3>${r.title}</h3>
          <div class="history-meta">${r.timestamp} · ${r.zoneLabels.length > 0 ? 'con zonas marcadas' : 'estable'}</div>
          <div class="history-situation">Situación: ${r.situation}</div>
        </div>
      `
      )
      .join('');
  } else {
    applyZoneHighlights([]);
    latestCard.innerHTML = `
      <h2>Sin escaneos en ${titles[stageKey]}</h2>
      <p class="card-sub">Presiona el botón inferior para registrar una evaluación en esta fase.</p>
    `;
    recordsList.innerHTML = `<div class="history-card"><p class="card-sub">Aún no hay registros en esta etapa.</p></div>`;
  }

  // Mover el visor 3D a la subvista de etapa
  const stageSlot = document.getElementById('stage-3d-slot');
  stageSlot.appendChild(container);

  navButtons.forEach((b) => b.classList.remove('active'));
  tabViews.forEach((v) => v.classList.remove('active'));
  document.getElementById('view-stage-detail').classList.add('active');
  resizeViewer();
}

document.querySelectorAll('.stage-item-card').forEach((card) => {
  card.addEventListener('click', () => openStageDetail(card.dataset.stage));
});

document.getElementById('btn-mission-pill').addEventListener('click', () => openStageDetail('before'));
document.getElementById('btn-back-home').addEventListener('click', () => switchTab('view-inicio'));

document.getElementById('btn-stage-register').addEventListener('click', () => {
  const defaultTitles = {
    before: 'Escaneo inicial · antes del viaje',
    during: 'Nuevo escaneo durante la estancia',
    after: 'Escaneo posterior · regreso'
  };
  pendingStageKey = activeStageDetail;
  pendingStageTitle = defaultTitles[activeStageDetail];
  modalSituation.classList.remove('hidden');
});

// ==================== 8. ESTADO DE ÁNIMO Y API CLIMA ESPACIAL ====================
let selectedMood = 'Bien';
document.querySelectorAll('.mood-btn').forEach((btn) => {
  btn.addEventListener('click', () => {
    document.querySelectorAll('.mood-btn').forEach((b) => b.classList.remove('active'));
    btn.classList.add('active');
    selectedMood = btn.dataset.mood;
    showToast(`Estado seleccionado: ${selectedMood}`);
  });
});

document.getElementById('btn-save-mood').addEventListener('click', () => {
  const note = document.getElementById('mood-note').value.trim();
  localStorage.setItem('astrovital_mood_v1', JSON.stringify({ mood: selectedMood, note }));
  document.getElementById('mood-feedback').textContent = `✓ Guardado: ${selectedMood}${note ? ` — "${note}"` : ''}`;
  showToast('Estado de ánimo guardado en este dispositivo');
});

async function refreshSpaceWeather() {
  const now = new Date();
  const stamp = `${now.toLocaleDateString('es-GT')} ${now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`;
  try {
    const res = await fetch('data/nasa_space_weather.json');
    if (res.ok) {
      const json = await res.json();
      document.getElementById('api-updated-text').textContent =
        `API actualizada ${stamp} · ${json.events_count || 14} eventos en respuesta guardada`;
    } else {
      document.getElementById('api-updated-text').textContent =
        `API actualizada ${stamp} · 14 eventos en respuesta API guardada`;
    }
  } catch {
    document.getElementById('api-updated-text').textContent =
      `API actualizada ${stamp} · 14 eventos en respuesta API guardada`;
  }
  showToast('Datos de clima espacial actualizados');
}

document.getElementById('btn-refresh-api').addEventListener('click', refreshSpaceWeather);
document.getElementById('btn-refresh-cumulative').addEventListener('click', refreshSpaceWeather);

// ==================== 9. SIGNOS VITALES CADA 4 SEGUNDOS ====================
function refreshLiveVitals() {
  const now = new Date();
  const timeStr = now.toTimeString().split(' ')[0];
  const jitter = Math.floor(Math.random() * 5) - 2;

  const hr = Math.max(58, currentBpm + jitter);
  const spo2 = 97 + (jitter > 0 ? 1 : 0);
  const temp = (36.2 + Math.random() * 0.5).toFixed(1);
  const sleep = (6.6 + Math.random() * 0.8).toFixed(1);
  const stress = Math.min(92, Math.max(18, Math.round((hr - 50) * 1.4)));

  document.getElementById('vital-timestamp').textContent = timeStr;
  document.getElementById('vital-hr').textContent = `${hr} lpm`;
  document.getElementById('vital-spo2').textContent = `${spo2} %`;
  document.getElementById('vital-temp').textContent = `${temp} °C`;
  document.getElementById('vital-sleep').textContent = `${sleep} h`;
  document.getElementById('vital-stress').textContent = `${stress} /100`;
}

refreshLiveVitals();
setInterval(refreshLiveVitals, 4000);

// ==================== 10. PERFIL DEL TRIPULANTE ====================
const PROFILE_KEY = 'astrovital_crew_profile_v1';

function applyProfileToUI(profile) {
  const name = (profile.name || '').trim();
  const duration = Number(profile.duration) || 180;

  document.getElementById('greeting-title').textContent = name ? `Hola, ${name}` : 'Hola, tripulante';
  document.getElementById('cumulative-horizon').textContent = `Horizonte: ${duration} días · duración fija del perfil`;

  const femurRisk = Math.min(100, (duration / 428) * 100).toFixed(1);
  const muscleRisk = Math.min(100, (duration / 300) * 100).toFixed(1);
  document.getElementById('cumulative-femur').textContent = `Hueso (fémur): ${femurRisk}% de índice estimado`;
  document.getElementById('cumulative-muscle').textContent = `Músculo (piernas): ${muscleRisk}% de índice estimado`;
}

function loadProfile() {
  try {
    const saved = JSON.parse(localStorage.getItem(PROFILE_KEY) || '{}');
    document.getElementById('prof-name').value = saved.name || '';
    document.getElementById('prof-age').value = saved.age || '';
    document.getElementById('prof-blood').value = saved.blood || '';
    document.getElementById('prof-allergies').value = saved.allergies || '';
    document.getElementById('prof-conditions').value = saved.conditions || '';
    document.getElementById('prof-contact').value = saved.contact || '';
    document.getElementById('prof-duration').value = saved.duration || '';
    applyProfileToUI(saved);
  } catch {
    // Ignorar si está vacío
  }
}

document.getElementById('profile-form').addEventListener('submit', (e) => {
  e.preventDefault();
  const profile = {
    name: document.getElementById('prof-name').value,
    age: document.getElementById('prof-age').value,
    blood: document.getElementById('prof-blood').value,
    allergies: document.getElementById('prof-allergies').value,
    conditions: document.getElementById('prof-conditions').value,
    contact: document.getElementById('prof-contact').value,
    duration: document.getElementById('prof-duration').value
  };

  localStorage.setItem(PROFILE_KEY, JSON.stringify(profile));
  applyProfileToUI(profile);
  showToast('Perfil guardado en este dispositivo');
});

loadProfile();
updateHomeStageSummaries();
renderHistory();

// ==================== 11. ANIMACIÓN Y RESIZE ====================
const clock = new THREE.Clock();
function animate() {
  requestAnimationFrame(animate);
  const t = clock.getElapsedTime();
  const pulse = 1 + Math.sin(t * (currentBpm / 60) * Math.PI * 2) * 0.08;

  if (heartPivot) {
    const base = heartPivot.userData.baseScale || 1;
    heartPivot.scale.setScalar(base * pulse);
  }

  controls.update();
  renderer.render(scene, camera);
}
animate();

function resizeViewer() {
  if (!container.clientWidth || !container.clientHeight) return;
  camera.aspect = container.clientWidth / container.clientHeight;
  camera.updateProjectionMatrix();
  renderer.setSize(container.clientWidth, container.clientHeight);
}

window.addEventListener('resize', resizeViewer);
