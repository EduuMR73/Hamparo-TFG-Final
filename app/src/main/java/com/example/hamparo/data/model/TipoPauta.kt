package com.example.hamparo.data.model

enum class TipoFrecuencia(val label: String) {
    RUTINA("Rutina"),       // Desayuno, Comida...
    INTERVALO("Intervalo"), // Cada X horas
    SI_PRECISA("Si precisa") // A demanda
}

enum class TipoPauta { FIJA, VARIABLE, SI_PRECISA }