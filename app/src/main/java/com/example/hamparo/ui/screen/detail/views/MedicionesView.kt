package com.example.hamparo.ui.screen.detail.views

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.hamparo.data.model.Medicion
import com.example.hamparo.data.model.TipoMedicion
import com.example.hamparo.ui.components.HealthChart
import com.example.hamparo.ui.screen.detail.PatientDetailViewModel
import com.example.hamparo.ui.utils.ReportGenerator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicionesView(
    mediciones: List<Medicion>,
    viewModel: PatientDetailViewModel
) {
    val context = LocalContext.current

    // Obtenemos el paciente del ViewModel para sacar nombre y apellidos
    val paciente by viewModel.paciente.collectAsState()

    var tipoSeleccionado by remember { mutableStateOf(TipoMedicion.TENSION) }
    var mostrarDialogo by remember { mutableStateOf(false) }

    // FILTRADO MAESTRO
    val datosFiltrados = remember(mediciones, tipoSeleccionado) {
        mediciones.filter { medicion ->
            medicion.tipo == tipoSeleccionado &&
                    !medicion.archivada &&
                    medicion.valor1.replace(",", ".").toDoubleOrNull() != null
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { mostrarDialogo = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Añadir", tint = Color.White)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // --- 1. BARRA DE PESTAÑAS ---
            Text("Selecciona Constante", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(TipoMedicion.values()) { tipo ->
                    FilterChip(
                        selected = (tipo == tipoSeleccionado),
                        onClick = { tipoSeleccionado = tipo },
                        label = { Text(tipo.label) },
                        leadingIcon = if (tipo == tipoSeleccionado) {
                            { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- CABECERA DE LA GRÁFICA CON BOTÓN PDF ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Gráfica de Evolución",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // 👇 BOTÓN DE DESCARGA PDF (RA5)
                IconButton(
                    onClick = {
                        // Construimos el nombre completo (Nombre + Apellidos)
                        val nombreParaPDF = if (paciente != null) {
                            "${paciente!!.nombre} ${paciente!!.apellidos}"
                        } else {
                            "Paciente"
                        }

                        // Se lo pasamos al generador
                        ReportGenerator.generarInformePDF(
                            context = context,
                            datos = datosFiltrados,
                            tipoNombre = tipoSeleccionado.label,
                            nombreCompleto = nombreParaPDF // Nuevo parámetro obligatorio
                        )
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Descargar Informe PDF",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- 2. GRÁFICA ---
            HealthChart(
                datos = datosFiltrados.reversed(),
                titulo = tipoSeleccionado.label,
                unidad = tipoSeleccionado.unidad
            )

            // --- 3. LISTA HISTORIAL ---
            Spacer(modifier = Modifier.height(24.dp))
            Text("Historial reciente", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            if (datosFiltrados.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No hay registros de ${tipoSeleccionado.label}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(datosFiltrados) { med ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ListItem(
                                headlineContent = {
                                    val valorTexto = if (med.valor2.isNotEmpty()) "${med.valor1} / ${med.valor2}" else med.valor1
                                    Text(
                                        "$valorTexto ${tipoSeleccionado.unidad}",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                },
                                supportingContent = { Text("${med.fecha} - ${med.hora}", style = MaterialTheme.typography.bodySmall) },
                                leadingContent = {
                                    Text(
                                        text = when(tipoSeleccionado) {
                                            TipoMedicion.TENSION -> "❤️"
                                            TipoMedicion.PESO -> "⚖️"
                                            TipoMedicion.GLUCOSA -> "🩸"
                                            TipoMedicion.TEMPERATURA -> "🌡️"
                                            else -> "📈"
                                        },
                                        style = MaterialTheme.typography.headlineSmall
                                    )
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )
                        }
                    }
                }
            }
        }
    }

    // --- DIÁLOGO REGISTRO ---
    if (mostrarDialogo) {
        key(tipoSeleccionado) {
            DialogoRegistroLocal(
                tipo = tipoSeleccionado,
                onDismiss = { mostrarDialogo = false },
                onConfirm = { v1, v2 ->
                    viewModel.guardarMedicion(tipoSeleccionado, v1, v2)
                    mostrarDialogo = false
                }
            )
        }
    }
}

// Mantener el DialogoRegistroLocal aquí abajo...
@Composable
fun DialogoRegistroLocal(
    tipo: TipoMedicion,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var valor1 by remember { mutableStateOf("") }
    var valor2 by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Nuevo: ${tipo.label}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (tipo == TipoMedicion.TENSION) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = valor1,
                            onValueChange = { if (it.length <= 3) valor1 = it },
                            label = { Text("Alta") },
                            placeholder = { Text("120") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = valor2,
                            onValueChange = { if (it.length <= 3) valor2 = it },
                            label = { Text("Baja") },
                            placeholder = { Text("80") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = valor1,
                        onValueChange = { valor1 = it },
                        label = { Text("Valor (${tipo.unidad})") },
                        placeholder = { Text("Ej: 98") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(valor1, valor2) },
                        enabled = valor1.isNotEmpty()
                    ) {
                        Text("Guardar")
                    }
                }
            }
        }
    }
}