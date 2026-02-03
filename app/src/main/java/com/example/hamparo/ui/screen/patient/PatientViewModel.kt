package com.example.hamparo.ui.screen.patient

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hamparo.data.model.*
import com.example.hamparo.data.repository.FirestoreRepository
import com.example.hamparo.ui.utils.NotificationScheduler
import com.example.hamparo.ui.utils.TTSManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.math.ceil

// --- CLASES AUXILIARES ---
data class BloqueHorario(
    val titulo: String,
    val pautas: List<PautaMedica>,
    val estado: EstadoBloque,
    val esAlerta: Boolean = false
)

enum class EstadoBloque { PASADO, ACTUAL, PREVIA }

data class EstadoConfirmacionVoz(
    val mostrar: Boolean = false,
    val tipo: TipoMedicion = TipoMedicion.TENSION,
    val valor1: String = "",
    val valor2: String = "",
    val textoDetectado: String = ""
)

data class InfoFelicitacion(
    val mostrar: Boolean = false,
    val mensajePrincipal: String = "",
    val subtitulo: String = ""
)

@HiltViewModel
class PatientViewModel @Inject constructor(
    private val firestoreRepository: FirestoreRepository,
    private val notificationScheduler: NotificationScheduler,
    private val ttsManager: TTSManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // --- DATOS ---
    private val _pautas = MutableStateFlow<List<PautaMedica>>(emptyList())
    val pautas: StateFlow<List<PautaMedica>> = _pautas

    private val _citas = MutableStateFlow<List<CitaMedica>>(emptyList())
    val citas: StateFlow<List<CitaMedica>> = _citas

    // --- ESTADOS UI ---
    private val _bloques = MutableStateFlow<List<BloqueHorario>>(emptyList())
    val bloques: StateFlow<List<BloqueHorario>> = _bloques

    // 🔥 FILTRO DE SEGURIDAD PARA "SI PRECISA"
    val pautasSiPrecisa = _pautas.map { lista ->
        lista.filter { pauta ->
            if (pauta.tipoFrecuencia == TipoFrecuencia.SI_PRECISA) {
                val intervaloSeguridad = 4 * 60 * 60 * 1000 // 4 Horas
                val ahora = System.currentTimeMillis()
                val tiempoDesdeUltima = ahora - pauta.ultimaToma
                pauta.ultimaToma == 0L || tiempoDesdeUltima > intervaloSeguridad
            } else {
                false
            }
        }
    }

    // --- ESTADOS GENERALES ---
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _proximaTomaInfo = MutableStateFlow<String>("")
    val proximaTomaInfo: StateFlow<String> = _proximaTomaInfo

    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje: StateFlow<String?> = _mensaje

    private val _confirmacionVoz = MutableStateFlow(EstadoConfirmacionVoz())
    val confirmacionVoz: StateFlow<EstadoConfirmacionVoz> = _confirmacionVoz

    private val _nombreFamiliaConectada = MutableStateFlow("Cargando...")
    val nombreFamiliaConectada: StateFlow<String> = _nombreFamiliaConectada

    private val _mostrarAyuda = MutableStateFlow(false)
    val mostrarAyuda: StateFlow<Boolean> = _mostrarAyuda

    private val _nombrePaciente = MutableStateFlow("Paciente")
    val nombrePaciente: StateFlow<String> = _nombrePaciente

    private val _tomaExitosa = MutableStateFlow(InfoFelicitacion())
    val tomaExitosa: StateFlow<InfoFelicitacion> = _tomaExitosa

    // PIN DE SEGURIDAD
    private val _pinSeguridad = MutableStateFlow("1234")
    val pinSeguridad: StateFlow<String> = _pinSeguridad

    private var grupoId: String = ""
    private var pacienteId: String = ""

    init {
        cargarPacienteReal()
    }

    private fun cargarPacienteReal() {
        val prefs = context.getSharedPreferences("HamparoPrefs", Context.MODE_PRIVATE)
        val codigoVinculacion = prefs.getString("CODIGO_VINCULACION", "") ?: ""

        if (codigoVinculacion.isNotEmpty()) {
            _isLoading.value = true
            viewModelScope.launch {
                firestoreRepository.obtenerPacientePorCodigo(codigoVinculacion).collect { paciente ->
                    if (paciente != null) {
                        val listaProcesada = procesarPautasDiarias(paciente.medicacionActual)
                        _pautas.value = listaProcesada
                        _citas.value = paciente.citasMedicas.sortedBy { it.hora }

                        recalcularBloques(listaProcesada)

                        pacienteId = paciente.id
                        grupoId = paciente.adminId
                        _nombrePaciente.value = paciente.nombre.split(" ").firstOrNull() ?: "Abuelo"
                        _pinSeguridad.value = paciente.pinDesbloqueo.ifEmpty { "1234" }

                        obtenerNombreFamilia(grupoId)
                    } else {
                        _mensaje.value = "Error: Código no encontrado."
                        ttsManager.hablar("Hubo un problema con tu cuenta.")
                    }
                    _isLoading.value = false
                }
            }
        } else {
            _mensaje.value = "Bienvenido. Vincula tu dispositivo."
            _nombreFamiliaConectada.value = "Sin vincular"
        }
    }

    fun validarPinSalida(inputPin: String): Boolean {
        return inputPin == _pinSeguridad.value
    }

    private fun procesarPautasDiarias(lista: List<PautaMedica>): List<PautaMedica> {
        val hoy = Calendar.getInstance()
        return lista.map { pauta ->
            if (pauta.ultimaToma > 0) {
                val fechaToma = Calendar.getInstance().apply { timeInMillis = pauta.ultimaToma }
                val esHoy = fechaToma.get(Calendar.DAY_OF_YEAR) == hoy.get(Calendar.DAY_OF_YEAR) &&
                        fechaToma.get(Calendar.YEAR) == hoy.get(Calendar.YEAR)
                pauta.copy(tomadaHoy = esHoy)
            } else {
                pauta.copy(tomadaHoy = false)
            }
        }
    }

    private fun recalcularBloques(lista: List<PautaMedica>) {
        val fijas = lista.filter { it.tipoFrecuencia != TipoFrecuencia.SI_PRECISA }
        val cal = Calendar.getInstance()
        val minutosActuales = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

        val pendientesRetrasadas = mutableListOf<PautaMedica>()
        val ahora = mutableListOf<PautaMedica>()
        val enBreve = mutableListOf<PautaMedica>()

        fijas.forEach { pauta ->
            if (!pauta.tomadaHoy) {
                val minutosPauta = obtenerMinutosPauta(pauta)
                val diferencia = minutosPauta - minutosActuales

                when {
                    diferencia > 60 -> { /* Invisible */ }
                    diferencia in 1..60 -> enBreve.add(pauta)
                    diferencia < -60 -> pendientesRetrasadas.add(pauta)
                    else -> ahora.add(pauta)
                }
            }
        }

        val resultado = mutableListOf<BloqueHorario>()
        if (pendientesRetrasadas.isNotEmpty()) {
            resultado.add(BloqueHorario("⚠️ SE TE HA PASADO (Tranquilo, tómala ahora)", pendientesRetrasadas, EstadoBloque.PASADO, true))
        }
        if (ahora.isNotEmpty()) {
            resultado.add(BloqueHorario("✅ ES LA HORA / DISPONIBLE", ahora, EstadoBloque.ACTUAL))
        }
        if (enBreve.isNotEmpty()) {
            resultado.add(BloqueHorario("⏳ EN BREVE (Ve preparándote)", enBreve, EstadoBloque.PREVIA))
        }

        if (resultado.isEmpty() && fijas.isNotEmpty()) {
            val proximaReal = fijas.filter { !it.tomadaHoy }.minByOrNull { obtenerMinutosPauta(it) }
            if (proximaReal != null) {
                _proximaTomaInfo.value = "Próxima: ${proximaReal.nombreMedicamento} a las ${proximaReal.horaInicio}"
            } else {
                _proximaTomaInfo.value = "¡Todo listo por hoy!"
            }
        } else {
            _proximaTomaInfo.value = ""
        }
        _bloques.value = resultado
    }

    private fun obtenerMinutosPauta(pauta: PautaMedica): Int {
        return try {
            val partes = pauta.horaInicio.split(":")
            val hora = partes[0].toInt()
            val min = partes[1].toInt()
            hora * 60 + min
        } catch (e: Exception) { 0 }
    }

    // ACCIÓN DE TOMAR (CORREGIDA PARA AVISAR SI SE ACABA)

    fun tomarPauta(pauta: PautaMedica) {
        viewModelScope.launch {
            val timestamp = System.currentTimeMillis()

            // 1. CÁLCULO DEL NUEVO STOCK (pauta.stock ya es Int)
            val consumo = ceil(pauta.cantidad).toInt()
            val nuevoStock = (pauta.stock - consumo).coerceAtLeast(0)

            // 2. CREAMOS LA COPIA ACTUALIZADA
            val pautaActualizada = pauta.copy(
                tomadaHoy = true,
                ultimaToma = timestamp,
                stock = nuevoStock
            )

            // 3. ACTUALIZAMOS LOCALMENTE
            val nuevaLista = _pautas.value.map {
                if (it.id == pauta.id) pautaActualizada else it
            }
            _pautas.value = nuevaLista
            recalcularBloques(nuevaLista)

            // 4. GUARDADO EN FIREBASE
            if (pacienteId.isNotEmpty()) {
                firestoreRepository.actualizarPauta(pacienteId, pautaActualizada)

                firestoreRepository.registrarToma(
                    medicamentoId = pauta.id,
                    nuevoStock = nuevoStock.toString(),
                    fechaToma = timestamp
                )

                notificationScheduler.cancelarAlarma(pauta.nombreMedicamento)
            }

            enviarMedicion(TipoMedicion.PULSO, "MEDICACION", "TOMADA", "Toma confirmada: ${pauta.nombreMedicamento}")

            // 5. FELICITACIÓN
            val mensajeSiguiente = if (nuevoStock == 0) {
                // CASO 1: SE HA ACABADO
                "¡Atención! Has terminado este medicamento. Avisa para reponer."
            } else if (pauta.tipoFrecuencia == TipoFrecuencia.INTERVALO) {
                // CASO 2: QUEDAN Y ES POR HORARIO
                val cal = Calendar.getInstance()
                cal.add(Calendar.HOUR_OF_DAY, pauta.cadaCuantasHoras)
                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                "La siguiente toma es a las ${sdf.format(cal.time)}"
            } else {
                // CASO 3: SI PRECISA
                "Recuerda: Solo si vuelves a encontrarte mal."
            }

            val nombre = _nombrePaciente.value.uppercase()
            _tomaExitosa.value = InfoFelicitacion(
                mostrar = true,
                mensajePrincipal = if (nuevoStock == 0) "¡Medicamento Terminado!" else "¡Muy bien, $nombre!",
                subtitulo = mensajeSiguiente
            )

            // Texto hablado también adaptado
            val textoVoz = if (nuevoStock == 0) {
                "Atención $nombre, se ha terminado ${pauta.nombreMedicamento}."
            } else {
                "Muy bien $nombre. Medicamento anotado."
            }
            ttsManager.hablar(textoVoz)
        }
    }

    fun cerrarFelicitacion() { _tomaExitosa.value = InfoFelicitacion(mostrar = false) }

    // =========================================================================
    // 🗣️ LÓGICA DE VOZ Y ALERTAS
    // =========================================================================

    private fun obtenerNombreFamilia(idGrupo: String) {
        if (idGrupo.isEmpty()) return
        viewModelScope.launch {
            try { _nombreFamiliaConectada.value = "Familia Conectada" } catch (e: Exception) {}
        }
    }

    fun enviarAlertaSOS() {
        if (grupoId.isEmpty() || pacienteId.isEmpty()) {
            _mensaje.value = "Error: No conectado."
            ttsManager.hablar("No tienes conexión. No puedo avisar.")
            return
        }

        viewModelScope.launch {
            val sdfFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val sdfHora = SimpleDateFormat("HH:mm", Locale.getDefault())
            val ahora = Date()

            val alerta = Alerta(
                id = UUID.randomUUID().toString(),
                pacienteId = pacienteId,
                titulo = "⚠️ ¡SOS! AYUDA REQUERIDA",
                mensaje = "El paciente ${_nombrePaciente.value} ha pulsado el botón de emergencia.",
                fecha = sdfFecha.format(ahora),
                hora = sdfHora.format(ahora),
                timestamp = System.currentTimeMillis(),
                tipo = TipoAlerta.URGENTE,
                leido = false
            )

            firestoreRepository.enviarAlerta(alerta)

            _mensaje.value = "🚨 AVISO ENVIADO A LA FAMILIA"
            ttsManager.hablar("¡Tranquilo! He enviado una alerta urgente a tu familia. La ayuda está en camino.")
        }
    }

    fun enviarMedicion(tipo: TipoMedicion, valor1: String, valor2: String = "", notas: String = "") {
        if (valor1 == "ALERTA") {
            enviarAlertaSOS()
            return
        }

        if (grupoId.isEmpty()) {
            _mensaje.value = "Error: No conectado."
            ttsManager.hablar("No tienes conexión con tus cuidadores.")
            return
        }
        viewModelScope.launch {
            val sdfFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val sdfHora = SimpleDateFormat("HH:mm", Locale.getDefault())
            val ahora = Date()

            val nuevaMedicion = Medicion(
                id = UUID.randomUUID().toString(),
                tipo = tipo,
                valor1 = valor1,
                valor2 = valor2,
                fecha = sdfFecha.format(ahora),
                hora = sdfHora.format(ahora),
                notas = notas.ifEmpty { "Registro de voz" },
                grupoId = grupoId,
                pacienteNombre = _nombrePaciente.value,
                pacienteId = pacienteId
            )

            firestoreRepository.guardarMedicion(nuevaMedicion)

            if (valor1 == "SINTOMA" || valor1 == "ESTADO") {
                _mensaje.value = "Nota enviada al cuidador ❤️"
                ttsManager.hablar("Entendido. He dejado una nota a tu cuidador para que sepa cómo estás.")
            }
            else {
                if (valor1 != "MEDICACION") {
                    _mensaje.value = "Dato guardado correctamente ✅"
                    ttsManager.hablar("Guardado correctamente.")
                }
            }
            cerrarDialogoConfirmacion()
        }
    }

    fun abrirAyuda() {
        _mostrarAyuda.value = true
        ttsManager.hablar("Mira, puedes pedirme cualquiera de estas cosas. ¿Qué necesitas?")
    }

    fun cerrarAyuda() { _mostrarAyuda.value = false }

    fun procesarComandoVoz(textoBruto: String) {
        var texto = textoBruto.lowercase().trim().replace(",", ".")
        texto = convertirPalabrasANumeros(texto)

        if (texto.contains("socorro") || texto.contains("ayuda") || texto.contains("emergencia") || texto.contains("caíd") || texto.contains("caer")) {
            enviarAlertaSOS()
            return
        }

        val numeros = Regex("\\d+(\\.\\d+)?").findAll(texto).map { it.value }.toList()

        if (numeros.isNotEmpty()) {
            val num1 = numeros[0]
            val num2 = if (numeros.size > 1) numeros[1] else ""

            when {
                texto.contains("tensi") || texto.contains("presi") -> {
                    if (num2.isNotEmpty()) {
                        lanzarConfirmacion(TipoMedicion.TENSION, num1, num2, textoBruto)
                        ttsManager.hablar("Has dicho tensión $num1 y $num2. ¿Es correcto?")
                    } else {
                        ttsManager.hablar("He entendido la alta en $num1. Me falta la baja, pero puedo guardar esto.")
                    }
                }
                texto.contains("oxí") || texto.contains("satur") -> {
                    lanzarConfirmacion(TipoMedicion.SATURACION, num1, "", textoBruto)
                    ttsManager.hablar("Oxígeno al $num1 por ciento. ¿Guardar?")
                }
                texto.contains("pulso") || texto.contains("ritmo") -> {
                    lanzarConfirmacion(TipoMedicion.PULSO, num1, "", textoBruto)
                    ttsManager.hablar("Pulso de $num1. ¿Guardar?")
                }
                texto.contains("azú") || texto.contains("gluc") -> {
                    lanzarConfirmacion(TipoMedicion.GLUCOSA, num1, "", textoBruto)
                    ttsManager.hablar("Azúcar en $num1. ¿Guardar?")
                }
                texto.contains("temp") || texto.contains("fiebre") -> {
                    lanzarConfirmacion(TipoMedicion.TEMPERATURA, num1, "", textoBruto)
                    ttsManager.hablar("Temperatura $num1 grados. ¿Confirmar?")
                }
                texto.contains("peso") -> {
                    lanzarConfirmacion(TipoMedicion.PESO, num1, "", textoBruto)
                    ttsManager.hablar("Peso $num1 kilos. ¿Confirmar?")
                }
                else -> {
                    ttsManager.hablar("He oído el número $num1, pero no sé de qué es.")
                }
            }
            return
        }

        if (texto.contains("duele") || texto.contains("dolor")) {
            enviarMedicion(TipoMedicion.PULSO, "SINTOMA", "", "Queja de dolor: $textoBruto")
            return
        }
        if (texto.contains("mal") || texto.contains("mare") || texto.contains("triste") || texto.contains("sol") || texto.contains("cansad")) {
            enviarMedicion(TipoMedicion.PULSO, "ESTADO", "", "Estado anímico/físico: $textoBruto")
            return
        }

        _mensaje.value = "No te he entendido."
        ttsManager.hablar("No te he entendido. Prueba a decir: Tensión 12 8, o Socorro.")
    }

    private fun convertirPalabrasANumeros(textoOriginal: String): String {
        var texto = textoOriginal
        texto = texto.replace(" y medio", ".5")
        texto = texto.replace(" con medio", ".5")
        texto = texto.replace(" y media", ".5")
        texto = texto.replace(" con media", ".5")

        val simples = mapOf(
            "uno" to "1", "una" to "1", "un" to "1", "dos" to "2", "tres" to "3",
            "cuatro" to "4", "cinco" to "5", "seis" to "6", "siete" to "7", "ocho" to "8",
            "nueve" to "9", "diez" to "10", "once" to "11", "doce" to "12", "trece" to "13",
            "catorce" to "14", "quince" to "15", "dieciseis" to "16", "diecisiete" to "17",
            "dieciocho" to "18", "diecinueve" to "19", "veinte" to "20",
            "treinta" to "30", "cuarenta" to "40", "cincuenta" to "50",
            "sesenta" to "60", "setenta" to "70", "ochenta" to "80", "noventa" to "90",
            "cien" to "100", "ciento" to "100"
        )
        simples.forEach { (k, v) -> texto = texto.replace(Regex("\\b$k\\b"), v) }

        val prefijos = mapOf(
            "20 y " to "2", "30 y " to "3", "40 y " to "4", "50 y " to "5",
            "60 y " to "6", "70 y " to "7", "80 y " to "8", "90 y " to "9"
        )
        prefijos.forEach { (k, v) -> texto = texto.replace(k, v) }
        texto = texto.replace(" .", ".")
        return texto
    }

    private fun lanzarConfirmacion(tipo: TipoMedicion, v1: String, v2: String, texto: String) {
        _confirmacionVoz.value = EstadoConfirmacionVoz(true, tipo, v1, v2, texto)
    }

    fun cerrarDialogoConfirmacion() { _confirmacionVoz.value = EstadoConfirmacionVoz(mostrar = false) }
    fun limpiarMensaje() { _mensaje.value = null }
    override fun onCleared() { super.onCleared(); ttsManager.stop() }
}