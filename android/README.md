# Astrovital · ZeroG-Pulse

Aplicación Android nativa en Kotlin para seguimiento visual de una misión. Perfil, JSON, caché de actividad espacial y escaneos se guardan localmente.

## Instalar

Abre `ZeroG-Pulse.apk` en Android. Versión 0.7.0 (versionCode 7), firmada con la clave de depuración usada en las versiones anteriores de PRUEBA2 para permitir actualizar la instalación. El ícono del lanzador usa el pulso proporcionado y el nombre visible es Astrovital.

## Modelo 3D

El visor GLB se dibuja sobre la interfaz, muestra un estado de carga y permite girar la figura arrastrando sobre ella. Si el archivo GLB no puede abrirse, la aplicación intenta mostrar el STL incluido como respaldo. Los archivos están en `app/src/main/assets`, así que funcionan sin internet.

## Escaneos de misión

- **Antes del viaje:** registra el escaneo inicial como línea base.
- **Durante la misión:** guarda el escaneo de salida del planeta y tantos escaneos de estancia como necesites.
- **Después del viaje:** registra el regreso y consulta los efectos acumulados estimados para la duración fija del perfil.
- Antes de guardar cada escaneo se puede elegir una situación de demostración: órbita nominal, EVA, microgravedad prolongada, evento solar, exposición a radiación, aumento de CO₂, descanso insuficiente, tormenta solar, fuga de CO₂, fatiga, erupciones solares/clima espacial, biología del vuelo espacial, dosimetría o telemetría de cabina ISS. Las zonas rojas son referencias de atención del escenario, no lesiones. Los eventos solares no dan la dosis de una persona; la telemetría ISS y la dosimetría actuales requieren fuentes medidas que este proyecto no tiene. DosTel y OSDR solo aportan referencias históricas/curatoriales. NASA estudia efectos potenciales de radiación en varios tejidos y señala que la valoración depende de dosis, periodo y otras condiciones ([HRP: riesgos de radiación espacial](https://www.nasa.gov/hrp/hazard-space-radiation/), [NASA: efectos de la radiación](https://www.nasa.gov/reference/about-the-space-radiation-element/)). EVA, fatiga y descanso solo modifican signos demostrativos, no predicciones clínicas.
- Cada captura guarda fecha, lecturas de demostración, valores disponibles del JSON y de la API, zonas marcadas y cálculos. Aparece en la ficha de etapa y en **Historial**; al tocarla vuelve a abrirse.
- Verde indica que los signos simulados de esa captura están dentro de rangos ilustrativos. Rojo identifica zonas destacadas por reglas experimentales; no confirma lesiones.
- El modelo no tiene una malla muscular independiente. El indicador de músculo se informa como estimación y usa el esqueleto solo como referencia visual.

## Fuentes de datos

- Signos vitales variables cada cuatro segundos: son una demostración de interfaz, no mediciones de una persona. Solo se guardan al pulsar **Escanear** y quedan identificados como simulados.
- El JSON de entrada está incluido en `app/src/main/assets/risk_inputs.json` usando el esquema `schema_version` / `inputs` de `risk_inputs.json`. Si no hay un JSON válido, se muestra la opción para importarlo una vez; al conectarse uno válido, la opción desaparece y queda disponible sin conexión.
- Las llamaradas solares sí se consultan en vivo desde DONKI CCMC (`/DONKI-API/get/FLR`) al abrir la app o al pulsar actualizar; la última respuesta válida queda en caché. Esta consulta pública no requiere la clave de `api.nasa.gov` y no mide la dosis individual de la tripulación.
- Los datos DosTel del JSON son una referencia histórica y los registros OSDR son referencias curatoriales, no conexiones en vivo a esas APIs. El JSON entregado no contiene CO₂ de cabina confirmado ni un conteo de llamaradas confirmado; esos campos no deben presentarse como cero ni como mediciones actuales.
- Al guardar un escaneo, Android muestra una notificación local. Si frecuencia cardíaca, SpO₂ o temperatura de demostración salen de los rangos configurados, la notificación incluye una recomendación de pausa, aviso al responsable médico y repetir la medición con un dispositivo real. Si quedan dentro del rango, confirma que están bien y aclara que son datos demostrativos.
- Al guardar correctamente el perfil, la app cierra el formulario, vuelve a Inicio y muestra una confirmación breve. Los campos de nombre y edad son obligatorios.
- Al guardar un registro de ánimo, «Mal», «Cansado» o una nota que mencione síntomas de alerta (por ejemplo dolor, mareo, fiebre o falta de aire) genera una alerta con recomendación. «Muy bien» y «Bien» generan una confirmación sin alerta. La nota personal no se incluye en la notificación. Android solicita permiso al guardar el primer registro; si se deniega, hay que habilitar las notificaciones de Astrovital en Ajustes.
- Las notificaciones son avisos locales de la app, no mensajes a terceros ni un servicio de vigilancia en segundo plano. Los signos usados por los escaneos son simulados y no son mediciones clínicas ni diagnóstico.
- Los efectos son estimaciones experimentales adaptadas de `model/risk.js`. Los valores provisionales no se presentan como mediciones reales ni como consejo clínico.

Abre el proyecto en Android Studio con JDK 17 o posterior y Android SDK 35.
