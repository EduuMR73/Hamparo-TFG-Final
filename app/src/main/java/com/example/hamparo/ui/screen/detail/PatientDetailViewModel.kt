package com.example.hamparo.ui.screen.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hamparo.data.model.*
import com.example.hamparo.data.repository.FirestoreRepository
import com.example.hamparo.ui.utils.NotificationHelper
import com.example.hamparo.ui.utils.NotificationScheduler
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * ViewModel encargado de la lógica de negocio y gestión de estado para la vista de detalle del paciente.
 *
 * ARQUITECTURA:
 * Implementa el patrón MVVM (Model-View-ViewModel), actuando como intermediario entre la capa de datos (Repository)
 * y la capa de presentación (Compose UI).
 *
 * CARACTERÍSTICAS TÉCNICAS:
 * 1. Inyección de Dependencias (DI): Utiliza Dagger-Hilt (@HiltViewModel) para desacoplar la creación de instancias,
 * facilitando el testing y la modularidad.
 * 2. Gestión de Estado Reactivo: Uso de [StateFlow] para implementar el patrón "Unidirectional Data Flow" (UDF),
 * garantizando que la UI siempre refleje una única fuente de verdad.
 * 3. Concurrencia Estructurada: Uso de [viewModelScope] para gestionar el ciclo de vida de las Coroutines,
 * asegurando la cancelación automática de tareas al destruirse el ViewModel.
 * 4. Patrón Observer: Suscripción en tiempo real a cambios en Firestore para una experiencia de usuario fluida.
 */
@HiltViewModel
class PatientDetailViewModel @Inject constructor(
    private val repository: FirestoreRepository,
    private val auth: FirebaseAuth,
    private val notificationHelper: NotificationHelper,
    private val notificationScheduler: NotificationScheduler
) : ViewModel() {


    /**
     * Flujo que observa el estado de autenticación en tiempo real.
     *
     * DECISIÓN TÉCNICA:
     * Se utiliza [callbackFlow] para adaptar la API basada en callbacks de Firebase Auth a un flujo reactivo (Cold Flow).
     * Posteriormente, se convierte a un [StateFlow] (Hot Flow) mediante [stateIn] para que sobreviva a cambios
     * de configuración y mantenga el último valor en caché.
     *
     * GESTIÓN DE RECURSOS:
     * El bloque [awaitClose] garantiza que el listener se elimine cuando el scope se cancele, previniendo Memory Leaks.
     */
    val usuarioActualEmail: StateFlow<String> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            val email = auth.currentUser?.email ?: ""
            trySend(email)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000), // Timeout para optimizar recursos en rotaciones
        initialValue = auth.currentUser?.email ?: ""
    )

    // --- ESTADOS (Backing Property Pattern) ---
    // Encapsulamiento: _mutable es privado para evitar modificaciones desde la UI.
    // La pública es inmutable (StateFlow).

    private val _paciente = MutableStateFlow<Paciente?>(null)
    val paciente: StateFlow<Paciente?> = _paciente.asStateFlow()

    private val _cuidador = MutableStateFlow<Usuario?>(null)
    val cuidador: StateFlow<Usuario?> = _cuidador.asStateFlow()

    private val _alertas = MutableStateFlow<List<Alerta>>(emptyList())
    val alertas: StateFlow<List<Alerta>> = _alertas.asStateFlow()

    private val _alertasHistorial = MutableStateFlow<List<Alerta>>(emptyList())
    val alertasHistorial: StateFlow<List<Alerta>> = _alertasHistorial.asStateFlow()

    private val _alertasNoLeidas = MutableStateFlow(0)
    val alertasNoLeidas: StateFlow<Int> = _alertasNoLeidas.asStateFlow()

    private val _medicionesNoLeidas = MutableStateFlow(0)
    val medicionesNoLeidas: StateFlow<Int> = _medicionesNoLeidas.asStateFlow()

    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje: StateFlow<String?> = _mensaje.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val db = FirebaseFirestore.getInstance()
    private var esModoDemo = false

    // GESTIÓN DE JOBS (Concurrencia)
    // Almacenamos referencias a los Jobs para poder cancelarlos manualmente si se recarga el paciente,
    // evitando condiciones de carrera (Race Conditions) y múltiples suscripciones activas.
    private var escuchaAlertasJob: Job? = null
    private var escuchaAlertasHistorialJob: Job? = null

    // 🔥🔥 DOBLE ESCUCHA DE MEDICIONES
    private var escuchaMedicionesActivasJob: Job? = null
    private var escuchaMedicionesHistorialJob: Job? = null

    private var pacienteListener: ListenerRegistration? = null

    // Almacenamiento temporal en memoria para realizar operaciones de fusión (merge) y ordenamiento
    // sin necesidad de realizar nuevas peticiones a red. Eficiencia computacional.
    private var cacheMedicionesActivas: List<Medicion> = emptyList()
    private var cacheMedicionesHistorial: List<Medicion> = emptyList()

    init {
        identificarUsuarioReal()
    }

    /**
     * Determina la identidad y rol del usuario actual.
     * Incluye lógica específica para el "Modo Demo" (requisito de negocio) y gestión de fallbacks
     * si el usuario no tiene un perfil completo en base de datos.
     */
    private fun identificarUsuarioReal() {
        val user = auth.currentUser
        if (user == null) return

        val email = user.email ?: ""
        val uid = user.uid

        // Lógica de negocio para cuentas de demostración
        if (email.contains("anita", ignoreCase = true) || email.contains("ana", ignoreCase = true)) {
            esModoDemo = true
            _cuidador.value = Usuario(
                id = uid,
                nombre = "Ana",
                apellidos = "Barrios Busto",
                email = "anita@prueba.es",
                rol = "PROPIETARIO"
            )
            return
        }

        viewModelScope.launch {
            val usuarioEncontrado = repository.getUsuario(uid)
            if (usuarioEncontrado != null) {
                _cuidador.value = usuarioEncontrado
            } else {
                // Fallback: Construcción de usuario temporal si falla la persistencia
                val nombreDisplay = user.displayName?.takeIf { it.isNotEmpty() }
                    ?: email.substringBefore("@").replaceFirstChar { it.uppercase() }
                    ?: "Usuario"

                _cuidador.value = Usuario(
                    id = uid,
                    nombre = nombreDisplay,
                    email = email,
                    rol = "CUIDADOR"
                )
            }
        }
    }

    /**
     * Utilidad para normalizar timestamps.
     * Garantiza la consistencia cronológica incluso si los datos provienen de sistemas legacy
     * con formatos de fecha en String.
     */
    private fun calcularTimestampReal(m: Medicion): Long {
        if (m.timestamp > 0) return m.timestamp
        return try {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val fechaCompletaTexto = "${m.fecha} ${m.hora}"
            val date = sdf.parse(fechaCompletaTexto)
            date?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Punto de entrada para la inicialización de datos del paciente.
     * Orquesta el arranque de múltiples listeners en paralelo (Alertas, Mediciones, Cambios en Paciente).
     */
    fun cargarPaciente(pacienteInicial: Paciente) {
        _paciente.value = pacienteInicial
        iniciarEscuchaDeAlertas(pacienteInicial.id)
        iniciarEscuchaMediciones(pacienteInicial.adminId, pacienteInicial.id)
        suscribirseCambiosPaciente(pacienteInicial.id)
    }

    /**
     * Establece un canal directo con Firestore para recibir actualizaciones del documento del paciente.
     *
     * IMPORTANCIA DE 'snapshotListener':
     * Permite que la UI reaccione instantáneamente a cambios críticos (ej. modificación de medicación por otro cuidador),
     * manteniendo la consistencia de datos en entornos multi-usuario.
     */
    private fun suscribirseCambiosPaciente(pacienteId: String) {
        // Limpieza preventiva del listener anterior
        pacienteListener?.remove()
        pacienteListener = db.collection("pacientes").document(pacienteId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null && snapshot.exists()) {
                    try {
                        val pacienteActualizado = snapshot.toObject(Paciente::class.java)
                        if (pacienteActualizado != null) {

                            // 🔥 CORRECCIÓN: Quitamos el filtro. Pasamos TODO (activas y archivadas).
                            // La pantalla 'MedicacionView' se encargará de mostrar cada una en su pestaña.
                            // Esto delega la responsabilidad de presentación a la UI (Separation of Concerns).

                            val historialCombinado = (cacheMedicionesActivas + cacheMedicionesHistorial)
                                .sortedByDescending { calcularTimestampReal(it) }

                            _paciente.value = pacienteActualizado.copy(
                                medicacionActual = pacienteActualizado.medicacionActual, // ✅ ¡Sin filtrar!
                                historialMedico = historialCombinado
                            )
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }
            }
    }

    /**
     * Inicia la recolección de flujos (Flow Collection) para mediciones activas e históricas.
     *
     * CONCURRENCIA:
     * Se lanzan dos corrutinas independientes en el [viewModelScope] para no bloquear la ejecución.
     * Se utiliza [collectLatest] para procesar solo el último valor emitido, descartando valores intermedios obsoletos
     * si la emisión es muy rápida (Backpressure handling).
     */
    private fun iniciarEscuchaMediciones(grupoId: String, pacienteId: String) {
        escuchaMedicionesActivasJob?.cancel()
        escuchaMedicionesActivasJob = viewModelScope.launch {
            repository.obtenerHistorial(grupoId, esHistorial = false).collectLatest { lista ->
                val misMediciones = lista.filter {
                    it.pacienteId == pacienteId || it.pacienteNombre == _paciente.value?.nombre
                }
                cacheMedicionesActivas = misMediciones
                actualizarListaCompletaMediciones()
            }
        }

        escuchaMedicionesHistorialJob?.cancel()
        escuchaMedicionesHistorialJob = viewModelScope.launch {
            repository.obtenerHistorial(grupoId, esHistorial = true).collectLatest { lista ->
                val misMediciones = lista.filter {
                    it.pacienteId == pacienteId || it.pacienteNombre == _paciente.value?.nombre
                }
                cacheMedicionesHistorial = misMediciones
                actualizarListaCompletaMediciones()
            }
        }
    }

    /**
     * Sincroniza las cachés locales y actualiza el StateFlow principal.
     * Realiza operaciones computacionales (conteo, ordenamiento) en el hilo del ViewModel
     * para liberar al hilo principal (Main Thread) de carga de trabajo.
     */
    private fun actualizarListaCompletaMediciones() {
        val listaTotal = (cacheMedicionesActivas + cacheMedicionesHistorial)
            .sortedByDescending { calcularTimestampReal(it) }

        _medicionesNoLeidas.value = listaTotal.count { !it.leido && !it.archivada }

        val pacienteActual = _paciente.value
        if (pacienteActual != null) {
            _paciente.value = pacienteActual.copy(historialMedico = listaTotal)
        }
    }

    /**
     * Gestiona la escucha de alertas y la lógica de NOTIFICACIONES PUSH locales.
     *
     * LÓGICA DE ALARMA CRÍTICA:
     * Analiza el flujo entrante en busca de alertas urgentes y recientes. Si se cumplen las condiciones,
     * dispara una notificación del sistema mediante [notificationHelper].
     */
    private fun iniciarEscuchaDeAlertas(pacienteId: String) {
        escuchaAlertasJob?.cancel()
        escuchaAlertasJob = viewModelScope.launch {
            repository.obtenerAlertasEnTiempoReal(pacienteId, esHistorial = false).collectLatest { listaAlertas ->
                val alertasOrdenadas = listaAlertas.sortedByDescending { it.timestamp }

                val ultimaAlerta = alertasOrdenadas.firstOrNull()
                if (ultimaAlerta != null && !ultimaAlerta.leido) {
                    val esUrgente = ultimaAlerta.tipo == TipoAlerta.URGENTE || ultimaAlerta.titulo.contains("SOS", true)
                    val esReciente = (System.currentTimeMillis() - ultimaAlerta.timestamp) < 60000

                    if (esUrgente && esReciente) {
                        notificationHelper.mostrarNotificacion(
                            titulo = ultimaAlerta.titulo,
                            mensaje = ultimaAlerta.mensaje
                        )
                    }
                }
                _alertas.value = alertasOrdenadas
                _alertasNoLeidas.value = listaAlertas.count { !it.leido }
            }
        }

        escuchaAlertasHistorialJob?.cancel()
        escuchaAlertasHistorialJob = viewModelScope.launch {
            repository.obtenerAlertasEnTiempoReal(pacienteId, esHistorial = true).collectLatest { listaAlertas ->
                _alertasHistorial.value = listaAlertas.sortedByDescending { it.timestamp }
            }
        }
    }

    fun actualizarDato(nuevoPaciente: Paciente) {
        val historialCombinado = (cacheMedicionesActivas + cacheMedicionesHistorial)
            .sortedByDescending { calcularTimestampReal(it) }
        _paciente.value = nuevoPaciente.copy(historialMedico = historialCombinado)
    }


    /**
     * Crea una nueva pauta médica y programa los recordatorios locales.
     *
     * FLUJO DE TRABAJO:
     * 1. Actualización optimista de la UI (Feedback inmediato).
     * 2. Persistencia asíncrona en Firestore.
     * 3. Programación de `AlarmManager` para notificaciones locales.
     * 4. Gestión de errores y estados de carga (Loading State).
     */
    fun guardarNuevaPautaMedica(
        nombre: String, dosis: String, tipoFrecuencia: TipoFrecuencia,
        indicacion: String, intervalo: Int, horaInicio: String,
        cantidad: Float, stock: Int, descripcion: String,
        advertencias: String, urlPdf: String, codigoNacional: String
    ) {
        val pacienteActual = _paciente.value ?: return

        val nuevaPauta = PautaMedica(
            id = UUID.randomUUID().toString(),
            nombreMedicamento = nombre,
            dosis = dosis,
            stock = stock,
            cantidad = cantidad, // ✅ Aceptamos Float
            tipoFrecuencia = tipoFrecuencia, // ✅ Aceptamos Enum
            tomas = emptyList(),
            cadaCuantasHoras = intervalo,
            horaInicio = horaInicio,
            indicacion = indicacion,
            descripcionApi = descripcion,
            advertencias = advertencias,
            prospectoUrl = urlPdf,
            codigoNacional = codigoNacional
        )

        val nuevaLista = pacienteActual.medicacionActual + nuevaPauta
        val pacienteModificado = pacienteActual.copy(medicacionActual = nuevaLista)
        actualizarDato(pacienteModificado)

        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.actualizarPaciente(pacienteModificado)

                val milisHoras = intervalo * 60 * 60 * 1000L
                val delay = if (intervalo == 0) 10000L else milisHoras
                notificationScheduler.programarAlarmaMedicina(nombre, delay)

                _mensaje.value = "Medicina guardada y alarma activada ⏰"
            } catch (e: Exception) {
                _mensaje.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // --- GESTIÓN DE MEDICIONES ---

    fun guardarMedicionManual(medicion: Medicion) {
        val timestampReal = if (medicion.timestamp == 0L) System.currentTimeMillis() else medicion.timestamp
        val medicionLista = medicion.copy(
            leido = true,
            archivada = false,
            timestamp = timestampReal
        )
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.guardarMedicion(medicionLista)
                _mensaje.value = "Registro guardado correctamente"
            } catch (e: Exception) {
                _mensaje.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Marca una medición como leída actualizando tanto la caché local (para reactividad inmediata)
     * como el repositorio remoto (persistencia).
     */
    fun marcarMedicionComoLeida(medicion: Medicion) {
        if (medicion.leido) return
        if (medicion.archivada) {
            cacheMedicionesHistorial = cacheMedicionesHistorial.map { if (it.id == medicion.id) it.copy(leido = true) else it }
        } else {
            cacheMedicionesActivas = cacheMedicionesActivas.map { if (it.id == medicion.id) it.copy(leido = true) else it }
        }
        actualizarListaCompletaMediciones()

        viewModelScope.launch {
            repository.marcarMedicionComoLeida(medicion.id)
        }
    }

    /**
     * Alterna el estado de archivado de una medición.
     * Mueve el elemento entre las listas de caché (Activas <-> Historial) y sincroniza con backend.
     */
    fun toggleArchivarMedicion(medicion: Medicion) {
        val nuevoEstado = !medicion.archivada
        val medicionModificada = medicion.copy(archivada = nuevoEstado, leido = true)

        if (nuevoEstado) {
            cacheMedicionesActivas = cacheMedicionesActivas.filter { it.id != medicion.id }
            cacheMedicionesHistorial = cacheMedicionesHistorial + medicionModificada
        } else {
            cacheMedicionesHistorial = cacheMedicionesHistorial.filter { it.id != medicion.id }
            cacheMedicionesActivas = cacheMedicionesActivas + medicionModificada
        }
        actualizarListaCompletaMediciones()

        viewModelScope.launch {
            repository.marcarMedicionComoArchivada(medicion.id, nuevoEstado)
            val accion = if (nuevoEstado) "archivada 📂" else "reactivada 📊"
            _mensaje.value = "Medición $accion"
        }
    }

    fun borrarMedicion(medicionId: String) {
        cacheMedicionesActivas = cacheMedicionesActivas.filter { it.id != medicionId }
        cacheMedicionesHistorial = cacheMedicionesHistorial.filter { it.id != medicionId }
        actualizarListaCompletaMediciones()

        viewModelScope.launch {
            try {
                repository.borrarMedicion(medicionId)
                _mensaje.value = "Registro eliminado 🗑️"
            } catch (e: Exception) {
                _mensaje.value = "Error al borrar: ${e.message}"
            }
        }
    }

    // --- ALERTAS (ESTADO: LEÍDO) ---

    fun marcarAlertaComoLeida(alerta: Alerta) {
        if (alerta.leido) return
        val listaActualizada = _alertas.value.map {
            if (it.id == alerta.id) it.copy(leido = true) else it
        }
        _alertas.value = listaActualizada
        _alertasNoLeidas.value = listaActualizada.count { !it.leido }

        viewModelScope.launch {
            try {
                repository.marcarAlertaComoLeida(alerta.pacienteId, alerta.id)
            } catch (e: Exception) {
            }
        }
    }

    /**
     * Genera una Alerta de alta prioridad (SOS).
     * Esta función es crítica para la seguridad del paciente, asegurando que la alerta se registre
     * con un timestamp preciso y tipo URGENTE.
     */
    fun enviarAlertaSOS() {
        val p = _paciente.value ?: return
        viewModelScope.launch {
            val sdfFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val sdfHora = SimpleDateFormat("HH:mm", Locale.getDefault())
            val ahora = Date()
            val alerta = Alerta(
                id = UUID.randomUUID().toString(),
                pacienteId = p.id,
                titulo = "¡SOS! SOLICITUD DE AYUDA",
                mensaje = "El paciente ${p.nombre} ha pulsado el botón de emergencia.",
                fecha = sdfFecha.format(ahora),
                hora = sdfHora.format(ahora),
                timestamp = System.currentTimeMillis(),
                tipo = TipoAlerta.URGENTE,
                leido = false
            )
            repository.enviarAlerta(alerta)
            _mensaje.value = "🚨 Alerta SOS enviada a los cuidadores"
        }
    }

    // --- CURAS ---

    fun guardarCura(
        tipoHerida: String, zona: String, dolor: Int,
        estado: String, materiales: String, fotoUri: String?
    ) {
        val pacienteActual = _paciente.value ?: return
        val user = _cuidador.value
        val usuarioActual = if (user != null) "${user.nombre} ${user.apellidos}".trim() else "Cuidador"
        val nuevaCura = Cura(
            tipoHerida = tipoHerida, zona = zona, ancho = "", largo = "", profundidad = "",
            escalaDolor = dolor, estado = estado, materiales = materiales, imagenUri = fotoUri,
            realizadaPor = usuarioActual, fecha = System.currentTimeMillis(),
            archivada = false
        )
        val listaActualizada = listOf(nuevaCura) + pacienteActual.historialCuras
        val pacienteModificado = pacienteActual.copy(historialCuras = listaActualizada)
        actualizarDato(pacienteModificado)
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.actualizarPaciente(pacienteModificado)
                _mensaje.value = "Evolución registrada correctamente ✨"
            } catch (e: Exception) {
                _mensaje.value = "Error al guardar la cura: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleArchivarCura(cura: Cura) {
        val pacienteActual = _paciente.value ?: return
        val nuevoEstado = !cura.archivada
        val listaActualizada = pacienteActual.historialCuras.map {
            if (it.id == cura.id) it.copy(archivada = nuevoEstado) else it
        }
        val pacienteModificado = pacienteActual.copy(historialCuras = listaActualizada)
        actualizarDato(pacienteModificado)
        viewModelScope.launch {
            repository.actualizarPaciente(pacienteModificado)
            val textoAccion = if (nuevoEstado) "archivada en Historial 📂" else "recuperada a Activas 🩹"
            _mensaje.value = "Cura $textoAccion"
        }
    }

    fun borrarCura(curaId: String) {
        val pacienteActual = _paciente.value ?: return
        val listaActualizada = pacienteActual.historialCuras.filter { it.id != curaId }
        val pacienteModificado = pacienteActual.copy(historialCuras = listaActualizada)
        actualizarDato(pacienteModificado)
        viewModelScope.launch {
            repository.actualizarPaciente(pacienteModificado)
            _mensaje.value = "Registro de cura eliminado 🗑️"
        }
    }

    fun guardarMedicion(tipo: TipoMedicion, valor1: String, valor2: String) {
        val calendario = Calendar.getInstance()
        val fechaHoy = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(calendario.time)
        val horaAhora = SimpleDateFormat("HH:mm", Locale.getDefault()).format(calendario.time)
        val timestampAhora = calendario.timeInMillis

        val pacienteActual = _paciente.value
        val pacienteIdActual = pacienteActual?.id ?: ""
        val pacienteNombreActual = pacienteActual?.nombre ?: ""
        val grupoId = pacienteActual?.adminId ?: ""

        val nuevaMedicion = Medicion(
            id = UUID.randomUUID().toString(),
            fecha = fechaHoy,
            hora = horaAhora,
            timestamp = timestampAhora,
            tipo = tipo,
            valor1 = valor1,
            valor2 = valor2,
            pacienteId = pacienteIdActual,
            pacienteNombre = pacienteNombreActual,
            grupoId = grupoId,
            leido = true,
            archivada = false
        )

        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.guardarMedicion(nuevaMedicion)
                _mensaje.value = "Medición registrada correctamente 📈"
            } catch (e: Exception) {
                _mensaje.value = "Error al guardar: ${e.message}"
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun limpiarMensaje() {
        _mensaje.value = null
    }

    fun guardarFicha() {
        val p = _paciente.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.actualizarPaciente(p)
                _mensaje.value = "Datos guardados correctamente ✅"
            } catch (e: Exception) {
                _mensaje.value = "Error al guardar: ${e.message}"
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun borrarPauta(id: String) {
        val p = _paciente.value ?: return
        val nuevaLista = p.medicacionActual.filter { it.id != id }
        val actualizado = p.copy(medicacionActual = nuevaLista)
        actualizarDato(actualizado)
        viewModelScope.launch {
            repository.actualizarPaciente(actualizado)
        }
    }

    fun borrarMedicamentoDefinitivo(medicamentoId: String) {
        viewModelScope.launch {
            try {
                repository.borrarMedicamento(medicamentoId)
                borrarPauta(medicamentoId)
                _mensaje.value = "Medicamento eliminado definitivamente 🗑️"
            } catch (e: Exception) {
                _mensaje.value = "Error al eliminar: ${e.message}"
            }
        }
    }

    fun toggleArchivarPauta(pauta: PautaMedica) {
        val pacienteActual = _paciente.value ?: return
        val listaActualizada = pacienteActual.medicacionActual.map {
            if (it.id == pauta.id) it.copy(archivada = !it.archivada) else it
        }
        val pacienteModificado = pacienteActual.copy(medicacionActual = listaActualizada)
        actualizarDato(pacienteModificado)
        viewModelScope.launch {
            repository.actualizarPaciente(pacienteModificado)

            val accion = if (!pauta.archivada) "archivado 📂" else "reactivado 💊"
            _mensaje.value = "Medicamento $accion"
        }
    }

    fun toggleArchivarCita(cita: CitaMedica) {
        val pacienteActual = _paciente.value ?: return
        val listaActualizada = pacienteActual.citasMedicas.map {
            if (it.id == cita.id) it.copy(archivada = !it.archivada) else it
        }
        val pacienteModificado = pacienteActual.copy(citasMedicas = listaActualizada)
        actualizarDato(pacienteModificado)
        viewModelScope.launch {
            repository.actualizarPaciente(pacienteModificado)
            val accion = if (!cita.archivada) "archivada 📂" else "reactivada 📅"
            _mensaje.value = "Cita $accion"
        }
    }

    fun borrarCita(id: String) {
        val pacienteActual = _paciente.value ?: return
        val nuevaLista = pacienteActual.citasMedicas.filter { it.id != id }
        val actualizado = pacienteActual.copy(citasMedicas = nuevaLista)
        actualizarDato(actualizado)
        viewModelScope.launch {
            repository.actualizarPaciente(actualizado)
            _mensaje.value = "Cita eliminada 🗑️"
        }
    }

    fun toggleArchivarAlerta(alerta: Alerta) {
        viewModelScope.launch {
            repository.marcarAlertaComoArchivada(alerta.pacienteId, alerta.id, !alerta.archivada)
            val accion = if (!alerta.archivada) "marcada como atendida ✅" else "reactivada ⚠️"
            _mensaje.value = "Alerta $accion"
        }
    }

    fun borrarAlerta(id: String, pacienteId: String) {
        viewModelScope.launch {
            repository.borrarAlerta(pacienteId, id)
            _mensaje.value = "Alerta eliminada del registro 🗑️"
        }
    }

    fun cambiarPinSeguridad(nuevoPin: String) {
        val pacienteActual = _paciente.value ?: return
        if (nuevoPin.length < 4) {
            _mensaje.value = "El PIN debe tener al menos 4 dígitos"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            val exito = repository.actualizarPinPaciente(pacienteActual.id, nuevoPin)
            if (exito) {
                _mensaje.value = "PIN de seguridad actualizado correctamente 🔐"
                _paciente.value = pacienteActual.copy(pinDesbloqueo = nuevoPin)
            } else {
                _mensaje.value = "Error al actualizar el PIN"
            }
            _isLoading.value = false
        }
    }

    /**
     * Callback de limpieza del ViewModel.
     *
     * GESTIÓN DE MEMORIA CRÍTICA:
     * Elimina explícitamente los listeners de Firebase (que no son corrutinas) para evitar fugas de memoria
     * cuando el usuario navega fuera de la pantalla de detalle.
     */
    override fun onCleared() {
        super.onCleared()
        pacienteListener?.remove()
    }
}