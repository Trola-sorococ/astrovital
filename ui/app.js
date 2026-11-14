document.addEventListener("DOMContentLoaded", () => {
    console.log("AstroVital inicializado correctamente.");
    const statusEl = document.getElementById("connection-status");
    if (statusEl) {
        statusEl.textContent = "Interfaz lista. Esperando contrato de datos (data/risk.json).";
    }
});