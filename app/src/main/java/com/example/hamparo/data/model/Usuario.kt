package com.example.hamparo.data.model

data class Usuario(
    val id: String = "",
    val nombre: String = "",
    val apellidos: String = "",
    val email: String = "",
    val rol: String = "CUIDADOR",
    val fotoUrl: String? = null,
    val pacientesVinculados: List<String> = emptyList()
)