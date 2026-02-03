package com.example.hamparo.ui

import androidx.lifecycle.ViewModel
import com.example.hamparo.ui.utils.NotificationHelper
import com.example.hamparo.ui.utils.NotificationScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val notificationScheduler: NotificationScheduler,
    private val notificationHelper: NotificationHelper
) : ViewModel() {

    /**
     * Función para cuando el usuario guarda una medicina (Alarma futura).
     */
    fun guardarMedicina(nombre: String, horas: Int) {
        // 1. Convertimos las horas a milisegundos
        val tiempoMilis = horas * 60 * 60 * 1000L

        // 2. Llamamos al scheduler con el formato correcto (Nombre, Milisegundos)
        notificationScheduler.programarAlarmaMedicina(
            nombreMedicina = nombre,
            tiempoMilis = tiempoMilis
        )
    }

    /**
     * Función para probar el sonido AHORA (Botón de pánico o test).
     */
    fun pruebaInmediata() {
        notificationHelper.mostrarNotificacion(
            titulo = "Prueba de Sonido",
            mensaje = "Si escuchas esto, las notificaciones de Hamparo funcionan correctamente."
        )
    }
}