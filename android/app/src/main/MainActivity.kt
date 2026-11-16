package org.orbita.bienestar

import android.app.Activity
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
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
import java.util.Date
import java.util.Locale
import kotlin.random.Random

/** Aplicación de bienestar conectada a JSON y a la API pública DONKI. */
class MainActivity : Activity() {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = background
        window.navigationBarColor = background
        window.decorView.systemUiVisibility = 0
        val prefs = getSharedPreferences("orbita_data", MODE_PRIVATE)
        sourceJson = (prefs.getString("imported_json", null) ?: runCatching {
            assets.open("risk_inputs.json").bufferedReader().use { it.readText() }
        }.getOrDefault("{}")).let { runCatching { JSONObject(it) }.getOrDefault(JSONObject()) }
        sourceStatus = if (prefs.contains("imported_json")) "JSON importado (caché local)" else "JSON incluido con la aplicación"
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

        content.addView(text("◉  ÓRBITA  /  BIENESTAR", 15, mint, true))
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
        content.addView(sectionTitle("Fuentes conectadas", "JSON + DONKI"), top(20))
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
        (content.getChildAt(content.childCount - 1) as Button).setOnClickListener {
            if (name.text.toString().isBlank() || age.text.toString().toIntOrNull() == null) {
                Toast.makeText(this, "Agrega un nombre y una edad válida para guardar el perfil", Toast.LENGTH_LONG).show()
            } else {
                prefs.edit().putString("name", name.text.toString().trim()).putString("age", age.text.toString().trim())
                    .putString("blood", blood.text.toString().trim()).putString("allergies", allergies.text.toString().trim())
                    .putString("conditions", conditions.text.toString().trim()).putString("contact", contact.text.toString().trim())
                    .putString("mission_day", missionDay.text.toString().trim()).apply()
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
        content.addView(actionButton("Importar archivo JSON"), top(12))
        (content.getChildAt(content.childCount - 1) as Button).setOnClickListener { importJsonFile() }
        content.addView(actionButton("Actualizar datos ahora"), top(8))
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
        val generated = sourceJson.optString("generated", "sin fecha")
        val rad = sourceJson.opt("radiation_uSv_h")
        val co2 = sourceJson.opt("cabin_co2_mmHg")
        val quality = sourceJson.optJSONObject("_quality") ?: JSONObject()
        addView(text("DONKI · llamaradas solares (últimos 30 días)", 12, ink, true))
        addView(text(if (flareDataKnown) "$flareStatus · ${flareJson.length()} eventos en respuesta API guardada" else "$flareStatus · no hay respuesta válida; cero eventos no confirmado", 10, mint), top(5))
        for (i in 0 until minOf(3, flareJson.length())) {
            val event = flareJson.optJSONObject(i) ?: continue
            addView(text("• ${event.optString("beginTime", "Fecha no informada")} · clase ${event.optString("classType", "sin clasificar")}", 9, muted), top(4))
        }
        addView(text("JSON · fecha $generated · estado: $sourceStatus", 10, muted), top(9))
        addView(text("Radiación: ${if (rad == JSONObject.NULL || rad == null) "sin dato" else "$rad µSv/h"} · ${quality.optString("radiation_uSv_h", "calidad no especificada")}", 10, muted), top(7))
        addView(text("CO₂ cabina: ${if (co2 == JSONObject.NULL || co2 == null) "sin dato" else "$co2 mmHg"} · ${quality.optString("cabin_co2_mmHg", "calidad no especificada")}", 10, muted), top(5))
        addView(text("Los valores provisionales se muestran como referencia, nunca como mediciones confirmadas.", 9, muted), top(8))
        addView(actionButton("Importar / reemplazar JSON"), top(10))
        (getChildAt(childCount - 1) as Button).setOnClickListener { importJsonFile() }
        addView(actionButton("Actualizar DONKI"), top(7))
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
            addView(text("Corazón: ${formatValue(heart * 100, 1)} % de índice estimado · ${flareJson.length()} eventos", 10, ink), top(8))
        } else addView(text("Índice cardíaco: pendiente · necesita radiación y respuesta válida de DONKI.", 10, muted), top(8))
        addMissionGuidance(
            "Radiación · cómo interpretar",
            "El JSON informa ${if (rad.isNaN()) "sin tasa disponible" else "$rad µSv/h"}${if (provisional) " como valor provisional" else ""}. DONKI aporta eventos de llamaradas, no la dosis de la tripulación. Para valorar exposición se requiere una fuente de dosimetría validada y su periodo de medición; esta pantalla no calcula dosis acumulada observada ni recomienda tratamiento. Si el dato es provisional o falta, confirmar con el equipo de protección radiológica."
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

    /** Consulta la API real de DONKI FLR y conserva la última respuesta para usar sin internet. */
    private fun refreshSolarFlares(showToast: Boolean = false) {
        io.execute {
            try {
                val connection = (URL("https://api.nasa.gov/DONKI/FLR?startDate=${dateDaysAgo(30)}&endDate=${dateToday()}&api_key=DEMO_KEY").openConnection() as HttpURLConnection)
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
            sourceJson = parsed
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
                    addView(text("●  $result", 10, if (zones.length() == 0) Color.rgb(95, 225, 160) else Color.rgb(255, 95, 95)), top(7))
                    val values = scan.optJSONObject("vitals") ?: JSONObject()
                    addView(text("FC ${values.optInt("hr")} lpm · SpO₂ ${values.optInt("spo2")}% · Temp. ${values.optDouble("temp")} °C", 10, muted), top(6))
                    setOnClickListener { selectedPhase = scan.optString("stage", "before"); selectedScanTime = scan.optLong("time"); activeTab = "Etapa"; setContentView(buildScreen()) }
                }, top(8))
            }
        }
        content.addView(text("DONKI: ${if (flareDataKnown) "${flareJson.length()} eventos en la última respuesta válida" else "sin respuesta válida"} · $flareStatus", 10, mint), top(18))
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
                val stable = displayed.optBoolean("stable", true)
                val markedZones = scanZones(displayed)
                addView(text(if (stable) "●  Indicadores simulados estables" else "●  Indicadores simulados fuera de rango ilustrativo", 11, if (stable) Color.rgb(95, 225, 160) else Color.rgb(255, 95, 95)), top(8))
                addView(text(if (markedZones.isEmpty()) "Figura verde · sin zonas marcadas en este registro" else "Zonas señaladas en rojo: ${zonesToLabel(markedZones)}", 10, if (markedZones.isEmpty()) Color.rgb(95, 225, 160) else Color.rgb(255, 105, 105)), top(4))
                val vitals = displayed.optJSONObject("vitals") ?: JSONObject()
                addView(text("Frecuencia cardíaca: ${vitals.optInt("hr")} lpm", 11, ink), top(8))
                addView(text("Oxígeno: ${vitals.optInt("spo2")}% · Temperatura: ${vitals.optDouble("temp")} °C", 11, ink), top(4))
                addView(text("Sueño (dato demo): ${vitals.optDouble("sleep")} h · Estrés (dato demo): ${vitals.optInt("stress")}/100", 10, muted), top(4))
                addView(text("DONKI al guardar: ${displayed.optInt("solarFlares", -1).let { if (it < 0) "sin respuesta válida" else "$it eventos" }}", 10, muted), top(7))
                addView(text("Radiación JSON: ${displayed.optString("radiation", "sin dato")} · CO₂: ${displayed.optString("co2", "sin dato")}", 10, muted), top(4))
                addView(text("JSON ${displayed.optString("jsonGenerated", "sin fecha")} · Calidad radiación: ${displayed.optString("radiationQuality", "sin nota")} · CO₂: ${displayed.optString("co2Quality", "sin nota")}", 9, muted), top(4))
                if (selectedPhase == "after") addView(text("Efectos acumulados estimados: ${displayed.optString("effects", "pendientes")}", 10, mint), top(7))
            }, top(12))
        }

        content.addView(card().apply {
            addView(sectionTitle("Tripulante", "MODELO 3D GLB"))
            addView(text("Gira la figura arrastrando sobre ella. Verde = indicadores de demostración estables; rojo = zona señalada por reglas ilustrativas del escaneo.", 10, muted), top(5))
            bodyModelView = GlbBodyView(this@MainActivity, displayed?.let { scanZones(it) } ?: emptySet())
            addView(bodyModelView, LinearLayout.LayoutParams(-1, dp(300)).apply { topMargin = dp(10) })
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
        (content.getChildAt(content.childCount - 1) as Button).setOnClickListener { saveMissionScan(stage, title) }
    }

    /** Captura valores de demostración, datos JSON/API vigentes y zonas calculadas; persiste el escaneo localmente. */
    private fun saveMissionScan(stage: String, title: String) {
        demoStamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val vitals = JSONObject().apply { demoVitals.forEach { (key, value) -> put(key, value) } }
        val zones = JSONArray()
        fun mark(zone: String) { if ((0 until zones.length()).none { zones.optString(it) == zone }) zones.put(zone) }
        if ((demoVitals["hr"] ?: 72.0) !in 50.0..100.0) mark("heart")
        if ((demoVitals["spo2"] ?: 98.0) < 95.0) mark("lungs")
        if ((demoVitals["temp"] ?: 36.7) !in 36.1..37.5) mark("body")
        val effects = mutableListOf<String>()
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
                effects.add("índice cardíaco experimental ${formatValue(heartIndex * 100, 1)}% · ${flareJson.length()} eventos")
                if (heartIndex >= .30) mark("heart")
            } else effects.add("índice cardíaco pendiente: falta radiación o respuesta válida")
        }
        val scan = JSONObject().put("time", System.currentTimeMillis()).put("title", title).put("stage", stage)
            .put("stageLabel", when { stage == "before" -> "Antes del viaje"; stage == "after" -> "Después del viaje"; else -> "Durante la misión" })
            .put("vitals", vitals).put("zones", zones)
            .put("stable", (demoVitals["hr"] ?: 72.0) in 50.0..100.0 && (demoVitals["spo2"] ?: 98.0) >= 95.0 && (demoVitals["temp"] ?: 36.7) in 36.1..37.5)
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
    }

    private fun scansFor(stage: String): List<JSONObject> = (0 until missionScans.length()).mapNotNull { missionScans.optJSONObject(it) }
        .filter { if (stage == "during") it.optString("stage").startsWith("during") else it.optString("stage") == stage }

    private fun latestScan(stage: String): JSONObject? = scansFor(stage).maxByOrNull { it.optLong("time") }
    private fun scanByTime(time: Long): JSONObject? = (0 until missionScans.length()).mapNotNull { missionScans.optJSONObject(it) }.firstOrNull { it.optLong("time") == time }
    private fun scanZones(scan: JSONObject): Set<String> { val a = scan.optJSONArray("zones") ?: JSONArray(); return (0 until a.length()).map { a.optString(it) }.toSet() }
    private fun zonesToLabel(zones: JSONArray): String = (0 until zones.length()).joinToString(", ") { when (zones.optString(it)) { "heart" -> "corazón"; "lungs" -> "pulmones"; "bones" -> "esqueleto (referencia ósea)"; "muscle" -> "extremidades (referencia visual del esqueleto)"; "body" -> "cuerpo"; else -> zones.optString(it) } }
    private fun zonesToLabel(zones: Set<String>): String = zones.joinToString(", ") { when (it) { "heart" -> "corazón"; "lungs" -> "pulmones"; "bones" -> "esqueleto (referencia ósea)"; "muscle" -> "extremidades (referencia visual del esqueleto)"; "body" -> "cuerpo"; else -> it } }
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
        val viewport = FrameLayout(this@MainActivity).apply {
            background = GradientDrawable().apply {
                setColor(Color.argb(125, 0, 0, 0)); cornerRadius = dp(16).toFloat()
                setStroke(dp(1), Color.argb(115, 205, 209, 215), dp(7).toFloat(), dp(5).toFloat())
            }
            addView(bodyModelView, FrameLayout.LayoutParams(-1, -1))
        }
        addView(viewport, LinearLayout.LayoutParams(-1, dp(208)).apply { topMargin = dp(12) })
        addView(actionButton("◉  Escanear y guardar resultados"), top(10))
        (getChildAt(childCount - 1) as Button).setOnClickListener { showScanStagePicker() }
    }

    /** Permite elegir en qué casilla de misión guardar el nuevo escaneo. */
    private fun showScanStagePicker() {
        val options = arrayOf("Escaneo inicial · antes del viaje", "Escaneo al salir del planeta", "Nuevo escaneo durante la estancia", "Escaneo posterior · regreso")
        val keys = arrayOf("before", "during_departure", "during_stay", "after")
        android.app.AlertDialog.Builder(this).setTitle("¿En qué etapa estás?").setItems(options) { _, which ->
            val title = options[which]
            saveMissionScan(keys[which], title)
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


