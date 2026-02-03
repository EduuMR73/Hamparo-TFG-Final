package com.example.hamparo.data.model

import java.util.Calendar

data class Medicamento(
    val id: String = "",
    val nombre: String = "",        // Nombre técnico completo (del QR/CIMA)
    val nombreCorto: String = "",   // Alias manual (ej: "Pastilla azul")
    val dosis: String = "",
    val frecuencia: String = "",    // Texto descriptivo (ej: "Cada 8 horas")
    val stock: String = "0",        // Cantidad restante (Firebase lo guarda como String)
    val grupoId: String = "",
    val pacienteNombre: String = "",
    val ultimaToma: Long = 0L,      // Timestamp de la última vez que se tomó
    val nregistro: String = "",     // NECESARIO para volver a consultar la API

    val archivada: Boolean = false, // Para ocultarla sin borrarla de la BD
    val frecuenciaHoras: Int = 0    // El número limpio (ej: 8) para calcular la próxima alarma
) {

    /**
     * Convierte el stock de String a Int de forma segura.
     * Si está vacío o es erróneo, devuelve 0.
     */
    fun stockActual(): Int = stock.toIntOrNull() ?: 0

    /**
     * Comprueba si la 'ultimaToma' fue HOY.
     * Esto permite pintar la tarjeta de verde aunque se cierre la app.
     */
    fun fueTomadaHoy(): Boolean {
        if (ultimaToma == 0L) return false

        val calendarioHoy = Calendar.getInstance()
        val calendarioToma = Calendar.getInstance().apply { timeInMillis = ultimaToma }

        return calendarioHoy.get(Calendar.YEAR) == calendarioToma.get(Calendar.YEAR) &&
                calendarioHoy.get(Calendar.DAY_OF_YEAR) == calendarioToma.get(Calendar.DAY_OF_YEAR)
    }

    // --- FUNCIONES VISUALES (EXISTENTE) ---

    fun obtenerNombreVisual(): String {
        return if (nombreCorto.isNotBlank()) {
            nombreCorto
        } else {
            val palabras = nombre.split(" ")
            if (palabras.size > 2) {
                palabras.take(3).joinToString(" ") + "..."
            } else {
                nombre
            }
        }
    }
}