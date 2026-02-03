package com.example.hamparo.data.model

import java.util.UUID

data class CitaMedica(
    val id: String = UUID.randomUUID().toString(),
    val fecha: String = "",        // String para "25/10/2024"
    val hora: String = "",         // String para "10:30"
    val especialista: String = "", // "Cardiólogo"
    val centroMedico: String = "", // "Hospital La Fe"
    val observaciones: String = "",
    val completada: Boolean = false,

    // false = Pestaña "Próximas", true = Pestaña "Historial"
    val archivada: Boolean = false
)