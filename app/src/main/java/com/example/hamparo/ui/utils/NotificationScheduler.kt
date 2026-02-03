package com.example.hamparo.ui.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.hamparo.AlarmReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class NotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    // Función para programar la alarma de medicina
    fun programarAlarmaMedicina(nombreMedicina: String, tiempoMilis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Intent que apunta a tu AlarmReceiver
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("TITULO", "¡Hora de tu medicina!")
            putExtra("MENSAJE", "Te toca tomar: $nombreMedicina")
            putExtra("NOMBRE_MEDICINA", nombreMedicina)
        }

        // PendingIntent único para esta medicina
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            nombreMedicina.hashCode(), // Usamos el hash del nombre como ID único
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Calculamos el momento exacto en el futuro (Ahora + tiempoMilis)
        val triggerTime = System.currentTimeMillis() + tiempoMilis

        try {
            // Lógica para diferentes versiones de Android (Doze Mode)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } else {
                    // Si no tenemos permiso exacto, usamos el normal
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
            Log.d("Hamparo", "Alarma programada para $nombreMedicina en ${tiempoMilis/1000}s")
        } catch (e: SecurityException) {
            Log.e("Hamparo", "Error de seguridad (permiso de alarmas): ${e.message}")
        }
    }

    // Función para cancelar una alarma específica
    fun cancelarAlarma(nombreMedicina: String) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                nombreMedicina.hashCode(),
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
            )

            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
                Log.d("Hamparo", "Alarma cancelada para: $nombreMedicina")
            }
        } catch (e: Exception) {
            Log.e("Hamparo", "Error al cancelar alarma: ${e.message}")
        }
    }
}