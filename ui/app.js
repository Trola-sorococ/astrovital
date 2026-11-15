import * as THREE from 'three';
import { OrbitControls } from 'three/addons/controls/OrbitControls.js';
import { GLTFLoader } from 'three/addons/loaders/GLTFLoader.js';
import { DRACOLoader } from 'three/addons/loaders/DRACOLoader.js';

// ==================== 1. NAVEGACIÓN ENTRE PESTAÑAS ====================
const navButtons = document.querySelectorAll('.nav-btn');
const tabViews = document.querySelectorAll('.tab-view');

navButtons.forEach((btn) => {
  btn.addEventListener('click', () => {
    const targetId = btn.dataset.target;
    navButtons.forEach((b) => b.classList.remove('active'));
    tabViews.forEach((v) => v.classList.remove('active'));

    btn.classList.add('active');
    document.getElementById(targetId).classList.add('active');

    if (targetId === 'view-inicio') {
      resizeViewer();
    }
  });
});

// ==================== 2. VISOR 3D THREE.JS ====================
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
        color: 0xd8f3ea,
        emissive: 0x000000,
        emissiveIntensity: 0.55,
        roughness: 0.35
      });
    }

    nodes[matchedKey].push(obj);
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
    updateSimulation();
  },
  undefined,
  (error) => {
    console.error('Error cargando GLB:', error);
    statusEl.textContent = 'Error cargando modelo 3D';
  }
);

// ==================== 3. SIMULACIÓN Y DIAGNÓSTICO ====================
const daysSlider = document.getElementById('days-slider');
const bpmSlider = document.getElementById('bpm-slider');
const exerciseSlider = document.getElementById('exercise-slider');
const stageSelect = document.getElementById('stage-select');
const situationSelect = document.getElementById('situation-select');

let currentTelemetry = {
  bpm: 78,
  spo2: 98,
  temp: 36.7,
  markedZones: []
};

function getRiskColor(score) {
  if (score < 0.35) return { hex: 0x00e699, label: 'Óptimo', css: '#00e699' };
  if (score < 0.70) return { hex: 0xffcc00, label: 'Moderado', css: '#ffcc00' };
  return { hex: 0xff453a, label: 'Crítico', css: '#ff453a' };
}

function paintMeshes(meshList, colorHex) {
  meshList.forEach((mesh) => {
    mesh.material.color.setHex(colorHex);
    mesh.material.emissive.setHex(colorHex);
  });
}

function updateSimulation() {
  const days = Number(daysSlider.value);
  const bpm = Number(bpmSlider.value);
  const exercise = Number(exerciseSlider.value) / 100;
  const situation = situationSelect.value;

  document.getElementById('days-val').textContent = `${days} días`;
  document.getElementById('bpm-val').textContent = `${bpm} lpm`;
  document.getElementById('exercise-val').textContent = `${Math.round(exercise * 100)}%`;

  let radiationBoost = situation === 'Exposición a radiación espacial' ? 0.45 : 0;
  let co2Boost = situation === 'Aumento de CO₂ en cabina' ? 0.3 : 0;

  const boneStress = Math.min(1, (days / 240) * (1.15 - exercise * 0.75) + radiationBoost);
  const muscleStress = Math.min(1, (days / 200) * (1.1 - exercise * 0.8));
  const heartStress = Math.min(1, Math.max(0, (bpm - 65) / 95) + co2Boost + radiationBoost * 0.5);

  const heartState = getRiskColor(heartStress);
  const boneState = getRiskColor(boneStress);
  const bodyState = getRiskColor(muscleStress);

  paintMeshes(nodes.heart, heartState.hex);
  paintMeshes(nodes.bone_spine, boneState.hex);
  paintMeshes(nodes.bone_left_femur, boneState.hex);
  paintMeshes(nodes.bone_right_femur, boneState.hex);
  paintMeshes(nodes.body, bodyState.hex);

  const elHeart = document.getElementById('risk-heart');
  const elSpine = document.getElementById('risk-spine');
  const elFemur = document.getElementById('risk-femur');
  const elBody = document.getElementById('risk-body');

  elHeart.textContent = heartState.label; elHeart.style.color = heartState.css;
  elSpine.textContent = boneState.label; elSpine.style.color = boneState.css;
  elFemur.textContent = boneState.label; elFemur.style.color = boneState.css;
  elBody.textContent = bodyState.label; elBody.style.color = bodyState.css;

  const marked = [];
  if (boneStress >= 0.35) marked.push('esqueleto (referencia visual a tejidos formadores de sangre)');
  if (heartStress >= 0.35) marked.push('corazón y vasos (referencia visual)');
  if (muscleStress >= 0.35) marked.push('masa muscular (referencia visual)');

  currentTelemetry = {
    bpm,
    spo2: situation === 'Aumento de CO₂ en cabina' ? 94 : 98,
    temp: +(36.6 + (bpm - 70) * 0.008).toFixed(2),
    markedZones: marked
  };
}

[daysSlider, bpmSlider, exerciseSlider, situationSelect].forEach((el) =>
  el.addEventListener('input', updateSimulation)
);

document.getElementById('btn-nominal').addEventListener('click', () => {
  stageSelect.value = 'Durante la misión';
  situationSelect.value = 'Órbita nominal';
  daysSlider.value = 30; bpmSlider.value = 72; exerciseSlider.value = 80;
  updateSimulation();
});

document.getElementById('btn-eva').addEventListener('click', () => {
  stageSelect.value = 'Durante la misión';
  situationSelect.value = 'Exposición a radiación espacial';
  daysSlider.value = 120; bpmSlider.value = 115; exerciseSlider.value = 50;
  updateSimulation();
});

document.getElementById('btn-critical').addEventListener('click', () => {
  stageSelect.value = 'Durante la misión';
  situationSelect.value = 'Exposición a radiación espacial';
  daysSlider.value = 300; bpmSlider.value = 162; exerciseSlider.value = 10;
  updateSimulation();
});

// ==================== 4. PESTAÑA SIGNOS (ACTUALIZA CADA 4s) ====================
function refreshLiveVitals() {
  const now = new Date();
  const timeStr = now.toTimeString().split(' ')[0];
  const baseBpm = Number(bpmSlider.value);
  const jitter = Math.floor(Math.random() * 5) - 2;

  const hr = Math.max(52, baseBpm + jitter);
  const spo2 = situationSelect.value === 'Aumento de CO₂ en cabina' ? 94 + (jitter > 0 ? 1 : 0) : 97 + (jitter > 0 ? 1 : 0);
  const temp = (36.5 + Math.random() * 0.4).toFixed(1);
  const sleep = (6.4 + Math.random() * 1.4).toFixed(1);
  const stress = Math.min(95, Math.max(12, Math.round((hr - 55) * 0.85)));

  document.getElementById('vital-timestamp').textContent = timeStr;
  document.getElementById('vital-hr').textContent = `${hr} lpm`;
  document.getElementById('vital-spo2').textContent = `${spo2} %`;
  document.getElementById('vital-temp').textContent = `${temp} °C`;
  document.getElementById('vital-sleep').textContent = `${sleep} h`;
  document.getElementById('vital-stress').textContent = `${stress} /100`;
}

refreshLiveVitals();
setInterval(refreshLiveVitals, 4000);

// ==================== 5. PESTAÑA HISTORIAL (LOCALSTORAGE) ====================
const HISTORY_KEY = 'astrovital_scan_history_v1';

const defaultScans = [
  {
    title: 'Escaneo inicial · antes del viaje',
    date: '15/11/2026 · 21:25 · Antes del viaje',
    situation: 'Órbita nominal',
    markedZones: [],
    bpm: 65,
    spo2: 98,
    temp: 36.93
  },
  {
    title: 'Escaneo durante la estancia',
    date: '15/11/2026 · 21:17 · Durante la misión',
    situation: 'Exposición a radiación espacial',
    markedZones: [
      'esqueleto (referencia visual a tejidos formadores de sangre)',
      'corazón y vasos (referencia visual)',
      'cerebro/SNC (referencia visual)'
    ],
    bpm: 79,
    spo2: 97,
    temp: 36.64
  }
];

function loadHistory() {
  try {
    const raw = localStorage.getItem(HISTORY_KEY);
    return raw ? JSON.parse(raw) : defaultScans;
  } catch {
    return defaultScans;
  }
}

function renderHistory() {
  const listEl = document.getElementById('history-list');
  const scans = loadHistory();

  if (scans.length === 0) {
    listEl.innerHTML = `<div class="history-card"><p class="card-sub">No hay escaneos guardados aún. Usa "Escanear y guardar resultados" en Inicio.</p></div>`;
    return;
  }

  listEl.innerHTML = scans
    .map((item) => {
      const hasAlerts = item.markedZones && item.markedZones.length > 0;
      const statusHtml = hasAlerts
        ? `<div class="history-status-alert">● Zonas marcadas: ${item.markedZones.join(', ')}</div>`
        : `<div class="history-status-ok">● Estado estable · sin zonas marcadas</div>`;

      return `
        <article class="history-card">
          <h3>${item.title}</h3>
          <div class="history-meta">${item.date}</div>
          <div class="history-situation">Situación: ${item.situation}</div>
          ${statusHtml}
          <div class="history-telemetry">FC ${item.bpm} lpm · SpO₂ ${item.spo2}% · Temp. ${item.temp} °C</div>
        </article>
      `;
    })
    .join('');
}

document.getElementById('btn-scan').addEventListener('click', () => {
  const now = new Date();
  const datePart = now.toLocaleDateString('es-GT');
  const timePart = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  const stage = stageSelect.value;

  const newEntry = {
    title: stage === 'Antes del viaje' ? 'Escaneo inicial · antes del viaje' : 'Escaneo durante la estancia',
    date: `${datePart} · ${timePart} · ${stage}`,
    situation: situationSelect.value,
    markedZones: [...currentTelemetry.markedZones],
    bpm: currentTelemetry.bpm,
    spo2: currentTelemetry.spo2,
    temp: currentTelemetry.temp
  };

  const scans = loadHistory();
  scans.unshift(newEntry);
  localStorage.setItem(HISTORY_KEY, JSON.stringify(scans));
  renderHistory();

  const btn = document.getElementById('btn-scan');
  btn.innerHTML = '<span>✓</span> Resultados guardados en Historial';
  setTimeout(() => {
    btn.innerHTML = '<span>✳</span> Escanear y guardar resultados';
  }, 1800);
});

document.getElementById('btn-clear-history').addEventListener('click', () => {
  localStorage.setItem(HISTORY_KEY, JSON.stringify([]));
  renderHistory();
});

renderHistory();

// ==================== 6. PESTAÑA PERFIL (LOCALSTORAGE) ====================
const PROFILE_KEY = 'astrovital_crew_profile_v1';

function applyProfileToUI(profile) {
  const name = (profile.name || '').trim();
  const duration = (profile.duration || '').trim();

  document.getElementById('greeting-title').textContent = name ? `Hola, ${name}` : 'Hola, tripulante';
  document.getElementById('mission-bar-text').textContent = duration
    ? `SEGUIMIENTO DE MISIÓN · ${duration} DÍAS PROGRAMADOS`
    : 'SEGUIMIENTO DE MISIÓN · ASTROVITAL';
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

  const fb = document.getElementById('profile-feedback');
  fb.textContent = '✓ Perfil guardado en este dispositivo.';
  fb.style.color = '#00e699';
  setTimeout(() => {
    fb.textContent = 'Los datos del perfil son informativos y no sustituyen una evaluación clínica.';
    fb.style.color = '';
  }, 2200);
});

loadProfile();

// ==================== 7. RENDER LOOP Y RESIZE ====================
const clock = new THREE.Clock();
function animate() {
  requestAnimationFrame(animate);
  const t = clock.getElapsedTime();
  const bpm = Number(bpmSlider.value);
  const pulse = 1 + Math.sin(t * (bpm / 60) * Math.PI * 2) * 0.08;

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
