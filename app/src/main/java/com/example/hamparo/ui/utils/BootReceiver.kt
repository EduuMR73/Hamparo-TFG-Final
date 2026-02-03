package com.example.hamparo.ui.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.hamparo.data.repository.FirestoreRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var firestoreRepository: FirestoreRepository

    @Inject
    lateinit var notificationScheduler: NotificationScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {

            val pendingResult = goAsync()
            val job = SupervisorJob()
            val scope = CoroutineScope(Dispatchers.IO + job)

            scope.launch {
                try {
                    // 1. Recuperar el GRUPO_ID
                    val prefs = context.getSharedPreferences("HamparoPrefs", Context.MODE_PRIVATE)
                    val grupoId = prefs.getString("GRUPO_ID", null)

                    if (grupoId != null) {
                        // 2. Obtener medicinas actuales de la base de datos
                        val listaMedicamentos = firestoreRepository.obtenerMedicamentos(grupoId).first()

                        // 3. Reprogramar alarmas
                        listaMedicamentos.forEach { medicina ->
                            val horasFrecuencia = medicina.frecuencia.toIntOrNull() ?: 8
                            val ultimaToma = medicina.ultimaToma

                            // --- LÓGICA DE RECUPERACIÓN DE TIEMPO ---
                            if (ultimaToma > 0) {
                                val intervaloMilis = horasFrecuencia * 60 * 60 * 1000L
                                val proximaToma = ultimaToma + intervaloMilis
                                val ahora = System.currentTimeMillis()

                                // Calculamos cuánto falta desde AHORA hasta la próxima toma
                                var tiempoRestante = proximaToma - ahora

                                // Si el tiempo ya pasó mientras el móvil estaba apagado,
                                // ponemos la alarma para dentro de 1 minuto (60000ms) para avisar ya.
                                if (tiempoRestante < 0) {
                                    tiempoRestante = 60000L
                                }

                                // 👇 CORRECCIÓN: Llamamos con el parámetro 'tiempoMilis' calculado
                                notificationScheduler.programarAlarmaMedicina(
                                    nombreMedicina = medicina.nombre,
                                    tiempoMilis = tiempoRestante
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                    job.cancel()
                }
            }
        }
    }
}