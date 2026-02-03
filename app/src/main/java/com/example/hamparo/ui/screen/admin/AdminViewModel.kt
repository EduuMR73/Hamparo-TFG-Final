package com.example.hamparo.ui.screen.admin

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hamparo.data.local.InfoMedicamento
import com.example.hamparo.data.model.Medicamento
import com.example.hamparo.data.model.Paciente
import com.example.hamparo.data.model.PautaMedica
import com.example.hamparo.data.model.TipoFrecuencia
import com.example.hamparo.data.model.Medicion
import com.example.hamparo.data.network.MedicamentoResumen
import com.example.hamparo.data.repository.AuthRepository
import com.example.hamparo.data.repository.FirestoreRepository
import com.example.hamparo.data.repository.ScannerRepository
import com.example.hamparo.ui.utils.TTSManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val firestoreRepository: FirestoreRepository,
    private val authRepository: AuthRepository,
    private val scannerRepository: ScannerRepository,
    private val ttsManager: TTSManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // --- ESTADOS UI ---
    private val _necesitaSetup = MutableStateFlow(true)
    val necesitaSetup: StateFlow<Boolean> = _necesitaSetup

    private val _listaPacientes = MutableStateFlow<List<Paciente>>(emptyList())
    val listaPacientes: StateFlow<List<Paciente>> = _listaPacientes

    private val _pacienteActivo = MutableStateFlow<Paciente?>(null)
    val pacienteActivo: StateFlow<Paciente?> = _pacienteActivo

    // Compatibilidad
    private val _medicamentos = MutableStateFlow<List<Medicamento>>(emptyList())
    val medicamentos: StateFlow<List<Medicamento>> = _medicamentos

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje: StateFlow<String?> = _mensaje

    private val _nombreGrupo = MutableStateFlow<String?>(null)
    val nombreGrupo: StateFlow<String?> = _nombreGrupo

    private val _codigoAcceso = MutableStateFlow<String?>(null)
    val codigoAcceso: StateFlow<String?> = _codigoAcceso

    // VARIABLES DEL JEFE (NOMBRE Y EMAIL)
    private val _emailAdminGrupo = MutableStateFlow<String?>(null)
    val emailAdminGrupo: StateFlow<String?> = _emailAdminGrupo

    private val _nombreAdminGrupo = MutableStateFlow<String?>(null)
    val nombreAdminGrupo: StateFlow<String?> = _nombreAdminGrupo

    private val _esModoFamiliar = MutableStateFlow(false)
    val esModoFamiliar: StateFlow<Boolean> = _esModoFamiliar

    // --- ESTADOS PARA EL ESCÁNER ---
    private val _medicamentoEscaneado = MutableStateFlow<InfoMedicamento?>(null)
    val medicamentoEscaneado: StateFlow<InfoMedicamento?> = _medicamentoEscaneado

    private val _showScanDialog = MutableStateFlow(false)
    val showScanDialog: StateFlow<Boolean> = _showScanDialog

    // LISTA DE CANDIDATOS PARA EL BUSCADOR MANUAL ASISTIDO
    private val _candidatosManuales = MutableStateFlow<List<MedicamentoResumen>>(emptyList())
    val candidatosManuales: StateFlow<List<MedicamentoResumen>> = _candidatosManuales

    // Variables internas
    private var grupoIdActual: String = ""
    private var esFamiliar: Boolean = false

    // Variable para recordar el código "roto" y poder vincularlo después
    private var _ultimoCodigoEscaneado: String? = null

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    val miEmail: String get() = auth.currentUser?.email ?: "Acceso Familiar"

    init {
        // Carga inicial de datos de usuario
        val user = auth.currentUser
        if (user != null) {
            _emailAdminGrupo.value = user.email
            _nombreAdminGrupo.value = user.displayName
        }
        verificarSesion()
    }

    // CEREBRO DEL INICIO DE SESIÓN

    private fun verificarSesion() {
        viewModelScope.launch {
            _isLoading.value = true

            val prefs = context.getSharedPreferences("HamparoPrefs", Context.MODE_PRIVATE)
            val rol = prefs.getString("ROL", "PACIENTE")

            // 1. CASO FAMILIAR
            if (rol == "FAMILIAR") {
                esFamiliar = true
                _esModoFamiliar.value = true
                val pacienteId = prefs.getString("PACIENTE_ID", null)

                if (pacienteId != null) {
                    _nombreGrupo.value = "Cuidando a mi familiar"
                    grupoIdActual = pacienteId
                    _necesitaSetup.value = false
                    cargarDatosDashboard()
                } else {
                    _mensaje.value = "Error: No hay paciente vinculado"
                    _necesitaSetup.value = true
                }
                _isLoading.value = false
                return@launch
            }

            // 2. CASO CUIDADOR / PROFESIONAL (ADMIN)
            esFamiliar = false
            _esModoFamiliar.value = false

            val user = auth.currentUser
            _emailAdminGrupo.value = user?.email
            _nombreAdminGrupo.value = user?.displayName

            val userId = user?.uid
            val idGuardadoLocal = prefs.getString("GRUPO_ID", "") ?: ""
            var grupoDoc: DocumentSnapshot? = null

            try {
                // ESTRATEGIA A: Si tenemos ID local, intentamos buscar ese grupo directamente
                if (idGuardadoLocal.isNotEmpty()) {
                    val doc = db.collection("grupos").document(idGuardadoLocal).get().await()
                    if (doc.exists()) {
                        grupoDoc = doc
                    }
                }

                // ESTRATEGIA B: Si no había local o falló, buscamos por el dueño (UserID)
                if (grupoDoc == null && userId != null) {
                    val query = db.collection("grupos").whereEqualTo("adminId", userId).get().await()
                    if (!query.isEmpty) {
                        grupoDoc = query.documents[0]
                    }
                }

                // 3. PROCESAMOS EL RESULTADO FINAL
                if (grupoDoc != null) {
                    grupoIdActual = grupoDoc.id
                    _nombreGrupo.value = grupoDoc.getString("nombre")
                    _codigoAcceso.value = grupoDoc.getString("codigoAcceso")

                    val emailBD = grupoDoc.getString("adminEmail")
                    val nombreBD = grupoDoc.getString("adminName")
                    if (!emailBD.isNullOrBlank()) _emailAdminGrupo.value = emailBD
                    if (!nombreBD.isNullOrBlank()) _nombreAdminGrupo.value = nombreBD

                    prefs.edit().putString("GRUPO_ID", grupoIdActual).apply()
                    _necesitaSetup.value = false
                    cargarDatosDashboard()
                } else {
                    // Si no encontramos nada de nada, hay que crear grupo
                    _necesitaSetup.value = true
                }

            } catch (e: Exception) {
                _mensaje.value = "Error conectando: ${e.message}"
                _necesitaSetup.value = true
            }

            _isLoading.value = false
        }
    }

    // 🔥 LÓGICA DEL ESCÁNER INTELIGENTE (Corregida) 🔥
    fun procesarCodigoEscaneado(codigoBruto: String) {
        viewModelScope.launch {
            _isLoading.value = true
            // Limpiamos primero para evitar "ecos"
            _medicamentoEscaneado.value = null
            _ultimoCodigoEscaneado = codigoBruto

            // 1. REPOSITORIO (CIMA / Local / Memoria de Grupo)
            val resultado = scannerRepository.buscarMedicamento(codigoBruto, grupoIdActual)

            if (resultado != null) {
                mostrarResultado(resultado, "Detectado")
            } else {
                // 2. FALLO -> MODO MANUAL ASISTIDO (Abrir diálogo con buscador)
                abrirDialogoManual()
            }
            _isLoading.value = false
        }
    }

    // FUNCIÓN: ESCANER SILENCIOSO (SOLO PARA RELLENAR FORMULARIOS)

    fun escanearParaFormulario(codigoBruto: String) {
        viewModelScope.launch {
            _isLoading.value = true

            // LIMPIEZA PREVIA IMPORTANTE PARA EVITAR EL BUG DEL "ECHO"
            _medicamentoEscaneado.value = null

            // 1. Buscamos el medicamento en el repositorio
            val resultado = scannerRepository.buscarMedicamento(codigoBruto, grupoIdActual)

            if (resultado != null) {
                // 2. ¡IMPORTANTE! Actualizamos el estado del medicamento...
                _medicamentoEscaneado.value = resultado

                // 3. ...PERO FORZAMOS QUE NO SALGA EL DIÁLOGO DE CONFIRMACIÓN
                _showScanDialog.value = false

                ttsManager.hablar("Medicamento detectado: ${resultado.nombre}")
            } else {
                ttsManager.hablar("Código no encontrado")
                _mensaje.value = "Medicamento no encontrado en la base de datos"
                // Guardamos el código crudo por si luego lo rellenan a mano
                _ultimoCodigoEscaneado = codigoBruto
            }
            _isLoading.value = false
        }
    }

    // FUNCIÓN ESCOBA: Resetea el escáner para evitar datos zombis
    fun limpiarMedicamentoEscaneado() {
        _medicamentoEscaneado.value = null
        _ultimoCodigoEscaneado = null
    }

    // Búsqueda manual en tiempo real
    fun buscarFarmacoManual(nombre: String) {
        if (nombre.length < 3) return
        viewModelScope.launch {
            val lista = scannerRepository.buscarCandidatosPorNombre(nombre)
            _candidatosManuales.value = lista
        }
    }

    // Al seleccionar uno de la lista, traemos los datos OFICIALES
    fun seleccionarFarmacoManual(item: MedicamentoResumen) {
        viewModelScope.launch {
            _isLoading.value = true
            val oficial = scannerRepository.obtenerDetalleOficial(item.nregistro)

            if (oficial != null) {
                // Rellenamos el diálogo con los datos oficiales (PDF incluido)
                _medicamentoEscaneado.value = oficial.copy(
                    tipo = "Vinculado", // Marca interna para saber que viene del buscador
                    uso = oficial.uso,
                    advertencia = oficial.advertencia
                )
                _candidatosManuales.value = emptyList() // Ocultamos la lista
            }
            _isLoading.value = false
        }
    }

    fun limpiarBusquedaManual() {
        _candidatosManuales.value = emptyList()
    }

    private fun abrirDialogoManual() {
        _mensaje.value = "Código no reconocido. Busca por nombre."
        ttsManager.hablar("Código no reconocido. Puedes buscarlo por nombre.")

        // Creamos un objeto vacío marcado como "Nuevo" para activar el modo buscador en la UI
        _medicamentoEscaneado.value = InfoMedicamento(
            nombre = "",
            dosis = "",
            tipo = "Nuevo",
            uso = "",
            advertencia = ""
        )
        _showScanDialog.value = true
    }

    private fun mostrarResultado(info: InfoMedicamento, msg: String) {
        _medicamentoEscaneado.value = info
        _showScanDialog.value = true
        ttsManager.hablar("$msg: ${info.nombre}. ${info.uso}")
    }

    fun cerrarDialogoScanner() {
        _showScanDialog.value = false
        _medicamentoEscaneado.value = null
        limpiarBusquedaManual()
        ttsManager.stop()
    }

    // GESTIÓN Y GUARDADO
    fun agregarPautaMedica(
        nombre: String,
        dosis: String,
        tipoFrecuencia: TipoFrecuencia,
        indicacion: String,
        intervalo: Int = 0,
        horaInicio: String = "08:00",
        tomas: List<String> = emptyList(),
        stockInicial: Int = 0,
        // PARÁMETROS NUEVOS PARA LA API
        descripcionApi: String = "",
        advertencias: String = "",
        prospectoUrl: String = "",
        codigoNacional: String = ""
    ) {
        val pacienteActual = _pacienteActivo.value ?: return

        // 1. Guardar en paciente con la nueva estructura
        val nuevaPauta = PautaMedica(
            id = UUID.randomUUID().toString(),
            nombreMedicamento = nombre,
            dosis = dosis,
            tipoFrecuencia = tipoFrecuencia,
            tomas = tomas,
            cadaCuantasHoras = intervalo,
            horaInicio = horaInicio,
            indicacion = indicacion,
            stock = stockInicial,

            // ✅ GUARDAMOS LA INFO RICA
            descripcionApi = descripcionApi,
            advertencias = advertencias,
            prospectoUrl = prospectoUrl,
            codigoNacional = codigoNacional
        )

        val pActualizado = pacienteActual.copy(medicacionActual = pacienteActual.medicacionActual + nuevaPauta)
        actualizarEstadoPaciente(pActualizado)

        // 2. APRENDIZAJE: Si venimos de un escaneo fallido, guardamos la vinculación en el grupo
        if (_ultimoCodigoEscaneado != null && grupoIdActual.isNotEmpty()) {
            viewModelScope.launch {
                scannerRepository.aprenderCodigo(grupoIdActual, _ultimoCodigoEscaneado!!, nombre, dosis)
                _ultimoCodigoEscaneado = null // Olvidamos el código tras aprenderlo
            }
        }

        // Limpiamos el escáner para evitar que se repita en el futuro
        limpiarMedicamentoEscaneado()
        _mensaje.value = "Pauta añadida correctamente."
    }

    private fun cargarDatosDashboard() {
        if (grupoIdActual.isEmpty()) return
        if (esFamiliar) {
            viewModelScope.launch {
                try {
                    val doc = db.collection("pacientes").document(grupoIdActual).get().await()
                    val paciente = doc.toObject(Paciente::class.java)
                    if (paciente != null) {
                        _listaPacientes.value = listOf(paciente)
                        _pacienteActivo.value = paciente
                        if (paciente.adminId.isNotEmpty()) {
                            val docGrupo = db.collection("grupos").document(paciente.adminId).get().await()
                            if (docGrupo.exists()) {
                                val cod = docGrupo.getString("codigoAcceso")
                                if (!cod.isNullOrBlank()) _codigoAcceso.value = cod
                                val emailFound = docGrupo.getString("adminEmail")
                                val nameFound = docGrupo.getString("adminName")
                                if (!emailFound.isNullOrBlank()) _emailAdminGrupo.value = emailFound
                                _nombreAdminGrupo.value = if (!nameFound.isNullOrBlank()) nameFound else "Cuidador"
                            }
                        }
                    }
                } catch (e: Exception) { }
            }
            return
        }
        viewModelScope.launch {
            firestoreRepository.obtenerPacientes(grupoIdActual).collect { lista ->
                _listaPacientes.value = lista
                if (_pacienteActivo.value != null) {
                    val actualizado = lista.find { it.id == _pacienteActivo.value!!.id }
                    if (actualizado != null) _pacienteActivo.value = actualizado
                }
            }
        }
    }

    fun seleccionarPaciente(paciente: Paciente) {
        _pacienteActivo.value = paciente
        if (!esFamiliar) {
            val user = auth.currentUser
            if (_emailAdminGrupo.value == null) _emailAdminGrupo.value = user?.email
            if (_nombreAdminGrupo.value == null) _nombreAdminGrupo.value = user?.displayName
        }
    }

    fun deseleccionarPaciente() {
        _pacienteActivo.value = null
    }

    fun guardarCambiosFicha() {
        val paciente = _pacienteActivo.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val resultado = firestoreRepository.guardarPaciente(paciente)
            if (resultado.isSuccess) _mensaje.value = "Cambios guardados"
            else _mensaje.value = "Error al guardar"
            _isLoading.value = false
        }
    }

    fun actualizarEstadoPaciente(pacienteModificado: Paciente) {
        _pacienteActivo.value = pacienteModificado
        viewModelScope.launch {
            firestoreRepository.guardarPaciente(pacienteModificado)
        }
    }

    fun borrarPautaMedica(pautaId: String) {
        val pacienteActual = _pacienteActivo.value ?: return
        val pacienteActualizado = pacienteActual.copy(medicacionActual = pacienteActual.medicacionActual.filter { it.id != pautaId })
        actualizarEstadoPaciente(pacienteActualizado)
        _mensaje.value = "Pauta eliminada"
    }

    fun añadirPacienteProfesional(nombreNuevo: String, habitacionOpcional: String) {
        if (esFamiliar) return
        if (grupoIdActual.isEmpty() || nombreNuevo.isBlank()) return

        viewModelScope.launch {
            _isLoading.value = true
            val nuevoId = UUID.randomUUID().toString()
            val tokenSeguridad = Random.nextInt(1000, 9999).toString()
            val nuevaFicha = Paciente(
                id = nuevoId,
                adminId = grupoIdActual,
                nombre = nombreNuevo,
                nhc = String.format("%04d", Random.nextInt(1, 9999)),
                habitacion = habitacionOpcional,
                codigoVinculacion = tokenSeguridad,
                fechaIngreso = System.currentTimeMillis()
            )
            firestoreRepository.guardarPaciente(nuevaFicha)
            _mensaje.value = "Paciente creado. Cód: $tokenSeguridad"
            _isLoading.value = false
        }
    }

    fun crearGrupoFamiliar(nombre: String) {
        val user = auth.currentUser ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val codigo = Random.nextInt(100000, 999999).toString()
            val ref = db.collection("grupos").document()
            val datos = hashMapOf(
                "id" to ref.id, "grupoId" to ref.id, "adminId" to user.uid,
                "adminEmail" to (user.email ?: ""), "adminName" to (user.displayName ?: "Cuidador"),
                "nombre" to nombre, "codigoAcceso" to codigo, "fechaCreacion" to System.currentTimeMillis()
            )
            try {
                ref.set(datos).await()
                grupoIdActual = ref.id
                _nombreGrupo.value = nombre
                _codigoAcceso.value = codigo
                _emailAdminGrupo.value = user.email
                _nombreAdminGrupo.value = user.displayName
                context.getSharedPreferences("HamparoPrefs", Context.MODE_PRIVATE)
                    .edit().putString("GRUPO_ID", grupoIdActual).apply()
                _necesitaSetup.value = false
                _mensaje.value = "Grupo creado. Código: $codigo"
                cargarDatosDashboard()
            } catch (e: Exception) {
                _mensaje.value = "Error: ${e.message}"
            }
            _isLoading.value = false
        }
    }

    fun cerrarSesion() {
        authRepository.cerrarSesion()
        context.getSharedPreferences("HamparoPrefs", Context.MODE_PRIVATE).edit().clear().apply()
        _pacienteActivo.value = null
        _listaPacientes.value = emptyList()
        _emailAdminGrupo.value = null
        _nombreAdminGrupo.value = null
        grupoIdActual = ""
        esFamiliar = false
        _necesitaSetup.value = true
        ttsManager.stop()
    }

    fun limpiarMensaje() { _mensaje.value = null }
    override fun onCleared() { super.onCleared(); ttsManager.stop() }

    // Dummies
    fun actualizarNombreGrupo(n: String) {}
    fun onNombreChange(text: String) {}
    fun onDosisChange(text: String) {}
    fun onFrecuenciaChange(text: String) {}
    fun onStockChange(text: String) {}
    fun guardarMedicamento() {}
    fun borrarMedicamento(m: Medicamento) {}
    fun borrarAviso(m: Medicion) {}
    fun archivarAviso(m: Medicion) {}
    fun cambiarTipoGrafico(tipo: String) {}
}