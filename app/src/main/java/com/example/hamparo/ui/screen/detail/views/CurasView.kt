package com.example.hamparo.ui.screen.detail.views

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.SentimentDissatisfied
import androidx.compose.material.icons.rounded.SentimentNeutral
import androidx.compose.material.icons.rounded.SentimentVerySatisfied
import androidx.compose.material.icons.rounded.Sick
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.hamparo.data.model.Cura
import com.example.hamparo.data.model.Paciente
import com.example.hamparo.ui.screen.detail.PatientDetailViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CurasView(
    paciente: Paciente,
    viewModel: PatientDetailViewModel
) {
    var mostrarDialogo by remember { mutableStateOf(false) }
    var fotoParaVerEnGrande by remember { mutableStateOf<String?>(null) }

    // --- PESTAÑAS: 0 = ACTIVAS, 1 = HISTORIAL ---
    var tabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("ACTIVAS", "HISTORIAL")

    // Filtramos la lista según la pestaña seleccionada
    val curasMostradas = remember(paciente.historialCuras, tabIndex) {
        if (tabIndex == 0) {
            paciente.historialCuras.filter { !it.archivada } // Solo activas
        } else {
            paciente.historialCuras.filter { it.archivada } // Solo historial
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 1. BARRA DE PESTAÑAS SUPERIOR
        TabRow(selectedTabIndex = tabIndex, containerColor = Color.White) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    text = { Text(title, fontWeight = FontWeight.Bold) },
                    selected = tabIndex == index,
                    onClick = { tabIndex = index },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = Color.Gray
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (curasMostradas.isEmpty()) {
                // ESTADO VACÍO
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (tabIndex == 0) Icons.Default.Healing else Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.LightGray.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (tabIndex == 0) "No hay curas activas" else "Historial vacío",
                        color = Color.Gray,
                        style = MaterialTheme.typography.titleMedium
                    )

                    // Solo mostramos botón de añadir en la pestaña Activas si está vacía
                    if (tabIndex == 0) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { mostrarDialogo = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Add, null)
                            Spacer(Modifier.width(8.dp))
                            Text("AÑADIR CURA")
                        }
                    }
                }
            } else {
                // LISTA DE CURAS
                LazyColumn(
                    contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    items(
                        items = curasMostradas,
                        key = { it.id }
                    ) { cura ->
                        SwipeableCuraItem(
                            cura = cura,
                            esHistorial = (tabIndex == 1),
                            // 🔥 ACCIÓN REVERSIBLE: Archivar o Recuperar
                            onSwipeAction = {
                                viewModel.toggleArchivarCura(cura)
                            },
                            // 🔥 ACCIÓN DESTRUCTIVA: Borrar (botón interno)
                            onPermanentDelete = {
                                viewModel.borrarCura(cura.id)
                            },
                            onImageClick = { fotoParaVerEnGrande = it }
                        )
                    }
                }
            }

            // BOTÓN FLOTANTE (+) (Solo visible en pestaña Activas)
            if (tabIndex == 0) {
                FloatingActionButton(
                    onClick = { mostrarDialogo = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(24.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    elevation = FloatingActionButtonDefaults.elevation(8.dp)
                ) {
                    Icon(Icons.Default.Add, "Añadir cura", tint = Color.White)
                }
            }
        }
    }

    // --- DIÁLOGOS ---

    // 1. DIÁLOGO DE REGISTRO
    if (mostrarDialogo) {
        DialogoRegistroCuraInternal(
            onDismiss = { mostrarDialogo = false },
            onGuardar = { tipo, zona, dolor, estado, mat, foto ->
                viewModel.guardarCura(tipo, zona, dolor, estado, mat, foto)
                mostrarDialogo = false
            }
        )
    }

    // 2. VISOR DE FOTO A PANTALLA COMPLETA
    if (fotoParaVerEnGrande != null) {
        Dialog(
            onDismissRequest = { fotoParaVerEnGrande = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable { fotoParaVerEnGrande = null },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(fotoParaVerEnGrande)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Foto ampliada",
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Fit
                )
                IconButton(
                    onClick = { fotoParaVerEnGrande = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(Color.Black.copy(0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, null, tint = Color.White)
                }
            }
        }
    }
}

//  WRAPPER PARA SWIPE (ARCHIVAR O RECUPERAR)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableCuraItem(
    cura: Cura,
    esHistorial: Boolean,
    onSwipeAction: () -> Unit,
    onPermanentDelete: () -> Unit,
    onImageClick: (String) -> Unit
) {
    // Si es historial, fondo VERDE (Recuperar). Si es activa, fondo AZUL/GRIS (Archivar).
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it != SwipeToDismissBoxValue.Settled) {
                onSwipeAction()
                true
            } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color = if (esHistorial) Color(0xFF4CAF50) else Color(0xFF1976D2) // Verde o Azul
            val icon = if (esHistorial) Icons.Rounded.Restore else Icons.Rounded.Archive

            Box(
                Modifier
                    .fillMaxSize()
                    .background(color, RoundedCornerShape(16.dp))
                    .padding(horizontal = 20.dp),
                contentAlignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                Icon(icon, null, tint = Color.White)
            }
        },
        content = {
            CuraCard(cura, onImageClick, onPermanentDelete)
        }
    )
}

// TARJETA DE CURA (CON ICONOS DE DOLOR Y BOTÓN BORRAR)

@Composable
fun CuraCard(
    cura: Cura,
    onImageClick: (String) -> Unit,
    onPermanentDelete: () -> Unit
) {
    var expandido by remember { mutableStateOf(false) }
    val formatoFecha = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expandido = !expandido },
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // CABECERA
            Row(verticalAlignment = Alignment.Top) {

                // 🔥 CARITAS DE DOLOR
                val (iconVector, colorDolor) = when (cura.escalaDolor) {
                    0 -> Icons.Rounded.SentimentVerySatisfied to Color(0xFF4CAF50)
                    in 1..3 -> Icons.Rounded.SentimentNeutral to Color(0xFFFF9800)
                    in 4..6 -> Icons.Rounded.SentimentDissatisfied to Color(0xFFFB8C00)
                    else -> Icons.Rounded.Sick to Color(0xFFD32F2F)
                }

                Surface(
                    shape = CircleShape,
                    color = colorDolor.copy(alpha = 0.1f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(iconVector, null, tint = colorDolor, modifier = Modifier.size(28.dp))
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(cura.zona, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(8.dp))
                        if(cura.imagenUri != null) Icon(Icons.Default.PhotoCamera, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                    }
                    if (cura.tipoHerida.isNotEmpty()) {
                        Text(
                            text = cura.tipoHerida,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = formatoFecha.format(Date(cura.fecha)),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                Icon(
                    imageVector = if (expandido) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = Color.Gray
                )
            }

            // CONTENIDO EXPANDIDO
            AnimatedVisibility(visible = expandido) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Divider(color = Color.LightGray.copy(0.2f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // 1. NIVEL DE DOLOR CON TEXTO
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Speed, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Nivel de Dolor: ${cura.escalaDolor}/10",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // 2. ESTADO
                    if (cura.estado.isNotEmpty()) {
                        InfoRow(icon = Icons.Default.Info, label = "Estado", text = cura.estado)
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // 3. MATERIALES
                    if (cura.materiales.isNotEmpty()) {
                        InfoRow(icon = Icons.Default.MedicalServices, label = "Materiales", text = cura.materiales)
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // 4. FOTO
                    if (cura.imagenUri != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("EVIDENCIA FOTOGRÁFICA", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))

                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(cura.imagenUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onImageClick(cura.imagenUri) },
                            contentScale = ContentScale.Crop
                        )
                        Text(
                            "Toca la imagen para ampliar",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.LightGray,
                            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp)
                        )
                    }

                    // 🔥 BOTÓN DE BORRADO DEFINITIVO
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(
                            onClick = onPermanentDelete,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Eliminar definitivamente")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, null, modifier = Modifier.size(16.dp).padding(top = 2.dp), tint = Color.Gray)
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.Gray)
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

//  DIÁLOGO INTERNO - TUS EJEMPLOS EXACTOS Y MAYÚSCULAS

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogoRegistroCuraInternal(
    onDismiss: () -> Unit,
    onGuardar: (String, String, Int, String, String, String?) -> Unit
) {
    val tiposHerida = remember {
        listOf(
            "Arañazo", "Dermatitis", "Herida", "Quemadura", "Úlcera por presión",
            "Quirúrgica", "Escara", "Hematoma", "Infección"
        ).sorted()
    }

    var tipoHeridaSeleccionada by remember { mutableStateOf(tiposHerida[0]) }
    var zona by remember { mutableStateOf("") }
    var estado by remember { mutableStateOf("") }
    var materiales by remember { mutableStateOf("") }
    var dolor by remember { mutableFloatStateOf(0f) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    // 1. OBTENEMOS EL GESTOR DE FOCO
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            val flag = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            try {
                context.contentResolver.takePersistableUriPermission(it, flag)
            } catch (e: Exception) { e.printStackTrace() }
            imageUri = it
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Registrar Cura",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(Modifier.height(16.dp))

                // TIPO DE HERIDA
                Text("Tipo de Herida", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                Spacer(Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(tiposHerida) { tipo ->
                        FilterChip(
                            selected = (tipo == tipoHeridaSeleccionada),
                            onClick = { tipoHeridaSeleccionada = tipo },
                            label = { Text(tipo) },
                            leadingIcon = if (tipo == tipoHeridaSeleccionada) {
                                { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // --- CAMPO 1: ZONA ---
                OutlinedTextField(
                    value = zona,
                    onValueChange = { zona = it },
                    label = { Text("Zona") },
                    // 🔥 TU EJEMPLO EXACTO (Aparece al tocar el campo)
                    placeholder = { Text("Ej: Talon Izq", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        capitalization = KeyboardCapitalization.Sentences,
                        autoCorrect = true,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                )

                Spacer(Modifier.height(12.dp))

                // --- CAMPO 2: ASPECTO O ESTADO ---
                OutlinedTextField(
                    value = estado,
                    onValueChange = { estado = it },
                    label = { Text("Aspecto o Estado") },
                    placeholder = { Text("Necrotica, Granulado...", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        capitalization = KeyboardCapitalization.Sentences,
                        autoCorrect = true,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                )

                Spacer(Modifier.height(12.dp))

                // --- CAMPO 3: MATERIALES ---
                OutlinedTextField(
                    value = materiales,
                    onValueChange = { materiales = it },
                    label = { Text("Materiales y Procedimiento") },
                    placeholder = { Text("Ej: Putilon +Urgo ag + Fiber + Talonera + Vendaje", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = false,
                    maxLines = 5,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        capitalization = KeyboardCapitalization.Sentences,
                        autoCorrect = true,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    )
                )

                Spacer(Modifier.height(16.dp))

                // 4. DOLOR
                Text("Nivel de dolor: ${dolor.toInt()}/10", style = MaterialTheme.typography.labelLarge)
                Slider(
                    value = dolor,
                    onValueChange = { dolor = it },
                    valueRange = 0f..10f,
                    steps = 9,
                    colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary)
                )

                Spacer(Modifier.height(16.dp))

                // 5. FOTO
                OutlinedButton(
                    onClick = { launcher.launch(arrayOf("image/*")) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (imageUri != null) "Foto seleccionada (Cambiar)" else "Adjuntar Foto")
                }

                if (imageUri != null) {
                    Spacer(Modifier.height(8.dp))
                    AsyncImage(
                        model = imageUri,
                        contentDescription = null,
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .align(Alignment.CenterHorizontally),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(Modifier.height(24.dp))

                // BOTONES
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (zona.isNotBlank()) {
                                onGuardar(tipoHeridaSeleccionada, zona, dolor.toInt(), estado, materiales, imageUri?.toString())
                            }
                        },
                        enabled = zona.isNotBlank()
                    ) {
                        Text("Guardar")
                    }
                }
            }
        }
    }
}