package com.example.hamparo.ui.screen.admin

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.hamparo.R // ⚠️ Asegúrate de que este import coincida con tu paquete
import com.example.hamparo.data.local.entities.MedicamentoEntity
import com.example.hamparo.data.local.entities.MedicionType
import com.example.hamparo.data.repository.HamparoRepository
import com.example.hamparo.utils.programarAlarma
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminViewModel @Inject constructor(
    application: Application,
    private val repository: HamparoRepository
) : AndroidViewModel(application) {

    // --- 1. BLOQUE VIGILANTE DE EMERGENCIAS (NUEVO) ---
    // Este bloque se ejecuta nada más abrir la pantalla del Admin.
    // Observa silenciosamente si entra una ALERTA nueva en la base de datos.
    init {
        viewModelScope.launch {
            // Observamos el historial en tiempo real
            repository.obtenerHistorial(1).collect { lista ->
                if (lista.isNotEmpty()) {
                    // Como la consulta ordena por fecha DESC, el primero es el más nuevo
                    val ultimaMedicion = lista.first()

                    // 1. ¿Es una alerta de socorro?
                    val esAlerta = ultimaMedicion.tipo == MedicionType.ALERTA

                    // 2. ¿Es reciente? (Menos de 1 minuto)
                    // Esto evita que suene una alerta vieja al abrir la app mañana.
                    val esReciente = (System.currentTimeMillis() - ultimaMedicion.timestamp) < 60000

                    if (esAlerta && esReciente) {
                        lanzarNotificacionEmergencia()
                    }
                }
            }
        }
    }

    // --- 2. DATOS DE LA UI ---

    // Historial (Gráficos y Alertas SOS visuales)
    val historial = repository.obtenerHistorial(1)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Inventario de medicinas
    val inventario = repository.obtenerInventario(1)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // --- 3. FUNCIONES DE MEDICAMENTOS ---

    fun guardarNuevaMedicina(nombre: String, dosis: String, frecuencia: String, stock: String, idOriginal: Int = 0) {
        viewModelScope.launch {
            val freqInt = frecuencia.toIntOrNull() ?: 8
            val stockInt = stock.toIntOrNull() ?: 1

            repository.guardarMedicamento(nombre, dosis, freqInt, stockInt, 1, idOriginal)

            programarAlarma(
                context = getApplication(),
                nombreMedicina = nombre,
                horasFrecuencia = freqInt
            )

            println("✅ GUARDADO Y ALARMA PROGRAMADA: $nombre cada $freqInt horas")
        }
    }

    fun borrarMedicina(medicina: MedicamentoEntity) {
        viewModelScope.launch {
            repository.borrarMedicamento(medicina)
            println("🗑️ BORRADO: ${medicina.nombre}")
        }
    }

    // --- 4. FUNCIÓN PRIVADA PARA LANZAR NOTIFICACIÓN (NUEVO) ---
    private fun lanzarNotificacionEmergencia() {
        val context = getApplication<Application>().applicationContext
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Construimos la notificación de ALTA PRIORIDAD
        val notificacion = NotificationCompat.Builder(context, "canal_medicinas") // Usamos el mismo canal que creaste en MainActivity
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Icono de la app
            .setContentTitle("🚨 ¡EMERGENCIA DETECTADA!")
            .setContentText("El paciente ha pedido SOCORRO por voz.")
            .setPriority(NotificationCompat.PRIORITY_MAX) // Prioridad Máxima (Sale por encima de todo)
            .setDefaults(NotificationCompat.DEFAULT_ALL) // Sonido, Vibración y Luces
            .setAutoCancel(true)
            .build()

        // Lanzamos la notificación con ID único (usamos el tiempo actual para que no se pisen)
        notificationManager.notify(System.currentTimeMillis().toInt(), notificacion)
    }
}