# Referencias y créditos

El JSON `data/risk_inputs.json` usa los nombres y tipos del contrato del equipo. Misión, radiación y CO₂ son provisionales y no deben presentarse como mediciones reales. El campo `_quality` permite distinguir ese estado.

## Datos abiertos de la NASA

| Campo / uso | Fuente | Estado / dato |
| --- | --- | --- |
| `generated` | Fecha UTC de generación local | Fecha de actualización. |
| `mission_day` | Ejemplo del contrato del equipo | Provisional: 112. Reemplazar con el día real de misión. |
| `radiation_uSv_h` | [API RadLab](https://visualization.osdr.nasa.gov/radlab/) | El contrato requiere µSv/h (dosis equivalente). La consulta ISS disponible devuelve µGy/h (dosis absorbida), que no se convierte automáticamente; el 28 µSv/h del JSON es provisional. La referencia histórica DosTel (28.303083 µGy/h, 2023-01-01) se usa solo como contexto. |
| `cabin_co2_mmHg` | [Environmental Data Application](https://visualization.osdr.nasa.gov/eda/) | EDA ofrece descarga de tablas; no se importó CSV. El valor 3.8 mmHg es provisional del contrato. |
| `solar_flares_30d` | [NASA DONKI FLR (CCMC)](https://ccmc.gsfc.nasa.gov/DONKI-API/get/FLR) · [portal DONKI](https://ccmc.gsfc.nasa.gov/tools/DONKI/) | Ventana de 30 días. El endpoint antiguo `api.nasa.gov/DONKI/FLR` devuelve HTTP 404; se usa el de CCMC (sin clave). La web lo consulta en vivo; si falla muestra "sin dato", no cero. |
| Estudios de hueso y músculo | [OSDR Biological Data API](https://visualization.osdr.nasa.gov/biodata/api/) · [OSD-980](https://osdr.nasa.gov/bio/repo/data/studies/OSD-980) · [OSD-781](https://osdr.nasa.gov/bio/repo/data/studies/OSD-781) | La consulta de metadatos devuelve HTTP 400; se conservan enlaces NASA directos. |
| Contexto de radiación | [NASA HRP: Space Radiation](https://www.nasa.gov/hrp/hazard-space-radiation/) · [NASA: space radiation element](https://www.nasa.gov/reference/about-the-space-radiation-element/) | Referencia textual. |

## Código, librerías y recursos externos

| Recurso | Uso | Licencia / enlace |
| --- | --- | --- |
| [Three.js r170](https://threejs.org/) (`vendor/three/`, incluye `OrbitControls`, `GLTFLoader`, `DRACOLoader`, `BufferGeometryUtils`) | Visor 3D | MIT · [licencia](../vendor/three/LICENSE) |
| [Draco 3D decoder](https://github.com/google/draco) (`vendor/three/examples/jsm/libs/draco/`) | Descompresión del modelo GLB | Apache-2.0 |
| `assets/modelo_zerog_pulse.glb` | Modelo anatómico 3D | **Confirmar autor y licencia del modelo** |
| [Python `requests`](https://requests.readthedocs.io/) | Script de ingesta `data/ingest/fetch_nasa_data.py` | Apache-2.0 |
| Kotlin / Android SDK | App nativa en `android/` | Apache-2.0 |
| [GitHub Pages](https://pages.github.com/) | Despliegue web | — |

## Actualización de datos

Instala Python 3 y requests (`python -m pip install requests`) y ejecuta `python data/ingest/fetch_nasa_data.py`. No hace falta clave de API. El proceso no convierte una consulta fallida en cero ni µGy/h en µSv/h, y guarda la respuesta cruda de DONKI en `data/flares_raw.json`.

Los campos provisionales deben sustituirse por mediciones verificadas antes de usar la app con fines distintos de la demostración.
