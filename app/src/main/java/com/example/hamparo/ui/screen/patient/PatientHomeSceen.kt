package com.example.hamparo.ui.screen.patient

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PhonelinkRing
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.hamparo.data.model.PautaMedica
import com.example.hamparo.ui.navigation.AppScreens
import com.example.hamparo.ui.utils.VoiceHelper
import java.text.SimpleDateFormat
import java.util.*

// COLORES SUAVES Y CLAROS

private val ColorFondo = Color(0xFFF5F7FA)
private val ColorAccion = Color(0xFF00668B)
private val ColorAlerta = Color(0xFFFF9800)
private val ColorAviso = Color(0xFFE65100)
private val ColorExito = Color(0xFF2E7D32)
private val ColorAyuda = Color(0xFF00796B)
private val ColorGrisBloqueado = Color(0xFF9E9E9E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientHomeScreen(
    navController: NavController,
    viewModel: PatientViewModel = hiltViewModel()
) {
    // --- ESTADOS ---
    val bloques by viewModel.bloques.collectAsState()
    val pautasSiPrecisa by viewModel.pautasSiPrecisa.collectAsState(initial = emptyList())
    val todasLasCitas by viewModel.citas.collectAsState()
    val proximaTomaInfo by viewModel.proximaTomaInfo.collectAsState()
    val nombrePaciente by viewModel.nombrePaciente.collectAsState()
    val mensaje by viewModel.mensaje.collectAsState()
    val nombreFamilia by viewModel.nombreFamiliaConectada.collectAsState()
    val mostrarAyuda by viewModel.mostrarAyuda.collectAsState()
    val confirmacionVoz by viewModel.confirmacionVoz.collectAsState()
    val tomaExitosa by viewModel.tomaExitosa.collectAsState()

    val citasFiltradas = remember(todasLasCitas) {
        val sdfFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val sdfHora = SimpleDateFormat("HH:mm", Locale.getDefault())
        val ahora = Date()

        val calHoy = Calendar.getInstance().apply {
            time = ahora
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val hoyMadrugada = calHoy.time

        val calMañanaFin = Calendar.getInstance().apply {
            time = hoyMadrugada
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59)
        }
        val limiteVentana = calMañanaFin.time

        todasLasCitas.filter { cita ->
            try {
                val fechaCita = sdfFecha.parse(cita.fecha) ?: return@filter true
                if (fechaCita.after(limiteVentana)) return@filter false
                if (fechaCita.before(hoyMadrugada)) return@filter false
                if (fechaCita.after(hoyMadrugada)) return@filter true

                val horaCita = sdfHora.parse(cita.hora) ?: return@filter true
                val calCita = Calendar.getInstance().apply {
                    time = ahora
                    val calHora = Calendar.getInstance().apply { time = horaCita }
                    set(Calendar.HOUR_OF_DAY, calHora.get(Calendar.HOUR_OF_DAY))
                    set(Calendar.MINUTE, calHora.get(Calendar.MINUTE))
                }
                calCita.add(Calendar.MINUTE, 30)
                ahora.before(calCita.time)
            } catch (e: Exception) { true }
        }.sortedWith(compareBy({
            try { sdfFecha.parse(it.fecha)?.time ?: 0L } catch(e:Exception) { 0L }
        }, {
            try { sdfHora.parse(it.hora)?.time ?: 0L } catch(e:Exception) { 0L }
        }))
    }

    val nombrePila = remember(nombrePaciente) { nombrePaciente.split(" ").firstOrNull()?.uppercase() ?: "ABUELO" }
    val context = LocalContext.current
    var showLogoutDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var mostrarDetalleCitas by remember { mutableStateOf(false) }

    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val texto = VoiceHelper.parseResult(result.data)
            viewModel.procesarComandoVoz(texto)
        }
    }
    val launcherPermisos = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                launcherPermisos.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(mensaje) {
        if (mensaje != null) {
            Toast.makeText(context, mensaje, Toast.LENGTH_SHORT).show()
            viewModel.limpiarMensaje()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("HOLA, $nombrePila 👋", fontWeight = FontWeight.Black, fontSize = 22.sp)
                        Text(nombreFamilia, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.9f))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = ColorAccion,
                    titleContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(Icons.Default.Lock, "Salir", tint = Color.White)
                    }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 20.dp, color = Color.White) {
                Button(
                    onClick = { viewModel.enviarAlertaSOS() },
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(80.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(8.dp)
                ) {
                    Icon(Icons.Rounded.PhonelinkRing, null, tint = Color.White, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.width(16.dp))
                    Text("PEDIR AYUDA", fontSize = 24.sp, fontWeight = FontWeight.Black)
                }
            }
        },
        containerColor = ColorFondo
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            if (citasFiltradas.isNotEmpty()) {
                item {
                    val esParaHoy = remember(citasFiltradas) {
                        val hoyStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                        citasFiltradas.any { it.fecha == hoyStr }
                    }
                    val textoPrincipal = if(esParaHoy) "Tienes ${citasFiltradas.size} citas pendientes, $nombrePila" else "Mañana tienes ${citasFiltradas.size} citas, $nombrePila"

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { mostrarDetalleCitas = true },
                        border = BorderStroke(1.dp, Color(0xFFFFB300)),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = CircleShape, color = Color(0xFFFFB300), modifier = Modifier.size(48.dp)) {
                                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Event, null, tint = Color.White, modifier = Modifier.size(24.dp)) }
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = textoPrincipal, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFFE65100))
                                Text(text = "Pincha aquí para ver cuándo", fontSize = 14.sp, color = Color.Gray)
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = Color(0xFFE65100))
                        }
                    }
                }
            }

            item { TarjetaMicro(voiceLauncher, viewModel::abrirAyuda) }

            if (bloques.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, Color(0xFFC8E6C9))
                    ) {
                        Column(Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, null, tint = ColorExito, modifier = Modifier.size(32.dp))
                                Spacer(Modifier.width(12.dp))
                                Text("¡Todo listo por ahora!", fontWeight = FontWeight.Bold, color = ColorExito, fontSize = 18.sp)
                            }
                            Spacer(Modifier.height(8.dp))
                            val info = if(proximaTomaInfo.isNotEmpty()) proximaTomaInfo else "Descansa tranquilo."
                            Text(info, fontWeight = FontWeight.Bold, color = ColorExito)
                        }
                    }
                }
            } else {
                items(bloques) { bloque ->
                    BloqueHorarioView(bloque = bloque, viewModel = viewModel, nombrePila = nombrePila)
                }
            }

            if (pautasSiPrecisa.isNotEmpty()) {
                item {
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MedicalServices, null, tint = Color.Gray)
                        Spacer(Modifier.width(8.dp))
                        Text("SOLO SI LO NECESITO", fontWeight = FontWeight.Black, color = Color.Gray, fontSize = 16.sp)
                    }
                }
                items(pautasSiPrecisa) { pauta ->
                    TarjetaMedicamento(
                        pauta = pauta,
                        esPrevia = false,
                        esOlvido = false,
                        onTomar = { viewModel.tomarPauta(it) }
                    )
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (mostrarDetalleCitas) {
        AlertDialog(
            onDismissRequest = { mostrarDetalleCitas = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp),
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Event, null, tint = Color(0xFFE65100), modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Tus Próximas Citas", fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color.Black)
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    citasFiltradas.forEach { cita ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(80.dp)) {
                                Text(text = cita.fecha.take(5), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Gray)
                                Text(text = cita.hora, fontWeight = FontWeight.Black, fontSize = 22.sp, color = Color(0xFFE65100))
                            }
                            Column(modifier = Modifier.padding(start = 12.dp)) {
                                Text(cita.especialista.uppercase(), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
                                if (cita.centroMedico.isNotEmpty()) {
                                    Text(cita.centroMedico, fontSize = 14.sp, color = Color.Gray)
                                }
                            }
                        }
                        HorizontalDivider(color = Color.LightGray.copy(0.3f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)), shape = RoundedCornerShape(12.dp)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Groups, null, tint = ColorAccion)
                            Spacer(Modifier.width(12.dp))
                            Text(text = "Hoy es un día para cuidarte. Tu familia está conectada y pendiente para que todo salga bien.", style = MaterialTheme.typography.bodyMedium, color = ColorAccion)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { mostrarDetalleCitas = false }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = ColorAccion), shape = RoundedCornerShape(12.dp)) {
                    Text("VALE, GRACIAS", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (tomaExitosa.mostrar) {
        AlertDialog(
            onDismissRequest = { viewModel.cerrarFelicitacion() },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp),
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.SentimentVerySatisfied, null, tint = ColorExito, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(16.dp))
                    Text(tomaExitosa.mensajePrincipal, textAlign = TextAlign.Center, fontWeight = FontWeight.Black, fontSize = 22.sp, color = ColorExito)
                }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(text = tomaExitosa.subtitulo, textAlign = TextAlign.Center, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.Black)
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.cerrarFelicitacion() }, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = ColorExito), shape = RoundedCornerShape(16.dp)) {
                    Text("GRACIAS", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (mostrarAyuda) {
        AlertDialog(
            onDismissRequest = { viewModel.cerrarAyuda() },
            containerColor = Color.White,
            modifier = Modifier.fillMaxWidth(0.95f),
            shape = RoundedCornerShape(28.dp),
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Face, null, Modifier.size(56.dp), tint = ColorAyuda)
                    Spacer(Modifier.height(12.dp))
                    Text("PUEDES DECIRME TODO ESTO:", fontWeight = FontWeight.Black, fontSize = 18.sp, textAlign = TextAlign.Center, color = ColorAyuda)
                }
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    EjemploVozDetallado("❤️", "Corazón y Oxígeno", "• Tensión 12 8\n• Pulso 70\n• Oxígeno 98")
                    EjemploVozDetallado("🌡️", "Cuerpo y Azúcar", "• Fiebre 38\n• Azúcar 110")
                    EjemploVozDetallado("🗣️", "Cuéntame qué te pasa", "• Me duele la cabeza\n• Estoy mareado\n• Estoy triste")
                    EjemploVozDetallado("🆘", "Emergencias", "• ¡Socorro!\n• ¡Ayuda, me he caído!")
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.cerrarAyuda() }, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = ColorAyuda), shape = RoundedCornerShape(16.dp)) {
                    Text("¡ENTENDIDO!", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (confirmacionVoz.mostrar) {
        AlertDialog(
            onDismissRequest = { viewModel.cerrarDialogoConfirmacion() },
            icon = { Icon(Icons.Default.CloudUpload, null, tint = ColorAccion) },
            title = { Text(confirmacionVoz.tipo.label) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("¿Quieres guardar este valor?", color = Color.Gray)
                    Text(confirmacionVoz.valor1 + " " + confirmacionVoz.tipo.unidad, fontSize = 24.sp, fontWeight = FontWeight.Black, color = ColorAccion)
                }
            },
            confirmButton = { Button(onClick = { viewModel.enviarMedicion(confirmacionVoz.tipo, confirmacionVoz.valor1) }, colors = ButtonDefaults.buttonColors(containerColor = ColorAccion)) { Text("SÍ, GUARDAR") } },
            dismissButton = { OutlinedButton(onClick = { viewModel.cerrarDialogoConfirmacion() }) { Text("NO") } }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false; pinInput = "" },
            title = { Text("Zona Segura") },
            text = {
                Column {
                    Text("Introduce PIN para salir:")
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if(it.length <= 4) pinInput = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (viewModel.validarPinSalida(pinInput)) {
                        context.getSharedPreferences("HamparoPrefs", Context.MODE_PRIVATE).edit().clear().apply()
                        showLogoutDialog = false
                        // ✅ SOLUCIÓN: Volvemos a la pantalla de BIENVENIDA, no a la de vincular.
                        navController.navigate(AppScreens.Welcome.route) {
                            popUpTo(0) // Limpiamos todo el historial
                        }
                    } else {
                        Toast.makeText(context, "PIN Incorrecto", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("SALIR") }
            },
            dismissButton = { TextButton(onClick = { showLogoutDialog = false }) { Text("CANCELAR") } }
        )
    }
}

// FUNCIONES DE APOYO (SIN CAMBIOS)

fun limpiarNombreMedicamento(nombreOriginal: String): String {
    var nombre = nombreOriginal.uppercase()
    val palabrasBorrar = listOf(
        "COMPRIMIDOS", "CAPSULAS", "CÁPSULAS", "SOBRES", "SOLUCION", "SOLUCIÓN", "JARABE",
        "RECUBIERTOS", "CON", "PELICULA", "PELÍCULA", "EFG", "DURA", "BLANDAS", "GRANULADO",
        "ORAL", "INYECTABLE", "SUSPENSION", "POLVO", "PARA"
    )
    palabrasBorrar.forEach { palabra ->
        nombre = nombre.replace(Regex("\\b$palabra\\b"), "")
    }
    return nombre.replace(Regex("\\s+"), " ").trim()
}

@Composable
fun BloqueHorarioView(bloque: BloqueHorario, viewModel: PatientViewModel, nombrePila: String) {
    val esOlvido = bloque.esAlerta
    val esPrevia = bloque.estado == EstadoBloque.PREVIA
    val colorTitulo = if(esOlvido) ColorAlerta else if(esPrevia) ColorGrisBloqueado else ColorAccion
    val alpha = if (esPrevia) 0.8f else 1f

    Column(modifier = Modifier.alpha(alpha)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(if(esPrevia) Icons.Default.HourglassEmpty else Icons.Default.AccessTimeFilled, null, tint = colorTitulo)
            Spacer(Modifier.width(8.dp))
            Text(bloque.titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = colorTitulo)
        }
        Spacer(Modifier.height(12.dp))

        bloque.pautas.forEach { pauta ->
            if (esPrevia) {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Color.LightGray)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, null, tint = Color.Gray)
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(limpiarNombreMedicamento(pauta.nombreMedicamento), fontWeight = FontWeight.Bold, color = Color.Gray)
                            Text("$nombrePila, en un ratito te toca. Ve preparando el agua.", fontSize = 13.sp, color = Color.Gray, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                        }
                    }
                }
            } else {
                TarjetaMedicamento(pauta = pauta, esPrevia = false, esOlvido = esOlvido, onTomar = { viewModel.tomarPauta(it) })
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
fun TarjetaMedicamento(pauta: PautaMedica, esPrevia: Boolean, esOlvido: Boolean, onTomar: (PautaMedica) -> Unit) {
    val tomada = pauta.tomadaHoy
    val bg = if(tomada) Color(0xFFE8F5E9) else Color.White
    val borde = if(esOlvido && !tomada) ColorAlerta else if(tomada) Color.Transparent else Color.LightGray.copy(0.5f)
    val alpha = if(tomada) 0.6f else 1f
    val cantidadTexto = remember(pauta.cantidad) {
        val c = pauta.cantidad
        val num = if(c % 1 == 0f) c.toInt().toString() else c.toString()
        "$num unidad(es)"
    }
    val nombreLimpio = remember(pauta.nombreMedicamento) { limpiarNombreMedicamento(pauta.nombreMedicamento) }

    Card(colors = CardDefaults.cardColors(containerColor = bg), border = BorderStroke(if(esOlvido) 2.dp else 1.dp, borde), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth().alpha(alpha), elevation = CardDefaults.cardElevation(if (tomada) 0.dp else 4.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                val textoMostrado = if (nombreLimpio.contains(pauta.dosis, ignoreCase = true)) nombreLimpio else "$nombreLimpio ${pauta.dosis}"
                Text(textoMostrado, fontWeight = FontWeight.Black, fontSize = 18.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, color = Color.Black)
                if (!tomada && pauta.indicacion.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Surface(color = ColorAviso.copy(alpha = 0.1f), shape = RoundedCornerShape(6.dp)) {
                        Text("PARA: ${pauta.indicacion.uppercase()}", color = ColorAviso, fontWeight = FontWeight.Black, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
                if (!tomada) {
                    Spacer(Modifier.height(10.dp))
                    Text(cantidadTexto, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = ColorAccion)
                    Spacer(Modifier.height(4.dp))
                    Surface(color = if (pauta.stock <= 5) Color(0xFFFFEBEE) else Color(0xFFF5F5F5), shape = RoundedCornerShape(6.dp)) {
                        Text(text = "Quedan: ${pauta.stock}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (pauta.stock <= 5) Color.Red else Color.Gray, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                } else {
                    Text("TOMADA ✅", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ColorExito)
                }
            }
            Spacer(Modifier.width(16.dp))
            if (!tomada && !esPrevia) {
                Button(onClick = { onTomar(pauta) }, shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = if(esOlvido) ColorAlerta else ColorAccion), modifier = Modifier.height(64.dp).width(110.dp)) {
                    Text("TOMAR", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun EjemploVozDetallado(icono: String, titulo: String, ejemplos: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(0.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Text(icono, fontSize = 32.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(titulo, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                Spacer(modifier = Modifier.height(4.dp))
                Text(ejemplos, fontSize = 14.sp, color = Color.DarkGray, lineHeight = 20.sp)
            }
        }
    }
}

@Composable
fun TarjetaMicro(launcher: androidx.activity.result.ActivityResultLauncher<android.content.Intent>, onAyuda: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp), shape = RoundedCornerShape(24.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("¿Cómo estás?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text("Pulsa para hablar o ver ayuda", color = Color.Gray)
                }
                IconButton(onClick = onAyuda) {
                    Icon(Icons.Default.HelpOutline, null, tint = ColorAccion, modifier = Modifier.size(32.dp))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                FilledIconButton(onClick = { try { launcher.launch(VoiceHelper.getIntent()) } catch(e:Exception){} }, modifier = Modifier.size(72.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = ColorAccion)) {
                    Icon(Icons.Rounded.Mic, null, tint = Color.White, modifier = Modifier.size(36.dp))
                }
            }
        }
    }
}

fun getIconoMedicamento(nombre: String): ImageVector {
    val n = nombre.lowercase()
    return when {
        n.contains("jarabe") || n.contains("gotas") -> Icons.Default.WaterDrop
        n.contains("inyeccion") || n.contains("insulina") -> Icons.Default.Vaccines
        n.contains("inhalador") -> Icons.Default.Air
        n.contains("crema") -> Icons.Default.Healing
        else -> Icons.Default.Medication
    }
}