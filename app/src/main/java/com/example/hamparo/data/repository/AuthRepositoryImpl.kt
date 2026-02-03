package com.example.hamparo.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth
) : AuthRepository {

    override suspend fun registrarUsuario(email: String, clave: String, nombre: String): Result<FirebaseUser?> {
        return try {
            // 1. Creamos el usuario
            val resultado = auth.createUserWithEmailAndPassword(email, clave).await()
            val usuario = resultado.user

            // 2. Si se creó bien y tenemos nombre, actualizamos su perfil
            if (usuario != null && nombre.isNotBlank()) {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(nombre) // Aquí guardamos "Dra. García"
                    .build()

                usuario.updateProfile(profileUpdates).await()
            }

            // 3. Devolvemos éxito
            Result.success(usuario)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun loginUsuario(email: String, clave: String): Result<FirebaseUser?> {
        return try {
            val resultado = auth.signInWithEmailAndPassword(email, clave).await()
            Result.success(resultado.user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun cerrarSesion() {
        auth.signOut()
    }

    override fun getUsuarioActual(): FirebaseUser? {
        return auth.currentUser
    }
}