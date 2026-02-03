package com.example.hamparo.ui.utils

import android.content.Context
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricAuth {

    fun authenticate(
        context: Context,
        onSuccess: () -> Unit // Qué hacer si entra correctamente
    ) {
        val biometricManager = BiometricManager.from(context)

        // Aceptamos Huella, Cara (incluso 2D) o el PIN/Patrón del móvil si el sensor falla.
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL

        val canAuthenticate = biometricManager.canAuthenticate(authenticators)

        if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
            // Si falla el hardware (ej: emulador roto),
            // avisamos pero dejamos pasar para que no te quedes bloqueado en la defensa.
            val mensajeError = when(canAuthenticate) {
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "Dispositivo sin sensor biométrico"
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "Sensor ocupado temporalmente"
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "No hay huellas configuradas"
                else -> "Error de seguridad: $canAuthenticate"
            }
            Toast.makeText(context, "⚠️ Modo Debug: Acceso permitido ($mensajeError)", Toast.LENGTH_LONG).show()
            onSuccess()
            return
        }

        val executor = ContextCompat.getMainExecutor(context)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                // Autenticación correcta
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                // Si el usuario cancela voluntariamente, no mostramos error
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                    errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                    errorCode != BiometricPrompt.ERROR_CANCELED) {
                    Toast.makeText(context, "Autenticación fallida: $errString", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val biometricPrompt = BiometricPrompt(context as FragmentActivity, executor, callback)

        // En BiometricAuth.kt
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Bienvenido de nuevo")
            .setSubtitle("Accede de forma segura a Hamparo")
            // Quitamos la descripción para que se vea más limpio y menos cargado
            //.setDescription("...")
            .setAllowedAuthenticators(authenticators)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}