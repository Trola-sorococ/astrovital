import * as THREE from 'three';
import { OrbitControls } from 'three/addons/controls/OrbitControls.js';
import { GLTFLoader } from 'three/addons/loaders/GLTFLoader.js';
import { DRACOLoader } from 'three/addons/loaders/DRACOLoader.js';

const container = document.getElementById('canvas-container');
const statusEl = document.getElementById('connection-status');

// 1. Escena, Cámara y Renderizador
const scene = new THREE.Scene();
const camera = new THREE.PerspectiveCamera(36, container.clientWidth / container.clientHeight, 0.05, 100);
camera.position.set(0, 0.95, 1.85);

const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });
renderer.setSize(container.clientWidth, container.clientHeight);
renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
container.appendChild(renderer.domElement);

// 2. Controles Orbitales
const controls = new OrbitControls(camera, renderer.domElement);
controls.enableDamping = true;
controls.target.set(0, 0.92, 0);

// 3. Iluminación estilo Escáner Biomédico
scene.add(new THREE.AmbientLight(0xaad4ff, 1.4));
const dirLight = new THREE.DirectionalLight(0xffffff, 2.0);
dirLight.position.set(2, 4, 3);
scene.add(dirLight);
const rimLight = new THREE.DirectionalLight(0x00e699, 1.2);
rimLight.position.set(-2, 2, -3);
scene.add(rimLight);

// 4. Referencias a los nodos del modelo
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

// 5. Cargar modelo_zerog_pulse.glb con DRACOLoader
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

// 6. Lógica de Riesgo y Colores Dinámicos
const daysSlider = document.getElementById('days-slider');
const bpmSlider = document.getElementById('bpm-slider');
const exerciseSlider = document.getElementById('exercise-slider');

function getRiskColor(score) {
  if (score < 0.35) return { hex: 0x00e699, label: 'Óptimo', css: '#00e699' };
  if (score < 0.70) return { hex: 0xffcc00, label: 'Moderado', css: '#ffcc00' };
  return { hex: 0xff3b5c, label: 'Crítico', css: '#ff3b5c' };
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

  document.getElementById('days-val').textContent = `${days} días`;
  document.getElementById('bpm-val').textContent = `${bpm} BPM`;
  document.getElementById('exercise-val').textContent = `${Math.round(exercise * 100)}%`;

  const boneStress = Math.min(1, (days / 240) * (1.15 - exercise * 0.75));
  const muscleStress = Math.min(1, (days / 200) * (1.1 - exercise * 0.8));
  const heartStress = Math.min(1, Math.max(0, (bpm - 65) / 95));

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
}

[daysSlider, bpmSlider, exerciseSlider].forEach((el) => el.addEventListener('input', updateSimulation));

// Presets
document.getElementById('btn-nominal').addEventListener('click', () => {
  daysSlider.value = 30; bpmSlider.value = 72; exerciseSlider.value = 80;
  updateSimulation();
});
document.getElementById('btn-eva').addEventListener('click', () => {
  daysSlider.value = 120; bpmSlider.value = 138; exerciseSlider.value = 55;
  updateSimulation();
});
document.getElementById('btn-critical').addEventListener('click', () => {
  daysSlider.value = 300; bpmSlider.value = 165; exerciseSlider.value = 10;
  updateSimulation();
});

// Botón "Escanear y guardar resultados"
document.getElementById('btn-scan').addEventListener('click', () => {
  const now = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
  const days = daysSlider.value;
  const bpm = bpmSlider.value;
  const heartLabel = document.getElementById('risk-heart').textContent;
  const spineLabel = document.getElementById('risk-spine').textContent;

  document.getElementById('scan-log').textContent =
    `Escaneo guardado (${now}) · ${days}d en órbita · ${bpm} BPM · Corazón: ${heartLabel} · Óseo: ${spineLabel}`;
});

// 7. Animación de latido
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

window.addEventListener('resize', () => {
  camera.aspect = container.clientWidth / container.clientHeight;
  camera.updateProjectionMatrix();
  renderer.setSize(container.clientWidth, container.clientHeight);
});
