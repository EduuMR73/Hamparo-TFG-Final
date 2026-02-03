package com.example.hamparo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hamparo.data.model.Paciente
import com.example.hamparo.data.repository.AuthRepository
import com.example.hamparo.data.repository.FirestoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// 1. Estado de la UI
data class VincularUiState(
    val codigoInput: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val pacienteEncontrado: Paciente? = null,
    val busquedaTerminada: Boolean = false,
    val vinculacionExitosa: Boolean = false // Flag para navegar al Home tras vincular
)

@HiltViewModel
class VincularViewModel @Inject constructor(
    private val repository: FirestoreRepository,
    private val authRepository: AuthRepository // Saber quién es el usuario actual
) : ViewModel() {

    // Estado observable
    private val _uiState = MutableStateFlow(VincularUiState())
    val uiState: StateFlow<VincularUiState> = _uiState.asStateFlow()

    // Input de texto
    fun onCodigoChanged(nuevoCodigo: String) {
        _uiState.update { it.copy(codigoInput = nuevoCodigo, error = null) }
    }

    // PASO 1: Buscar al paciente por código
    fun buscarPaciente() {
        val codigo = _uiState.value.codigoInput.trim()

        if (codigo.isEmpty()) {
            _uiState.update { it.copy(error = "Por favor, escribe un código.") }
            return
        }

        viewModelScope.launch {
            // Reseteamos estado y mostramos carga
            _uiState.update { it.copy(isLoading = true, error = null, pacienteEncontrado = null) }

            try {
                val paciente = repository.buscarPacientePorCodigo(codigo)

                if (paciente != null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            pacienteEncontrado = paciente,
                            busquedaTerminada = true
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "No se encontró ningún paciente con el código $codigo.",
                            busquedaTerminada = true
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = "Error de conexión: ${e.message}")
                }
            }
        }
    }

    // PASO 2: Confirmar y Guardar el Vínculo en Firebase
    // Esta función se llama cuando el usuario pulsa "Sí, es mi familiar"
    fun confirmarVinculacion(onSuccess: () -> Unit) {
        val paciente = _uiState.value.pacienteEncontrado
        val usuarioActual = authRepository.getUsuarioActual() // Obtenemos a Fany desde Auth

        if (paciente == null || usuarioActual == null) {
            _uiState.update { it.copy(error = "Error: Datos de sesión o paciente no válidos.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // 1. Guardamos el ID del paciente en el documento del usuario
            val resultado = repository.vincularPacienteAUsuario(usuarioActual.uid, paciente.id)

            if (resultado) {
                // 2. Añadimos a Fany a la lista de familiares del Paciente (para que el Admin lo sepa)
                repository.agregarFamiliarAPaciente(
                    pacienteId = paciente.id,
                    email = usuarioActual.email ?: "",
                    nombre = usuarioActual.displayName ?: "Familiar"
                )

                _uiState.update { it.copy(isLoading = false, vinculacionExitosa = true) }
                onSuccess() // Ejecutamos la navegación
            } else {
                _uiState.update {
                    it.copy(isLoading = false, error = "No se pudo guardar la vinculación. Intenta de nuevo.")
                }
            }
        }
    }

    fun limpiarEstado() {
        _uiState.value = VincularUiState()
    }
}