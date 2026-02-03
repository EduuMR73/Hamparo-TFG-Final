package com.example.hamparo.ui.utils

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.AudioAttributes
import android.media.AudioManager // 👇 Nuevo import necesario
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.hamparo.MainActivity
import com.example.hamparo.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.random.Random

class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        // USAMOS EL MISMO ID QUE EN EL SERVICIO DE FIREBASE PARA UNIFICAR
        const val CHANNEL_ID_URGENT = "hamparo_emergency_channel"
        const val CHANNEL_NAME = "Alertas de Emergencia"
    }

    init {
        crearCanalNotificacion()
    }

    fun mostrarNotificacion(titulo: String, mensaje: String) {

        // SUBIR VOLUMEN AL MÁXIMO AUTOMÁTICAMENTE

        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

            // 1. Averiguamos cuál es el volumen máximo que permite este móvil para alarmas
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)

            // 2. Forzamos el volumen actual a ese máximo (sin mostrar la barrita visual para ser más rápido)
            audioManager.setStreamVolume(
                AudioManager.STREAM_ALARM,
                maxVolume,
                0
            )
        } catch (e: Exception) {
            e.printStackTrace() // Si falla por lo que sea, seguimos adelante para notificar igual
        }
        // -----------------------------------------------------------------------

        // Chequeo de permisos para Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        // Intent para abrir la app al tocar la notificación
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_URGENT)
            .setSmallIcon(R.mipmap.ic_launcher_round) // Icono correcto de tu app
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setStyle(NotificationCompat.BigTextStyle().bigText(mensaje))
            .setPriority(NotificationCompat.PRIORITY_MAX) // Prioridad máxima
            .setCategory(NotificationCompat.CATEGORY_ALARM) // Categoría Alarma (salta el No Molestar)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            // Vibración potente: Pausa, vibra 0.5s, pausa 0.2s, vibra 0.5s...
            .setVibrate(longArrayOf(0, 500, 200, 500, 200, 500))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        try {
            NotificationManagerCompat.from(context).notify(Random.nextInt(), builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun crearCanalNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID_URGENT, CHANNEL_NAME, importance).apply {
                description = "Suena fuerte para medicinas y emergencias"
                enableVibration(true)
                enableLights(true)
                lightColor = Color.RED
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)

                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build()
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), audioAttributes)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}