package com.example.hamparo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.hamparo.ui.utils.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    // Inyectamos el "experto en ruido" que ya usamos para el SOS
    @Inject
    lateinit var notificationHelper: NotificationHelper

    override fun onReceive(context: Context, intent: Intent) {
        try {
            // 1. Recogemos los datos que nos manda el programador
            val nombreMedicina = intent.getStringExtra("NOMBRE_MEDICINA") ?: "Medicina"
            val mensajeExtra = intent.getStringExtra("MENSAJE")

            val tituloNotificacion = "💊 ¡HORA DE TU MEDICINA!"
            val cuerpoNotificacion = mensajeExtra ?: "Te toca tomar: $nombreMedicina"

            Log.d("Hamparo", "⏰ ¡ALARMA DISPARADA! -> $nombreMedicina")


            notificationHelper.mostrarNotificacion(tituloNotificacion, cuerpoNotificacion)

        } catch (e: Exception) {
            Log.e("Hamparo", "Error al procesar la alarma: ${e.message}")
            e.printStackTrace()
        }
    }
}