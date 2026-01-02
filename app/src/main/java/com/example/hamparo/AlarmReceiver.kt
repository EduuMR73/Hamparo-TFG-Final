package com.example.hamparo.utils

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.hamparo.MainActivity
import com.example.hamparo.R

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Recogemos el nombre de la medicina que nos pasan
        val nombreMedicina = intent.getStringExtra("NOMBRE_MEDICINA") ?: "Medicina"

        // Creamos la notificación
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Intent para abrir la App al tocar la notificación
        val tapIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            tapIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // Diseño de la Notificación 🔔
        val notificacion = NotificationCompat.Builder(context, "canal_medicinas")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Asegúrate de tener un icono aquí
            .setContentTitle("💊 HORA DE TU MEDICINA")
            .setContentText("Te toca tomar: $nombreMedicina")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // ¡Lanzamos la notificación!
        notificationManager.notify(nombreMedicina.hashCode(), notificacion)
    }
}