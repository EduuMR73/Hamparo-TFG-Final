package com.example.hamparo.ui.screen.admin

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.hamparo.ui.components.MedicineCard
import com.example.hamparo.ui.navigation.AppScreens
import com.google.gson.Gson
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHomeScreen(
    navController: NavController,
    viewModel: AdminViewModel = hiltViewModel()
) {
    // ESTADOS DEL VIEWMODEL
    val nombreGrupo by viewModel.nombreGrupo.collectAsState()
    val codigoAcceso by viewModel.codigoAcceso.collectAsState()

    val medicamentoEscaneado by viewModel.medicamentoEscaneado.collectAsState()
    val showScanDialog by viewModel.showScanDialog.collectAsState()
    val candidatos by viewModel.candidatosManuales.collectAsState()

    // 1. GESTIÓN DE VISTAS
    VistaDashboardCompleto(navController, viewModel, nombreGrupo, codigoAcceso)

    if (showScanDialog && medicamentoEscaneado != null) {

        var nombreManual by remember { mutableStateOf("") }
        var dosisManual by remember { mutableStateOf("") }

        LaunchedEffect(medicamentoEscaneado) {
            nombreManual = medicamentoEscaneado!!.nombre
            dosisManual = medicamentoEscaneado!!.dosis
        }

        AlertDialog(
            onDismissRequest = {
                viewModel.cerrarDialogoScanner()
            },
            title = {
                Text(if (medicamentoEscaneado!!.tipo == "Nuevo") "Vincular Medicamento" else "Confirmar Datos")
            },
            text = {
                Column {
                    if (medicamentoEscaneado!!.tipo == "Nuevo") {
                        Text("El código no se reconoció. Búscalo por nombre para obtener el prospecto oficial.", fontSize = 12.sp, color = Color.Gray)
                        Spacer(Modifier.height(16.dp))

                        OutlinedTextField(
                            value = nombreManual,
                            onValueChange = {
                                nombreManual = it
                                viewModel.buscarFarmacoManual(it)
                            },
                            label = { Text("Nombre del medicamento") },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = { Icon(Icons.Default.Search, null) },
                            singleLine = true
                        )

                        if (candidatos.isNotEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth().heightIn(max = 150.dp).padding(vertical = 4.dp),
                                elevation = CardDefaults.cardElevation(4.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                LazyColumn {
                                    items(candidatos) { item ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { viewModel.seleccionarFarmacoManual(item) }
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.MedicalServices, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text(item.nombre, fontSize = 12.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                        }
                                        HorizontalDivider(color = Color.LightGray, thickness = 0.5.dp)
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = dosisManual,
                            onValueChange = { dosisManual = it },
                            label = { Text("Dosis (ej: 1 Pastilla)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    } else {
                        MedicineCard(
                            info = medicamentoEscaneado!!,
                            onDownloadPdf = null
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val nombreFinal = if(medicamentoEscaneado!!.tipo == "Nuevo") nombreManual else medicamentoEscaneado!!.nombre
                    val dosisFinal = if(medicamentoEscaneado!!.tipo == "Nuevo") dosisManual else medicamentoEscaneado!!.dosis

                    if (nombreFinal.isNotBlank()) {
                        viewModel.agregarPautaMedica(
                            nombre = nombreFinal,
                            dosis = dosisFinal,
                            tipoFrecuencia = com.example.hamparo.data.model.TipoFrecuencia.RUTINA,
                            indicacion = "Escaneado",
                            tomas = emptyList()
                        )
                        viewModel.cerrarDialogoScanner()
                        viewModel.limpiarBusquedaManual()
                    }
                }) {
                    Text(if (medicamentoEscaneado!!.tipo == "Nuevo") "GUARDAR MANUAL" else "CONFIRMAR")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.cerrarDialogoScanner()
                    viewModel.limpiarBusquedaManual()
                }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VistaDashboardCompleto(
    navController: NavController,
    viewModel: AdminViewModel,
    nombreGrupo: String?,
    codigoAcceso: String?
) {
    val listaPacientes by viewModel.listaPacientes.collectAsState()
    val mensaje by viewModel.mensaje.collectAsState()
    val emailAdmin by viewModel.emailAdminGrupo.collectAsState()
    val context = LocalContext.current

    var mostrarDialogoNuevo by rememberSaveable { mutableStateOf(false) }
    var nuevoNombre by rememberSaveable { mutableStateOf("") }
    var nuevaUbicacion by rememberSaveable { mutableStateOf("") }

    // Permisos y Escáner
    val launcherPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                launcherPermiso.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val scannerLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            viewModel.procesarCodigoEscaneado(result.contents)
        }
    }

    LaunchedEffect(mensaje) {
        if (mensaje != null) {
            Toast.makeText(context, mensaje, Toast.LENGTH_SHORT).show()
            viewModel.limpiarMensaje()
        }
    }

    // Diálogo nuevo paciente
    if (mostrarDialogoNuevo) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoNuevo = false },
            title = { Text("Nuevo Ingreso") },
            text = {
                Column {
                    Text("Se asignará un ID (NHC) automáticamente.", fontSize = 12.sp, color = Color.Gray)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(value = nuevoNombre, onValueChange = { nuevoNombre = it }, label = { Text("Nombre y Apellidos") }, singleLine = true)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = nuevaUbicacion, onValueChange = { nuevaUbicacion = it }, label = { Text("Ubicación (Opcional)") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.añadirPacienteProfesional(nuevoNombre, nuevaUbicacion)
                    mostrarDialogoNuevo = false
                    nuevoNombre = ""
                    nuevaUbicacion = ""
                }) { Text("GENERAR FICHA") }
            },
            dismissButton = { TextButton(onClick = { mostrarDialogoNuevo = false }) { Text("Cancelar") } }
        )
    }

    // ESTRUCTURA PRINCIPAL (SCAFFOLD)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(nombreGrupo ?: "Mi Unidad", maxLines = 1) },
                actions = {
                    // Botón Diccionario
                    IconButton(onClick = {
                        navController.navigate(AppScreens.DrugsDictionary.route)
                    }) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Diccionario", tint = MaterialTheme.colorScheme.primary)
                    }

                    // Botón Escáner
                    IconButton(onClick = {
                        val options = ScanOptions()
                        options.setPrompt("Enfoca el código de barras")
                        options.setBeepEnabled(true)
                        options.setOrientationLocked(false)
                        scannerLauncher.launch(options)
                    }) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Escanear")
                    }

                    // Botón Salir
                    IconButton(onClick = {
                        viewModel.cerrarSesion()
                        navController.navigate(AppScreens.Login.route) { popUpTo(0) }
                    }) { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Cerrar Sesión") }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { mostrarDialogoNuevo = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.PersonAdd, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("NUEVO INGRESO")
            }
        }
    ) { p ->
        Column(modifier = Modifier.padding(p).padding(16.dp)) {

            // Tarjeta Código de Grupo
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("CÓDIGO DE UNIDAD", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = codigoAcceso ?: "...", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    }
                    IconButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Código", codigoAcceso))
                        Toast.makeText(context, "Copiado", Toast.LENGTH_SHORT).show()
                    }) { Icon(Icons.Default.ContentCopy, null) }
                }
            }

            if (emailAdmin != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color(0xFFE3F2FD), RoundedCornerShape(8.dp)).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Link, null, tint = Color(0xFF1976D2))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("UNIDAD VINCULADA A:", style = MaterialTheme.typography.labelSmall, color = Color(0xFF1976D2), fontWeight = FontWeight.Bold)
                        Text(emailAdmin ?: "", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            // LISTA DE PACIENTES
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listaPacientes) { paciente ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // NAVEGACIÓN DIRECTA AL DETALLE DEL PACIENTE
                                val json = Uri.encode(Gson().toJson(paciente))
                                val emailSafe = emailAdmin ?: "unknown"
                                navController.navigate("patient_detail/$json/$emailSafe")
                            },
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(width = 50.dp, height = 40.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(text = paciente.nhc.take(4).ifEmpty { "?" }, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = "${paciente.nombre} ${paciente.apellidos}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (paciente.habitacion.isNotEmpty()) "📍 ${paciente.habitacion}" else "Sin ubicación definida",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray)
                        }
                    }
                }
                // Padding final para el FAB
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}