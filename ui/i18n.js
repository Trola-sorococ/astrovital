// Internacionalización ligera (ES / EN).
// El español es el idioma fuente: t('texto en español') devuelve la traducción si el idioma activo es inglés.
// Los textos con valores usan {0}, {1}… como marcadores.

const LANG_KEY = 'astrovital_lang_v1';

function detectLang() {
  const params = new URLSearchParams(location.search);
  const q = params.get('lang');
  if (q === 'en' || q === 'es') {
    localStorage.setItem(LANG_KEY, q);
    return q;
  }
  const saved = localStorage.getItem(LANG_KEY);
  if (saved === 'en' || saved === 'es') return saved;
  return (navigator.language || 'es').toLowerCase().startsWith('es') ? 'es' : 'en';
}

export const LANG = detectLang();
export const LOCALE = LANG === 'en' ? 'en-US' : 'es-GT';

const EN = {
  // ---- Encabezado y navegación ----
  'ÓRBITA / BIENESTAR': 'ORBIT / WELLNESS',
  'Cargando modelo...': 'Loading model...',
  'Modelo listo · En línea': 'Model ready · Online',
  'Error cargando modelo 3D': 'Error loading 3D model',
  'Inicio': 'Home',
  'Signos': 'Vitals',
  'Historial': 'History',
  'Perfil': 'Profile',
  'Navegación principal': 'Main navigation',

  // ---- Inicio ----
  'CENTRO DE SALUD · MISIÓN AURORA': 'HEALTH CENTER · AURORA MISSION',
  'Hola, tripulante': 'Hello, crew member',
  'Hola, {0}': 'Hello, {0}',
  'Resumen personal de bienestar y signos vitales.': 'Personal summary of wellness and vital signs.',
  'SEGUIMIENTO DE MISIÓN': 'MISSION TRACKING',
  'Tripulante': 'Crew member',
  'Modelo anatómico local · arrastra la figura para girarla': 'Local anatomical model · drag the figure to rotate it',
  'ESCANEO 3D': '3D SCAN',
  'Modelo anatómico listo · arrastra para girarlo': 'Anatomical model ready · drag to rotate it',
  'Escanear y guardar resultados': 'Scan and save results',
  'Seguimiento por misión': 'Mission tracking',
  'Consulta los registros guardados en cada fase': 'Review the records saved in each phase',
  'TRES ETAPAS': 'THREE STAGES',
  'Antes del viaje': 'Before the trip',
  'Durante la misión': 'During the mission',
  'Después del viaje': 'After the trip',
  'Aún no hay escaneos · toca para abrir ·': 'No scans yet · tap to open ·',
  'Último registro: {0} · toca para ver ·': 'Last record: {0} · tap to view ·',
  '{0} · Zonas señaladas: {1}': '{0} · Highlighted zones: {1}',

  // ---- Fuentes conectadas ----
  'Fuentes conectadas': 'Connected sources',
  'DONKI · llamaradas solares (últimos 30 días) + OSDR': 'DONKI · solar flares (last 30 days) + OSDR',
  'JSON + API en línea': 'JSON + online API',
  'Consultando DONKI…': 'Querying DONKI…',
  'JSON · cargando data/risk_inputs.json…': 'JSON · loading data/risk_inputs.json…',
  'Radiación DosTel (OSDR RadLab): 28.303083 µGy/h · referencia histórica (2023-01-01)':
    'DosTel radiation (OSDR RadLab): 28.303083 µGy/h · historical reference (2023-01-01)',
  'No equivale a µSv/h ni representa exposición actual de la tripulación.':
    'This is not µSv/h and does not represent the crew’s current exposure.',
  'CO₂ cabina: sin dato · no incluido en este JSON': 'Cabin CO₂: no data · not included in this JSON',
  'Llamaradas DONKI: consultando… · no se asume cero si falla la consulta':
    'DONKI flares: querying… · zero is not assumed if the query fails',
  'Actualizar API de clima espacial': 'Refresh space weather API',
  'JSON · generado {0} · estado: incluido con la aplicación · misión, radiación y CO₂ provisionales':
    'JSON · generated {0} · status: bundled with the app · mission, radiation and CO₂ are provisional',
  'sin fecha': 'no date',
  'JSON · no disponible (sin datos de entrada) · no se inventan valores':
    'JSON · unavailable (no input data) · values are not invented',
  '• {0} · clase {1}': '• {0} · class {1}',
  'Llamaradas DONKI: {0} evento(s) en 30 días': 'DONKI flares: {0} event(s) in 30 days',
  ' · caché del {0}': ' · cached on {0}',
  'Llamaradas DONKI: sin dato (unavailable) · no se asume cero': 'DONKI flares: no data (unavailable) · zero is not assumed',
  'API DONKI actualizada {0} · {1} llamaradas en los últimos 30 días':
    'DONKI API updated {0} · {1} flares in the last 30 days',
  'Sin conexión con DONKI · mostrando la última respuesta válida ({0})':
    'No connection to DONKI · showing the last valid response ({0})',
  'Sin conexión con DONKI · sin respuesta guardada': 'No connection to DONKI · no saved response',
  'Datos de clima espacial actualizados': 'Space weather data updated',
  'No se pudo consultar DONKI; se muestra el último dato guardado': 'Could not query DONKI; showing the last saved data',

  // ---- Estado de ánimo ----
  '¿Cómo te sientes?': 'How are you feeling?',
  'Registro subjetivo de bienestar diario': 'Subjective daily wellness log',
  'GUARDADO EN EL TELÉFONO': 'SAVED ON THE DEVICE',
  'Muy bajo': 'Very low',
  'Bajo': 'Low',
  'Neutral': 'Neutral',
  'Bien': 'Good',
  'Muy bien': 'Very good',
  'Nota personal (opcional)': 'Personal note (optional)',
  'Sueño, ánimo o algo que quieras recordar...': 'Sleep, mood or anything you want to remember...',
  'Guardar registro en este dispositivo': 'Save log on this device',
  'Estado actual: Bien': 'Current status: Good',
  'Estado actual: {0}': 'Current status: {0}',
  'Estado seleccionado: {0}': 'Selected status: {0}',
  '✓ Guardado: {0}': '✓ Saved: {0}',
  'Estado de ánimo guardado en este dispositivo': 'Mood saved on this device',

  // ---- Detalle de etapa ----
  '← Volver a Inicio': '← Back to Home',
  'Escaneo visual de demostración · los valores no son mediciones clínicas.':
    'Demo visual scan · the values are not clinical measurements.',
  'Sin escaneos en esta etapa': 'No scans in this stage',
  'Sin escaneos en {0}': 'No scans in {0}',
  'Presiona el botón inferior para registrar una evaluación en esta fase.': 'Press the button below to record an assessment in this phase.',
  'Verde = indicadores estables; rojo = zona destacada por la situación.': 'Green = stable indicators; red = zone highlighted by the situation.',
  'MODELO 3D GLB': '3D GLB MODEL',
  'La malla del archivo tiene corazón, pulmones, cerebro, esqueleto y órganos.': 'The model mesh includes heart, lungs, brain, skeleton and organs.',
  'Registrar escaneo en esta etapa': 'Record scan in this stage',
  'Registros de esta etapa': 'Records for this stage',
  'Historial filtrado por fase de misión': 'History filtered by mission phase',
  '{0} · registro local': '{0} · local record',
  'Situación evaluada:': 'Assessed situation:',
  'simulación': 'simulation',
  '● Zonas destacadas por simulación: {0}': '● Zones highlighted by simulation: {0}',
  '● Indicadores simulados estables · sin zonas marcadas': '● Stable simulated indicators · no zones marked',
  'Frecuencia cardíaca: {0} lpm · Oxígeno: {1}% · Temperatura: {2} °C (datos simulados)':
    'Heart rate: {0} bpm · Oxygen: {1}% · Temperature: {2} °C (simulated data)',
  'Sueño (dato demo): {0} h · Estrés (dato demo): {1}/100': 'Sleep (demo data): {0} h · Stress (demo data): {1}/100',
  'Aún no hay registros en esta etapa.': 'No records in this stage yet.',
  'con zonas marcadas': 'with marked zones',
  'estable': 'stable',

  // ---- Signos ----
  'Seguimiento de misión': 'Mission tracking',
  'Signos vitales de demostración · cambian cada 4 segundos': 'Demo vital signs · they change every 4 seconds',
  'LECTURAS SIMULADAS': 'SIMULATED READINGS',
  'Sirven para revisar la interfaz y el flujo de seguimiento. No son mediciones de una persona.':
    'They are used to review the interface and the tracking flow. They are not measurements of a person.',
  'Actualización:': 'Updated:',
  'Frecuencia cardíaca': 'Heart rate',
  'Oxígeno en sangre': 'Blood oxygen',
  'Temperatura corporal': 'Body temperature',
  'Sueño': 'Sleep',
  'Estrés': 'Stress',
  'Ejemplo variable no clínico': 'Variable non-clinical example',
  '{0} lpm': '{0} bpm',
  'Efectos acumulados de misión': 'Accumulated mission effects',
  'Horizonte: {0} días · duración fija del perfil': 'Horizon: {0} days · fixed profile duration',
  'Hueso (columna): pendiente · falta radiación válida': 'Bone (spine): pending · valid radiation data missing',
  'Hueso (fémur): calculando…': 'Bone (femur): calculating…',
  'Músculo (piernas): calculando…': 'Muscle (legs): calculating…',
  'Hueso (fémur): {0}% de índice estimado': 'Bone (femur): {0}% estimated index',
  'Músculo (piernas): {0}% de índice estimado': 'Muscle (legs): {0}% estimated index',
  '+ Huesos: cómo revisar': '+ Bones: how to review',
  'En microgravedad prolongada se reduce la carga mecánica sobre columna lumbar y fémur. Se recomienda ejercicio resistivo diario (ARED) y monitoreo densitométrico.':
    'In prolonged microgravity the mechanical load on the lumbar spine and femur is reduced. Daily resistive exercise (ARED) and bone-density monitoring are recommended.',
  '+ Músculos: cómo revisar': '+ Muscles: how to review',
  'Los grupos posturales y de extremidades inferiores pierden masa sin gravedad. Evalúa fuerza de agarre, resistencia en cinta y protocolo de contramedidas.':
    'Postural and lower-limb muscle groups lose mass without gravity. Assess grip strength, treadmill endurance and the countermeasure protocol.',
  '+ Índice cardíaco': '+ Cardiac index',
  'El desplazamiento cefálico de fluidos modifica el volumen sistólico. El índice cardíaco requiere telemetría de frecuencia y validación de respuesta API.':
    'The headward fluid shift changes stroke volume. The cardiac index requires heart-rate telemetry and API response validation.',
  '+ Radiación: cómo interpretar': '+ Radiation: how to interpret',
  'DosTel mide tasa de dosis absorbida (µGy/h). Valores elevados durante tormentas solares incrementan el riesgo acumulativo en tejidos formadores de sangre.':
    'DosTel measures absorbed dose rate (µGy/h). High values during solar storms increase cumulative risk in blood-forming tissues.',
  '+ Clima espacial: respuesta operativa': '+ Space weather: operational response',
  'Ante llamaradas clase M o X en los últimos 30 días, se prioriza resguardo en módulos con mayor blindaje y seguimiento de dosimetría.':
    'With class M or X flares in the last 30 days, shelter in better-shielded modules and dosimetry follow-up are prioritized.',
  'Modelo experimental adaptado de model/risk.js: no es diagnóstico ni pronóstico clínico.':
    'Experimental model adapted from model/risk.js: not a diagnosis or clinical prognosis.',
  'Actualizar datos de clima espacial': 'Refresh space weather data',

  // ---- Historial ----
  'Escaneos guardados por etapa · solo en este dispositivo': 'Scans saved by stage · only on this device',
  'Limpiar historial': 'Clear history',
  'Historial limpiado': 'History cleared',
  'No hay escaneos guardados aún. Usa "Escanear y guardar resultados" en Inicio.':
    'No scans saved yet. Use "Scan and save results" on Home.',
  '● Zonas marcadas: {0}': '● Marked zones: {0}',
  '● Estado estable · sin zonas marcadas': '● Stable status · no zones marked',
  'Situación: {0}': 'Situation: {0}',
  'FC {0} lpm · SpO₂ {1}% · Temp. {2} °C (simulado)': 'HR {0} bpm · SpO₂ {1}% · Temp. {2} °C (simulated)',
  'Escaneo guardado en su etapa y en Historial': 'Scan saved in its stage and in History',

  // ---- Perfil ----
  'Perfil del tripulante': 'Crew member profile',
  'Información básica para personalizar el seguimiento. Se guarda solo en este dispositivo.':
    'Basic information to personalize tracking. Saved only on this device.',
  'Nombre': 'Name',
  'Edad': 'Age',
  'Tipo de sangre': 'Blood type',
  'Alergias': 'Allergies',
  'Condiciones': 'Conditions',
  'Contacto': 'Contact',
  'Duración total de misión (días)': 'Total mission duration (days)',
  'Ej. Alex': 'E.g. Alex',
  'Años': 'Years',
  'Ej. O+': 'E.g. O+',
  'Indica alergias conocidas': 'List known allergies',
  'Información que el equipo de salud deba conocer': 'Information the health team should know',
  'Nombre y teléfono': 'Name and phone',
  'Ej. 360 días en total': 'E.g. 360 days in total',
  'Guardar perfil en este dispositivo': 'Save profile on this device',
  'Los datos del perfil son informativos y no sustituyen una evaluación clínica.':
    'Profile data is informational and does not replace a clinical evaluation.',
  'Perfil guardado en este dispositivo': 'Profile saved on this device',

  // ---- Créditos ----
  'Créditos y datos abiertos': 'Credits and open data',
  'Descargar app Android': 'Download Android app',
  'Versión nativa de AstroVital para Android. La app está solo en español. Si Android lo pide, permite instalar apps desde esta fuente.':
    'Native Android version of AstroVital. The app is in Spanish only. If Android asks, allow installing apps from this source.',
  'Descargar APK (Android)': 'Download APK (Android)',
  'APK · versión 0.7.0 · 7.7 MB': 'APK · version 0.7.0 · 7.7 MB',
  'Datos abiertos:': 'Open data:',
  '· llamaradas solares': '· solar flares',
  '· estudios OSD-980 y OSD-781': '· studies OSD-980 and OSD-781',
  '· dosimetría DosTel ISS': '· ISS DosTel dosimetry',
  '· visor 3D (MIT)': '· 3D viewer (MIT)',
  '· referencias completas': '· full references',
  'Proyecto educativo del equipo ZeroG-Pulse (Space Apps Challenge). No está afiliado a ninguna agencia espacial. No es un dispositivo médico ni sustituye una evaluación clínica.':
    'Educational project by team ZeroG-Pulse (Space Apps Challenge). Not affiliated with any space agency. It is not a medical device and does not replace a clinical evaluation.',

  // ---- Modales ----
  '¿En qué etapa estás?': 'Which stage are you in?',
  'Escaneo inicial · antes del viaje': 'Initial scan · before the trip',
  'Escaneo al salir del planeta': 'Scan when leaving the planet',
  'Nuevo escaneo durante la estancia': 'New scan during the stay',
  'Escaneo posterior · regreso': 'Post-trip scan · return',
  'CANCELAR': 'CANCEL',
  '¿Qué situación quieres evaluar? (simulación)': 'Which situation do you want to assess? (simulation)',

  // ---- Situaciones ----
  'Órbita nominal': 'Nominal orbit',
  'Actividad extravehicular (EVA)': 'Extravehicular activity (EVA)',
  'Microgravedad prolongada': 'Prolonged microgravity',
  'Evento o tormenta solar': 'Solar event or storm',
  'Exposición a radiación espacial': 'Space radiation exposure',
  'Aumento de CO₂ en cabina': 'Rising cabin CO₂',
  'Descanso insuficiente': 'Insufficient rest',
  'Tormenta solar': 'Solar storm',
  'Fuga de CO₂ en cabina': 'Cabin CO₂ leak',
  'Fatiga del tripulante': 'Crew fatigue',
  'Erupciones solares y clima espacial': 'Solar flares and space weather',
  'Biología del vuelo espacial': 'Spaceflight biology',
  'Dosimetría de radiación': 'Radiation dosimetry',
  'Telemetría de cabina ISS': 'ISS cabin telemetry',

  'corazón y vasos (referencia visual)': 'heart and vessels (visual reference)',
  'extremidades (referencia visual del esqueleto)': 'limbs (skeleton visual reference)',
  'esqueleto (columna y fémur)': 'skeleton (spine and femur)',
  'masa muscular (referencia visual)': 'muscle mass (visual reference)',
  'esqueleto (referencia visual a tejidos formadores de sangre)': 'skeleton (visual reference to blood-forming tissues)',
  'cerebro/SNC (referencia visual)': 'brain/CNS (visual reference)',
  'pulmones': 'lungs',

  'Indicadores simulados estables · sin zonas marcadas.': 'Stable simulated indicators · no zones marked.',
  'Mayor demanda cardiovascular y carga articular durante caminata espacial.': 'Higher cardiovascular demand and joint load during a spacewalk.',
  'Desacondicionamiento óseo y pérdida de masa muscular por falta de carga gravitacional.':
    'Bone deconditioning and muscle mass loss due to lack of gravitational load.',
  'Incremento de flujo de partículas energéticas; requiere monitoreo de dosimetría.': 'Increased flux of energetic particles; requires dosimetry monitoring.',
  'Exposición acumulada a radiación cósmica galáctica y partículas solares.': 'Accumulated exposure to galactic cosmic radiation and solar particles.',
  'Elevación leve de presión parcial de CO₂; compensación respiratoria y cardiovascular.':
    'Mild rise in CO₂ partial pressure; respiratory and cardiovascular compensation.',
  'Sueño y estrés se modifican como datos de demostración, no como mediciones.': 'Sleep and stress are modified as demo data, not as measurements.',
  'Alerta geomagnética activa; se sugiere resguardo en zona blindada.': 'Active geomagnetic alert; shelter in a shielded area is suggested.',
  'Variación crítica de atmósfera interna; revisión inmediata de depuradores.': 'Critical variation in the internal atmosphere; immediate check of the scrubbers.',
  'Fatiga acumulada tras jornadas extendidas en órbita.': 'Accumulated fatigue after extended days in orbit.',
  'Actividad solar clase M/X registrada en los últimos 30 días.': 'Class M/X solar activity recorded in the last 30 days.',
  'Adaptación fisiológica general al entorno de caída libre.': 'General physiological adaptation to the free-fall environment.',
  'La dosimetría requiere lecturas y periodo de exposición; el dato DosTel disponible es histórico.':
    'Dosimetry requires readings and an exposure period; the available DosTel data is historical.',
  'Parámetros ambientales nominales dentro de rango operativo.': 'Nominal environmental parameters within operating range.',

  // ---- Metadatos ----
  'Órbita / Bienestar | AstroVital': 'Orbit / Wellness | AstroVital',
  'AstroVital: seguimiento visual del estado de salud de un astronauta con datos abiertos (DONKI, OSDR, RadLab). Uso educativo, no clínico.':
    'AstroVital: visual tracking of an astronaut’s health status using open data (DONKI, OSDR, RadLab). Educational, non-clinical use.'
};

/** Traduce un texto fuente en español al idioma activo y sustituye {0}, {1}… */
export function t(es, ...args) {
  let s = LANG === 'en' ? (EN[es] ?? es) : es;
  args.forEach((a, i) => {
    s = s.split(`{${i}}`).join(String(a));
  });
  return s;
}

function tr(text) {
  const norm = text.replace(/\s+/g, ' ').trim();
  return norm && EN[norm] ? EN[norm] : null;
}

/** Traduce el HTML estático (nodos de texto y atributos) cuando el idioma activo es inglés. */
export function translateDocument() {
  document.documentElement.lang = LANG;
  const btn = document.getElementById('btn-lang');
  if (btn) {
    btn.textContent = LANG === 'en' ? 'ES' : 'EN';
    btn.title = LANG === 'en' ? 'Cambiar a español' : 'Switch to English';
    btn.addEventListener('click', () => setLang(LANG === 'en' ? 'es' : 'en'));
  }
  if (LANG !== 'en') return;

  document.title = tr(document.title) || document.title;
  const meta = document.querySelector('meta[name="description"]');
  if (meta) meta.content = tr(meta.content) || meta.content;

  const walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT);
  const nodes = [];
  while (walker.nextNode()) nodes.push(walker.currentNode);
  nodes.forEach((node) => {
    if (node.parentElement && ['SCRIPT', 'STYLE'].includes(node.parentElement.tagName)) return;
    const out = tr(node.nodeValue);
    if (out) node.nodeValue = node.nodeValue.replace(node.nodeValue.trim(), out);
  });

  document.querySelectorAll('[placeholder],[title],[aria-label]').forEach((el) => {
    ['placeholder', 'title', 'aria-label'].forEach((attr) => {
      if (el.hasAttribute(attr) && el.id !== 'btn-lang') {
        const out = tr(el.getAttribute(attr));
        if (out) el.setAttribute(attr, out);
      }
    });
  });
}

export function setLang(lang) {
  localStorage.setItem(LANG_KEY, lang);
  const url = new URL(location.href);
  url.searchParams.delete('lang');
  location.replace(url.toString());
}
