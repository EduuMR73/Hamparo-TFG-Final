package com.example.hamparo.data.repository

import com.google.firebase.auth.FirebaseUser

interface AuthRepository {
    suspend fun registrarUsuario(email: String, clave: String, nombre: String): Result<FirebaseUser?>

    // Login normal (solo email y clave)
    suspend fun loginUsuario(email: String, clave: String): Result<FirebaseUser?>

    fun cerrarSesion()

    fun getUsuarioActual(): FirebaseUser?
}