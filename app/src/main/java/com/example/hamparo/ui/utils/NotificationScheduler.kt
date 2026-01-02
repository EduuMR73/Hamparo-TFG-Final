package com.example.hamparo.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

// Función para programar una alarma que se repite
fun programarAlarma(context: Context, nombreMedicina: String, horasFrecuencia: Int) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    val intent = Intent(context, AlarmReceiver::class.java).apply {
        putExtra("NOMBRE_MEDICINA", nombreMedicina)
    }

    // Usamos el hashCode del nombre como ID único para no sobreescribir otras medicinas
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        nombreMedicina.hashCode(),
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    // Calculamos cuándo debe sonar la primera vez (Ej: Ahora + X horas)
    // Para probarlo rápido, puedes cambiar 'horasFrecuencia * 3600000L' por '10000L' (10 segundos)
    val tiempoEspera = horasFrecuencia * 60 * 60 * 1000L
    val triggerTime = System.currentTimeMillis() + tiempoEspera

    // Programamos la alarma repetitiva
    alarmManager.setRepeating(
        AlarmManager.RTC_WAKEUP, // Despierta al móvil si duerme
        triggerTime,
        tiempoEspera, // Se repite cada X horas
        pendingIntent
    )
}