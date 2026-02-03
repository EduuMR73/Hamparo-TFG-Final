package com.example.hamparo.ui.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.hamparo.R
import com.example.hamparo.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d("FCM", "¡Mensaje recibido! Datos: ${remoteMessage.data}")

        // 1. GESTIÓN DE WAKE LOCK (RA8.f Uso de recursos / Fiabilidad)
        // Despierta la CPU si el móvil está en reposo (Doze mode) para asegurar que procesamos la alerta.
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "Hamparo:AlertaEmergencia"
        )
        // Mantenemos despierto el procesador 3 segundos
        wakeLock.acquire(3000)

        // 2. Extraer datos con prioridad al payload de 'data'
        var titulo = "Alerta Hamparo"
        var mensaje = "Aviso importante"

        // Priorizamos los datos 'data' porque funcionan mejor en background que 'notification'
        if (remoteMessage.data.isNotEmpty()) {
            titulo = remoteMessage.data["titulo"] ?: remoteMessage.data["title"] ?: titulo
            mensaje = remoteMessage.data["mensaje"] ?: remoteMessage.data["message"] ?: mensaje
        }

        // Fallback por si llega como objeto notification
        remoteMessage.notification?.let {
            titulo = it.title ?: titulo
            mensaje = it.body ?: mensaje
        }

        mostrarNotificacionBlindada(titulo, mensaje)

        // Liberamos el recurso
        if (wakeLock.isHeld) wakeLock.release()
    }

    private fun mostrarNotificacionBlindada(titulo: String, mensaje: String) {
        val channelId = "hamparo_emergency_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Usamos sonido de ALARMA, no de notificación corta
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

        // Configuración del canal (Obligatorio Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            // Configurar atributos de audio para que Android respete el sonido de alarma
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM) // ¡Clave para que suene fuerte!
                .build()

            val channel = NotificationChannel(
                channelId,
                "Alertas de Emergencia",
                NotificationManager.IMPORTANCE_HIGH // Máxima prioridad
            ).apply {
                description = "Notificaciones de SOS y medicación crítica"
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 1000) // Patrón: Pausa, vibra, pausa, vibra...
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setSound(alarmSound, audioAttributes) // Asignamos el sonido al canal
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Intent para abrir la app al tocar
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        // Construcción de la notificación
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            // -------------------------------------------------------------------------
            // TODO: CAMBIO VISUAL (RA4.g Diseño visual)
            // Para sacar nota máxima, cambia este icono por uno monocromático (transparente/blanco).
            // Si usas 'ic_launcher_round' aquí, en Android 12+ se verá un cuadrado blanco.
            // Ejemplo: .setSmallIcon(R.drawable.ic_stat_sos)
            // -------------------------------------------------------------------------
            .setSmallIcon(R.mipmap.ic_launcher_round)

            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setAutoCancel(true)
            .setSound(alarmSound) // Para versiones anteriores a Android 8
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM) // Salta el modo "No Molestar"
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(pendingIntent, true) // Muestra alerta en pantalla completa si está bloqueado

        // Usamos System.currentTimeMillis().toInt() para que no se sobrescriban si llegan varias
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "Nuevo token: $token")
        // Aquí deberías llamar a tu repositorio para actualizar el token en Firestore
        // si el usuario ya ha iniciado sesión.
    }
}