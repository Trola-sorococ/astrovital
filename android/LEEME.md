# ZeroG-Pulse · Órbita Bienestar

Aplicación Android nativa en Kotlin para seguimiento visual de una misión. Perfil, JSON, caché de actividad espacial y escaneos se guardan localmente.

## Instalar

Abre `ZeroG-Pulse.apk` en Android. Versión 0.4.0 (versionCode 4), firmada con la clave de depuración usada en las versiones anteriores de PRUEBA2 para permitir actualizar la instalación.

## Modelo 3D

El visor GLB se dibuja sobre la interfaz, muestra un estado de carga y permite girar la figura arrastrando sobre ella. Si el archivo GLB no puede abrirse, la aplicación intenta mostrar el STL incluido como respaldo. Los archivos están en `app/src/main/assets`, así que funcionan sin internet.

## Escaneos de misión

- **Antes del viaje:** registra el escaneo inicial como línea base.
- **Durante la misión:** guarda el escaneo de salida del planeta y tantos escaneos de estancia como necesites.
- **Después del viaje:** registra el regreso y consulta los efectos acumulados estimados para la duración fija del perfil.
- Cada captura guarda fecha, lecturas de demostración, valores disponibles del JSON y de la API, zonas marcadas y cálculos. Aparece en la ficha de etapa y en **Historial**; al tocarla vuelve a abrirse.
- Verde indica que los signos simulados de esa captura están dentro de rangos ilustrativos. Rojo identifica zonas destacadas por reglas experimentales; no confirma lesiones.
- El modelo no tiene una malla muscular independiente. El indicador de músculo se informa como estimación y usa el esqueleto solo como referencia visual.

## Fuentes de datos

- Signos vitales variables cada cuatro segundos: son una demostración de interfaz, no mediciones de una persona. Solo se guardan al pulsar **Escanear** y quedan identificados como simulados.
- El JSON inicial está incluido en `app/src/main/assets/risk_inputs.json`. Se puede importar otra exportación desde Signos; queda disponible sin conexión y sus notas `_quality` aparecen junto al escaneo.
- La API de actividad espacial consulta eventos solares de los últimos 30 días; la última respuesta válida queda en caché. Requiere internet para obtener datos nuevos y no mide la dosis individual de la tripulación.
- Los efectos son estimaciones experimentales adaptadas de `model/risk.js`. Los valores provisionales no se presentan como mediciones reales ni como consejo clínico.

Abre el proyecto en Android Studio con JDK 17 o posterior y Android SDK 35.
