// model/risk.js
// Modelo experimental y educativo. NO es diagnóstico ni pronóstico clínico.

/**
 * Calcula un índice de riesgo (0 a 1) para cada zona del cuerpo.
 * @param {Object} inputs - Datos de entrada (contrato de data/risk_inputs.json):
 *   mission_day, radiation_uSv_h, solar_flares_30d (lista).
 * @returns {Object} Riesgo (0 a 1), motivo y fuente por zona.
 */
export function calcularRiesgo(inputs = {}) {
  // Valores por defecto seguros (?? para no tratar 0 como "falta de dato")
  const diasMision = Math.max(1, Number(inputs.mission_day) || 1);
  const radiacionUSvH = Number(inputs.radiation_uSv_h ?? 10);
  const flaresRecientes = Array.isArray(inputs.solar_flares_30d) ? inputs.solar_flares_30d : [];

  // Columna: crece con los días en microgravedad y con la radiación
  const riesgoSpine = Math.min(1.0, (diasMision / 365) * 0.8 + (radiacionUSvH / 100) * 0.2);
  // Fémur: pérdida por falta de carga mecánica
  const riesgoFemur = Math.min(1.0, (diasMision / 300) * 0.7);
  // Piernas: atrofia por desuso
  const riesgoMusculo = Math.min(1.0, (diasMision / 180) * 0.75);
  // Corazón: estrés cardiovascular por radiación y erupciones solares
  const factorFlares = flaresRecientes.length * 0.05;
  const riesgoHeart = Math.min(1.0, (radiacionUSvH / 50) * 0.4 + factorFlares + 0.1);

  return {
    bone_spine: {
      risk: Number(riesgoSpine.toFixed(2)),
      why: `Desmineralización acumulada en ${diasMision} días de misión sin gravedad efectiva.`,
      source: 'Human Research Program (HRP) - HRP-4701'
    },
    bone_femur: {
      risk: Number(riesgoFemur.toFixed(2)),
      why: `Pérdida de densidad ósea por falta de carga axial en extremidades inferiores (${diasMision} días).`,
      source: 'STD-3001 (Flight Physiological Requirements)'
    },
    muscle_legs: {
      risk: Number(riesgoMusculo.toFixed(2)),
      why: 'Atrofia por desuso muscular en entorno de microgravedad prolongada.',
      source: 'OSDR (Open Science Data Repository)'
    },
    heart: {
      risk: Number(riesgoHeart.toFixed(2)),
      why: `Estrés cardiovascular inducido por radiación de ${radiacionUSvH} uSv/h y ${flaresRecientes.length} eventos solares recientes.`,
      source: 'Space Radiation Analysis Group (SRAG)'
    }
  };
}
