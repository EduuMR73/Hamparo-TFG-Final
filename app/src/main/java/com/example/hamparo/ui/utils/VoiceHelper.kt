package com.example.hamparo.ui.utils

import android.content.Intent
import android.speech.RecognizerIntent

/**
 * Especialista encargado de configurar el micrófono de Google.
 * Usamos INTENT porque es más compatible con Huawei y dispositivos sin servicios completos.
 */
object VoiceHelper {

    fun getIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            // 1. Modelo de lenguaje libre (para frases naturales)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)

            // 2. FORZAR ESPAÑOL DE ESPAÑA
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Diga el dato (Ej: Tensión 12 8)")

            // 3. TRUCOS "ANTI-CORTE" (Del archivo VoiceButton que tenías):
            // Le damos 3 segundos de silencio antes de cortar, ideal para personas mayores que pausan al hablar.
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 2000L)
        }
    }

    // Extrae el texto limpio del resultado de Android
    fun parseResult(data: Intent?): String {
        val resultados = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        return resultados?.get(0) ?: "" // Devuelve el texto o vacío si falló
    }
}