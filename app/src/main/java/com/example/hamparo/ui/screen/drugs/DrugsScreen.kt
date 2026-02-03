package com.example.hamparo.ui.screen.drugs

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.hamparo.ui.components.MedicineCard
import com.example.hamparo.ui.utils.PDFDownloader
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrugsScreen(
    navController: NavController,
    viewModel: DrugsViewModel = hiltViewModel()
) {
    // Variable para el texto de búsqueda
    var textoBusqueda by rememberSaveable { mutableStateOf("") }

    // Estado para controlar el borde ROJO de error
    var hayError by remember { mutableStateOf(false) }

    // Estados observados del ViewModel
    val resultadoDetalle by viewModel.medicamentoInfo.collectAsState()
    val listaResultados by viewModel.listaResultados.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    // Herramientas del sistema
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    // Sincronización: Si el ViewModel encuentra el detalle, actualizamos el texto de la barra
    LaunchedEffect(resultadoDetalle) {
        if (resultadoDetalle != null) {
            textoBusqueda = resultadoDetalle!!.nombre
            hayError = false
        }
    }

    // CONFIGURADOR DEL ESCÁNER (UX Corregida)
    val scannerLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            textoBusqueda = "Identificando fármaco..."
            hayError = false
            viewModel.procesarEscaneo(result.contents)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Diccionario de Fármacos") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                actions = {
                    if (resultadoDetalle != null || listaResultados.isNotEmpty() || error != null) {
                        IconButton(onClick = {
                            viewModel.limpiar()
                            textoBusqueda = ""
                            hayError = false
                        }) {
                            Icon(Icons.Default.DeleteSweep, "Limpiar")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // 1. ZONA DE BÚSQUEDA Y ESCÁNER

            OutlinedTextField(
                value = if (isLoading && textoBusqueda == "Identificando fármaco...") "⏳ Buscando en AEMPS..." else textoBusqueda,
                onValueChange = {
                    textoBusqueda = it
                    hayError = false
                },
                label = { Text("Nombre o Código Nacional") },
                placeholder = { Text("Ej: Paracetamol o usa el escáner") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp),
                isError = hayError,
                trailingIcon = {
                    val iconColor = if (hayError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

                    IconButton(onClick = {
                        hayError = false
                        val options = ScanOptions()
                        options.setPrompt("Enfoca el código de barras de la caja")
                        options.setBeepEnabled(true)
                        options.setOrientationLocked(false)
                        scannerLauncher.launch(options)
                    }) {
                        Icon(Icons.Default.QrCodeScanner, "Escanear", tint = iconColor)
                    }
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(onSearch = {
                    focusManager.clearFocus()
                    if (textoBusqueda.isBlank()) {
                        hayError = true
                    } else {
                        viewModel.realizarBusqueda(textoBusqueda)
                    }
                })
            )

            if (hayError) {
                Text(
                    text = "⚠️ Escribe un nombre o escanea un código.",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp, start = 8.dp).align(Alignment.Start)
                )
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    focusManager.clearFocus()
                    if (textoBusqueda.isBlank()) {
                        hayError = true
                        Toast.makeText(context, "El campo de búsqueda está vacío", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.realizarBusqueda(textoBusqueda)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("CONSULTANDO...")
                } else {
                    Text("BUSCAR MEDICAMENTO")
                }
            }

            Spacer(Modifier.height(24.dp))

            // 2. PANEL DE RESULTADOS
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {

                // ESTADO: Error (No encontrado o fallo de red)
                if (error != null) {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(60.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = error!!,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // ESTADO: Detalle del Fármaco (MedicineCard Reutilizable)
                else if (resultadoDetalle != null) {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        MedicineCard(
                            info = resultadoDetalle!!,
                            onDownloadPdf = { url ->
                                PDFDownloader.descargar(
                                    context = context,
                                    url = url,
                                    nombreMedicamento = resultadoDetalle!!.nombre
                                )
                            }
                        )
                        Spacer(Modifier.height(32.dp))
                    }
                }

                // ESTADO: Lista de Sugerencias (Varios resultados)
                else if (listaResultados.isNotEmpty()) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 20.dp)
                    ) {
                        item {
                            Text(
                                "Se han encontrado ${listaResultados.size} coincidencias:",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.Gray,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        items(listaResultados) { item ->
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable { viewModel.seleccionarDeLista(item) },
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                item.nombre.take(1).uppercase(),
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(16.dp))
                                    Column {
                                        Text(
                                            item.nombre,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (!item.labtitular.isNullOrBlank()) {
                                            Text(
                                                item.labtitular,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ESTADO: Pantalla Vacía / Inicio
                else if (!isLoading) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.MedicalInformation,
                            contentDescription = null,
                            tint = Color.LightGray.copy(alpha = 0.5f),
                            modifier = Modifier.size(100.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Busca información oficial de la AEMPS\npor nombre o código de barras.",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}