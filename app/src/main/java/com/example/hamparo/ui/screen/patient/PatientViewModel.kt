package com.example.hamparo.ui.screen.patient

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hamparo.data.local.entities.MedicamentoEntity
import com.example.hamparo.data.local.entities.MedicionEntity
import com.example.hamparo.data.local.entities.MedicionType
import com.example.hamparo.data.local.entities.UsuarioEntity
import com.example.hamparo.data.local.entities.UserRole
import com.example.hamparo.data.repository.HamparoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PatientViewModel @Inject constructor(
    private val repository: HamparoRepository
) : ViewModel() {

    // Lista de medicinas
    val medicinas = repository.obtenerInventario(1)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Restar stock (Botón verde)
    fun tomarMedicina(medicina: MedicamentoEntity) {
        viewModelScope.launch {
            repository.restarStock(medicina.id)
        }
    }

    // 🛡️ FUNCIÓN BLINDADA CONTRA CRASHEOS
    fun registrarSaludPorVoz(tipo: MedicionType, valor: Float) {
        viewModelScope.launch {
            try {
                // 1. Intentamos guardar la medición directamente
                guardarEnBaseDeDatos(tipo, valor)

            } catch (e: Exception) {
                // 🚨 ¡ERROR! Seguramente el Usuario 1 no existe
                Log.e("PatientVM", "Error al guardar: ${e.message}")

                // 2. SOLUCIÓN AUTOMÁTICA: Creamos al usuario "fantasma"
                try {
                    val abuelo = UsuarioEntity(
                        id = 1,
                        nombre = "Abuelo",
                        edad = 80, // <--- ✅ AÑADIDO: Tu base de datos obliga a poner edad
                        // password = "123", <--- 🗑️ BORRADO: Tu base de datos NO tiene password
                        rol = UserRole.PACIENTE
                    )
                    repository.crearUsuario(abuelo)

                    // 3. Reintentamos guardar ahora que el usuario existe
                    guardarEnBaseDeDatos(tipo, valor)

                } catch (e2: Exception) {
                    Log.e("PatientVM", "Imposible recuperar: ${e2.message}")
                }
            }
        }
    }

    // Función auxiliar privada
    private suspend fun guardarEnBaseDeDatos(tipo: MedicionType, valor: Float) {
        val nuevaMedicion = MedicionEntity(
            usuarioId = 1,
            timestamp = System.currentTimeMillis(),
            tipo = tipo,
            valor1 = valor,
            valor2 = 0f
        )
        repository.guardarMedicion(nuevaMedicion)
    }
}