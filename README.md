# AstroVital · Órbita / Bienestar

Equipo **ZeroG-Pulse** · NASA Space Apps Challenge

Aplicación web instalable (PWA) y app Android para seguir de forma visual el estado de bienestar de un astronauta antes, durante y después de una misión, usando datos abiertos de la NASA.

**Demo en línea (sin registro):** <https://trola-sorococ.github.io/astrovital/>

> Proyecto educativo. Los signos vitales son simulados y el modelo de riesgo es experimental: no es un dispositivo médico ni ofrece diagnóstico.

## Qué incluye

- Modelo 3D del cuerpo con zonas verdes (estables) y rojas (atención) según la situación evaluada.
- Seguimiento por etapa de misión, historial, perfil y registro de ánimo (todo guardado en el dispositivo).
- Llamaradas solares de los últimos 30 días **en vivo** desde NASA DONKI, con caché y sin asumir cero si falla la consulta.
- Efectos acumulados calculados con `model/risk.js`.
- Funciona sin conexión (Three.js y el decodificador Draco están en `vendor/`) y se puede instalar desde el navegador.
- App Android nativa en `android/` (APK: `android/ZeroG-Pulse.apk`).

## Ejecutar en local

El navegador exige un servidor HTTP (no abrir `index.html` con doble clic):

```bash
python -m http.server 8000
# abrir http://localhost:8000
```

## Estructura

| Ruta | Contenido |
| --- | --- |
| `index.html`, `ui/` | Interfaz web |
| `model/risk.js` | Modelo experimental de riesgo |
| `data/` | `risk_inputs.json` y script de ingesta (`data/ingest/fetch_nasa_data.py`) |
| `assets/` | Modelo 3D GLB e íconos |
| `vendor/three/` | Three.js r170 (MIT) y decodificador Draco, copia local |
| `manifest.webmanifest`, `sw.js` | Instalación y uso sin conexión (PWA) |
| `android/` | App Android (Kotlin) |
| `docs/` | Entrega para NASA Space Apps y referencias |

## Entrega NASA Space Apps

- Título, resumen, explicación detallada, uso de datos NASA y declaración: [`docs/ENTREGA_NASA_SPACE_APPS.md`](docs/ENTREGA_NASA_SPACE_APPS.md)
- Referencias y créditos: [`docs/REFERENCIAS.md`](docs/REFERENCIAS.md)

## Datos NASA utilizados

- [NASA DONKI](https://ccmc.gsfc.nasa.gov/tools/DONKI/): llamaradas solares
- [NASA OSDR RadLab](https://visualization.osdr.nasa.gov/radlab/): dosimetría DosTel (ISS)
- [NASA OSDR](https://osdr.nasa.gov/bio/repo/data/studies/OSD-980): estudios OSD-980 y [OSD-781](https://osdr.nasa.gov/bio/repo/data/studies/OSD-781)

## Licencia

MIT (ver [`LICENSE`](LICENSE)). Las librerías de terceros conservan sus licencias.
