import * as THREE from 'three';
import { OrbitControls } from 'three/addons/controls/OrbitControls.js';
import { GLTFLoader } from 'three/addons/loaders/GLTFLoader.js';
import { DRACOLoader } from 'three/addons/loaders/DRACOLoader.js';

const container = document.getElementById('canvas-container');
const statusEl = document.getElementById('connection-status');

// 1. Escena, Cámara y Renderizador
const scene = new THREE.Scene();
const camera = new THREE.PerspectiveCamera(45, container.clientWidth / container.clientHeight, 0.05, 100);
camera.position.set(0, 1.0, 2.4);

const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });
renderer.setSize(container.clientWidth, container.clientHeight);
renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
container.appendChild(renderer.domElement);

// 2. Controles Orbitales
const controls = new OrbitControls(camera, renderer.domElement);
controls.enableDamping = true;
controls.target.set(0, 0.9, 0);

// 3. Iluminación Holográfica
scene.add(new THREE.AmbientLight(0x88ccff, 1.4));
const dirLight = new THREE.DirectionalLight(0xffffff, 2.2);
dirLight.position.set(2, 4, 3);
scene.add(dirLight);
const rimLight = new THREE.DirectionalLight(0x00f0ff, 1.5);
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

let heartGroup = null;
let heartBaseScale = new THREE.Vector3(1, 1, 1);

function assignNodeMaterials(root) {
  root.traverse((obj) => {
    if (!obj.isMesh) return;

    let current = obj;
    let matchedKey = 'other';
    while (current) {
      const name = (current.name || '').toLowerCase();
      if (name === 'heart') {
        matchedKey = 'heart';
        if (!heartGroup) {
          heartGroup = current;
          heartBaseScale.copy(current.scale);
        }
        break;
      }
      if (name === 'bone_spine') { matchedKey = 'bone_spine'; break; }
      if (name === 'bone_left_femur') { matchedKey = 'bone_left_femur'; break; }
      if (name === 'bone_right_femur') { matchedKey = 'bone_right_femur'; break; }
      if (name === 'body') { matchedKey = 'body'; break; }
      current = current.parent;
    }

    if (matchedKey === 'body') {
      obj.material = new THREE.MeshStandardMaterial({
        color: 0x00e5ff,
        transparent: true,
        opacity: 0.22,
        roughness: 0.2,
        depthWrite: false
      });
    } else {
      obj.material = new THREE.MeshStandardMaterial({
        color: 0xd8e8ff,
        emissive: 0x000000,
        emissiveIntensity: 0.4,
        roughness: 0.4
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

    // Ocultar temporalmente el corazón si quedó desproporcionado para encuadrar el cuerpo
    if (heartGroup) heartGroup.visible = false;

    const box = new THREE.Box3().setFromObject(model);
    const size = box.getSize(new THREE.Vector3());
    const center = box.getCenter(new THREE.Vector3());
    const maxDim = Math.max(size.x, size.y, size.z);
    const scale = 1.85 / maxDim;

    model.scale.setScalar(scale);
    model.position.sub(center.multiplyScalar(scale));
    model.position.y += 0.9;

    // Reubicar el corazón dentro del pecho y ajustar su escala al torso
    if (heartGroup) {
      heartGroup.visible = true;
      const heartBox = new THREE.Box3().setFromObject(heartGroup);
      const heartSize = heartBox.getSize(new THREE.Vector3());
      const maxHeart = Math.max(heartSize.x, heartSize.y, heartSize.z);
      if (maxHeart > 0.28) {
        const fixRatio = 0.16 / maxHeart;
        heartBaseScale.multiplyScalar(fixRatio);
        heartGroup.scale.copy(heartBaseScale);
      }
      // Centrar en el pecho (ligeramente a la izquierda del esternón)
      heartGroup.position.set(-0.04 / scale, 0.35 / scale, 0.03 / scale);
    }

    scene.add(model);
    statusEl.textContent = 'Modelo 3D Activo • Simulación en vivo';
    updateSimulation();
  },
  undefined,
  (error) => {
    console.error('Error cargando GLB:', error);
    statusEl.textContent = 'Error al cargar modelo_zerog_pulse.glb';
  }
);

// 6. Lógica de Riesgo en Microgravedad y Colores Dinámicos
const daysSlider = document.getElementById('days-slider');
const bpmSlider = document.getElementById('bpm-slider');
const exerciseSlider = document.getElementById('exercise-slider');

function getRiskColor(score) {
  if (score < 0.35) return { hex: 0x00ff88, label: 'Óptimo', css: '#00ff88' };
  if (score < 0.70) return { hex: 0xffcc00, label: 'Moderado', css: '#ffcc00' };
  return { hex: 0xff3355, label: 'Crítico', css: '#ff3355' };
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

// 7. Animación de latido respetando la escala original del corazón
const clock = new THREE.Clock();
function animate() {
  requestAnimationFrame(animate);
  const t = clock.getElapsedTime();
  const bpm = Number(bpmSlider.value);
  const pulse = 1 + Math.sin(t * (bpm / 60) * Math.PI * 2) * 0.06;

  if (heartGroup) {
    heartGroup.scale.copy(heartBaseScale).multiplyScalar(pulse);
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
