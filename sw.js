// Service worker de AstroVital: permite instalar la app y usarla sin conexión.
// Estrategia: cache de la "app shell" al instalar; los datos de la NASA (DONKI) siempre van a la red.
const CACHE = 'astrovital-v1';
const SHELL = [
  './',
  './index.html',
  './manifest.webmanifest',
  './ui/styles.css',
  './ui/app.js',
  './model/risk.js',
  './data/risk_inputs.json',
  './assets/modelo_zerog_pulse.glb',
  './assets/icon-192.png',
  './assets/icon-512.png',
  './vendor/three/build/three.module.js',
  './vendor/three/examples/jsm/controls/OrbitControls.js',
  './vendor/three/examples/jsm/loaders/GLTFLoader.js',
  './vendor/three/examples/jsm/loaders/DRACOLoader.js',
  './vendor/three/examples/jsm/utils/BufferGeometryUtils.js',
  './vendor/three/examples/jsm/libs/draco/gltf/draco_decoder.js',
  './vendor/three/examples/jsm/libs/draco/gltf/draco_decoder.wasm',
  './vendor/three/examples/jsm/libs/draco/gltf/draco_wasm_wrapper.js'
];

self.addEventListener('install', (event) => {
  event.waitUntil(caches.open(CACHE).then((c) => c.addAll(SHELL)).then(() => self.skipWaiting()));
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches
      .keys()
      .then((keys) => Promise.all(keys.filter((k) => k !== CACHE).map((k) => caches.delete(k))))
      .then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', (event) => {
  const req = event.request;
  const url = new URL(req.url);
  // Solo recursos del mismo origen; la API de la NASA nunca se cachea aquí.
  if (req.method !== 'GET' || url.origin !== self.location.origin) return;
  // Red primero (siempre la versión más nueva); si no hay conexión, se usa la copia guardada.
  event.respondWith(
    fetch(req)
      .then((res) => {
        if (res.ok) {
          const copy = res.clone();
          caches.open(CACHE).then((c) => c.put(req, copy));
        }
        return res;
      })
      .catch(() => caches.match(req).then((hit) => hit || caches.match('./index.html')))
  );
});
