// model/risk.js

/**
 * Calcula el riesgo biológico y fisiológico para cada zona del cuerpo.
 * @param {Object} inputs - Datos de entrada provenientes de data/risk_inputs.json
 * @returns {Object} Objeto con el riesgo (0 a 1), motivo y fuente para cada zona
 */
function calcularRiesgo(inputs) {
    // 1. Extraer variables clave con valores por defecto seguros
    const diasMision = inputs.mission_day || 1;
    const radiacionUSvH = inputs.radiation_uSv_h || 10;
    const flaresRecientes = inputs.solar_flares_30d || [];
  
    // 2. Modelo óseo (Columna / Vértebras) - Aumenta progresivamente con los días en microgravedad
    let riesgoSpine = Math.min(1.0, (diasMision / 365) * 0.8 + (radiacionUSvH / 100) * 0.2);
    
    // 3. Modelo óseo (Fémur) - Pérdida por falta de carga mecánica
    let riesgoFemur = Math.min(1.0, (diasMision / 300) * 0.7);
  
    // 4. Modelo muscular (Piernas) - Atrofia por desuso en microgravedad
    let riesgoMusculo = Math.min(1.0, (diasMision / 180) * 0.75);
  
    // 5. Modelo cardíaco - Estrés cardiovascular por radiación y erupciones solares
    let factorFlares = flaresRecientes.length * 0.05;
    let riesgoHeart = Math.min(1.0, (radiacionUSvH / 50) * 0.4 + factorFlares + 0.1);
  
    // Retorno estructurado exactamente según el contrato de datos
    return {
      "bone_spine": {
        "risk": Number(riesgoSpine.toFixed(2)),
        "why": `Desmineralización acumulada en ${diasMision} días de misión sin gravedad efectiva.`,
        "source": "NASA Human Research Program (HRP) - HRP-4701"
      },
      "bone_femur": {
        "risk": Number(riesgoFemur.toFixed(2)),
        "why": `Pérdida de densidad ósea por falta de carga axial en extremidades inferiores (${diasMision} días).`,
        "source": "NASA-STD-3001 (Flight Physiological Requirements)"
      },
      "muscle_legs": {
        "risk": Number(riesgoMusculo.toFixed(2)),
        "why": `Atrofia por desuso muscular en entorno de microgravedad prolongada.`,
        "source": "OSDR (Open Science Data Repository)"
      },
      "heart": {
        "risk": Number(riesgoHeart.toFixed(2)),
        "why": `Estrés cardiovascular inducido por radiación de ${radiacionUSvH} uSv/h y ${flaresRecientes.length} eventos solares recientes.`,
        "source": "NASA Space Radiation Analysis Group (SRAG)"
      }
    };
  }