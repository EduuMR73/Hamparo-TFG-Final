package com.example.hamparo.data.model

import java.util.UUID

enum class TipoAlerta {
    URGENTE,    // SOS (Rojo)
    MEDICACION, // Olvido de pastilla (Naranja)
    INFO        // Mensaje del sistema (Azul)
}

data class Alerta(
    val id: String = UUID.randomUUID().toString(),
    val pacienteId: String = "",
    val titulo: String = "",
    val mensaje: String = "",
    val fecha: String = "", // Ej: "26/01/2026"
    val hora: String = "",  // Ej: "19:30"
    val timestamp: Long = System.currentTimeMillis(), // Para ordenar cronológicamente
    val tipo: TipoAlerta = TipoAlerta.INFO,
    val leido: Boolean = false, // LA CLAVE: Determina si sale el globo rojo

    // Para mover la alerta al Historial
    // false = Bandeja de Entrada (Activa/Pendiente), true = Historial (Resuelta/Atendida)
    val archivada: Boolean = false
)