package com.example.hamparo.data.model

import java.util.UUID

data class Cura(
    val id: String = UUID.randomUUID().toString(),
    val fecha: Long = System.currentTimeMillis(),

    // DATOS BÁSICOS
    val tipoHerida: String = "Úlcera (UPP)", // Ej: Úlcera, Corte, Quemadura...
    val zona: String = "",

    // EVOLUCIÓN
    val ancho: String = "", // cm
    val largo: String = "", // cm
    val profundidad: String = "", // cm (opcional)
    val escalaDolor: Int = 0, // 0 - 10 (EVA)

    // ESTADO
    val estado: String = "", // Necrótica, Granulando...

    // TRATAMIENTO
    val materiales: String = "",

    // INFO ADICIONAL
    val imagenUri: String? = null,
    val realizadaPor: String = "Cuidador",

    // GESTIÓN DE PESTAÑAS
    var archivada: Boolean = false
)