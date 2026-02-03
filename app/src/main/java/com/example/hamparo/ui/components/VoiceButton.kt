package com.example.hamparo.ui.components

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun VoiceButton(
    onResult: (String) -> Unit
) {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val textoEscuchado = results?.get(0) ?: ""

            if (textoEscuchado.isNotEmpty()) {
                onResult(textoEscuchado)
            }
        }
    }

    FloatingActionButton(
        onClick = {
            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    // 1. Modelo de lenguaje libre (para que entienda frases naturales)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)

                    // 2. FORZAR ESPAÑOL Y TEXTO DE AYUDA
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Diga el dato. Ej: 'Tensión 12 8' o 'Ayuda'")

                    // Le decimos a Google que espere 3 segundos de silencio absoluto antes de cortar
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
                    // Intentamos forzar una duración mínima de escucha
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 5000L)
                }
                launcher.launch(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Error: Su móvil no tiene reconocimiento de voz", Toast.LENGTH_SHORT).show()
            }
        },
        containerColor = MaterialTheme.colorScheme.tertiary, // Color azulito/terciario para destacar
        contentColor = Color.White,
        shape = CircleShape,
        modifier = Modifier.size(80.dp) // Botón bien grande para el abuelo
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = "Grabar voz",
            modifier = Modifier.size(40.dp)
        )
    }
}