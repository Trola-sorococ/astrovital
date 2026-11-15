package org.orbita.bienestar

import android.content.Context
import android.graphics.PixelFormat
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.view.MotionEvent
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import org.json.JSONObject
import kotlin.math.max

/** Visor GLB local y giratorio. El modelo se dibuja dentro de la app, sin conexión ni servicio externo. */
class GlbBodyView(context: Context, private val affectedZones: Set<String>) : GLSurfaceView(context) {
    private lateinit var bodyRenderer: BodyRenderer
    var onModelState: ((String) -> Unit)? = null
    private var lastX = 0f
    private var lastY = 0f

    init {
        setEGLContextClientVersion(2)
        // El SurfaceView se dibuja encima del fondo de las tarjetas que lo contienen.
        setZOrderOnTop(true)
        holder.setFormat(PixelFormat.OPAQUE)
        bodyRenderer = BodyRenderer(context, affectedZones) { state -> post { onModelState?.invoke(state) } }
        setRenderer(bodyRenderer)
        renderMode = RENDERMODE_WHEN_DIRTY
        setBackgroundColor(0xFF090B0E.toInt())
        requestRender()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> { lastX = event.x; lastY = event.y; parent?.requestDisallowInterceptTouchEvent(true); return true }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - lastX
                val dy = event.y - lastY
                lastX = event.x; lastY = event.y
                bodyRenderer.rotate(dx * 0.55f, dy * 0.35f)
                requestRender()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> parent?.requestDisallowInterceptTouchEvent(false)
        }
        return true
    }

    private class BodyRenderer(context: Context, private val affected: Set<String>, private val reportState: (String) -> Unit) : Renderer {
        private val appContext = context.applicationContext
        private val projection = FloatArray(16)
        private val model = FloatArray(16)
        private val mvp = FloatArray(16)
        private var program = 0
        private var positionHandle = 0
        private var mvpHandle = 0
        private var colorHandle = 0
        private var yaw = 0f
        private var pitch = -6f
        private var parts: List<Part> = emptyList()

        @Synchronized fun rotate(dx: Float, dy: Float) {
            yaw = (yaw + dx) % 360f
            pitch = (pitch + dy).coerceIn(-65f, 65f)
        }

        override fun onSurfaceCreated(gl: javax.microedition.khronos.opengles.GL10?, config: javax.microedition.khronos.egl.EGLConfig?) {
            GLES20.glClearColor(0.035f, 0.043f, 0.055f, 1f)
            GLES20.glEnable(GLES20.GL_DEPTH_TEST)
            GLES20.glEnable(GLES20.GL_BLEND)
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
            program = linkProgram(VERTEX_SHADER, FRAGMENT_SHADER)
            positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
            mvpHandle = GLES20.glGetUniformLocation(program, "uMvp")
            colorHandle = GLES20.glGetUniformLocation(program, "uColor")
            val glb = runCatching { readModel() }
            if (glb.isSuccess && !glb.getOrNull().isNullOrEmpty()) {
                parts = glb.getOrThrow()
                reportState("Modelo anatómico listo · arrastra para girarlo")
            } else {
                val failure = glb.exceptionOrNull()?.message ?: "geometría vacía"
                val stl = runCatching { readStlFallback() }
                if (stl.isSuccess && !stl.getOrNull().isNullOrEmpty()) {
                    parts = stl.getOrThrow()
                    reportState("Se muestra el modelo STL de respaldo; no se pudo abrir el GLB: $failure")
                } else {
                    parts = emptyList()
                    reportState("No se pudo cargar la figura 3D: $failure")
                }
            }
        }

        override fun onSurfaceChanged(gl: javax.microedition.khronos.opengles.GL10?, width: Int, height: Int) {
            GLES20.glViewport(0, 0, width, height)
            val aspect = width.toFloat() / max(1, height)
            Matrix.orthoM(projection, 0, -1.2f * aspect, 1.2f * aspect, -1.2f, 1.2f, -12f, 12f)
        }

        override fun onDrawFrame(gl: javax.microedition.khronos.opengles.GL10?) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
            if (program == 0 || parts.isEmpty()) return
            GLES20.glUseProgram(program)
            synchronized(this) {
                Matrix.setIdentityM(model, 0)
                Matrix.rotateM(model, 0, yaw, 0f, 1f, 0f)
                Matrix.rotateM(model, 0, pitch, 1f, 0f, 0f)
            }
            Matrix.multiplyMM(mvp, 0, projection, 0, model, 0)
            GLES20.glUniformMatrix4fv(mvpHandle, 1, false, mvp, 0)

            // El contorno corporal se dibuja translúcido para dejar ver los órganos y huesos interiores.
            parts.filter { it.zone != "body" }.forEach { drawPart(it) }
            GLES20.glDepthMask(false)
            parts.filter { it.zone == "body" }.forEach { drawPart(it) }
            GLES20.glDepthMask(true)
        }

        private fun drawPart(part: Part) {
            val isAffected = part.zone in affected || (part.zone == "bones" && "muscle" in affected) || (part.zone == "fallback" && affected.isNotEmpty())
            val rgba = when {
                isAffected -> floatArrayOf(1f, 0.20f, 0.20f, 0.98f)
                part.zone == "body" -> floatArrayOf(0.30f, 0.95f, 0.70f, if (affected.isEmpty()) 0.24f else 0.12f)
                part.zone == "fallback" -> floatArrayOf(0.30f, 0.95f, 0.70f, 1f)
                else -> floatArrayOf(0.23f, 0.88f, 0.69f, 0.88f)
            }
            GLES20.glUniform4fv(colorHandle, 1, rgba, 0)
            GLES20.glEnableVertexAttribArray(positionHandle)
            GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 12, part.vertices)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, part.vertexCount)
            GLES20.glDisableVertexAttribArray(positionHandle)
        }

        /** Lee nodos, transformaciones y triángulos de un GLB 2.0 con un único buffer binario. */
        private fun readModel(): List<Part> {
            val file = appContext.assets.open("modelo_zerog_pulse.glb").use { it.readBytes() }
            val glb = ByteBuffer.wrap(file).order(ByteOrder.LITTLE_ENDIAN)
            require(glb.int == 0x46546C67) { "No es un archivo GLB" }
            require(glb.int == 2) { "Se requiere GLB 2.0" }
            glb.int
            var json: JSONObject? = null
            var binary: ByteBuffer? = null
            while (glb.remaining() >= 8) {
                val length = glb.int
                val type = glb.int
                val chunk = ByteArray(length)
                glb.get(chunk)
                if (type == 0x4E4F534A) json = JSONObject(String(chunk, Charsets.UTF_8).trimEnd('\u0000', ' '))
                if (type == 0x004E4942) binary = ByteBuffer.wrap(chunk).order(ByteOrder.LITTLE_ENDIAN)
            }
            val root = requireNotNull(json) { "No se encontró el bloque JSON del GLB" }
            val bin = requireNotNull(binary) { "No se encontró el bloque geométrico del GLB" }
            val accessors = root.getJSONArray("accessors")
            val views = root.getJSONArray("bufferViews")
            val meshes = root.getJSONArray("meshes")
            val nodes = root.getJSONArray("nodes")
            val rawParts = mutableListOf<RawPart>()
            var minX = Float.POSITIVE_INFINITY; var minY = Float.POSITIVE_INFINITY; var minZ = Float.POSITIVE_INFINITY
            var maxX = Float.NEGATIVE_INFINITY; var maxY = Float.NEGATIVE_INFINITY; var maxZ = Float.NEGATIVE_INFINITY

            fun walk(nodeIndex: Int, parent: FloatArray, groupPath: String) {
                val node = nodes.getJSONObject(nodeIndex)
                val name = node.optString("name", "").lowercase()
                val path = "$groupPath/$name"
                val world = FloatArray(16)
                Matrix.multiplyMM(world, 0, parent, 0, nodeMatrix(node), 0)
                if (node.has("mesh")) {
                    val mesh = meshes.getJSONObject(node.getInt("mesh"))
                    val primitives = mesh.getJSONArray("primitives")
                    for (p in 0 until primitives.length()) {
                        val primitive = primitives.getJSONObject(p)
                        if (primitive.optInt("mode", 4) != 4) continue
                        val attributes = primitive.getJSONObject("attributes")
                        val positionAccessor = accessors.getJSONObject(attributes.getInt("POSITION"))
                        val view = views.getJSONObject(positionAccessor.getInt("bufferView"))
                        val start = view.optInt("byteOffset", 0) + positionAccessor.optInt("byteOffset", 0)
                        val stride = view.optInt("byteStride", 12)
                        val count = positionAccessor.getInt("count")
                        val points = Array(count) { index ->
                            val offset = start + index * stride
                            val local = floatArrayOf(bin.getFloat(offset), bin.getFloat(offset + 4), bin.getFloat(offset + 8), 1f)
                            val transformed = FloatArray(4)
                            Matrix.multiplyMV(transformed, 0, world, 0, local, 0)
                            floatArrayOf(transformed[0], transformed[1], transformed[2])
                        }
                        val indexValues = if (primitive.has("indices")) readIndices(accessors.getJSONObject(primitive.getInt("indices")), views, bin) else IntArray(count) { it }
                        val triangles = FloatBuilder(indexValues.size * 3)
                        for (index in indexValues) {
                            if (index !in points.indices) continue
                            val point = points[index]
                            triangles.add(point[0]); triangles.add(point[1]); triangles.add(point[2])
                            minX = minOf(minX, point[0]); maxX = maxOf(maxX, point[0])
                            minY = minOf(minY, point[1]); maxY = maxOf(maxY, point[1])
                            minZ = minOf(minZ, point[2]); maxZ = maxOf(maxZ, point[2])
                        }
                        if (triangles.size > 0) rawParts.add(RawPart(zoneFor(path), triangles.toArray()))
                    }
                }
                val children = node.optJSONArray("children") ?: return
                for (i in 0 until children.length()) walk(children.getInt(i), world, path)
            }

            val identity = FloatArray(16).also { Matrix.setIdentityM(it, 0) }
            val sceneIndex = root.optInt("scene", 0)
            val roots = root.getJSONArray("scenes").getJSONObject(sceneIndex).getJSONArray("nodes")
            for (i in 0 until roots.length()) walk(roots.getInt(i), identity, "")
            require(rawParts.isNotEmpty()) { "El GLB no contiene triángulos visibles" }
            val cx = (minX + maxX) / 2f; val cy = (minY + maxY) / 2f; val cz = (minZ + maxZ) / 2f
            val scale = 1.8f / max(maxX - minX, max(maxY - minY, maxZ - minZ)).coerceAtLeast(0.001f)
            return rawParts.map { part ->
                val normalized = FloatArray(part.xyz.size)
                var i = 0
                while (i < part.xyz.size) {
                    normalized[i] = (part.xyz[i] - cx) * scale
                    normalized[i + 1] = (part.xyz[i + 1] - cy) * scale
                    normalized[i + 2] = (part.xyz[i + 2] - cz) * scale
                    i += 3
                }
                val buffer = ByteBuffer.allocateDirect(normalized.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
                buffer.put(normalized).position(0)
                Part(part.zone, buffer, normalized.size / 3)
            }
        }

        /** Si el GLB no puede abrirse, dibuja la figura binaria STL incluida en el mismo paquete. */
        private fun readStlFallback(): List<Part> {
            val bytes = appContext.assets.open("Modelo_ZEROG-PULSE.stl").use { it.readBytes() }
            val data = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            require(bytes.size >= 84) { "El archivo STL está incompleto" }
            val triangleCount = data.getInt(80)
            require(triangleCount > 0 && bytes.size == 84 + triangleCount * 50) { "El archivo STL no tiene formato binario válido" }
            val builder = FloatBuilder(triangleCount * 9)
            var minX = Float.POSITIVE_INFINITY; var minY = Float.POSITIVE_INFINITY; var minZ = Float.POSITIVE_INFINITY
            var maxX = Float.NEGATIVE_INFINITY; var maxY = Float.NEGATIVE_INFINITY; var maxZ = Float.NEGATIVE_INFINITY
            for (triangle in 0 until triangleCount) {
                val base = 84 + triangle * 50 + 12
                for (vertex in 0 until 3) {
                    val offset = base + vertex * 12
                    val x = data.getFloat(offset)
                    val y = data.getFloat(offset + 8)
                    val z = -data.getFloat(offset + 4)
                    builder.add(x); builder.add(y); builder.add(z)
                    minX = minOf(minX, x); maxX = maxOf(maxX, x)
                    minY = minOf(minY, y); maxY = maxOf(maxY, y)
                    minZ = minOf(minZ, z); maxZ = maxOf(maxZ, z)
                }
            }
            val xyz = builder.toArray()
            val cx = (minX + maxX) / 2f; val cy = (minY + maxY) / 2f; val cz = (minZ + maxZ) / 2f
            val scale = 1.8f / max(maxX - minX, max(maxY - minY, maxZ - minZ)).coerceAtLeast(0.001f)
            var i = 0
            while (i < xyz.size) {
                xyz[i] = (xyz[i] - cx) * scale
                xyz[i + 1] = (xyz[i + 1] - cy) * scale
                xyz[i + 2] = (xyz[i + 2] - cz) * scale
                i += 3
            }
            val buffer = ByteBuffer.allocateDirect(xyz.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
            buffer.put(xyz).position(0)
            return listOf(Part("fallback", buffer, xyz.size / 3))
        }

        private fun readIndices(accessor: JSONObject, views: org.json.JSONArray, bin: ByteBuffer): IntArray {
            val view = views.getJSONObject(accessor.getInt("bufferView"))
            val offset = view.optInt("byteOffset", 0) + accessor.optInt("byteOffset", 0)
            val type = accessor.getInt("componentType")
            val count = accessor.getInt("count")
            return IntArray(count) { i ->
                when (type) {
                    5121 -> bin.get(offset + i).toInt() and 0xFF
                    5123 -> bin.getShort(offset + i * 2).toInt() and 0xFFFF
                    5125 -> bin.getInt(offset + i * 4)
                    else -> error("Formato de índices GLB no compatible: $type")
                }
            }
        }

        /** Convierte la transformación glTF (matriz o TRS) al formato OpenGL column-major. */
        private fun nodeMatrix(node: JSONObject): FloatArray {
            if (node.has("matrix")) return FloatArray(16) { node.getJSONArray("matrix").getDouble(it).toFloat() }
            val out = FloatArray(16).also { Matrix.setIdentityM(it, 0) }
            val t = node.optJSONArray("translation")
            if (t != null) Matrix.translateM(out, 0, t.getDouble(0).toFloat(), t.getDouble(1).toFloat(), t.getDouble(2).toFloat())
            val r = node.optJSONArray("rotation")
            if (r != null) {
                val x = r.getDouble(0).toFloat(); val y = r.getDouble(1).toFloat(); val z = r.getDouble(2).toFloat(); val w = r.getDouble(3).toFloat()
                val angle = Math.toDegrees((2.0 * kotlin.math.acos(w.coerceIn(-1f, 1f))).toDouble()).toFloat()
                val sinHalf = kotlin.math.sqrt((1f - w * w).coerceAtLeast(0f))
                if (sinHalf > 0.0001f) Matrix.rotateM(out, 0, angle, x / sinHalf, y / sinHalf, z / sinHalf)
            }
            val s = node.optJSONArray("scale")
            if (s != null) Matrix.scaleM(out, 0, s.getDouble(0).toFloat(), s.getDouble(1).toFloat(), s.getDouble(2).toFloat())
            return out
        }

        private fun zoneFor(path: String): String = when {
            "/skeleton" in path -> "bones"
            "/heart" in path -> "heart"
            "/lungs" in path -> "lungs"
            "/brain" in path -> "brain"
            "/digestive system" in path || "/kidneys and bladder" in path -> "abdomen"
            "/body" in path -> "body"
            else -> "other"
        }

        private data class RawPart(val zone: String, val xyz: FloatArray)
        private data class Part(val zone: String, val vertices: FloatBuffer, val vertexCount: Int)

        /** Almacena coordenadas en primitivos para evitar millones de objetos Float temporales. */
        private class FloatBuilder(initialCapacity: Int) {
            private var values = FloatArray(initialCapacity.coerceAtLeast(3))
            var size = 0
                private set
            fun add(value: Float) {
                if (size == values.size) values = values.copyOf(values.size * 2)
                values[size++] = value
            }
            fun toArray() = values.copyOf(size)
        }

        private fun linkProgram(vertex: String, fragment: String): Int {
            fun shader(type: Int, source: String): Int {
                val id = GLES20.glCreateShader(type)
                GLES20.glShaderSource(id, source); GLES20.glCompileShader(id)
                val status = IntArray(1); GLES20.glGetShaderiv(id, GLES20.GL_COMPILE_STATUS, status, 0)
                check(status[0] == GLES20.GL_TRUE) { GLES20.glGetShaderInfoLog(id) }
                return id
            }
            val v = shader(GLES20.GL_VERTEX_SHADER, vertex); val f = shader(GLES20.GL_FRAGMENT_SHADER, fragment)
            val id = GLES20.glCreateProgram()
            GLES20.glAttachShader(id, v); GLES20.glAttachShader(id, f); GLES20.glLinkProgram(id)
            val status = IntArray(1); GLES20.glGetProgramiv(id, GLES20.GL_LINK_STATUS, status, 0)
            check(status[0] == GLES20.GL_TRUE) { GLES20.glGetProgramInfoLog(id) }
            GLES20.glDeleteShader(v); GLES20.glDeleteShader(f)
            return id
        }

        private companion object {
            const val VERTEX_SHADER = "attribute vec3 aPosition; uniform mat4 uMvp; void main(){ gl_Position=uMvp*vec4(aPosition,1.0); }"
            const val FRAGMENT_SHADER = "precision mediump float; uniform vec4 uColor; void main(){ gl_FragColor=uColor; }"
        }
    }
}
