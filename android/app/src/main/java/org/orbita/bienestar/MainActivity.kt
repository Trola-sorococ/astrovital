package org.orbita.bienestar

import android.app.Activity
import android.app.AlertDialog
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.Manifest
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.text.SimpleDateFormat
import java.text.Normalizer
import java.util.Date
import java.util.Locale
import kotlin.random.Random

/** Aplicación de bienestar conectada a JSON y a la API de actividad espacial. */
class MainActivity : Activity() {
    private data class ScanScenario(
        val id: String,
        val label: String,
        val note: String,
        val heartRateDelta: Double = 0.0,
        val stressDelta: Double = 0.0,
        val sleepDelta: Double = 0.0,
        val impact: String = "",
        val redZones: Set<String> = emptySet()
    )

    private val scanScenarios = listOf(
        ScanScenario("nominal", "Órbita nominal", "Referencia de demostración; conserva los signos simulados actuales."),
        ScanScenario("eva", "Actividad extravehicular (EVA)", "Esfuerzo añadido a frecuencia cardíaca y estrés de demostración; no predice una respuesta individual.", heartRateDelta = 25.0, stressDelta = 18.0),
        ScanScenario("microgravity", "Microgravedad prolongada", "Contexto de misión; hueso y músculo no se miden con este escaneo."),
        ScanScenario("solar", "Evento o tormenta solar", "Contexto ambiental de DONKI; no equivale a dosis personal de radiación."),
        ScanScenario("radiation", "Exposición a radiación espacial", "Simulación educativa: las zonas rojas son áreas de interés potencial, no daños confirmados.", impact = "La radiación espacial puede aumentar riesgos para varios tejidos; los riesgos estudiados incluyen cáncer, sistema cardiovascular, sistema nervioso central y efectos agudos. Se necesitan dosis medidas y evaluación especializada para valorar a una persona.", redZones = setOf("bones", "heart", "brain")),
        ScanScenario("cabin_co2", "Aumento de CO₂ en cabina", "Situación ambiental simulada; el JSON no tiene una lectura confirmada de CO₂."),
        ScanScenario("sleep", "Descanso insuficiente", "Sueño y estrés se modifican como datos de demostración, no como mediciones.", stressDelta = 15.0, sleepDelta = -2.5),
        ScanScenario("solar_storm", "Tormenta solar", "Alerta ambiental simulada. Los eventos DONKI no indican dosis recibida por la tripulación."),
        ScanScenario("co2_leak", "Fuga de CO₂ en cabina", "Fuga simulada: pulmones en rojo como zona de atención educativa; no hay concentración confirmada ni lesión medida.", impact = "Revisar ventilación y sensores de CO₂ en un escenario de entrenamiento.", redZones = setOf("lungs")),
        ScanScenario("fatigue", "Fatiga del tripulante", "Fatiga simulada: cerebro/SNC se destaca como área de atención cognitiva, no como daño.", stressDelta = 20.0, sleepDelta = -3.0, impact = "El sueño y la carga de trabajo pueden afectar alerta y desempeño; los valores mostrados son demostrativos.", redZones = setOf("brain")),
        ScanScenario("solar_weather", "Erupciones solares y clima espacial", "Muestra el contexto de eventos espaciales consultados; no calcula exposición individual."),
        ScanScenario("space_biology", "Biología del vuelo espacial", "Escenario educativo de cambios investigados durante el vuelo espacial; no estima la biología de una persona.", impact = "Hueso y músculo se destacan como áreas de estudio; el modelo muscular usa el esqueleto como referencia visual.", redZones = setOf("bones", "muscle")),
        ScanScenario("radiation_dosimetry", "Dosimetría de radiación", "La dosimetría requiere lecturas y periodo de exposición; el dato DosTel disponible es histórico, no de la tripulación.", impact = "Esqueleto, corazón y cerebro se marcan como referencias educativas de tejidos de interés, no como daños ni una medición de dosis.", redZones = setOf("bones", "heart", "brain")),
        ScanScenario("iss_telemetry", "Telemetría de cabina ISS", "Vista de entrenamiento: el proyecto no incluye una conexión validada a sensores en vivo de la cabina ISS.")
    )

    // Paleta de negro profundo y grises claros para dar una sensación de cristal ahumado.
    private val ink = Color.rgb(245, 246, 248)
    private val muted = Color.rgb(160, 164, 170)
    private val mint = Color.rgb(211, 215, 220)
    private val background = Color.BLACK
    private val panel = Color.argb(205, 31, 32, 35)
    private var selectedMood = ""
    private var activeTab = "Inicio"

    private var sourceJson = JSONObject()
    private var flareJson = JSONArray()
    private var flareStatus = "Aún no consultada"
    private var flareDataKnown = false
    private var sourceStatus = "JSON local"
    private var jsonConnected = false
    private val io = Executors.newSingleThreadExecutor()
    // Valores de demostración variables para revisar la interfaz hasta integrar una fuente real.
    private val demoVitals = mutableMapOf("hr" to 72.0, "spo2" to 98.0, "temp" to 36.7, "sleep" to 7.2, "stress" to 28.0)
    private val demoHandler = Handler(Looper.getMainLooper())
    private val demoValueViews = mutableMapOf<String, TextView>()
    private var demoStampView: TextView? = null
    private val demoTick = object : Runnable {
        override fun run() { updateDemoVitals(); demoHandler.postDelayed(this, 4_000L) }
    }
    private var demoStamp = ""
    private var missionScans = JSONArray()
    private var selectedPhase = "before"
    private var selectedScanTime: Long? = null
    private var bodyModelView: GlbBodyView? = null
    private var pendingNotification: Pair<String, String>? = null

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "orbita_scan_alerts"
        private const val NOTIFICATION_PERMISSION_REQUEST = 4107
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = background
        window.navigationBarColor = background
        window.decorView.systemUiVisibility = 0
        val prefs = getSharedPreferences("orbita_data", MODE_PRIVATE)
        val importedJson = prefs.getString("imported_json", null)
        sourceJson = (importedJson ?: runCatching {
            assets.open("risk_inputs.json").bufferedReader().use { it.readText() }
        }.getOrDefault("{}")).let { runCatching { JSONObject(it) }.getOrDefault(JSONObject()) }
        jsonConnected = validateRiskJson(sourceJson) == null
        sourceStatus = when {
            !jsonConnected -> "JSON no disponible"
            importedJson != null -> "JSON importado (caché local)"
            else -> "JSON incluido con la aplicación"
        }
        flareJson = runCatching { JSONArray(prefs.getString("donki_flares", "[]")) }.getOrDefault(JSONArray())
        flareStatus = prefs.getString("donki_status", "Aún no consultada") ?: "Aún no consultada"
        flareDataKnown = prefs.contains("donki_flares")
        missionScans = runCatching { JSONArray(getSharedPreferences("orbita_mission_scans", MODE_PRIVATE).getString("entries", "[]")) }.getOrDefault(JSONArray())
        setContentView(buildScreen())
        refreshSolarFlares()
        demoHandler.postDelayed(demoTick, 4_000L)
    }

    override fun onDestroy() { demoHandler.removeCallbacks(demoTick); io.shutdown(); super.onDestroy() }
    override fun onPause() { bodyModelView?.onPause(); super.onPause() }
    override fun onResume() { super.onResume(); bodyModelView?.onResume() }

    /** Construye la pantalla en código para mantener el prototipo ligero y sin dependencias externas. */
    private fun buildScreen(): View {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(this@MainActivity.background) }
        val scroll = ScrollView(this).apply { isFillViewport = true; isVerticalScrollBarEnabled = false }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(12), dp(18), dp(22))
        }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        content.addView(text("ASTROVITAL", 15, mint, true))
        if (activeTab != "Inicio") {
            when (activeTab) {
                "Signos" -> buildSignsPage(content)
                "Historial" -> buildHistoryPage(content)
                "Perfil" -> buildProfilePage(content)
                "Etapa" -> buildMissionStagePage(content)
            }
            root.addView(buildNavigation(), LinearLayout.LayoutParams(-1, -2))
            return root
        }
        content.addView(text("CENTRO DE SALUD · MISIÓN AURORA", 10, mint, true), top(18))
        val profilePrefs = getSharedPreferences("orbita_profile", MODE_PRIVATE)
        val profileName = profilePrefs.getString("name", "")?.trim().orEmpty()
        content.addView(text(if (profileName.isBlank()) "Hola, tripulante" else "Hola, $profileName", 31, ink, true), top(3))
        content.addView(text("Resumen personal de bienestar y signos vitales.", 13, muted), top(3))
        content.addView(chip("◷   SEGUIMIENTO DE MISIÓN"), top(12))
        // La figura GLB se carga desde los assets del APK para funcionar sin internet.
        content.addView(personPreview(), top(14))
        content.addView(sectionTitle("Fuentes conectadas", "JSON + API en línea"), top(20))
        content.addView(dataOverviewCard(), top(10))

        content.addView(sectionTitle("¿Cómo te sientes?", "GUARDADO EN EL TELÉFONO"), top(20))
        val moods = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        listOf("☀️" to "Muy bien", "🙂" to "Bien", "😐" to "Neutral", "😴" to "Cansado", "🌧️" to "Mal").forEach { (emoji, label) ->
            val button = Button(this).apply {
                text = emoji; textSize = 18f; contentDescription = label; minWidth = 0; minimumWidth = 0
                setPadding(0, 0, 0, 0); setTextColor(ink); background = rounded(Color.argb(190, 45, 46, 49), 12, Color.argb(55, 255, 255, 255))
                setOnClickListener { selectedMood = label; Toast.makeText(this@MainActivity, "Estado seleccionado: $label", Toast.LENGTH_SHORT).show() }
            }
            moods.addView(button, LinearLayout.LayoutParams(0, dp(48), 1f).apply { setMargins(dp(3), dp(13), dp(3), 0) })
        }
        content.addView(moods)
        content.addView(text("Nota personal (opcional)", 11, muted), top(12))
        val note = EditText(this).apply {
            hint = "Sueño, ánimo o algo que quieras recordar…"; setHintTextColor(Color.rgb(125, 128, 134)); setTextColor(ink)
            textSize = 13f; minLines = 2; gravity = Gravity.TOP; setPadding(dp(12), dp(10), dp(12), dp(10))
            background = rounded(Color.argb(200, 15, 16, 18), 12, Color.argb(62, 255, 255, 255))
        }
        content.addView(note, top(8))
        // REGISTRO LOCAL: los datos se guardan en preferencias privadas de esta aplicación.
        content.addView(actionButton("Guardar registro en este dispositivo"), top(8))
        val saveButton = content.getChildAt(content.childCount - 1) as Button
        saveButton.setOnClickListener {
            val prefs = getSharedPreferences("orbita_checkins", MODE_PRIVATE)
            prefs.edit().putString("last_mood", selectedMood).putString("last_note", note.text.toString())
                .putLong("saved_at", System.currentTimeMillis()).apply()
            Toast.makeText(this, "Registro guardado en este dispositivo", Toast.LENGTH_SHORT).show()
            notifyCheckIn(selectedMood, note.text.toString())
        }

        content.addView(sectionTitle("Seguimiento por misión", "TRES ETAPAS"), top(20))
        val phases = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        listOf("⌂" to "Antes del viaje", "✧" to "Durante la misión", "⌁" to "Después del viaje").forEach { (icon, phase) ->
            val item = card().apply {
                addView(text("$icon   $phase", 14, ink, true))
                val phaseKey = when (phase) { "Antes del viaje" -> "before"; "Durante la misión" -> "during"; else -> "after" }
                val latest = latestScan(phaseKey)
                addView(text(if (latest == null) "Aún no hay escaneos · toca para abrir →" else "Último registro: ${scanDate(latest)} · toca para ver →", 10, mint), top(5))
                setOnClickListener { openMissionStage(phaseKey) }
            }
            phases.addView(item, top(8))
        }
        content.addView(phases)

        root.addView(buildNavigation(), LinearLayout.LayoutParams(-1, -2))
        return root
    }

    /** Navegación entre pantallas principales; cada pantalla conserva el estilo visual de la app. */
    private fun buildNavigation() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER
        setBackgroundColor(Color.argb(245, 8, 8, 9))
        listOf("⌂\nInicio", "♡\nSignos", "▤\nHistorial", "◎\nPerfil").forEach { label ->
            val tab = label.substringAfter('\n')
            val b = Button(this@MainActivity).apply {
                text = label; textSize = 11f; isAllCaps = false
                setTextColor(if (activeTab == tab) mint else muted)
                background = android.graphics.drawable.ColorDrawable(Color.TRANSPARENT)
                setOnClickListener { activeTab = tab; setContentView(buildScreen()) }
            }
            addView(b, LinearLayout.LayoutParams(0, dp(58), 1f))
        }
    }

    /** Los campos del perfil se guardan localmente en el teléfono y se pueden editar. */
    private fun buildProfilePage(content: LinearLayout) {
        val prefs = getSharedPreferences("orbita_profile", MODE_PRIVATE)
        content.addView(text("Perfil del tripulante", 25, ink, true), top(20))
        content.addView(text("Información básica para personalizar el seguimiento. Se guarda solo en este dispositivo.", 12, muted), top(5))
        val name = profileInput("Nombre", "Ej. Alex Rivera", prefs.getString("name", "") ?: "")
        val age = profileInput("Edad", "Años", prefs.getString("age", "") ?: "", true)
        val blood = profileInput("Tipo de sangre (opcional)", "Ej. O+", prefs.getString("blood", "") ?: "")
        val allergies = profileInput("Alergias relevantes (opcional)", "Indica alergias conocidas", prefs.getString("allergies", "") ?: "")
        val conditions = profileInput("Condiciones o notas médicas (opcional)", "Información que el equipo de salud deba conocer", prefs.getString("conditions", "") ?: "")
        val contact = profileInput("Contacto de emergencia (opcional)", "Nombre y teléfono", prefs.getString("contact", "") ?: "")
        val missionDay = profileInput("Duración total de misión (días)", "Ej. 180 días en total", prefs.getString("mission_day", "") ?: "", true)
        listOf("Nombre" to name, "Edad" to age, "Tipo de sangre" to blood, "Alergias" to allergies, "Condiciones" to conditions, "Contacto" to contact, "Duración total de misión (días)" to missionDay).forEach { (label, field) ->
            content.addView(text(label, 11, muted, true), top(14)); content.addView(field, top(6))
        }
        content.addView(actionButton("Guardar perfil en este dispositivo"), top(20))
        val saveProfileButton = content.getChildAt(content.childCount - 1) as Button
        saveProfileButton.setOnClickListener {
            if (name.text.toString().isBlank() || age.text.toString().toIntOrNull() == null) {
                Toast.makeText(this, "Agrega un nombre y una edad válida para guardar el perfil", Toast.LENGTH_LONG).show()
            } else {
                prefs.edit().putString("name", name.text.toString().trim()).putString("age", age.text.toString().trim())
                    .putString("blood", blood.text.toString().trim()).putString("allergies", allergies.text.toString().trim())
                    .putString("conditions", conditions.text.toString().trim()).putString("contact", contact.text.toString().trim())
                    .putString("mission_day", missionDay.text.toString().trim()).apply()
                (getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager)
                    ?.hideSoftInputFromWindow(saveProfileButton.windowToken, 0)
                activeTab = "Inicio"
                setContentView(buildScreen())
                Toast.makeText(this, "Perfil guardado en este dispositivo", Toast.LENGTH_SHORT).show()
            }
        }
        content.addView(text("Los datos del perfil son informativos y no sustituyen una evaluación clínica.", 10, muted), top(12))
    }

    /** Campo reutilizable para el formulario local de perfil. */
    private fun profileInput(label: String, placeholder: String, value: String, numeric: Boolean = false) = EditText(this).apply {
        hint = placeholder; contentDescription = label; setHintTextColor(Color.rgb(125, 128, 134)); setTextColor(ink)
        setText(value); textSize = 14f; isSingleLine = !label.startsWith("Condiciones")
        if (numeric) inputType = android.text.InputType.TYPE_CLASS_NUMBER
        else if (label.startsWith("Contacto")) inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS
        else inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        setPadding(dp(12), dp(12), dp(12), dp(12))
        background = rounded(Color.argb(200, 15, 16, 18), 12, Color.argb(62, 255, 255, 255))
    }

    /** Muestra tendencias ilustrativas junto a los datos obtenidos del JSON/API. */
    private fun buildSignsPage(content: LinearLayout) {
        demoValueViews.clear()
        content.addView(text("Seguimiento de misión", 25, ink, true), top(20))
        content.addView(text("Signos vitales de demostración · cambian cada 4 segundos", 12, muted), top(5))
        content.addView(card().apply {
            addView(text("LECTURAS SIMULADAS", 10, mint, true))
            addView(text("Sirven para revisar la interfaz y el flujo de seguimiento. No son mediciones de una persona.", 11, muted), top(5))
            demoStampView = text("Actualización: $demoStamp", 10, muted).also { addView(it, top(5)) }
        }, top(12))
        demoVitals.forEach { (key, value) ->
            val (label, unit, decimals) = when (key) {
                "hr" -> Triple("Frecuencia cardíaca", "lpm", 0)
                "spo2" -> Triple("Oxígeno en sangre", "%", 0)
                "temp" -> Triple("Temperatura corporal", "°C", 1)
                "sleep" -> Triple("Sueño", "h", 1)
                else -> Triple("Estrés", "/100", 0)
            }
            content.addView(card().apply {
                addView(text(label, 11, muted, true))
                val valueView = text("${formatValue(value, decimals)} $unit", 23, ink, true)
                demoValueViews[key] = valueView
                addView(valueView, top(6))
                addView(text("Ejemplo variable · no clínico", 9, muted), top(3))
            }, top(8))
        }
        content.addView(sectionTitle("Datos conectados", "ARCHIVO + API"), top(20))
        content.addView(dataOverviewCard(), top(8))
        content.addView(text("Efectos acumulados de misión", 17, ink, true), top(20))
        content.addView(missionEffectsCard(), top(8))
        content.addView(actionButton("Actualizar datos de clima espacial"), top(8))
        (content.getChildAt(content.childCount - 1) as Button).setOnClickListener { refreshSolarFlares(true) }
    }

    /** Genera cambios pequeños únicamente para la vista de demostración de signos vitales. */
    private fun updateDemoVitals() {
        demoVitals["hr"] = ((demoVitals["hr"] ?: 72.0) + Random.nextDouble(-2.0, 2.0)).coerceIn(62.0, 88.0)
        demoVitals["spo2"] = ((demoVitals["spo2"] ?: 98.0) + Random.nextDouble(-0.3, 0.3)).coerceIn(96.0, 99.0)
        demoVitals["temp"] = ((demoVitals["temp"] ?: 36.7) + Random.nextDouble(-0.08, 0.08)).coerceIn(36.3, 37.1)
        demoVitals["sleep"] = ((demoVitals["sleep"] ?: 7.2) + Random.nextDouble(-0.05, 0.05)).coerceIn(5.0, 9.0)
        demoVitals["stress"] = ((demoVitals["stress"] ?: 28.0) + Random.nextDouble(-2.0, 2.0)).coerceIn(10.0, 65.0)
        demoStamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val units = mapOf("hr" to "lpm", "spo2" to "%", "temp" to "°C", "sleep" to "h", "stress" to "/100")
        val decimals = mapOf("hr" to 0, "spo2" to 0, "temp" to 1, "sleep" to 1, "stress" to 0)
        demoVitals.forEach { (key, value) -> demoValueViews[key]?.text = "${formatValue(value, decimals[key] ?: 0)} ${units[key]}" }
        demoStampView?.text = "Actualización: $demoStamp"
    }

    /** Tarjeta compacta de procedencia, calidad de los datos y estado de conexión. */
    private fun dataOverviewCard() = card().apply {
        val inputs = sourceJson.optJSONObject("inputs")
        val generated = sourceJson.optString("generated", sourceJson.optString("generated_at_utc", "sin fecha"))
        val rad = sourceJson.opt("radiation_uSv_h")
        val co2 = sourceJson.opt("cabin_co2_mmHg")
        val dose = inputs?.optJSONObject("iss_dosetel_absorbed_dose_rate")
        val flares = inputs?.optJSONObject("solar_flares_last_30_days")
        val quality = sourceJson.optJSONObject("_quality") ?: JSONObject()
        addView(text("API de clima espacial · llamaradas solares (últimos 30 días)", 12, ink, true))
        addView(text(if (flareDataKnown) "$flareStatus · ${flareJson.length()} eventos en respuesta API guardada" else "$flareStatus · no hay respuesta válida; cero eventos no confirmado", 10, mint), top(5))
        for (i in 0 until minOf(3, flareJson.length())) {
            val event = flareJson.optJSONObject(i) ?: continue
            addView(text("• ${event.optString("beginTime", "Fecha no informada")} · clase ${event.optString("classType", "sin clasificar")}", 9, muted), top(4))
        }
        addView(text("JSON · fecha $generated · estado: $sourceStatus", 10, muted), top(9))
        if (dose != null) {
            val doseValue = dose.opt("absorbed_dose_rate_mean_ugy_h")
            addView(text("Radiación DosTel: ${if (doseValue is Number) "${doseValue} µGy/h" else "sin dato"} · referencia histórica (${dose.optString("period_start", "periodo no indicado")})", 10, muted), top(7))
            addView(text("No equivale a µSv/h ni representa exposición actual de la tripulación.", 9, muted), top(4))
        } else {
            addView(text("Radiación: ${if (rad == JSONObject.NULL || rad == null) "sin dato" else "$rad µSv/h"} · ${quality.optString("radiation_uSv_h", "calidad no especificada")}", 10, muted), top(7))
        }
        addView(text("CO₂ cabina: ${if (co2 == JSONObject.NULL || co2 == null) "sin dato" else "$co2 mmHg"} · ${quality.optString("cabin_co2_mmHg", "no incluido en este JSON")}", 10, muted), top(5))
        if (flares != null) {
            val flareValue = flares.opt("value")
            addView(text("Llamaradas DONKI: ${if (flareValue is Number) flareValue.toString() else "sin dato (${flares.optString("status", "estado desconocido")})"} · no se asume cero", 10, muted), top(5))
        }
        addView(text("Los valores provisionales se muestran como referencia, nunca como mediciones confirmadas.", 9, muted), top(8))
        val latestScan = (0 until missionScans.length()).mapNotNull { missionScans.optJSONObject(it) }
            .maxByOrNull { it.optLong("time") }
        if (latestScan == null) {
            addView(text("Alertas corporales: aún no hay escaneos guardados.", 10, muted), top(8))
        } else {
            val zones = scanZones(latestScan)
            val alertText = if (zones.isEmpty()) "Sin zonas señaladas en el último escaneo" else "Zonas señaladas: ${zonesToLabel(zones)}"
            addView(text("${scanDate(latestScan)} · $alertText", 10, if (zones.isEmpty()) Color.rgb(95, 225, 160) else Color.rgb(255, 105, 105)), top(8))
        }
        if (jsonConnected.not()) {
            addView(actionButton("Importar JSON"), top(10))
            (getChildAt(childCount - 1) as Button).setOnClickListener { importJsonFile() }
        }
        addView(actionButton("Actualizar API de clima espacial"), top(7))
        (getChildAt(childCount - 1) as Button).setOnClickListener { refreshSolarFlares(true) }
    }

    /** Calcula estimaciones de riesgo basadas en el mismo contrato JSON del proyecto; etiqueta insumos provisionales. */
    private fun missionEffectsCard() = card().apply {
        val profileDays = getSharedPreferences("orbita_profile", MODE_PRIVATE).getString("mission_day", "")?.toIntOrNull()?.takeIf { it > 0 }
        val days = profileDays ?: sourceJson.optInt("mission_day", 0).takeIf { it > 0 }
        if (days == null || days <= 0) {
            addView(text("Agrega la duración total en Perfil para estimar efectos de misión.", 11, muted))
            return@apply
        }
        val radRaw = sourceJson.opt("radiation_uSv_h")
        val rad = if (radRaw is Number) radRaw.toDouble() else Double.NaN
        val riskQuality = sourceJson.optJSONObject("_quality")?.optString("radiation_uSv_h", "") ?: ""
        val provisional = riskQuality.contains("provisional", true)
        val femur = minOf(1.0, (days / 300.0) * .7)
        val muscle = minOf(1.0, (days / 180.0) * .75)
        val missionQuality = sourceJson.optJSONObject("_quality")?.optString("mission_day", "") ?: ""
        val horizonLabel = if (profileDays != null) "duración fija del perfil" else "día de misión del JSON (provisional)"
        addView(text("Horizonte: $days días · $horizonLabel${if (provisional || (profileDays == null && missionQuality.contains("provisional", true))) " · insumos provisionales" else ""}", 10, mint, true))
        if (!rad.isNaN()) {
            val spine = minOf(1.0, (days / 365.0) * .8 + (rad / 100.0) * .2)
            addView(text("Hueso (columna): ${formatValue(spine * 100, 1)} % de índice estimado", 11, ink), top(8))
        } else addView(text("Hueso (columna): pendiente · falta radiación válida", 11, muted), top(8))
        addView(text("Hueso (fémur): ${formatValue(femur * 100, 1)} % de índice estimado", 11, ink), top(5))
        addView(text("Músculo (piernas): ${formatValue(muscle * 100, 1)} % de índice estimado", 11, ink), top(5))
        addMissionGuidance(
            "Huesos · cómo revisar",
            "La estimación usa la duración fija ($days días)${if (provisional) " y radiación JSON provisional" else ""}. Revisar evolución de salud ósea con el responsable médico y las evaluaciones disponibles antes, durante y después de la misión. Las medidas preventivas o tratamiento deben ser definidos por el equipo clínico según protocolos de misión."
        )
        addMissionGuidance(
            "Músculos · cómo revisar",
            "El índice depende de la duración prevista ($days días); este JSON no aporta actividad física ni fuerza muscular individual. Comparar con las evaluaciones de la tripulación y revisar el plan de ejercicio y recuperación con el equipo médico/operativo. Sin esos datos, la app no puede recomendar una rutina ni tratamiento concreto."
        )
        if (!rad.isNaN() && flareDataKnown) {
            val heart = minOf(1.0, (rad / 50.0) * .4 + flareJson.length() * .05 + .1)
            addView(text("Corazón: ${formatValue(heart * 100, 1)} % de índice estimado · ${flareJson.length()} eventos de la API", 10, ink), top(8))
        } else addView(text("Índice cardíaco: pendiente · necesita radiación y una respuesta válida de la API.", 10, muted), top(8))
        addMissionGuidance(
            "Radiación · cómo interpretar",
            "El JSON informa ${if (rad.isNaN()) "sin tasa disponible" else "$rad µSv/h"}${if (provisional) " como valor provisional" else ""}. La API de actividad espacial aporta eventos de llamaradas, no la dosis de la tripulación. Para valorar exposición se requiere una fuente de dosimetría validada y su periodo de medición; esta pantalla no calcula dosis acumulada observada ni recomienda tratamiento. Si el dato es provisional o falta, confirmar con el equipo de protección radiológica."
        )
        addMissionGuidance(
            "Clima espacial · respuesta operativa",
            if (flareDataKnown && flareJson.length() > 0) "La API reporta ${flareJson.length()} llamaradas en la ventana consultada. Revisar fecha, clase y avisos operativos con el responsable de clima espacial; la presencia de una llamarada no determina por sí sola la exposición del tripulante ni una acción clínica." else "No hay una respuesta API válida que confirme eventos. Mantener este dato como pendiente y consultar el estado de la conexión; cero eventos no se asume cuando la API no respondió."
        )
        addView(text("Modelo experimental adaptado de model/risk.js; no es diagnóstico ni pronóstico clínico. Resultados dependen de la calidad y disponibilidad de los datos de entrada.", 9, muted), top(8))
    }

    /** Despliegue accesible con contexto de fuente y orientación de revisión para cada efecto. */
    private fun LinearLayout.addMissionGuidance(title: String, detail: String) {
        val toggle = Button(this@MainActivity).apply {
            text = "＋  $title"; textSize = 11f; isAllCaps = false; gravity = Gravity.START or Gravity.CENTER_VERTICAL
            setTextColor(ink); setPadding(dp(8), dp(5), dp(8), dp(5))
            background = android.graphics.drawable.ColorDrawable(Color.TRANSPARENT)
        }
        val details = text(detail, 10, muted).apply { visibility = View.GONE; setPadding(dp(10), dp(4), dp(8), dp(8)) }
        toggle.setOnClickListener {
            val open = details.visibility != View.VISIBLE
            details.visibility = if (open) View.VISIBLE else View.GONE
            toggle.text = (if (open) "−  " else "＋  ") + title
        }
        addView(toggle, top(5))
        addView(details)
    }

    /** El usuario selecciona el JSON exportado por el proyecto; la última versión queda disponible sin conexión. */
    private fun importJsonFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "application/json" }
        startActivityForResult(intent, 4103)
    }

    /** Valida el contrato JSON que consume la app antes de sustituir los datos locales. */
    private fun validateRiskJson(json: JSONObject): String? {
        // Esquema de ingesta de la guía: los datos fuente viven bajo `inputs` y pueden ser parciales.
        if (json.has("schema_version")) {
            if (json.optJSONObject("inputs") == null) return "El esquema ${json.optString("schema_version")} requiere un objeto inputs."
            return null
        }
        fun numberField(name: String, label: String, minimum: Double): String? {
            val value = json.opt(name)
            if (value !is Number) return "$label debe ser un número."
            val numeric = value.toDouble()
            if (!numeric.isFinite() || numeric < minimum) return "$label debe ser un número mayor o igual a $minimum."
            return null
        }
        numberField("mission_day", "mission_day", 0.0)?.let { return it }
        if (json.opt("mission_day") is Number && json.optDouble("mission_day") % 1.0 != 0.0) {
            return "mission_day debe ser un número entero de días."
        }
        numberField("radiation_uSv_h", "radiation_uSv_h", 0.0)?.let { return it }
        numberField("cabin_co2_mmHg", "cabin_co2_mmHg", 0.0)?.let { return it }
        if (json.has("solar_flares_30d") && json.opt("solar_flares_30d") !is JSONArray) {
            return "solar_flares_30d debe ser una lista (array) JSON."
        }
        if (json.has("sources")) {
            val sources = json.opt("sources") as? JSONArray ?: return "sources debe ser una lista (array) JSON."
            for (i in 0 until sources.length()) {
                if (sources.opt(i) !is String) return "Cada elemento de sources debe ser texto."
            }
        }
        if (json.has("_quality") && json.optJSONObject("_quality") == null) {
            return "_quality debe ser un objeto JSON."
        }
        if (json.has("generated") && json.opt("generated") !is String) {
            return "generated debe ser texto con la fecha de generación."
        }
        return null
    }

    /** Consulta la API de clima espacial y conserva la última respuesta para usar sin internet. */
    private fun refreshSolarFlares(showToast: Boolean = false) {
        io.execute {
            try {
                // DONKI migró su API pública en septiembre de 2026; el endpoint nuevo no requiere api.nasa.gov key.
                val connection = (URL("https://ccmc.gsfc.nasa.gov/DONKI-API/get/FLR?startDate=${dateDaysAgo(30)}&endDate=${dateToday()}").openConnection() as HttpURLConnection)
                connection.connectTimeout = 10000; connection.readTimeout = 10000; connection.requestMethod = "GET"
                val code = connection.responseCode
                if (code !in 200..299) throw IllegalStateException("HTTP $code")
                val result = JSONArray(connection.inputStream.bufferedReader().use { it.readText() })
                connection.disconnect()
                flareJson = result; flareDataKnown = true; flareStatus = "API actualizada ${SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date())}"
                getSharedPreferences("orbita_data", MODE_PRIVATE).edit().putString("donki_flares", result.toString()).putString("donki_status", flareStatus).apply()
            } catch (e: Exception) {
                flareStatus = "Error de conexión (${e.message ?: "sin red"}); mostrando caché"
                getSharedPreferences("orbita_data", MODE_PRIVATE).edit().putString("donki_status", flareStatus).apply()
            }
            runOnUiThread { if (showToast) Toast.makeText(this, flareStatus, Toast.LENGTH_LONG).show(); setContentView(buildScreen()) }
        }
    }

    private fun dateToday() = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    private fun dateDaysAgo(days: Int) = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(System.currentTimeMillis() - days * 86400000L))

    /** Importa de forma segura el JSON elegido con el selector de documentos de Android. */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != 4103 || resultCode != RESULT_OK || data?.data == null) return
        try {
            val raw = contentResolver.openInputStream(data.data!!)?.bufferedReader()?.use { it.readText() } ?: error("Archivo vacío")
            val parsed = JSONObject(raw)
            val validationError = validateRiskJson(parsed)
            if (validationError != null) {
                Toast.makeText(this, "JSON no compatible: $validationError No se guardaron cambios.", Toast.LENGTH_LONG).show()
                return
            }
            sourceJson = parsed
            jsonConnected = true
            sourceStatus = "Importado desde archivo · guardado sin conexión"
            getSharedPreferences("orbita_data", MODE_PRIVATE).edit().putString("imported_json", raw).apply()
            Toast.makeText(this, "JSON importado y disponible sin conexión", Toast.LENGTH_LONG).show()
            setContentView(buildScreen())
        } catch (e: Exception) { Toast.makeText(this, "No se pudo leer el JSON: ${e.message}", Toast.LENGTH_LONG).show() }
    }

    /** El historial se mantiene vacío hasta que se añadan registros de datos reales validados. */
    private fun buildHistoryPage(content: LinearLayout) {
        content.addView(text("Historial", 25, ink, true), top(20))
        content.addView(text("Escaneos guardados por etapa · solo en este dispositivo", 12, muted), top(5))
        if (missionScans.length() == 0) {
            content.addView(card().apply {
                addView(text("▤   Aún no hay escaneos", 17, ink, true))
                addView(text("Abre Antes del viaje, Durante la misión o Después del viaje y pulsa Escanear. Cada registro quedará disponible aquí y en su etapa.", 12, muted), top(8))
            }, top(18))
        } else {
            for (i in missionScans.length() - 1 downTo 0) {
                val scan = missionScans.optJSONObject(i) ?: continue
                val zones = scan.optJSONArray("zones") ?: JSONArray()
                val result = if (zones.length() == 0) "Estado estable · sin zonas marcadas" else "Zonas marcadas: ${zonesToLabel(zones)}"
                content.addView(card().apply {
                    addView(text(scan.optString("title", "Escaneo de misión"), 14, ink, true))
                    addView(text("${scanDate(scan)} · ${scan.optString("stageLabel", "Etapa")}", 10, muted), top(4))
                    if (scan.optString("scenario").isNotBlank()) addView(text("Situación: ${scan.optString("scenario")}", 10, mint), top(4))
                    addView(text("●  $result", 10, if (zones.length() == 0) Color.rgb(95, 225, 160) else Color.rgb(255, 95, 95)), top(7))
                    val values = scan.optJSONObject("vitals") ?: JSONObject()
                    addView(text("FC ${values.optInt("hr")} lpm · SpO₂ ${values.optInt("spo2")}% · Temp. ${values.optDouble("temp")} °C", 10, muted), top(6))
                    setOnClickListener { selectedPhase = scan.optString("stage", "before"); selectedScanTime = scan.optLong("time"); activeTab = "Etapa"; setContentView(buildScreen()) }
                }, top(8))
            }
        }
        content.addView(text("API de clima espacial: ${if (flareDataKnown) "${flareJson.length()} eventos en la última respuesta válida" else "sin respuesta válida"} · $flareStatus", 10, mint), top(18))
    }

    /** Ficha independiente para que cada etapa muestre sus resultados y sus escaneos guardados. */
    private fun buildMissionStagePage(content: LinearLayout) {
        val stageName = when (selectedPhase) { "before" -> "Antes del viaje"; "during" -> "Durante la misión"; "during_departure" -> "Durante · salida del planeta"; "during_stay" -> "Durante · estancia"; else -> "Después del viaje" }
        val stageKey = if (selectedPhase.startsWith("during")) "during" else selectedPhase
        content.addView(text("Seguimiento por misión", 12, mint, true), top(16))
        content.addView(text(stageName, 25, ink, true), top(5))
        content.addView(text("Escaneo visual de demostración · los valores no son mediciones clínicas.", 11, muted), top(5))

        val displayed = selectedScanTime?.let { scanByTime(it) } ?: latestScan(stageKey)
        if (displayed == null) {
            content.addView(card().apply {
                addView(text("Todavía no hay un escaneo guardado en esta etapa", 13, ink, true))
                addView(text("Pulsa el botón de escaneo para guardar un punto de referencia y ver los resultados en esta ficha y en Historial.", 11, muted), top(6))
            }, top(12))
        } else {
            val zones = scanZones(displayed)
            content.addView(card().apply {
                addView(text(displayed.optString("title", "Escaneo"), 14, ink, true))
                addView(text("${scanDate(displayed)} · registro local", 10, muted), top(4))
                if (displayed.optString("scenario").isNotBlank()) {
                    addView(text("Situación evaluada: ${displayed.optString("scenario")} · simulación", 11, mint, true), top(7))
                    addView(text(displayed.optString("scenarioNote"), 10, muted), top(3))
                }
                val stable = displayed.optBoolean("stable", true)
                val markedZones = scanZones(displayed)
                val highlightedScenario = displayed.optString("scenarioId") in setOf("radiation", "radiation_dosimetry", "co2_leak", "fatigue", "space_biology")
                val indicatorText = when {
                    highlightedScenario -> "●  Zonas destacadas por simulación · no son daños confirmados"
                    stable -> "●  Indicadores simulados estables"
                    else -> "●  Indicadores simulados fuera de rango ilustrativo"
                }
                addView(text(indicatorText, 11, if (highlightedScenario) Color.rgb(255, 190, 80) else if (stable) Color.rgb(95, 225, 160) else Color.rgb(255, 95, 95)), top(8))
                addView(text(if (markedZones.isEmpty()) "Figura verde · sin zonas marcadas en este registro" else "Zonas señaladas en rojo: ${zonesToLabel(markedZones)}", 10, if (markedZones.isEmpty()) Color.rgb(95, 225, 160) else Color.rgb(255, 105, 105)), top(4))
                if (displayed.optString("scenarioImpact").isNotBlank()) addView(text("Posibles efectos (educativo): ${displayed.optString("scenarioImpact")}", 10, muted), top(6))
                val vitals = displayed.optJSONObject("vitals") ?: JSONObject()
                addView(text("Frecuencia cardíaca: ${vitals.optInt("hr")} lpm", 11, ink), top(8))
                addView(text("Oxígeno: ${vitals.optInt("spo2")}% · Temperatura: ${vitals.optDouble("temp")} °C", 11, ink), top(4))
                addView(text("Sueño (dato demo): ${vitals.optDouble("sleep")} h · Estrés (dato demo): ${vitals.optInt("stress")}/100", 10, muted), top(4))
                addView(text("API de clima espacial al guardar: ${displayed.optInt("solarFlares", -1).let { if (it < 0) "sin respuesta válida" else "$it eventos" }}", 10, muted), top(7))
                addView(text("Radiación JSON: ${displayed.optString("radiation", "sin dato")} · CO₂: ${displayed.optString("co2", "sin dato")}", 10, muted), top(4))
                addView(text("JSON ${displayed.optString("jsonGenerated", "sin fecha")} · Calidad radiación: ${displayed.optString("radiationQuality", "sin nota")} · CO₂: ${displayed.optString("co2Quality", "sin nota")}", 9, muted), top(4))
                if (selectedPhase == "after") addView(text("Efectos acumulados estimados: ${displayed.optString("effects", "pendientes")}", 10, mint), top(7))
            }, top(12))
        }

        content.addView(card().apply {
            addView(sectionTitle("Tripulante", "MODELO 3D GLB"))
            val legend = "Gira la figura arrastrando sobre ella. Verde = indicadores de demostración estables; rojo = zona destacada por la situación seleccionada, no lesión confirmada ni dosis personal."
            addView(text(legend, 10, muted), top(5))
            bodyModelView = GlbBodyView(this@MainActivity, displayed?.let { scanZones(it) } ?: emptySet())
            val modelState = text("Cargando modelo 3D local…", 9, muted)
            bodyModelView?.onModelState = { modelState.text = it }
            addView(bodyModelView, LinearLayout.LayoutParams(-1, dp(300)).apply { topMargin = dp(10) })
            addView(modelState, top(4))
            addView(text("La malla del archivo tiene corazón, pulmones, cerebro, esqueleto y órganos. No incluye una malla muscular independiente.", 9, muted), top(5))
        }, top(12))

        when (selectedPhase) {
            "before" -> addScanButton(content, "Registrar escaneo inicial", "before", "Escaneo inicial · antes del viaje")
            "during_departure" -> addScanButton(content, "Registrar escaneo al salir", "during_departure", "Escaneo al salir del planeta")
            "during_stay" -> addScanButton(content, "Registrar nuevo escaneo de estancia", "during_stay", "Escaneo durante la estancia")
            "during" -> {
                addScanButton(content, "Escanear al salir del planeta", "during_departure", "Escaneo al salir del planeta")
                addScanButton(content, "Nuevo escaneo durante la estancia", "during_stay", "Escaneo durante la estancia")
            }
            else -> addScanButton(content, "Registrar escaneo posterior", "after", "Escaneo posterior · regreso")
        }

        content.addView(text("Registros de esta etapa", 16, ink, true), top(18))
        val scansForStage = scansFor(stageKey)
        if (scansForStage.isEmpty()) content.addView(text("Aún no hay registros en esta etapa.", 11, muted), top(6))
        scansForStage.reversed().forEach { scan ->
            val selected = displayed?.optLong("time") == scan.optLong("time")
            val row = card().apply {
                addView(text(scan.optString("title", "Escaneo"), 12, ink, true))
                addView(text("${scanDate(scan)} · ${if (scan.optBoolean("stable")) "estable" else "zonas marcadas: ${zonesToLabel(scanZones(scan))}"}", 10, if (scan.optBoolean("stable")) Color.rgb(95, 225, 160) else Color.rgb(255, 105, 105)), top(4))
                if (selected) addView(text("Mostrando este escaneo en la figura 3D", 9, mint), top(4))
                setOnClickListener { selectedScanTime = scan.optLong("time"); activeTab = "Etapa"; setContentView(buildScreen()) }
            }
            content.addView(row, top(7))
        }
    }

    private fun addScanButton(content: LinearLayout, buttonText: String, stage: String, title: String) {
        content.addView(actionButton("◉  $buttonText"), top(10))
        (content.getChildAt(content.childCount - 1) as Button).setOnClickListener { chooseScanScenario(stage, title) }
    }

    /** Cada captura queda asociada a un escenario de demostración seleccionado por el usuario. */
    private fun chooseScanScenario(stage: String, title: String) {
        AlertDialog.Builder(this)
            .setTitle("¿Qué situación quieres evaluar? (simulación)")
            .setItems(scanScenarios.map { it.label }.toTypedArray()) { _, selected ->
                saveMissionScan(stage, title, scanScenarios[selected])
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /** Captura valores de demostración, datos JSON/API vigentes y zonas calculadas; persiste el escaneo localmente. */
    private fun saveMissionScan(stage: String, title: String, scenario: ScanScenario) {
        demoStamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val scenarioVitals = demoVitals.toMutableMap().apply {
            this["hr"] = ((this["hr"] ?: 72.0) + scenario.heartRateDelta).coerceIn(40.0, 190.0)
            this["stress"] = ((this["stress"] ?: 28.0) + scenario.stressDelta).coerceIn(0.0, 100.0)
            this["sleep"] = ((this["sleep"] ?: 7.2) + scenario.sleepDelta).coerceIn(0.0, 16.0)
        }
        val vitals = JSONObject().apply { scenarioVitals.forEach { (key, value) -> put(key, value) } }
        val zones = JSONArray()
        fun mark(zone: String) { if ((0 until zones.length()).none { zones.optString(it) == zone }) zones.put(zone) }
        if ((scenarioVitals["hr"] ?: 72.0) !in 50.0..100.0) mark("heart")
        if ((scenarioVitals["spo2"] ?: 98.0) < 95.0) mark("lungs")
        if ((scenarioVitals["temp"] ?: 36.7) !in 36.1..37.5) mark("body")
        scenario.redZones.forEach(::mark)
        val effects = mutableListOf("Escenario ${scenario.label}: ${scenario.note}")
        if (scenario.impact.isNotBlank()) effects.add(scenario.impact)
        when (scenario.id) {
            "eva" -> effects.add("Datos demo modificados por escenario: FC +25 lpm y estrés +18 puntos.")
            "solar" -> effects.add("DONKI: ${if (flareDataKnown) "${flareJson.length()} eventos en la última respuesta" else "sin respuesta válida"}; no se calcula dosis personal.")
            "cabin_co2" -> effects.add("CO₂: sin lectura confirmada; no se inventa un valor ni se marca una zona corporal.")
            "microgravity" -> effects.add("Exposición prolongada simulada; revisar duración de misión y fuentes de investigación por separado.")
            "sleep" -> effects.add("Datos demo modificados por escenario: sueño −2.5 h y estrés +15 puntos.")
            "solar_storm" -> effects.add("DONKI: ${if (flareDataKnown) "${flareJson.length()} eventos registrados" else "sin respuesta válida"}; la cuenta de eventos no es una dosis personal.")
            "co2_leak" -> effects.add("La zona pulmonar roja es parte de la simulación; la app no reporta una fuga real.")
            "fatigue" -> effects.add("Datos demo modificados por escenario: sueño −3 h y estrés +20 puntos; no son datos fisiológicos reales.")
            "solar_weather" -> effects.add("DONKI: ${if (flareDataKnown) "${flareJson.length()} eventos en caché" else "sin respuesta válida"}; se muestran como contexto ambiental.")
            "space_biology" -> {
                val studies = sourceJson.optJSONObject("inputs")?.optJSONObject("musculoskeletal_microgravity_studies")?.optJSONArray("value")
                effects.add(if (studies == null) "Sin referencias OSDR dentro del JSON." else "${studies.length()} referencias OSDR curatoriales; no son mediciones del tripulante.")
            }
            "radiation_dosimetry" -> {
                val dosetel = sourceJson.optJSONObject("inputs")?.optJSONObject("iss_dosetel_absorbed_dose_rate")
                val dose = dosetel?.opt("absorbed_dose_rate_mean_ugy_h") as? Number
                val period = dosetel?.optString("period_start", "periodo no indicado") ?: "periodo no indicado"
                effects.add(if (dose == null) "Sin dato DosTel en el JSON." else "DosTel histórico: ${dose} µGy/h desde $period; referencia absorbida, no dosis actual ni exposición personal.")
            }
            "iss_telemetry" -> effects.add("No hay flujo validado de sensores ISS conectado; CO₂ y otras señales no se completan con valores inventados.")
        }
        if (stage == "after") {
            val days = getSharedPreferences("orbita_profile", MODE_PRIVATE).getString("mission_day", "")?.toIntOrNull()?.takeIf { it > 0 }
                ?: sourceJson.optInt("mission_day", 0).takeIf { it > 0 }
            val rad = sourceJson.opt("radiation_uSv_h") as? Number
            if (days != null) {
                if (rad != null) {
                    val boneIndex = minOf(1.0, (days / 365.0) * .8 + (rad.toDouble() / 100.0) * .2)
                    effects.add("índice óseo ${formatValue(boneIndex * 100, 1)}%${if ((sourceJson.optJSONObject("_quality")?.optString("radiation_uSv_h", "") ?: "").contains("provisional", true)) " (radiación provisional)" else ""}")
                if (boneIndex >= .25) mark("bones")
                } else effects.add("índice óseo pendiente: falta radiación en el JSON")
                val femurIndex = minOf(1.0, (days / 300.0) * .7)
                val muscleIndex = minOf(1.0, (days / 180.0) * .75)
                effects.add("índice de fémur ${formatValue(femurIndex * 100, 1)}% · músculo ${formatValue(muscleIndex * 100, 1)}%")
                if (femurIndex >= .20) mark("bones")
                if (muscleIndex >= .25) mark("muscle")
                // El GLB recibido no tiene malla muscular; la zona se superpone al esqueleto como referencia visual.
            } else effects.add("pendientes: ingresa duración total en Perfil")
            if (rad != null && flareDataKnown) {
                val heartIndex = minOf(1.0, (rad.toDouble() / 50.0) * .4 + flareJson.length() * .05 + .1)
                effects.add("índice cardíaco experimental ${formatValue(heartIndex * 100, 1)}% · API ${flareJson.length()} eventos")
                if (heartIndex >= .30) mark("heart")
            } else effects.add("índice cardíaco pendiente: falta radiación o respuesta válida de la API")
        }
        val scan = JSONObject().put("time", System.currentTimeMillis()).put("title", title).put("stage", stage)
            .put("stageLabel", when { stage == "before" -> "Antes del viaje"; stage == "after" -> "Después del viaje"; else -> "Durante la misión" })
            .put("scenarioId", scenario.id).put("scenario", scenario.label).put("scenarioNote", scenario.note).put("scenarioImpact", scenario.impact)
            .put("vitals", vitals).put("zones", zones)
            .put("stable", (scenarioVitals["hr"] ?: 72.0) in 50.0..100.0 && (scenarioVitals["spo2"] ?: 98.0) >= 95.0 && (scenarioVitals["temp"] ?: 36.7) in 36.1..37.5)
            .put("solarFlares", if (flareDataKnown) flareJson.length() else -1)
            .put("radiation", sourceJson.opt("radiation_uSv_h")?.toString() ?: "sin dato")
            .put("co2", sourceJson.opt("cabin_co2_mmHg")?.toString() ?: "sin dato")
            .put("jsonGenerated", sourceJson.optString("generated", "sin fecha"))
            .put("radiationQuality", sourceJson.optJSONObject("_quality")?.optString("radiation_uSv_h", "sin nota") ?: "sin nota")
            .put("co2Quality", sourceJson.optJSONObject("_quality")?.optString("cabin_co2_mmHg", "sin nota") ?: "sin nota")
            .put("effects", effects.ifEmpty { listOf("Sin efecto acumulado confirmado para esta etapa") }.joinToString(" · "))
        missionScans.put(scan)
        while (missionScans.length() > 300) missionScans.remove(0)
        getSharedPreferences("orbita_mission_scans", MODE_PRIVATE).edit().putString("entries", missionScans.toString()).apply()
        selectedPhase = stage; selectedScanTime = scan.optLong("time"); activeTab = "Etapa"
        setContentView(buildScreen())
        Toast.makeText(this, "Escaneo guardado en su etapa y en Historial", Toast.LENGTH_SHORT).show()
        notifyScan(scan)
    }

    /** Notifica el resultado del escaneo sin presentar escenarios simulados como diagnósticos. */
    private fun notifyScan(scan: JSONObject) {
        val vitals = scan.optJSONObject("vitals") ?: JSONObject()
        val outOfRange = mutableListOf<String>()
        val hr = vitals.optDouble("hr", Double.NaN)
        val spo2 = vitals.optDouble("spo2", Double.NaN)
        val temp = vitals.optDouble("temp", Double.NaN)
        val sleep = vitals.optDouble("sleep", Double.NaN)
        if (!hr.isNaN() && hr !in 50.0..100.0) outOfRange.add("frecuencia cardíaca")
        if (!spo2.isNaN() && spo2 < 95.0) outOfRange.add("oxígeno en sangre")
        if (!temp.isNaN() && temp !in 36.1..37.5) outOfRange.add("temperatura")
        if (!sleep.isNaN() && sleep < 5.0) outOfRange.add("sueño de demostración")

        val simulatedHazard = when (scan.optString("scenarioId")) {
            "co2_leak" -> "Fuga de CO₂ simulada. Comprueba ventilación y sensores, e informa al responsable de misión. No es una lectura real."
            "fatigue", "sleep" -> "Fatiga o descanso insuficiente simulado. Pausa la actividad, descansa e informa al responsable si persiste. No es una medición fisiológica."
            else -> null
        }

        if (outOfRange.isNotEmpty() || simulatedHazard != null) {
            val details = buildList {
                if (simulatedHazard != null) add(simulatedHazard)
                if (outOfRange.isNotEmpty()) add("Indicadores de demostración fuera del rango configurado (${outOfRange.joinToString()}).")
            }.joinToString(" ")
            postLocalNotification(
                "Alerta de bienestar · ${scan.optString("title", "Escaneo")}",
                "$details Pausa la actividad si corresponde, informa al responsable médico y repite cualquier lectura con un dispositivo real. Si hay síntomas intensos, busca ayuda urgente."
            )
        } else {
            postLocalNotification(
                "Escaneo registrado",
                "Todo bien: los indicadores de demostración están dentro de los rangos configurados. No son mediciones clínicas."
            )
        }
    }

    /** Alerta por estado reportado o síntoma escrito; la nota privada nunca se muestra en la notificación. */
    private fun notifyCheckIn(mood: String, note: String) {
        val normalizedNote = Normalizer.normalize(note.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
        val symptomKeywords = listOf(
            "dolor", "mareo", "dificultad para respirar", "falta de aire", "desmayo", "fiebre",
            "nausea", "vomito", "palpitaciones", "confusion", "vision borrosa", "debilidad intensa", "sangrado"
        )
        val reportedSymptom = symptomKeywords.firstOrNull { normalizedNote.contains(it) }
        val alert = mood == "Mal" || mood == "Cansado" || reportedSymptom != null
        if (alert) {
            val reason = when {
                reportedSymptom != null -> "Se registró un síntoma que requiere atención."
                mood == "Cansado" -> "Se registró cansancio."
                else -> "Se registró malestar."
            }
            val recommendation = if (mood == "Cansado" && reportedSymptom == null) {
                "Pausa la actividad, descansa e informa al responsable de misión si persiste."
            } else {
                "Informa al responsable médico y evita continuar una actividad exigente; si es intenso o empeora, busca ayuda urgente."
            }
            postLocalNotification("Alerta de bienestar", "$reason $recommendation")
        } else if (mood == "Muy bien" || mood == "Bien") {
            postLocalNotification("Registro guardado", "Todo bien: estado de ánimo registrado sin alertas.")
        } else {
            postLocalNotification("Registro guardado", "Ánimo neutral registrado; no se detectó una alerta en este registro.")
        }
    }

    /** Crea el canal y solicita el permiso de Android al guardar el primer registro si hace falta. */
    private fun postLocalNotification(title: String, message: String) {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(NOTIFICATION_CHANNEL_ID, "Alertas de bienestar", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Avisos al guardar escaneos y registros de bienestar"
                }
            )
        }
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            pendingNotification = title to message
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_PERMISSION_REQUEST)
            return
        }
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION") Notification.Builder(this)
        }.setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(Notification.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_STATUS)
            .build()
        manager.notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST) {
            val pending = pendingNotification
            pendingNotification = null
            if (grantResults.firstOrNull() == android.content.pm.PackageManager.PERMISSION_GRANTED && pending != null) {
                postLocalNotification(pending.first, pending.second)
            } else {
                Toast.makeText(this, "Permite notificaciones en Ajustes para recibir alertas de bienestar", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun scansFor(stage: String): List<JSONObject> = (0 until missionScans.length()).mapNotNull { missionScans.optJSONObject(it) }
        .filter { if (stage == "during") it.optString("stage").startsWith("during") else it.optString("stage") == stage }

    private fun latestScan(stage: String): JSONObject? = scansFor(stage).maxByOrNull { it.optLong("time") }
    private fun scanByTime(time: Long): JSONObject? = (0 until missionScans.length()).mapNotNull { missionScans.optJSONObject(it) }.firstOrNull { it.optLong("time") == time }
    private fun scanZones(scan: JSONObject): Set<String> { val a = scan.optJSONArray("zones") ?: JSONArray(); return (0 until a.length()).map { a.optString(it) }.toSet() }
    private fun zonesToLabel(zones: JSONArray): String = (0 until zones.length()).joinToString(", ") { when (zones.optString(it)) { "heart" -> "corazón y vasos (referencia visual)"; "lungs" -> "pulmones"; "brain" -> "cerebro/SNC (referencia visual)"; "bones" -> "esqueleto (referencia visual a tejidos formadores de sangre)"; "muscle" -> "extremidades (referencia visual del esqueleto)"; "body" -> "cuerpo"; else -> zones.optString(it) } }
    private fun zonesToLabel(zones: Set<String>): String = zones.joinToString(", ") { when (it) { "heart" -> "corazón y vasos (referencia visual)"; "lungs" -> "pulmones"; "brain" -> "cerebro/SNC (referencia visual)"; "bones" -> "esqueleto (referencia visual a tejidos formadores de sangre)"; "muscle" -> "extremidades (referencia visual del esqueleto)"; "body" -> "cuerpo"; else -> it } }
    private fun scanDate(scan: JSONObject): String = SimpleDateFormat("dd/MM/yyyy · HH:mm", Locale.getDefault()).format(Date(scan.optLong("time")))

    private fun openMissionStage(stage: String) {
        selectedPhase = stage
        selectedScanTime = latestScan(stage)?.optLong("time")
        activeTab = "Etapa"
        setContentView(buildScreen())
    }

    /** Una tarjeta visual reutilizable para que sea sencillo añadir nuevos bloques de datos. */
    private fun card() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(14), dp(14), dp(14))
        background = rounded(panel, 18, Color.argb(48, 255, 255, 255))
    }

    /** Reserva un visor amplio para colocar más adelante el modelo 3D de la persona. */
    private fun personPreview() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(14), dp(14), dp(14), dp(14))
        background = rounded(Color.argb(172, 24, 25, 28), 20, Color.argb(72, 255, 255, 255))
        addView(sectionTitle("Tripulante", "ESCANEO 3D"))
        addView(text("Modelo anatómico local · arrastra la figura para girarla", 10, muted), top(4))
        val zones = (0 until missionScans.length()).mapNotNull { missionScans.optJSONObject(it) }.maxByOrNull { it.optLong("time") }?.let { scanZones(it) } ?: emptySet()
        bodyModelView = GlbBodyView(this@MainActivity, zones)
        bodyModelView?.contentDescription = "Modelo 3D del tripulante. Verde representa indicadores simulados estables; rojo indica zonas marcadas en el último escaneo."
        val modelState = text("Cargando modelo 3D local…", 9, muted)
        bodyModelView?.onModelState = { modelState.text = it }
        val viewport = FrameLayout(this@MainActivity).apply {
            background = GradientDrawable().apply {
                setColor(Color.argb(125, 0, 0, 0)); cornerRadius = dp(16).toFloat()
                setStroke(dp(1), Color.argb(115, 205, 209, 215), dp(7).toFloat(), dp(5).toFloat())
            }
            addView(bodyModelView, FrameLayout.LayoutParams(-1, -1))
        }
        addView(viewport, LinearLayout.LayoutParams(-1, dp(208)).apply { topMargin = dp(12) })
        addView(modelState, top(4))
        addView(actionButton("◉  Escanear y guardar resultados"), top(10))
        (getChildAt(childCount - 1) as Button).setOnClickListener { showScanStagePicker() }
    }

    /** Permite elegir en qué casilla de misión guardar el nuevo escaneo. */
    private fun showScanStagePicker() {
        val options = arrayOf("Escaneo inicial · antes del viaje", "Escaneo al salir del planeta", "Nuevo escaneo durante la estancia", "Escaneo posterior · regreso")
        val keys = arrayOf("before", "during_departure", "during_stay", "after")
        android.app.AlertDialog.Builder(this).setTitle("¿En qué etapa estás?").setItems(options) { _, which ->
            val title = options[which]
            chooseScanScenario(keys[which], title)
        }.setNegativeButton("Cancelar", null).show()
    }

    private fun sectionTitle(title: String, detail: String) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        addView(text(title, 16, ink, true), LinearLayout.LayoutParams(0, -2, 1f))
        addView(text(detail, 8, muted, true))
    }

    private fun chip(label: String) = TextView(this).apply {
        text = label; textSize = 11f; setTextColor(mint); setPadding(dp(12), dp(8), dp(12), dp(8))
        background = rounded(Color.argb(180, 39, 40, 43), 24, Color.argb(72, 255, 255, 255))
    }

    private fun actionButton(label: String) = Button(this).apply {
        text = label; textSize = 12f; isAllCaps = false; setTextColor(Color.rgb(9, 32, 30))
        background = rounded(mint, 12, mint)
    }

    private fun text(value: String, size: Int, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = value; textSize = size.toFloat(); setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

    private fun formatValue(value: Double, decimals: Int) = String.format(Locale.getDefault(), "%.$decimals" + "f", value)

    private fun rounded(color: Int, radius: Int, stroke: Int) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat(); setStroke(dp(1), stroke)
    }

    private fun weighted(view: View) = LinearLayout.LayoutParams(0, -2, 1f)
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun top(value: Int) = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(value) }
    private fun left(value: Int) = LinearLayout.LayoutParams(-2, -2).apply { leftMargin = dp(value) }
}


