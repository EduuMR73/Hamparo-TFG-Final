package com.example.hamparo.ui.screen.drugs

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hamparo.data.local.InfoMedicamento
import com.example.hamparo.data.model.Medicamento
import com.example.hamparo.data.network.MedicamentoResumen
import com.example.hamparo.data.repository.DrugsRepository
import com.example.hamparo.data.repository.FirestoreRepository
import com.example.hamparo.ui.utils.NotificationScheduler
import com.example.hamparo.ui.utils.TTSManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DrugsViewModel @Inject constructor(
    private val drugsRepository: DrugsRepository,       // Repositorio de Búsqueda (CIMA)
    private val firestoreRepository: FirestoreRepository, // Repositorio de Tus Datos (Firebase)
    private val scheduler: NotificationScheduler,       // El que controla las alarmas
    private val ttsManager: TTSManager
) : ViewModel() {

    // --- ESTADOS DE BÚSQUEDA (CIMA) ---
    private val _medicamentoInfo = MutableStateFlow<InfoMedicamento?>(null)
    val medicamentoInfo: StateFlow<InfoMedicamento?> = _medicamentoInfo

    private val _listaResultados = MutableStateFlow<List<MedicamentoResumen>>(emptyList())
    val listaResultados: StateFlow<List<MedicamentoResumen>> = _listaResultados

    // --- ESTADOS DE GESTIÓN (TUS PASTILLAS) ---
    private val _misMedicamentos = MutableStateFlow<List<Medicamento>>(emptyList())
    val misMedicamentos: StateFlow<List<Medicamento>> = _misMedicamentos

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _urlProspecto = MutableStateFlow<String?>(null)
    val urlProspecto: StateFlow<String?> = _urlProspecto

    //  LÓGICA DE ALARMAS Y ARCHIVADO


    /**
     * Función para archivar un medicamento.
     */
    fun archivarMedicamento(medicamento: Medicamento) {
        viewModelScope.launch {
            try {
                // 1. Marcar como archivada en BD
                val medicamentoArchivado = medicamento.copy(archivada = true)
                firestoreRepository.guardarMedicamento(medicamentoArchivado)

                // 2. 🛑 CANCELAR LA ALARMA 🛑
                scheduler.cancelarAlarma(medicamento.nombre)

                Log.d("Hamparo", "Medicamento archivado: ${medicamento.nombre}")

                // Recargar lista para que desaparezca de la vista
                cargarMisMedicamentos(medicamento.grupoId)
            } catch (e: Exception) {
                _error.value = "Error al archivar: ${e.message}"
            }
        }
    }

    fun registrarToma(medicamento: Medicamento) {
        viewModelScope.launch {
            try {
                // 1. Usamos la función segura del modelo nuevo
                val stockActual = medicamento.stockActual()

                if (stockActual > 0) {
                    val nuevoStock = (stockActual - 1).toString()
                    val ahora = System.currentTimeMillis() // 🕒 TIMESTAMP CRÍTICO

                    // 2. Guardar en Firebase (Stock nuevo Y fecha actual)
                    // NOTA: Asegúrate de que 'registrarToma' en tu Repo acepte el 3er parámetro (fecha)
                    // Si no, usa 'guardarMedicamento' con el objeto modificado.
                    firestoreRepository.registrarToma(medicamento.id, nuevoStock, ahora)

                    Log.d("Hamparo", "Toma registrada: ${medicamento.nombre} - Quedan: $nuevoStock")

                    // 3. GESTIÓN DE ALARMAS INTELIGENTE
                    if (nuevoStock.toInt() == 0) {
                        // 🛑 SI SE ACABÓ: CANCELAMOS ALARMA Y AVISAMOS
                        scheduler.cancelarAlarma(medicamento.nombre)
                        ttsManager.hablar("Atención, se ha terminado ${medicamento.obtenerNombreVisual()}")
                    } else {
                        // ⏰ SI QUEDAN: REPROGRAMAMOS PARA LA PRÓXIMA TOMA
                        val horasFrecuencia = medicamento.frecuenciaHoras
                        if (horasFrecuencia > 0) {
                            val proximaToma = ahora + (horasFrecuencia * 60 * 60 * 1000)
                            scheduler.programarAlarmaMedicina(medicamento.nombre, proximaToma)
                        }
                    }

                    // 4. Recargamos la lista para actualizar la UI (Botón verde)
                    // Pequeño delay para dar tiempo a Firebase a procesar
                    delay(100)
                    cargarMisMedicamentos(medicamento.grupoId)
                }
            } catch (e: Exception) {
                _error.value = "Error al registrar la toma"
                Log.e("Hamparo", "Error toma: ", e)
            }
        }
    }

    /**
     * Carga las pastillas del paciente.
     * Filtramos localmente por si acaso Firebase devuelve archivadas.
     */
    fun cargarMisMedicamentos(grupoId: String) {
        viewModelScope.launch {
            firestoreRepository.obtenerMedicamentos(grupoId).collect { lista ->
                // Filtramos para asegurar que no se muestren archivadas en la lista principal
                val listaFiltrada = lista.filter { !it.archivada }
                _misMedicamentos.value = listaFiltrada
            }
        }
    }

    // LÓGICA DE BÚSQUEDA CIMA (EXISTENTE)


    fun realizarBusqueda(query: String) {
        if (query.isBlank()) return
        resetStates()
        if (query.all { it.isDigit() }) {
            buscarPorCodigo(query)
        } else {
            buscarListadoPorNombre(query)
        }
    }

    fun procesarEscaneo(codigoBruto: String) {
        resetStates()
        buscarPorCodigo(codigoBruto)
    }

    private fun buscarPorCodigo(codigoBruto: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val resultado = drugsRepository.obtenerDetalleMedicamento(codigoBruto, esNRegistro = false)
                if (resultado != null) {
                    mostrarResultado(resultado)
                } else {
                    _error.value = "Medicamento no identificado."
                    ttsManager.hablar("Código no reconocido.")
                }
            } catch (e: Exception) {
                _error.value = "Error de conexión."
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun buscarListadoPorNombre(nombre: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val resultados = drugsRepository.buscarPorNombre(nombre)
                if (resultados.isNotEmpty()) {
                    _listaResultados.value = resultados
                } else {
                    _error.value = "Sin resultados."
                }
            } catch (e: Exception) {
                _error.value = "Error al buscar."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun seleccionarDeLista(medicamento: MedicamentoResumen) {
        viewModelScope.launch {
            _isLoading.value = true
            _medicamentoInfo.value = null
            _urlProspecto.value = null
            try {
                val resultado = drugsRepository.obtenerDetalleMedicamento(medicamento.nregistro, esNRegistro = true)
                if (resultado != null) {
                    mostrarResultado(resultado)
                } else {
                    _error.value = "Error al cargar detalle."
                }
            } catch (e: Exception) {
                _error.value = "Error de conexión."
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun mostrarResultado(info: InfoMedicamento) {
        if (info.tipo.startsWith("http")) {
            _urlProspecto.value = info.tipo
        }
        _medicamentoInfo.value = info

        var primeraPalabra = info.nombre.split(" ", ",", ".", "-").firstOrNull() ?: "medicamento"
        if (primeraPalabra.any { it.isDigit() } && info.nombre.contains(" ")) {
            val partes = info.nombre.split(" ")
            if (partes.size > 1) primeraPalabra = partes[1]
        }
        val nombreParaLeer = primeraPalabra.lowercase().trim()
        val mensajeVoz = "He detectado $nombreParaLeer. ${info.uso}"
        ttsManager.hablar(mensajeVoz)
    }

    private fun resetStates() {
        _medicamentoInfo.value = null
        _listaResultados.value = emptyList()
        _error.value = null
        _urlProspecto.value = null
    }

    fun limpiar() {
        resetStates()
        ttsManager.stop()
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.stop()
    }
}