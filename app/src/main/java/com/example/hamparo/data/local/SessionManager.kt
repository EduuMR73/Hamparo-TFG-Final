package com.example.hamparo.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(@ApplicationContext context: Context) {
    // Guardamos datos simples de sesión
    private val prefs: SharedPreferences = context.getSharedPreferences("hamapro_session", Context.MODE_PRIVATE)

    companion object {
        const val KEY_TOKEN = "auth_token"
        const val KEY_ROLE = "user_role" // "ADMIN" o "PATIENT"
    }

    // Guardar sesión al entrar
    fun saveAuthToken(token: String, role: String) {
        val editor = prefs.edit()
        editor.putString(KEY_TOKEN, token)
        editor.putString(KEY_ROLE, role)
        editor.apply()
    }

    // Comprobar si ya está dentro (para saltar el login)
    fun fetchAuthToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }

    fun getUserRole(): String? {
        return prefs.getString(KEY_ROLE, null)
    }

    // Cerrar sesión (Solo para el cuidador)
    fun clearSession() {
        val editor = prefs.edit()
        editor.clear()
        editor.apply()
    }
}