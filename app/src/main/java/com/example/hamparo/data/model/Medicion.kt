package com.example.hamparo.data.model

import java.util.UUID

enum class TipoMedicion(val label: String, val unidad: String) {
    TENSION("Tensión Arterial", "mmHg"),
    PESO("Peso", "kg"),
    GLUCOSA("Glucosa", "mg/dL"),
    TEMPERATURA("Temperatura", "°C"),
    SATURACION("Sat. Oxígeno", "%"),
    PULSO("Frecuencia Cardíaca", "ppm")
}

data class Medicion(
    val id: String = UUID.randomUUID().toString(),
    val fecha: String = "",
    val hora: String = "",

    // Ordena por fecha real
    val timestamp: Long = 0L,

    val tipo: TipoMedicion = TipoMedicion.TENSION,

    val valor1: String = "",
    val valor2: String = "",
    val notas: String = "",
    val grupoId: String = "",
    val pacienteId: String = "",
    val pacienteNombre: String = "",
    val leido: Boolean = false,
    val archivada: Boolean = false
)