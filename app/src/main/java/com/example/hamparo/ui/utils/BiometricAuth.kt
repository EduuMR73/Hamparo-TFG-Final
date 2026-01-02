package com.example.hamparo.ui.utils

import android.content.Context
import android.os.Build
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

        // CAMBIO CLAVE: Aceptamos seguridad "WEAK" (Débil) que incluye desbloqueo facial 2D
        // y aseguramos compatibilidad con PIN/Patrón.
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL

        val canAuthenticate = biometricManager.canAuthenticate(authenticators)

        if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
            // Si hay error, mostramos POR QUÉ falla en un Toast para que tú lo veas
            val mensajeError = when(canAuthenticate) {
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "No tienes sensor de huella"
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "Sensor ocupado"
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "¡Configura una HUELLA en Ajustes!"
                else -> "Error de seguridad: $canAuthenticate"
            }

            // MODO DEBUG: Si falla, te dejamos pasar pero te avisamos del error
            Toast.makeText(context, "⚠️ Pasando en modo Debug ($mensajeError)", Toast.LENGTH_LONG).show()
            onSuccess()
            return
        }

        // 2. Preparamos el diálogo
        val executor = ContextCompat.getMainExecutor(context)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                // ¡ÉXITO!
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                // Si el usuario cancela (le da al botón Atrás o cancelar), NO entramos.
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    Toast.makeText(context, "Error: $errString", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val biometricPrompt = BiometricPrompt(context as FragmentActivity, executor, callback)

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Acceso a Cuidador")
            .setSubtitle("Usa tu Huella, Cara o PIN")
            // CAMBIO CLAVE: Usamos los mismos autenticadores aquí
            .setAllowedAuthenticators(authenticators)
            .build()

        // 3. ¡Mostramos la ventana!
        biometricPrompt.authenticate(promptInfo)
    }
}