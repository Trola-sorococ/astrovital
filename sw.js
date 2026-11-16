// Service worker de AstroVital: permite instalar la app y usarla sin conexión.
// Estrategia: cache de la "app shell" al instalar; los datos externos (DONKI) siempre van a la red.
const CACHE = 'astrovital-v1';
const SHELL = [
  './',
  './index.html',
  './manifest.webmanifest',
  './ui/styles.css',
  './ui/app.js',
  './ui/i18n.js',
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
  // Solo recursos del mismo origen; las APIs externas nunca se cachean aquí. El APK tampoco (es una descarga grande).
  if (req.method !== 'GET' || url.origin !== self.location.origin || url.pathname.endsWith('.apk')) return;
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
