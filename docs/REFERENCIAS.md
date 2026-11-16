# Referencias de Dev B — datos abiertos para el evento 

El JSON usa los nombres y tipos del contrato. Misión, radiación y CO2 son provisionales; no deben presentarse como mediciones reales. El campo _quality permite a Dev C distinguir ese estado.

| Campo | Fuente | Estado / dato |
| --- | --- | --- |
| generated | Fecha UTC de generación local | Fecha de actualización. |
| mission_day | Ejemplo del contrato del equipo | Provisional: 112. Reemplazar con el día real de misión. |
| radiation_uSv_h | [API RadLab](https://visualization.osdr.nasa.gov/radlab/gui/data-api/) | El contrato requiere µSv/h (dosis equivalente). La consulta ISS disponible devolvió µGy/h (dosis absorbida), que no se convierte automáticamente; el 28 µSv/h del JSON es provisional y proviene del ejemplo del contrato. |
| cabin_co2_mmHg | [Environmental Data Application](https://visualization.osdr.nasa.gov/eda/) | EDA ofrece descarga de tablas crudas y resumidas; no se importó CSV. El valor 3.8 mmHg es provisional del ejemplo del contrato. |
| solar_flares_30d | [NASA DONKI FLR](https://api.nasa.gov/DONKI/FLR) | Ventana de 30 días. La consulta devolvió HTTP 404; no se afirma que no hubiera erupciones. Si responde, el script guarda data/flares_raw.json y mapea fecha y clase. |
| estudios de hueso y músculo | [OSDR Biological Data API](https://visualization.osdr.nasa.gov/biodata/api/) · [OSD-980](https://osdr.nasa.gov/bio/repo/data/studies/OSD-980) · [OSD-781](https://osdr.nasa.gov/bio/repo/data/studies/OSD-781) | Estudios relevantes identificados. La consulta de metadatos devolvió HTTP 400; se conservan enlaces NASA directos. |

## Actualización

Instala Python 3 y requests con python -m pip install requests. Define ZeroG_API_KEY como variable de entorno, sin guardarla en GitHub, y ejecuta python data/ingest/fetch_nasa_data.py. El proceso no convierte una consulta fallida en cero ni µGy/h en µSv/h.

Este JSON es el primer ejemplo para que Dev C y Dev D integren el contrato. Los campos provisionales deben sustituirse por mediciones verificadas antes de la demo final.
