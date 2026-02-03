package com.example.hamparo.ui.screen.detail

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.hamparo.data.model.CitaMedica
import com.example.hamparo.data.model.FamiliarVinculado
import com.example.hamparo.data.model.Medicion
import com.example.hamparo.data.model.Paciente
import com.example.hamparo.ui.screen.admin.AdminViewModel
import com.example.hamparo.ui.screen.dialogs.DialogoEditarFoto
import com.example.hamparo.ui.screen.dialogs.DialogoFamiliar
import com.example.hamparo.ui.screen.dialogs.DialogoNuevaCita
import com.example.hamparo.ui.screen.dialogs.DialogoNuevaMedicina
import com.example.hamparo.ui.screen.dialogs.DialogoNuevoRegistro
import com.example.hamparo.ui.screen.detail.views.*
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDetailScreen(
    pacienteInicial: Paciente,
    emailLogueado: String,
    onBack: () -> Unit,
    viewModel: PatientDetailViewModel = hiltViewModel(),
    adminViewModel: AdminViewModel = hiltViewModel()
) {
    LaunchedEffect(pacienteInicial, emailLogueado) {
        viewModel.cargarPaciente(pacienteInicial)
    }

    val paciente by viewModel.paciente.collectAsState()
    val cuidador by viewModel.cuidador.collectAsState()
    val mensaje by viewModel.mensaje.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val context = LocalContext.current
    var currentView by rememberSaveable { mutableStateOf(DetailView.DASHBOARD) }
    var isEditing by rememberSaveable { mutableStateOf(false) }

    val prefs = remember { context.getSharedPreferences("HamparoPrefs", Context.MODE_PRIVATE) }
    val rolUsuario = remember { prefs.getString("ROL", "CUIDADOR") ?: "CUIDADOR" }
    val esFamiliar = rolUsuario == "FAMILIAR"

    var mostrarDialogoMedicina by rememberSaveable { mutableStateOf(false) }
    var mostrarDialogoFamiliar by rememberSaveable { mutableStateOf(false) }
    var familiarAEditar by remember { mutableStateOf<FamiliarVinculado?>(null) }
    var mostrarDialogoFoto by rememberSaveable { mutableStateOf(false) }
    var mostrarDialogoCita by rememberSaveable { mutableStateOf(false) }
    var mostrarDialogoHistorial by rememberSaveable { mutableStateOf(false) }

    fun handleBackNavigation() {
        when {
            isEditing -> isEditing = false
            currentView != DetailView.DASHBOARD -> currentView = DetailView.DASHBOARD
            else -> onBack()
        }
    }

    BackHandler {
        if (esFamiliar && currentView == DetailView.DASHBOARD) {
            (context as? Activity)?.finish()
        } else {
            handleBackNavigation()
        }
    }

    LaunchedEffect(mensaje) {
        mensaje?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.limpiarMensaje()
        }
    }

    if (paciente == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        if (currentView == DetailView.DASHBOARD) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "${paciente!!.nombre} ${paciente!!.apellidos}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "Cód. Vinculación: ${paciente!!.codigoVinculacion}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else {
                            val titulo = if (isEditing) "Editando" else when (currentView) {
                                DetailView.FICHA -> "Ficha Técnica"
                                DetailView.MEDICACION -> "Medicación"
                                DetailView.CITAS -> "Citas Médicas"
                                DetailView.HISTORIAL -> "Historial Médico"
                                DetailView.ALERTAS -> "Centro de Alertas"
                                DetailView.CURAS -> "Curas y Heridas"
                                DetailView.CONSTANTES -> "Constantes Vitales"
                                else -> currentView.name
                            }
                            Text(titulo, fontWeight = FontWeight.Bold)
                        }
                    },
                    navigationIcon = {
                        val debeMostrarFlecha = !esFamiliar || currentView != DetailView.DASHBOARD
                        if (debeMostrarFlecha) {
                            IconButton(onClick = { handleBackNavigation() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                            }
                        }
                    },
                    actions = {
                        if (currentView == DetailView.FICHA) {
                            if (isEditing) {
                                if (isLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                } else {
                                    TextButton(onClick = {
                                        viewModel.guardarFicha()
                                        isEditing = false
                                    }) {
                                        Text("GUARDAR", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            } else {
                                IconButton(onClick = { isEditing = true }) {
                                    Icon(Icons.Default.Edit, "Editar", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }

                        if (esFamiliar && currentView == DetailView.DASHBOARD) {
                            IconButton(onClick = {
                                prefs.edit().clear().apply()
                                val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                                intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                context.startActivity(intent)
                                (context as? Activity)?.finish()
                            }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                    contentDescription = "Cerrar Sesión",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xFFF5F5F5))
                )
            },
            floatingActionButton = {
                val showFab = currentView == DetailView.MEDICACION ||
                        currentView == DetailView.CITAS ||
                        currentView == DetailView.HISTORIAL

                if (showFab) {
                    FloatingActionButton(
                        onClick = {
                            if (currentView == DetailView.MEDICACION) mostrarDialogoMedicina = true
                            if (currentView == DetailView.CITAS) mostrarDialogoCita = true
                            if (currentView == DetailView.HISTORIAL) mostrarDialogoHistorial = true
                        },
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Default.Add, "Añadir", tint = Color.White)
                    }
                }
            },
            containerColor = Color(0xFFF5F5F5)
        ) { padding ->
            Box(Modifier.padding(padding)) {
                Crossfade(
                    targetState = currentView,
                    label = "ViewSwitch",
                    animationSpec = tween(300)
                ) { view ->
                    when (view) {
                        DetailView.DASHBOARD -> DashboardView(
                            paciente = paciente!!,
                            cuidador = cuidador,
                            onNavigate = { currentView = it },
                            viewModel = viewModel
                        )
                        DetailView.FICHA -> FichaTecnicaView(
                            paciente = paciente!!,
                            viewModel = viewModel,
                            isEditing = isEditing,
                            onAddFamiliar = {
                                familiarAEditar = null
                                mostrarDialogoFamiliar = true
                            },
                            onEditFamiliar = { f ->
                                familiarAEditar = f
                                mostrarDialogoFamiliar = true
                            },
                            onEditFoto = { mostrarDialogoFoto = true }
                        )
                        DetailView.MEDICACION -> MedicacionView(paciente!!, viewModel)
                        DetailView.CITAS -> CitasView(paciente!!, viewModel)
                        DetailView.HISTORIAL -> HistorialView(paciente!!, viewModel)
                        DetailView.ALERTAS -> AlertasView(viewModel)
                        DetailView.CURAS -> CurasView(paciente = paciente!!, viewModel = viewModel)
                        DetailView.CONSTANTES -> MedicionesView(
                            mediciones = paciente!!.historialMedico ?: emptyList(),
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }

    // --- DIÁLOGOS ---

    if (mostrarDialogoFamiliar) {
        DialogoFamiliar(familiarAEditar, { mostrarDialogoFamiliar = false }) { n, p, t ->
            val nuevo = if (familiarAEditar != null) familiarAEditar!!.copy(
                nombre = n, parentesco = p, telefono = t
            ) else FamiliarVinculado(nombre = n, parentesco = p, telefono = t)
            val lista = if (familiarAEditar != null) paciente!!.familiaresVinculados.map { if (it.id == nuevo.id) nuevo else it } else paciente!!.familiaresVinculados + nuevo
            viewModel.actualizarDato(paciente!!.copy(familiaresVinculados = lista))
            mostrarDialogoFamiliar = false
        }
    }

    if (mostrarDialogoFoto) {
        DialogoEditarFoto(paciente?.fotoUrl ?: "", { mostrarDialogoFoto = false }) { u ->
            viewModel.actualizarDato(paciente!!.copy(fotoUrl = u))
            mostrarDialogoFoto = false
        }
    }

    if (mostrarDialogoMedicina) {
        DialogoNuevaMedicina(
            adminViewModel = adminViewModel,
            onDismiss = { mostrarDialogoMedicina = false },
            onGuardar = { nombre, dosis, tipoFrecuencia, indicacion, intervalo, horaInicio, cantidad, stock, descripcion, advertencias, urlPdf, codigoNacional ->

                // Porque el ViewModel ya acepta Enum y Float
                viewModel.guardarNuevaPautaMedica(
                    nombre = nombre,
                    dosis = dosis,
                    tipoFrecuencia = tipoFrecuencia, // Pasamos el Objeto directamente
                    indicacion = indicacion,
                    intervalo = intervalo,
                    horaInicio = horaInicio,
                    cantidad = cantidad, // Pasamos el Float directamente
                    stock = stock,
                    descripcion = descripcion,
                    advertencias = advertencias,
                    urlPdf = urlPdf,
                    codigoNacional = codigoNacional
                )

                mostrarDialogoMedicina = false
            }
        )
    }

    if (mostrarDialogoCita) {
        DialogoNuevaCita(
            onDismiss = { mostrarDialogoCita = false },
            onGuardar = { fecha, hora, esp, centro ->
                val nuevaCita = CitaMedica(fecha = fecha, hora = hora, especialista = esp, centroMedico = centro)
                val nuevasCitas = paciente!!.citasMedicas + nuevaCita
                viewModel.actualizarDato(paciente!!.copy(citasMedicas = nuevasCitas))
                viewModel.guardarFicha()
                mostrarDialogoCita = false
            }
        )
    }

    if (mostrarDialogoHistorial) {
        DialogoNuevoRegistro(
            onDismiss = { mostrarDialogoHistorial = false },
            onGuardar = { tipo, v1, v2, fecha, hora, notas ->
                val nuevaMedicion = Medicion(
                    id = UUID.randomUUID().toString(),
                    fecha = if (fecha.isEmpty()) java.text.SimpleDateFormat("dd/MM/yyyy").format(java.util.Date()) else fecha,
                    hora = if (hora.isEmpty()) java.text.SimpleDateFormat("HH:mm").format(java.util.Date()) else hora,

                    tipo = tipo,

                    valor1 = v1,
                    valor2 = v2,
                    notas = notas.ifEmpty { "Registro manual" },
                    grupoId = paciente!!.adminId,
                    pacienteNombre = paciente!!.nombre,
                    pacienteId = paciente!!.id
                )
                viewModel.guardarMedicionManual(nuevaMedicion)
                mostrarDialogoHistorial = false
            }
        )
    }
}