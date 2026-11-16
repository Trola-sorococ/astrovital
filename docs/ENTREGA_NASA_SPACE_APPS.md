# Entrega · NASA Space Apps Challenge

Contenido listo para copiar al formulario del equipo en la página oficial de NASA Space Apps. Los campos marcados con **[COMPLETAR]** dependen de datos que solo el equipo conoce.

## 1. Título del proyecto

**AstroVital · Órbita / Bienestar** (equipo ZeroG-Pulse)

Reto elegido: **[COMPLETAR: nombre exacto del reto de NASA Space Apps]**

## 2. Resumen (Summary)

AstroVital es una aplicación web instalable (PWA) y una app Android que muestra el estado de bienestar de un astronauta en tres etapas de misión: antes del viaje, durante la estancia y después del regreso. Un modelo 3D del cuerpo marca en rojo las zonas de atención según la situación evaluada (microgravedad, radiación, CO₂ en cabina, actividad extravehicular, tormentas solares) y en verde los indicadores estables. La app consulta en vivo las llamaradas solares de los últimos 30 días de NASA DONKI y usa referencias de OSDR y RadLab para contextualizar el riesgo de radiación, hueso y músculo. Todo se guarda en el dispositivo y funciona sin conexión. Los signos vitales son simulados y se identifican como tales: la app no es un dispositivo médico. Es importante porque las misiones largas necesitan que tripulación y equipo de tierra entiendan, de forma visual y comprensible, cómo cambian los riesgos para la salud con el tiempo y el clima espacial.

*(≈ 155 palabras; el límite del formulario es 50–200.)*

## 3. Explicación detallada

**Problema.** En misiones de larga duración la microgravedad, la radiación y el ambiente de cabina afectan huesos, músculos, corazón y sistema nervioso. La información existe en datos abiertos de NASA, pero está dispersa y es difícil de interpretar para una persona no especialista.

**Solución.** Un panel de seguimiento centrado en el tripulante:

- **Inicio:** modelo 3D (GLB) del cuerpo, saludo personalizado, tres tarjetas de seguimiento por etapa de misión, estado de las fuentes de datos y registro de estado de ánimo.
- **Escaneo por etapa:** el usuario elige etapa y una de 14 situaciones de demostración; se guarda un registro con fecha, signos simulados y zonas marcadas, que se refleja en el modelo 3D.
- **Signos:** lecturas simuladas que cambian cada 4 s (identificadas como no clínicas) y "efectos acumulados" calculados con `model/risk.js` a partir de la duración de misión del perfil, la radiación y las llamaradas.
- **Historial y Perfil:** registros y datos del tripulante, guardados solo en el dispositivo (`localStorage`).

**Metodología.**
1. Datos de entrada con un contrato JSON (`data/risk_inputs.json`) generado por `data/ingest/fetch_nasa_data.py`.
2. Llamaradas solares consultadas en vivo en NASA DONKI al abrir la app; si no hay conexión se usa la última respuesta válida y, si no existe, se muestra "sin dato" (nunca se asume cero).
3. Modelo experimental de riesgo (`model/risk.js`) con índices 0–1 para columna, fémur, músculo y corazón, con motivo y fuente por zona.
4. Visualización 3D con Three.js; las zonas se asignan por nombre de nodo del modelo (corazón, pulmones, cerebro, columna, fémures, cuerpo).

**Honestidad de los datos.** Misión (día 112), radiación en µSv/h y CO₂ de cabina son valores provisionales del contrato del equipo y se marcan así (`_quality`). La referencia DosTel (28.303083 µGy/h, 2023-01-01) es dosis absorbida histórica y no se convierte a µSv/h. Las zonas rojas son referencias educativas, no lesiones ni dosis personales.

**Impacto esperado.** Herramienta educativa y de concientización sobre salud humana en el espacio, y prototipo de interfaz que podría conectarse a telemetría real (EDA, RadLab, sensores) en trabajo futuro.

**Herramientas.** HTML/CSS/JavaScript (módulos ES), Three.js 0.170 (copia local en `vendor/`), PWA (manifest + service worker), Python 3 + `requests` para la ingesta, Kotlin para la app Android nativa (carpeta `android/`), GitHub Pages para el despliegue.

**Limitaciones.** No hay sensores reales; los signos vitales son simulados; el modelo de riesgo es experimental y no ofrece diagnóstico ni pronóstico clínico.

## 4. Demostración visual (elegir una)

- Presentación de máximo 7 láminas (enlace público): <https://canva.link/1wiybgnzjpgqzbb>
- O video de hasta 30 s (YouTube/Vimeo, enlace público): **[COMPLETAR, solo si usan video]**

El formulario oficial exige todos los campos en inglés: ver [`SUBMISSION_FORM_EN.md`](SUBMISSION_FORM_EN.md).

Guion sugerido para 30 s: (0–5 s) problema y nombre; (5–15 s) escaneo → modelo 3D con zonas rojas/verdes; (15–23 s) datos NASA en vivo (DONKI) y efectos acumulados; (23–30 s) instalación sin conexión y aviso de uso no clínico.

## 5. Enlace al proyecto funcional

- Aplicación web: <https://trola-sorococ.github.io/astrovital/> (sin registro, claves ni pagos)
- Repositorio de código abierto: <https://github.com/Trola-sorococ/astrovital>
- APK Android: `android/ZeroG-Pulse.apk` en el repositorio

## 6. Uso de datos de la NASA

| Recurso | Uso en el proyecto | Enlace |
| --- | --- | --- |
| NASA DONKI (Space Weather Database) | Llamaradas solares de los últimos 30 días, consultadas en vivo | <https://ccmc.gsfc.nasa.gov/tools/DONKI/> · API: <https://ccmc.gsfc.nasa.gov/DONKI-API/get/FLR> |
| NASA OSDR RadLab (DosTel, ISS) | Referencia histórica de dosis absorbida | <https://visualization.osdr.nasa.gov/radlab/> |
| NASA OSDR estudios OSD-980 y OSD-781 | Evidencia cualitativa sobre hueso y músculo en microgravedad | <https://osdr.nasa.gov/bio/repo/data/studies/OSD-980> · <https://osdr.nasa.gov/bio/repo/data/studies/OSD-781> |
| NASA OSDR EDA | Fuente prevista para CO₂ de cabina (aún no importada) | <https://visualization.osdr.nasa.gov/eda/> |
| NASA HRP: riesgos de radiación espacial | Contexto para las zonas de atención | <https://www.nasa.gov/hrp/hazard-space-radiation/> |

## 7. Listado de referencias

Ver [`REFERENCIAS.md`](REFERENCIAS.md): datos NASA, librerías (Three.js, Draco), modelo 3D y herramientas.

## 8. Declaración y términos

Al enviar el formulario oficial, **una persona del equipo debe aceptar los términos y condiciones del hackatón** y confirmar que el trabajo es original del equipo. Texto de apoyo:

> Declaramos que AstroVital fue desarrollado por el equipo ZeroG-Pulse durante el hackatón, que el código propio se publica bajo licencia MIT (ver `LICENSE`) y que los recursos de terceros (datos NASA, Three.js, modelo 3D) están acreditados en `docs/REFERENCIAS.md`.

- [ ] Términos y condiciones aceptados en el formulario oficial **[COMPLETAR]**
- [ ] Integrantes del equipo: **[COMPLETAR]**
- [ ] Origen y licencia del modelo 3D `assets/modelo_zerog_pulse.glb` confirmados **[COMPLETAR]**
