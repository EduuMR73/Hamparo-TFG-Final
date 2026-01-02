package com.example.hamparo.data.local.entities

enum class UserRole {
    PACIENTE,
    CUIDADOR
}

enum class MedicionType {
    TENSION_ARTERIAL,
    GLUCOSA,
    PULSO,
    PESO,
    OXIGENO,
    ALERTA  // <--- ¡NUEVO! Registraremos las llamadas de auxilio aquí
}