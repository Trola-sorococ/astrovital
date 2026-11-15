# Referencias de datos NASA

Cada campo usado en data/risk_inputs.json conserva su fuente. Los estados historical_reference y unavailable dejan claro cuándo un valor no es actual o la API no respondió.

## Datos usados

| Dataset / fuente | Enlace | Dato tomado |
| --- | --- | --- |
| NASA DONKI, Solar Flare (FLR) | [API DONKI](https://api.nasa.gov/DONKI/FLR) | Se intentó la ventana de 30 días. El endpoint devolvió HTTP 404; el conteo queda como null, no como cero. El script guarda data/flares_raw.json cuando la API devuelve una respuesta válida. |
| RadLab, ISS DosTel | [Documentación de la API](https://visualization.osdr.nasa.gov/radlab/gui/data-api/) · [consulta del 2023-01-01](https://visualization.osdr.nasa.gov/radlab/api/?spacecraft=ISS&instrument=DosTel&timestamp%3E=2023-01-01&timestamp%3C2023-01-02&absorbed_dose_rate&timestamp&format=json) | 895 mediciones: promedio 28.303083 µGy/h y máximo 510.373322 µGy/h. Es una referencia histórica, no una estimación actual ni un umbral clínico. |
| OSDR OSD-980 | [Ficha del estudio](https://osdr.nasa.gov/bio/repo/data/studies/OSD-980) | Ratones Mus musculus; descarga de miembros posteriores como modelo de microgravedad; evidencia cualitativa sobre hueso. |
| OSDR OSD-781 | [Ficha del estudio](https://osdr.nasa.gov/bio/repo/data/studies/OSD-781) | Estudio de músculo esquelético en microgravedad; evidencia cualitativa musculoesquelética. |

## Fuentes revisadas, sin datos incorporados al JSON inicial

| Fuente | Enlace | Resultado |
| --- | --- | --- |
| OSDR Biological Data API | [Documentación](https://visualization.osdr.nasa.gov/biodata/api/) | Permite consultar metadatos con filtros por organismo. La consulta de prueba devolvió HTTP 400; el script conserva estudios curados mientras se recupera el servicio. |
| Environmental Data Application | [Portal EDA](https://visualization.osdr.nasa.gov/eda/) | Ofrece descarga de datos crudos y resumidos desde Data Tables. No se incorporó una tabla EDA a este primer conjunto compacto. |

## Actualización

Instala requests desde requirements.txt y ejecuta python data/ingest/fetch_nasa_data.py. Para evitar límites de DEMO_KEY, define NASA_API_KEY con una clave propia de [NASA Open APIs](https://api.nasa.gov/). El proceso no convierte una respuesta fallida en cero ni descarga volcados biológicos grandes.