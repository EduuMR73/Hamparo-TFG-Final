package com.example.hamparo.ui.screen.detail.views

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import com.example.hamparo.data.model.CitaMedica
import com.example.hamparo.data.model.Paciente
import com.example.hamparo.ui.screen.detail.PatientDetailViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CitasView(paciente: Paciente, viewModel: PatientDetailViewModel) {
    // 0 = Próximas, 1 = Historial
    var tabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Próximas", "Historial")

    // --- LÓGICA DE ORDENACIÓN INTELIGENTE ---
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    // Activas: Ordenadas por fecha más cercana (ascendente)
    val citasActivas = remember(paciente.citasMedicas) {
        paciente.citasMedicas.filter { !it.archivada }
            .sortedBy { try { sdf.parse(it.fecha)?.time ?: 0L } catch (e: Exception) { 0L } }
    }

    // Historial: Ordenadas por fecha más reciente (descendente)
    val citasHistorial = remember(paciente.citasMedicas) {
        paciente.citasMedicas.filter { it.archivada }
            .sortedByDescending { try { sdf.parse(it.fecha)?.time ?: 0L } catch (e: Exception) { 0L } }
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
                            if (index == 0) Icons.Default.CalendarToday else Icons.Default.History,
                            contentDescription = null
                        )
                    }
                )
            }
        }

        // --- CONTENIDO ---
        val listaMostrar = if (tabIndex == 0) citasActivas else citasHistorial

        if (listaMostrar.isEmpty()) {
            // ESTADO VACÍO
            Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (tabIndex == 0) Icons.Default.EventBusy else Icons.Default.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.LightGray.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = if (tabIndex == 0) "No hay citas próximas" else "El historial está vacío",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Gray
                    )
                    if (tabIndex == 0) {
                        Text(
                            "Pulsa el botón + para añadir una visita",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray
                        )
                    }
                }
            }
        } else {
            // LISTA
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = listaMostrar,
                    key = { it.id }
                ) { cita ->

                    if (tabIndex == 0) {
                        // 🟢 PESTAÑA PRÓXIMAS: Con Swipe para archivar
                        SwipeableCitaItem(
                            cita = cita,
                            onSwipe = { viewModel.toggleArchivarCita(cita) },
                            content = {
                                CitaCardPro(cita, viewModel, isArchived = false)
                            }
                        )
                    } else {
                        // PESTAÑA HISTORIAL: Fija, solo borrar (y restaurar si es futura)
                        CitaCardPro(cita, viewModel, isArchived = true)
                    }
                }
                // Espacio para el botón flotante
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

// WRAPPER PARA EL SWIPE (Solo archivar)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableCitaItem(
    cita: CitaMedica,
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
            val color = Color(0xFFFF9800) // Naranja Archivar
            val alignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
            Box(
                Modifier
                    .fillMaxSize()
                    .background(color, RoundedCornerShape(12.dp))
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment
            ) {
                Icon(Icons.Default.Archive, null, tint = Color.White)
            }
        },
        content = { content() }
    )
}

// TARJETA DE CITA (INTELIGENTE)

@Composable
fun CitaCardPro(
    cita: CitaMedica,
    viewModel: PatientDetailViewModel,
    isArchived: Boolean
) {
    // Lógica de Fechas
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val fechaCita = try { sdf.parse(cita.fecha) } catch (e: Exception) { null }
    val hoy = Date()

    // ¿Es antigua? (Ayer o antes)
    // Si la fecha es HOY, no se considera pasada (te dejamos restaurarla)
    val esPasada = fechaCita != null && fechaCita.before(hoy) && !isHoy(fechaCita, hoy)

    // COLORES
    // Si NO está archivada y YA PASÓ -> Rojo Claro (Alerta visual)
    val bgColor = if (!isArchived && esPasada) Color(0xFFFFEBEE) else Color.White
    val borderColor = if (!isArchived && esPasada) Color(0xFFFFCDD2) else Color(0xFFEEEEEE)
    val textColor = if (isArchived) Color.Gray else Color.Black

    Card(
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(if (isArchived) 0.dp else 2.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(Modifier.padding(16.dp)) {
            // 1. FECHA (Calendario Visual)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(end = 16.dp)
            ) {
                Surface(
                    color = if (isArchived) Color.LightGray else if (esPasada) Color(0xFFE57373) else MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(50.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        val dia = if (cita.fecha.length >= 2) cita.fecha.take(2) else "??"
                        Text(
                            text = dia,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isArchived || esPasada) Color.White else MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = cita.hora,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (esPasada && !isArchived) Color.Red else Color.Gray
                )
            }

            // 2. DATOS
            Column(Modifier.weight(1f)) {
                Text(
                    text = cita.especialista,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    textDecoration = if (isArchived) TextDecoration.LineThrough else null
                )

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(Icons.Default.LocationOn, null, Modifier.size(14.dp), tint = Color.Gray)
                    Spacer(Modifier.width(4.dp))
                    Text(cita.centroMedico, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }

                // Mensaje de aviso si ya pasó y sigue activa
                if (!isArchived && esPasada) {
                    Text(
                        text = "⚠️ Esta cita ya ha pasado",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFD32F2F),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                } else if (isArchived) {
                    // Etiqueta en el historial
                    Text(
                        text = if(esPasada) "FINALIZADA" else "ARCHIVADA",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.LightGray,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (cita.observaciones.isNotEmpty() && !isArchived) {
                    Spacer(Modifier.height(8.dp))
                    Surface(color = Color(0xFFF5F5F5), shape = RoundedCornerShape(4.dp)) {
                        Text(
                            cita.observaciones,
                            modifier = Modifier.padding(6.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.DarkGray
                        )
                    }
                }
            }

            // 3. ACCIONES (Solo en Historial)
            if (isArchived) {
                Column {
                    if (!esPasada) {
                        IconButton(onClick = { viewModel.toggleArchivarCita(cita) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Restore, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    // BORRAR SIEMPRE ESTÁ DISPONIBLE
                    IconButton(onClick = { viewModel.borrarCita(cita.id) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, null, tint = Color.Red.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
}

// Función auxiliar para saber si es el mismo día
fun isHoy(date1: Date, date2: Date): Boolean {
    val cal1 = Calendar.getInstance().apply { time = date1 }
    val cal2 = Calendar.getInstance().apply { time = date2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}