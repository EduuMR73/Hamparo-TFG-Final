package com.example.hamparo.ui.screens.admin

import android.widget.Toast // <--- IMPORTANTE PARA EL CHIVATO (DEBUG)
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.hamparo.data.local.entities.MedicamentoEntity
import com.example.hamparo.data.local.entities.MedicionEntity
import com.example.hamparo.data.local.entities.MedicionType
import com.example.hamparo.ui.components.HealthChart
import com.example.hamparo.ui.screen.admin.AdminViewModel
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHomeScreen(
    navController: NavController,
    viewModel: AdminViewModel = hiltViewModel()
) {
    val historial by viewModel.historial.collectAsState(initial = emptyList())
    val inventario by viewModel.inventario.collectAsState(initial = emptyList())

    var showDialog by remember { mutableStateOf(false) }
    var selectedMedicine by remember { mutableStateOf<MedicamentoEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Panel del Cuidador") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    selectedMedicine = null
                    showDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Añadir")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. RESUMEN
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Resumen del Paciente", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "${historial.size} registros / ${inventario.size} medicinas",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 2. INVENTARIO
            item {
                Text("💊 Inventario Farmacia", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }

            if (inventario.isEmpty()) {
                item { Text("No hay medicinas. Pulsa + para añadir.", style = MaterialTheme.typography.bodyMedium) }
            } else {
                items(inventario) { medicina ->
                    MedicamentoItem(
                        medicina = medicina,
                        onClick = {
                            selectedMedicine = medicina
                            showDialog = true
                        }
                    )
                }
            }

            // 3. GRÁFICA
            if (historial.isNotEmpty()) {
                item {
                    Text("📈 Evolución", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        HealthChart(data = historial)
                    }
                }
            }

            // 4. HISTORIAL
            item {
                Text("📋 Historial Salud", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }

            items(historial) { medicion ->
                if (medicion.tipo == MedicionType.ALERTA) {
                    // TARJETA DE ALERTA ROJA 🚨
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFCDD2)),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Alerta",
                                tint = Color(0xFFD32F2F),
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "¡S.O.S! PEDIDO DE AYUDA",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFB71C1C)
                                )
                                Text(
                                    text = "El paciente ha activado el aviso.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Black
                                )
                                val fecha = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(medicion.timestamp))
                                Text(
                                    text = "Recibido a las: $fecha",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.DarkGray
                                )
                            }
                        }
                    }
                } else {
                    MedicionItem(medicion)
                }
            }
        }

        if (showDialog) {
            AddMedicineDialog(
                medicinaAEditar = selectedMedicine,
                onDismiss = { showDialog = false },
                onConfirm = { name, dose, freq, stock ->
                    val id = selectedMedicine?.id ?: 0
                    viewModel.guardarNuevaMedicina(name, dose, freq, stock, id)
                    showDialog = false
                },
                onDelete = {
                    selectedMedicine?.let { viewModel.borrarMedicina(it) }
                    showDialog = false
                }
            )
        }
    }
}

// --- COMPONENTES AUXILIARES ---

@Composable
fun MedicamentoItem(medicina: MedicamentoEntity, onClick: () -> Unit) {
    Card(
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = if (medicina.stock < 5) Color.Red else MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(50.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "${medicina.stock}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                        color = if (medicina.stock < 5) Color.White else Color.Black
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = medicina.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = "${medicina.dosis} • Cada ${medicina.frecuenciaHoras}h", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            }
        }
    }
}

@Composable
fun MedicionItem(medicion: MedicionEntity) {
    Card(
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.MonitorHeart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = medicion.tipo.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                val fecha = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(medicion.timestamp))
                Text(text = fecha, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Spacer(modifier = Modifier.weight(1f))
            val unidad = if (medicion.tipo == MedicionType.OXIGENO) "%" else " ppm"
            Text(text = "${medicion.valor1.toInt()}$unidad", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
}

// 👇👇👇 DIÁLOGO MEJORADO CON ESCÁNER Y DEBUG 👇👇👇
@Composable
fun AddMedicineDialog(
    medicinaAEditar: MedicamentoEntity?,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(medicinaAEditar?.nombre ?: "") }
    var dose by remember { mutableStateOf(medicinaAEditar?.dosis ?: "") }
    var frequency by remember { mutableStateOf(medicinaAEditar?.frecuenciaHoras?.toString() ?: "8") }
    var stock by remember { mutableStateOf(medicinaAEditar?.stock?.toString() ?: "20") }

    val esEdicion = medicinaAEditar != null
    val context = LocalContext.current

    // CONFIGURACIÓN DEL ESCÁNER (ZXing)
    val scanLauncher = rememberLauncherForActivityResult(
        contract = ScanContract()
    ) { result ->
        if (result.contents != null) {
            val codigo = result.contents

            // 1. Buscamos el nombre en nuestra "Base de Datos"
            name = buscarMedicamentoEnBaseDeDatos(codigo)

            // 2. 🕵️ DEBUG: Te muestra el código leído en pantalla
            Toast.makeText(context, "Código leído: $codigo", Toast.LENGTH_LONG).show()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(if (esEdicion) "Editar Medicina" else "Nueva Medicina")
                if (esEdicion) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Borrar", tint = Color.Red)
                    }
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                // --- CAMPO NOMBRE ---
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nombre / Código") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    // Botón para Escanear
                    FilledIconButton(
                        onClick = {
                            val options = ScanOptions()
                            options.setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES)
                            options.setPrompt("Escanea el medicamento")
                            options.setBeepEnabled(true)
                            options.setOrientationLocked(false)
                            scanLauncher.launch(options)
                        },
                        modifier = Modifier.size(56.dp),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Escanear")
                    }
                }

                OutlinedTextField(value = dose, onValueChange = { dose = it }, label = { Text("Dosis") }, singleLine = true)
                OutlinedTextField(value = frequency, onValueChange = { frequency = it }, label = { Text("Cada (Horas)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = stock, onValueChange = { stock = it }, label = { Text("Stock") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
        },
        confirmButton = {
            Button(onClick = { if (name.isNotEmpty()) onConfirm(name, dose, frequency, stock) }) {
                Text(if (esEdicion) "Actualizar" else "Guardar")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

// 🧠 BASE DE DATOS SIMULADA INTELIGENTE 🧠
fun buscarMedicamentoEnBaseDeDatos(codigo: String): String {
    return when {
        // --- TUS MEDICINAS REALES (Por Foto) ---
        // Paracetamol Kern Pharma 1g (Contiene 658257 en el CN)
        codigo.contains("658257") -> "Paracetamol Kern 1g"

        // --- CÓDIGOS DATAMATRIX (Genéricos) ---
        codigo.contains("664627") || codigo.contains("847000153028") -> "Nolotil 575mg"
        codigo.contains("693827") || codigo.contains("847000693827") -> "Ibuprofeno 600mg"
        codigo.contains("882313") -> "Sintrom 4mg"
        codigo.contains("445566") -> "Adiro 100mg"

        // --- CÓDIGOS DE BARRAS CLÁSICOS ---
        codigo == "847000123456" -> "Paracetamol 1g (Demo)"
        codigo == "843000654321" -> "Ibuprofeno 600mg (Demo)"

        // --- NO ENCONTRADO ---
        else -> codigo // Devuelve el número para que puedas copiarlo
    }
}