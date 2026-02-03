package com.example.hamparo.ui.screen.detail.views

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hamparo.data.model.Medicion
import com.example.hamparo.data.model.Paciente
import com.example.hamparo.data.model.TipoMedicion
import com.example.hamparo.ui.screen.detail.PatientDetailViewModel

@Composable
fun HistorialView(
    paciente: Paciente,
    viewModel: PatientDetailViewModel
) {
    // 0 = Pendientes (Nuevos), 1 = Historial (Archivados)
    var tabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Pendientes", "Historial")
    var medicionSeleccionada by remember { mutableStateOf<Medicion?>(null) }

    // Filtramos las listas en tiempo real
    val historialPendiente = remember(paciente.historialMedico) {
        paciente.historialMedico.filter { !it.archivada }
            .sortedWith(compareByDescending<Medicion> { it.fecha }.thenByDescending { it.hora })
    }
    val historialArchivado = remember(paciente.historialMedico) {
        paciente.historialMedico.filter { it.archivada }
            .sortedWith(compareByDescending<Medicion> { it.fecha }.thenByDescending { it.hora })
    }

    Column(Modifier.fillMaxSize()) {
        // --- PESTAÑAS ---
        TabRow(
            selectedTabIndex = tabIndex,
            containerColor = Color.White,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = tabIndex == index,
                    onClick = { tabIndex = index },
                    text = { Text(title, fontWeight = FontWeight.Bold) },
                    icon = {
                        Icon(
                            if (index == 0) Icons.Default.MonitorHeart else Icons.Default.History,
                            contentDescription = null
                        )
                    }
                )
            }
        }

        // --- CONTENIDO ---
        val listaMostrar = if (tabIndex == 0) historialPendiente else historialArchivado

        if (listaMostrar.isEmpty()) {
            // ESTADO VACÍO
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (tabIndex == 0) Icons.Default.CheckCircle else Icons.Default.Timeline,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = if (tabIndex == 0) Color(0xFF4CAF50) else Color.LightGray.copy(0.5f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = if (tabIndex == 0) "Todo revisado" else "No hay registros antiguos",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Gray
                    )
                }
            }
        } else {
            // LISTA
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items = listaMostrar, key = { it.id }) { medicion ->

                    if (tabIndex == 0) {
                        // 🟢 PESTAÑA PENDIENTES: Swipe para archivar
                        SwipeableMedicionItem(
                            medicion = medicion,
                            onSwipe = { viewModel.toggleArchivarMedicion(medicion) },
                            content = {
                                HistorialCardUniversal(
                                    medicion = medicion,
                                    isArchived = false,
                                    onClick = {
                                        // Al hacer click: Marcamos como leída Y abrimos la ficha
                                        if (!medicion.leido) viewModel.marcarMedicionComoLeida(medicion)
                                        medicionSeleccionada = medicion
                                    },
                                    viewModel = viewModel
                                )
                            }
                        )
                    } else {
                        // PESTAÑA HISTORIAL: Fija (sin Swipe), con botones restaurar/borrar
                        HistorialCardUniversal(
                            medicion = medicion,
                            isArchived = true,
                            onClick = { medicionSeleccionada = medicion },
                            viewModel = viewModel
                        )
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    // DIÁLOGO FICHA TÉCNICA

    if (medicionSeleccionada != null) {
        val med = medicionSeleccionada!!
        val (titulo, valor, unidad) = procesarDatosParaVisualizar(med)
        val esNumerico = valor.any { it.isDigit() }
        val unidadFinal = if (esNumerico) unidad else ""

        AlertDialog(
            onDismissRequest = { medicionSeleccionada = null },
            modifier = Modifier.border(2.dp, Color.Black, RoundedCornerShape(28.dp)),
            containerColor = Color.White,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = getColorPorTipo(med.tipo).copy(alpha = 0.1f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                getIconoPorTipo(med.tipo),
                                null,
                                tint = getColorPorTipo(med.tipo),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Registro Médico", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text(titulo, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    HorizontalDivider(thickness = 1.dp, color = Color.LightGray.copy(0.3f))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Fecha:", color = Color.Gray, fontWeight = FontWeight.Medium)
                        Text("${med.fecha} • ${med.hora}", fontWeight = FontWeight.Bold)
                    }
                    if (valor != "TOMADA") {
                        Column {
                            Text("Valor Registrado:", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = valor,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.Black
                                )
                                if (unidadFinal.isNotEmpty()) {
                                    Spacer(Modifier.width(4.dp))
                                    Text(unidadFinal, fontSize = 16.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 4.dp))
                                }
                            }
                        }
                    }
                    if (med.notas.isNotEmpty()) {
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color.Black),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.EditNote, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("NOTAS", style = MaterialTheme.typography.labelSmall, color = Color.Black, fontWeight = FontWeight.Black)
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(text = med.notas, style = MaterialTheme.typography.bodyMedium, color = Color.Black)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { medicionSeleccionada = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("CERRAR FICHA", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(28.dp)
        )
    }
}

// WRAPPER PARA SWIPE

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableMedicionItem(
    medicion: Medicion,
    onSwipe: () -> Unit,
    content: @Composable () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it != SwipeToDismissBoxValue.Settled) {
                onSwipe()
                true
            } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            // Color Naranja (Archivar)
            val color = Color(0xFFFF9800)
            val alignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd

            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment
            ) {
                Icon(Icons.Default.Archive, null, tint = Color.White)
            }
        },
        content = { content() }
    )
}

//  TARJETA LISTADO
@Composable
fun HistorialCardUniversal(
    medicion: Medicion,
    isArchived: Boolean,
    onClick: () -> Unit,
    viewModel: PatientDetailViewModel
) {
    val (tituloLimpio, valorLimpio, unidadLimpia) = procesarDatosParaVisualizar(medicion)

    val esMedicacion = valorLimpio == "TOMADA"
    val colorBase = if (esMedicacion) Color(0xFF009688) else getColorPorTipo(medicion.tipo)
    val icono = if (esMedicacion) Icons.Default.Medication else getIconoPorTipo(medicion.tipo)
    val bgIcono = if(isArchived) Color.LightGray.copy(0.2f) else colorBase.copy(alpha = 0.1f)

    // Lógica Visual: Si no está leída y no está archivada -> Fondo azulito
    val isNuevo = !medicion.leido && !isArchived
    val cardBg = if (isNuevo) Color(0xFFE3F2FD) else Color.White
    val borderStroke = if (isNuevo) BorderStroke(1.dp, colorBase.copy(0.3f)) else BorderStroke(0.5.dp, Color.LightGray.copy(0.4f))

    val esValorNumerico = valorLimpio.any { it.isDigit() }
    val unidadFinal = if (esValorNumerico) unidadLimpia else ""

    Card(
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(if (isNuevo) 4.dp else 0.dp),
        border = borderStroke,
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ICONO
                Surface(
                    shape = CircleShape,
                    color = bgIcono,
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            icono,
                            null,
                            tint = if(isArchived) Color.Gray else colorBase,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(Modifier.width(14.dp))

                // DATOS TEXTO
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = tituloLimpio,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isArchived) Color.Gray else Color(0xFF212121),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${medicion.fecha} • ${medicion.hora}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        maxLines = 1
                    )
                }

                // VALOR O ACCIONES
                if (isArchived) {
                    // MODO HISTORIAL: Botones Restaurar y Borrar
                    Row {
                        IconButton(onClick = { viewModel.toggleArchivarMedicion(medicion) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Restore, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = { viewModel.borrarMedicion(medicion.id) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Delete, null, tint = Color.Red.copy(0.5f), modifier = Modifier.size(20.dp))
                        }
                    }
                } else {
                    // MODO PENDIENTE: Muestra el valor
                    if (esMedicacion) {
                        Surface(
                            color = Color(0xFFE8F5E9),
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, Color(0xFFC8E6C9))
                        ) {
                            Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Check, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("OK", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.wrapContentWidth()
                        ) {
                            Text(
                                text = valorLimpio,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = colorBase
                            )
                            if (unidadFinal.isNotEmpty()) {
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = unidadFinal,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ETIQUETA "NUEVO" (Esquina superior derecha)
            if (isNuevo) {
                Surface(
                    color = Color.Red,
                    shape = RoundedCornerShape(bottomStart = 12.dp),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(
                        text = "NUEVO",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

// --- UTILIDADES ---

fun procesarDatosParaVisualizar(med: Medicion): Triple<String, String, String> {
    val esMedicina = med.tipo.label.contains("MEDICACION", true) ||
            med.tipo.label.contains("TOMA", true) ||
            med.notas.contains("Toma confirmada", true) ||
            (med.tipo.label.contains("Frecuencia") && med.notas.contains("Toma"))

    if (esMedicina) {
        var nombre = med.notas
            .replace("Toma confirmada:", "", true)
            .replace("Toma confirmada", "", true)
            .replace("Medicación:", "", true)
            .replace("-", "")
            .trim()

        if (nombre.length < 3) nombre = "Medicación"
        return Triple(nombre, "TOMADA", "")
    } else {
        val valor = if (med.valor2.isNotEmpty()) "${med.valor1}/${med.valor2}" else med.valor1
        var titulo = med.tipo.label
        if (titulo.length > 18) titulo = titulo.replace("Frecuencia", "Frec.").replace("Temperatura", "Temp.")
        return Triple(titulo, valor, med.tipo.unidad)
    }
}

fun getColorPorTipo(tipo: TipoMedicion): Color {
    return when (tipo) {
        TipoMedicion.TENSION -> Color(0xFFD32F2F)
        TipoMedicion.PESO -> Color(0xFFF57C00)
        TipoMedicion.GLUCOSA -> Color(0xFF7B1FA2)
        TipoMedicion.TEMPERATURA -> Color(0xFF1976D2)
        TipoMedicion.SATURACION -> Color(0xFF00ACC1)
        TipoMedicion.PULSO -> Color(0xFFC2185B)
        else -> Color(0xFF388E3C)
    }
}

fun getIconoPorTipo(tipo: TipoMedicion): ImageVector {
    return when (tipo) {
        TipoMedicion.TENSION -> Icons.Default.FavoriteBorder
        TipoMedicion.PESO -> Icons.Default.MonitorWeight
        TipoMedicion.GLUCOSA -> Icons.Default.WaterDrop
        TipoMedicion.TEMPERATURA -> Icons.Default.Thermostat
        TipoMedicion.SATURACION -> Icons.Default.Air
        TipoMedicion.PULSO -> Icons.Default.MonitorHeart
        else -> Icons.Default.ShowChart
    }
}