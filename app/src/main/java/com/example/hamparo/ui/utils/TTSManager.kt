package com.example.hamparo.ui.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject

class TTSManager @Inject constructor(
    @ApplicationContext context: Context
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isLoaded = false
    private val colaPendiente = mutableListOf<String>()

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // Intentamos forzar Español de España
            val result = tts?.setLanguage(Locale("es", "ES"))

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Si falla España, intentamos Español genérico (Latinoamérica/Global)
                val fallback = tts?.setLanguage(Locale("es"))
                if (fallback == TextToSpeech.LANG_MISSING_DATA || fallback == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTSManager", "ALERTA: No hay voz en español instalada. Usará la por defecto (quizás inglés).")
                }
            }

            tts?.setSpeechRate(1.0f)
            tts?.setPitch(1.0f)

            isLoaded = true
            if (colaPendiente.isNotEmpty()) {
                colaPendiente.forEach { hablar(it) }
                colaPendiente.clear()
            }
        } else {
            Log.e("TTSManager", "Error al iniciar el motor TTS")
        }
    }

    fun hablar(texto: String) {
        // Truco: Pasamos todo a minúsculas aquí también por seguridad
        val textoSeguro = limpiarTextoParaVoz(texto).lowercase()

        if (isLoaded) {
            tts?.speak(textoSeguro, TextToSpeech.QUEUE_FLUSH, null, null)
        } else {
            colaPendiente.add(textoSeguro)
        }
    }

    private fun limpiarTextoParaVoz(texto: String): String {
        return texto
            .replace(" mg", " miligramos", ignoreCase = true)
            .replace(" g ", " gramos ", ignoreCase = true)
            .replace(" ml", " mililitros", ignoreCase = true)
            .replace("comp.", "comprimidos", ignoreCase = true)
            .replace("EFG", "", ignoreCase = true)
            .replace("/", " barra ")
            // Quitamos guiones que a veces provocan deletreo "P-a-r-a..."
            .replace("-", "")
            .trim()
    }

    fun stop() {
        if (tts != null) {
            tts?.stop()
            tts?.shutdown()
            isLoaded = false
        }
    }
}