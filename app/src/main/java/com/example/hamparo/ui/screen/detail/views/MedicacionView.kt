package com.example.hamparo.ui.screen.detail.views

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.hamparo.data.model.Paciente
import com.example.hamparo.data.model.PautaMedica
import com.example.hamparo.data.model.TipoFrecuencia
import com.example.hamparo.ui.screen.detail.PatientDetailViewModel

@Composable
fun MedicacionView(
    paciente: Paciente,
    viewModel: PatientDetailViewModel
) {
    // 0 = En Curso, 1 = Historial
    var tabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("En Curso", "Historial")

    // Filtramos las listas en tiempo real
    val medicacionActiva = remember(paciente.medicacionActual) {
        paciente.medicacionActual.filter { !it.archivada }
    }
    val medicacionArchivada = remember(paciente.medicacionActual) {
        paciente.medicacionActual.filter { it.archivada }
    }

    Column(Modifier.fillMaxSize()) {
        // --- PESTAÑAS SUPERIORES ---
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
                            if (index == 0) Icons.Default.Medication else Icons.Default.Archive,
                            contentDescription = null
                        )
                    }
                )
            }
        }

        // --- CONTENIDO DE LA LISTA ---
        val listaMostrar = if (tabIndex == 0) medicacionActiva else medicacionArchivada

        if (listaMostrar.isEmpty()) {
            // --- ESTADO VACÍO (Personalizado por pestaña) ---
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (tabIndex == 0) Icons.Default.CheckCircle else Icons.Default.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.LightGray.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = if (tabIndex == 0) "Sin medicación activa" else "El historial está vacío",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Gray
                    )
                    if (tabIndex == 0) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Pulsa el botón + para añadir una pauta",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray
                        )
                    }
                }
            }
        } else {
            // --- LISTA ---
            LazyColumn(
                contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp), // Espacio inferior para el FAB
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                items(
                    items = listaMostrar,
                    key = { it.id }
                ) { pauta ->

                    if (tabIndex == 0) {
                        // 🟢 PESTAÑA EN CURSO: Permite SWIPE para archivar
                        SwipeableMedicamentoItem(
                            pauta = pauta,
                            onSwipe = { viewModel.toggleArchivarPauta(pauta) },
                            content = {
                                MedicacionCardPro(
                                    pauta = pauta,
                                    viewModel = viewModel,
                                    isArchived = false
                                )
                            }
                        )
                    } else {
                        // 🔴 PESTAÑA HISTORIAL: Fija (Sin Swipe)
                        MedicacionCardPro(
                            pauta = pauta,
                            viewModel = viewModel,
                            isArchived = true
                        )
                    }
                }
            }
        }
    }
}

// WRAPPER PARA EL GESTO DE DESLIZAR (Solo archivar)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableMedicamentoItem(
    pauta: PautaMedica,
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
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val color = Color(0xFFFF9800) // Naranja Archivar
            val icon = Icons.Default.Archive

            // Determinamos la alineación del icono según la dirección del swipe
            val alignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd)
                Alignment.CenterStart
            else
                Alignment.CenterEnd

            Box(
                Modifier
                    .fillMaxSize()
                    .background(color, RoundedCornerShape(16.dp))
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment
            ) {
                Icon(icon, contentDescription = "Archivar", tint = Color.White)
            }
        },
        content = { content() }
    )
}

// TARJETA PROFESIONAL (Restaurar solo si hay stock)
@Composable
fun MedicacionCardPro(
    pauta: PautaMedica,
    viewModel: PatientDetailViewModel,
    isArchived: Boolean
) {
    // Lógica de colores según el tipo
    val esSOS = pauta.tipoFrecuencia == TipoFrecuencia.SI_PRECISA

    // Colores del Icono principal
    val iconoColor = when {
        isArchived -> Color.Gray
        esSOS -> Color(0xFFD32F2F) // Rojo emergencia
        else -> Color(0xFF00668B)  // Azul médico
    }

    val bgIcono = when {
        isArchived -> Color(0xFFEEEEEE)
        esSOS -> Color(0xFFFFEBEE)
        else -> Color(0xFFE1F5FE)
    }

    // Lógica de Stock
    val stockCero = pauta.stock == 0
    val stockBajo = pauta.stock < 5

    // Si stock es 0 y está activa, ponemos fondo rojo suave para alertar
    val cardBgColor = if (!isArchived && stockCero) Color(0xFFFFEBEE) else Color.White
    val cardBorderColor = if (!isArchived && stockCero) Color(0xFFFFCDD2) else Color(0xFFEEEEEE)

    // Colores de la etiqueta de stock
    val stockColor = when {
        stockCero -> Color(0xFFC62828)
        stockBajo -> Color(0xFFE65100)
        else -> Color(0xFF2E7D32)
    }
    val stockBg = when {
        stockCero -> Color.White
        stockBajo -> Color(0xFFFFE0B2)
        else -> Color(0xFFC8E6C9)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        elevation = CardDefaults.cardElevation(if (isArchived) 0.dp else 2.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, cardBorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                // 1. ICONO CIRCULAR
                Surface(
                    shape = CircleShape,
                    color = bgIcono,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isArchived) Icons.Default.Inventory2 else if (esSOS) Icons.Default.Warning else Icons.Default.Medication,
                            contentDescription = null,
                            tint = iconoColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(Modifier.width(16.dp))

                // 2. DATOS PRINCIPALES (Nombre y Dosis)
                Column(Modifier.weight(1f)) {
                    Text(
                        text = pauta.nombreMedicamento,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (isArchived) TextDecoration.LineThrough else null,
                        color = if (isArchived) Color.Gray else Color.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(4.dp))

                    if (!isArchived) {
                        // Fila de Dosis y Cantidad
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = pauta.dosis,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.DarkGray,
                                fontWeight = FontWeight.Medium
                            )
                            Text(" • ", color = Color.LightGray)
                            // Formateo para quitar decimales .0 si es entero
                            val cant = if (pauta.cantidad > 0) pauta.cantidad.toString() else "1"
                            val cantFmt = cant.removeSuffix(".0")
                            Text(
                                text = "$cantFmt ud/toma",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    } else {
                        // Texto de estado para archivados
                        Text(
                            text = if(pauta.stock == 0) "AGOTADO / ARCHIVADO" else "ARCHIVADO",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 3. ACCIONES DEL HISTORIAL (Solo visibles en pestaña historial)
                if (isArchived) {
                    Row {
                        if (pauta.stock > 0) {
                            IconButton(
                                onClick = { viewModel.toggleArchivarPauta(pauta) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Restore,
                                    contentDescription = "Restaurar",
                                    tint = Color(0xFF00668B),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                        }

                        // BOTÓN BORRAR
                        IconButton(
                            onClick = { viewModel.borrarPauta(pauta.id) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Borrar",
                                tint = Color.Red.copy(alpha = 0.6f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = if (stockCero && !isArchived) Color(0xFFFFCDD2) else Color(0xFFF5F5F5))
            Spacer(Modifier.height(12.dp))

            // 4. FOOTER (Info Pauta y Stock)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // IZQUIERDA: Frecuencia (cada X horas)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = if (isArchived) Color.Gray else if (esSOS) Color.Gray else iconoColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))

                    val textoPauta = when (pauta.tipoFrecuencia) {
                        TipoFrecuencia.INTERVALO -> "Cada ${pauta.cadaCuantasHoras}h (${pauta.horaInicio})"
                        TipoFrecuencia.SI_PRECISA -> "Si precisa (SOS)"
                        else -> "Pauta manual"
                    }

                    Text(
                        text = textoPauta,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isArchived) Color.Gray else if (esSOS) Color.DarkGray else iconoColor,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // DERECHA: El Stock (Solo si NO está archivado)
                if (!isArchived) {
                    Surface(
                        color = stockBg,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = if (stockCero) "AGOTADO" else "Quedan: ${pauta.stock}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = stockColor
                        )
                    }
                }
            }
        }
    }
}