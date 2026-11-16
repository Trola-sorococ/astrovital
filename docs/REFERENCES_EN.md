# References and credits (English)

## Open data

| Use | Source | Notes |
| --- | --- | --- |
| Solar flares, last 30 days (live) | [DONKI](https://ccmc.gsfc.nasa.gov/tools/DONKI/) · [FLR API](https://ccmc.gsfc.nasa.gov/DONKI-API/get/FLR) | Fetched when the app opens. If the query fails, the last valid response is shown; if there is none, "no data" is shown (never zero). The old `api.nasa.gov/DONKI/FLR` URL returns HTTP 404. |
| Radiation reference | [OSDR RadLab](https://visualization.osdr.nasa.gov/radlab/) (ISS, DosTel) | Historical absorbed dose (28.303083 µGy/h, 2023-01-01). Not converted to µSv/h. |
| Bone and muscle in microgravity | [OSD-980](https://osdr.nasa.gov/bio/repo/data/studies/OSD-980) · [OSD-781](https://osdr.nasa.gov/bio/repo/data/studies/OSD-781) · [OSDR biological data API](https://visualization.osdr.nasa.gov/biodata/api/) | Qualitative evidence only. |
| Cabin CO2 (planned) | [OSDR Environmental Data Application](https://visualization.osdr.nasa.gov/eda/) | Not imported yet; the 3.8 mmHg value is a provisional placeholder. |
| Radiation context | [Space radiation hazard](https://www.nasa.gov/hrp/hazard-space-radiation/) · [Space radiation element](https://www.nasa.gov/reference/about-the-space-radiation-element/) | Text reference. |

## Code, libraries and other resources

| Resource | Use | License |
| --- | --- | --- |
| [Three.js r170](https://threejs.org/) (`vendor/three/`) | 3D viewer | MIT |
| [Draco decoder](https://github.com/google/draco) | GLB decompression | Apache-2.0 |
| [Python requests](https://requests.readthedocs.io/) | Data ingestion script | Apache-2.0 |
| Kotlin / Android SDK | Native Android app (`android/`, Spanish only) | Apache-2.0 |
| 3D body model `assets/modelo_zerog_pulse.glb` | Anatomical model | Created by the team **[confirm]** |
| Mars image (slides/video only) | Visual | Channel "GeoEnciclopedia" on YouTube **[add link and permission, or remove]** |
| GitHub Pages | Hosting | — |

## AI tools used

- Claude and Gemini: translation and script assistance.
- Cursor (with Claude): code, documentation and review assistance.
- CapCut text-to-speech (voice "[name]"): video voice-over.

All AI-assisted content was reviewed by the team.
