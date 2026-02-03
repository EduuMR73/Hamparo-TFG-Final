package com.example.hamparo.ui.screen.detail.views


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hamparo.data.model.Alerta
import com.example.hamparo.data.model.TipoAlerta
import com.example.hamparo.ui.screen.detail.PatientDetailViewModel

@Composable
fun AlertasView(
    viewModel: PatientDetailViewModel
) {
    // 0 = Activas (Bandeja de Entrada), 1 = Historial (Atendidas)
    var tabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Pendientes", "Historial")

    val alertasActivas by viewModel.alertas.collectAsState()
    val alertasHistorial by viewModel.alertasHistorial.collectAsState()

    // Seleccionamos qué lista mostrar según la pestaña
    val listaMostrar = if (tabIndex == 0) alertasActivas else alertasHistorial

    Column(Modifier.fillMaxSize()) {
        // --- PESTAÑAS SUPERIORES ---
        TabRow(
            selectedTabIndex = tabIndex,
            containerColor = Color.White,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[tabIndex]),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = tabIndex == index,
                    onClick = { tabIndex = index },
                    text = { Text(title, fontWeight = FontWeight.Bold) },
                    icon = {
                        Icon(
                            if (index == 0) Icons.Default.NotificationsActive else Icons.Default.History,
                            contentDescription = null
                        )
                    }
                )
            }
        }

        // --- CONTENIDO DE LA LISTA ---
        if (listaMostrar.isEmpty()) {
            // ESTADO VACÍO (Placeholder bonito)
            Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        if (tabIndex == 0) Icons.Default.CheckCircle else Icons.Default.History,
                        null,
                        tint = if (tabIndex == 0) Color(0xFF4CAF50) else Color.LightGray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (tabIndex == 0) "¡Todo limpio! Sin alertas pendientes" else "No hay historial de alertas",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Gray
                    )
                }
            }
        } else {
            // LISTA REAL
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items = listaMostrar, key = { it.id }) { alerta ->

                    if (tabIndex == 0) {
                        // 🟢 PESTAÑA PENDIENTES: Swipe para marcar como ATENDIDA
                        SwipeableAlertaItem(
                            alerta = alerta,
                            onSwipe = { viewModel.toggleArchivarAlerta(alerta) },
                            content = {
                                AlertaItem(alerta, viewModel, isArchived = false)
                            }
                        )
                    } else {
                        // PESTAÑA HISTORIAL: No swipe, botones directos
                        AlertaItem(alerta, viewModel, isArchived = true)
                    }
                }
                // Espacio extra al final para que no tape el botón flotante si hubiera
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

// -----------------------------------------------------------
// WRAPPER PARA EL SWIPE (Marcar como Atendido)
// -----------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableAlertaItem(
    alerta: Alerta,
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
            // Fondo VERDE al deslizar (Acción positiva: Atendido)
            val color = Color(0xFF4CAF50)
            val alignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
            Box(
                Modifier
                    .fillMaxSize()
                    .background(color, RoundedCornerShape(16.dp))
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Check, null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("MARCAR ATENDIDO", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        },
        content = { content() }
    )
}

@Composable
fun AlertaItem(
    alerta: Alerta,
    viewModel: PatientDetailViewModel,
    isArchived: Boolean
) {
    // Si está archivada se ve más apagada (gris)
    val colorFondo = if (!alerta.leido && !isArchived) Color(0xFFE3F2FD) else Color.White
    val alphaTexto = if (isArchived) 0.6f else 1f

    val colorIcono = when (alerta.tipo) {
        TipoAlerta.URGENTE -> Color(0xFFD32F2F) // Rojo
        TipoAlerta.MEDICACION -> Color(0xFFFB8C00) // Naranja
        else -> Color.Gray
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = colorFondo),
        elevation = CardDefaults.cardElevation(if (!alerta.leido && !isArchived) 6.dp else 2.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                // Al hacer clic en pendiente, se marca como leída pero no se archiva
                if (!isArchived && !alerta.leido) {
                    viewModel.marcarAlertaComoLeida(alerta)
                }
            }
    ) {
        Column {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // ICONO
                Icon(
                    imageVector = if (isArchived) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isArchived) Color.LightGray else colorIcono,
                    modifier = Modifier.size(32.dp)
                )

                Spacer(Modifier.width(16.dp))

                // TEXTOS
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = alerta.titulo,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = (if (!alerta.leido && !isArchived) Color.Black else Color.Gray).copy(alpha = alphaTexto)
                    )
                    Text(
                        text = alerta.mensaje,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.DarkGray.copy(alpha = alphaTexto)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${alerta.fecha} - ${alerta.hora}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }

                // ETIQUETA "NUEVO" (Solo en pendientes no leídas)
                if (!alerta.leido && !isArchived) {
                    Surface(
                        color = Color.Red,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "NUEVO",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // 🔴 BARRA DE ACCIONES (SOLO EN HISTORIAL)
            // Aquí es donde añadimos los botones de Borrar y Restaurar
            if (isArchived) {
                Divider(color = Color.LightGray.copy(alpha = 0.3f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFAFAFA))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    // BOTÓN RESTAURAR (Devolver a pendientes)
                    TextButton(
                        onClick = { viewModel.toggleArchivarAlerta(alerta) }
                    ) {
                        Icon(Icons.Default.Restore, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Recuperar", fontSize = 12.sp)
                    }

                    Spacer(Modifier.width(8.dp))

                    // BOTÓN BORRAR DEFINITIVO (Basura Roja)
                    TextButton(
                        onClick = { viewModel.borrarAlerta(alerta.id, alerta.pacienteId) },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                    ) {
                        Icon(Icons.Default.DeleteForever, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Eliminar", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}