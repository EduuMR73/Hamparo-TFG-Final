package com.example.hamparo.ui.screen.dialogs

import android.app.TimePickerDialog
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.hamparo.data.model.FamiliarVinculado
import com.example.hamparo.ui.screen.admin.AdminViewModel
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import java.util.Calendar

// --- ESTILOS PREMIUM (LOOK & FEEL) ---

private val MedicalBlue = Color(0xFF00668B)
private val MedicalBg = Color(0xFFF5F7FA)

/**
 * Campo de texto con BORDES MÁS DEFINIDOS (Petición: Inputs menos tímidos)
 */
@Composable
fun ProTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    placeholder: String? = null,
    isError: Boolean = false
) {
    Column(modifier) {
        // Etiqueta fuera del input para mayor limpieza
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (isError) MaterialTheme.colorScheme.error else Color.Black, // Etiqueta negra para contraste
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
            )
        }

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = singleLine,
            isError = isError,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color.White,
                focusedContainerColor = Color.White,
                // 🔥 BORDE VISIBLE SIEMPRE (Gris medio cuando no tiene foco)
                unfocusedBorderColor = if (isError) MaterialTheme.colorScheme.error else Color(0xFFBDBDBD),
                focusedBorderColor = if (isError) MaterialTheme.colorScheme.error else Color.Black, // Negro al enfocar
                cursorColor = Color.Black,
                errorBorderColor = MaterialTheme.colorScheme.error,
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black
            ),
            leadingIcon = if (leadingIcon != null) {
                { Icon(leadingIcon, null, tint = if (isError) MaterialTheme.colorScheme.error else Color.Gray) }
            } else null,
            trailingIcon = trailingIcon,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            placeholder = if (placeholder != null) {
                { Text(placeholder, color = Color.LightGray) }
            } else null
        )
    }
}

/**
 * Tarjeta grande para seleccionar opciones (Pauta Fija vs Si Precisa)
 */
@Composable
fun SelectionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = MedicalBlue
) {
    val borderColor by animateColorAsState(if (isSelected) accentColor else Color.LightGray, label = "border")
    val bgColor by animateColorAsState(if (isSelected) accentColor.copy(alpha = 0.08f) else Color.White, label = "bg")
    val contentColor by animateColorAsState(if (isSelected) accentColor else Color.Gray, label = "content")
    val elevation = if(isSelected) 4.dp else 0.dp

    Card(
        modifier = modifier
            .height(100.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(if(isSelected) 2.dp else 1.dp, borderColor),
        elevation = CardDefaults.cardElevation(elevation)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(4.dp))
            Text(title, fontWeight = FontWeight.Bold, color = if (isSelected) accentColor else Color.Black, style = MaterialTheme.typography.bodyMedium)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = Color.Gray, textAlign = TextAlign.Center)
        }
    }
}

// --- 1. DIÁLOGO MEDICINA (VERSIÓN 12 PARÁMETROS) ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogoNuevaMedicina(
    adminViewModel: AdminViewModel,
    onDismiss: () -> Unit,
    // 👇 ESTO ES LO QUE TE FALTA: TIENE QUE HABER 12 TIPOS AQUÍ
    onGuardar: (String, String, com.example.hamparo.data.model.TipoFrecuencia, String, Int, String, Float, Int, String, String, String, String) -> Unit
) {
    var nombre by rememberSaveable { mutableStateOf("") }
    var dosisTexto by rememberSaveable { mutableStateOf("") }
    var cantidadPorToma by rememberSaveable { mutableStateOf("1") }
    var stock by rememberSaveable { mutableStateOf("") }
    var modoSeleccionado by rememberSaveable { mutableStateOf(com.example.hamparo.data.model.TipoFrecuencia.INTERVALO) }
    var horasIntervalo by rememberSaveable { mutableStateOf("8") }
    var horaInicio by rememberSaveable { mutableStateOf("08:00") }
    var indicacionTexto by rememberSaveable { mutableStateOf("") }

    var intentoGuardar by remember { mutableStateOf(false) }

    // VARIABLES EXTRA (API)
    var descripcionApi by remember { mutableStateOf("") }
    var advertenciasApi by remember { mutableStateOf("") }
    var prospectoApi by remember { mutableStateOf("") }
    var codigoNacionalApi by remember { mutableStateOf("") }

    val focusRequesterDosis = remember { FocusRequester() }
    val focusRequesterCantidad = remember { FocusRequester() }
    val focusRequesterStock = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    val calendar = Calendar.getInstance()
    val timePickerDialog = TimePickerDialog(
        context,
        { _, hour: Int, minute: Int -> horaInicio = String.format("%02d:%02d", hour, minute) },
        calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true
    )

    fun intentarGuardar() {
        intentoGuardar = true
        val tieneNombre = nombre.isNotBlank()
        val cantidadOk = (cantidadPorToma.toFloatOrNull() ?: 0f) > 0f
        val tieneFrecuencia = when (modoSeleccionado) {
            com.example.hamparo.data.model.TipoFrecuencia.INTERVALO -> (horasIntervalo.toIntOrNull() ?: 0) > 0 && horaInicio.isNotBlank()
            com.example.hamparo.data.model.TipoFrecuencia.SI_PRECISA -> indicacionTexto.isNotBlank()
            else -> false
        }

        if (tieneNombre && cantidadOk && tieneFrecuencia) {
            focusManager.clearFocus()
            onGuardar(
                nombre,
                dosisTexto,
                modoSeleccionado,
                indicacionTexto,
                horasIntervalo.toIntOrNull() ?: 0,
                horaInicio,
                cantidadPorToma.toFloatOrNull() ?: 1f,
                stock.toIntOrNull() ?: 0,
                descripcionApi,
                advertenciasApi,
                prospectoApi,
                codigoNacionalApi
            )
            nombre = ""; dosisTexto = ""; stock = ""; intentoGuardar = false
            descripcionApi = ""; advertenciasApi = ""; prospectoApi = ""; codigoNacionalApi = ""
            adminViewModel.limpiarMedicamentoEscaneado()
        } else {
            Toast.makeText(context, "Revisa los campos obligatorios", Toast.LENGTH_SHORT).show()
        }
    }

    val scannerLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            adminViewModel.escanearParaFormulario(result.contents)
        }
    }

    val medEncontrado by adminViewModel.medicamentoEscaneado.collectAsState()

    LaunchedEffect(medEncontrado) {
        if (medEncontrado != null) {
            nombre = medEncontrado!!.nombre
            dosisTexto = medEncontrado!!.dosis
            // COPIAMOS LOS DATOS RICOS
            descripcionApi = medEncontrado!!.descripcion ?: ""
            advertenciasApi = medEncontrado!!.advertencias ?: ""
            prospectoApi = medEncontrado!!.prospectoUrl ?: ""
            codigoNacionalApi = medEncontrado!!.codigoNacional ?: ""
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.6f)).clickable(enabled = false) {}, contentAlignment = Alignment.Center) {
            Card(
                modifier = Modifier.fillMaxWidth(0.95f).padding(vertical = 24.dp).animateContentSize(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(2.dp, Color.Black)
            ) {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Nueva Pauta", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, null) }
                    }
                    Spacer(Modifier.height(24.dp))

                    ProTextField(
                        value = nombre, onValueChange = { nombre = it }, label = "Medicamento *",
                        leadingIcon = Icons.Default.Medication, placeholder = "Nombre del fármaco",
                        isError = intentoGuardar && nombre.isBlank(),
                        trailingIcon = { IconButton({ scannerLauncher.launch(ScanOptions()) }) { Icon(Icons.Default.QrCodeScanner, null, tint = MedicalBlue) } },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusRequesterDosis.requestFocus() })
                    )

                    // Aviso visual si hay info extra
                    if (descripcionApi.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudDownload, null, tint = MedicalBlue, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Información oficial vinculada", fontSize = 12.sp, color = MedicalBlue, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    ProTextField(value = dosisTexto, onValueChange = { dosisTexto = it }, label = "Dosis", modifier = Modifier.focusRequester(focusRequesterDosis), placeholder = "Ej: 600mg")
                    Spacer(Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ProTextField(value = cantidadPorToma, onValueChange = { cantidadPorToma = it }, label = "Cant. *", modifier = Modifier.weight(1f).focusRequester(focusRequesterCantidad), isError = intentoGuardar && (cantidadPorToma.toFloatOrNull() ?: 0f) <= 0f)
                        ProTextField(value = stock, onValueChange = { if(it.all { c -> c.isDigit() }) stock = it }, label = "Stock", modifier = Modifier.weight(1f).focusRequester(focusRequesterStock))
                    }

                    Spacer(Modifier.height(24.dp))
                    Text("Frecuencia", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SelectionCard("Pauta Fija", "Cada X horas", Icons.Outlined.Timer, modoSeleccionado == com.example.hamparo.data.model.TipoFrecuencia.INTERVALO, { modoSeleccionado = com.example.hamparo.data.model.TipoFrecuencia.INTERVALO }, Modifier.weight(1f))
                        SelectionCard("Si Precisa", "Emergencia", Icons.Outlined.Warning, modoSeleccionado == com.example.hamparo.data.model.TipoFrecuencia.SI_PRECISA, { modoSeleccionado = com.example.hamparo.data.model.TipoFrecuencia.SI_PRECISA }, Modifier.weight(1f), Color(0xFFD32F2F))
                    }

                    Spacer(Modifier.height(20.dp))
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MedicalBg).padding(16.dp)) {
                        if (modoSeleccionado == com.example.hamparo.data.model.TipoFrecuencia.INTERVALO) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Repetir cada", fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.width(12.dp))
                                    BasicTextField(
                                        value = horasIntervalo, onValueChange = { if(it.length<=2 && it.all { c->c.isDigit() }) horasIntervalo = it },
                                        textStyle = TextStyle(textAlign = TextAlign.Center, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MedicalBlue),
                                        modifier = Modifier.width(60.dp).background(Color.White, RoundedCornerShape(8.dp)).border(1.dp, Color.Gray, RoundedCornerShape(8.dp)).padding(8.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text("horas")
                                }
                                Spacer(Modifier.height(16.dp))
                                ProTextField(value = horaInicio, onValueChange = { horaInicio = it }, label = "Hora inicio", trailingIcon = { IconButton({ timePickerDialog.show() }) { Icon(Icons.Default.AccessTime, null) } })
                            }
                        } else {
                            ProTextField(value = indicacionTexto, onValueChange = { indicacionTexto = it }, label = "Motivo de la toma *", placeholder = "Ej: Dolor, fiebre")
                        }
                    }
                    Spacer(Modifier.height(32.dp))
                    Button(onClick = { intentarGuardar() }, Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Black)) { Text("GUARDAR PAUTA", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}


// --- 2. DIÁLOGO FOTO (Simple) ---
@Composable
fun DialogoEditarFoto(urlActual: String, onDismiss: () -> Unit, onGuardar: (String) -> Unit) {
    var url by rememberSaveable { mutableStateOf(urlActual) }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { if (it != null) onGuardar(it.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Foto de Perfil") },
        text = {
            Column {
                OutlinedButton(onClick = { galleryLauncher.launch("image/*") }, Modifier.fillMaxWidth()) { Text("ABRIR GALERÍA") }
                Spacer(Modifier.height(16.dp))
                ProTextField(value = url, onValueChange = { url = it }, label = "URL Imagen (Opcional)")
            }
        },
        confirmButton = { Button(onClick = { onGuardar(url) }, colors = ButtonDefaults.buttonColors(containerColor = Color.Black)) { Text("GUARDAR") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        containerColor = Color.White, shape = RoundedCornerShape(24.dp)
    )
}

// --- 3. DIÁLOGO FAMILIAR ---

@Composable
fun DialogoFamiliar(familiar: FamiliarVinculado?, onDismiss: () -> Unit, onGuardar: (String, String, String) -> Unit) {
    var n by remember { mutableStateOf(familiar?.nombre ?: "") }
    var p by remember { mutableStateOf(familiar?.parentesco ?: "") }
    var t by remember { mutableStateOf(familiar?.telefono ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(2.dp, Color.Black),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(if (familiar != null) "Editar Familiar" else "Nuevo Familiar", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                ProTextField(n, { n = it }, "Nombre", keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words))
                ProTextField(p, { p = it }, "Parentesco")
                ProTextField(t, { t = it }, "Teléfono", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                Spacer(Modifier.height(8.dp))
                Button(onClick = { if (n.isNotEmpty()) onGuardar(n, p, t) }, Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Black)) { Text("GUARDAR") }
            }
        }
    }
}

// --- 4. DIÁLOGO CITA ---

@Composable
fun DialogoNuevaCita(onDismiss: () -> Unit, onGuardar: (String, String, String, String) -> Unit) {
    var f by remember { mutableStateOf("") }; var h by remember { mutableStateOf("") }
    var e by remember { mutableStateOf("") }; var c by remember { mutableStateOf("") }
    val ctx = LocalContext.current
    val cal = Calendar.getInstance()
    val dD = android.app.DatePickerDialog(ctx, { _, y, m, d -> f = "%02d/%02d/%d".format(d, m+1, y) }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
    val tD = TimePickerDialog(ctx, { _, hour, min -> h = "%02d:%02d".format(hour, min) }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(2.dp, Color.Black),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Nueva Cita", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                ProTextField(f, {}, "Fecha", trailingIcon = { IconButton({ dD.show() }) { Icon(Icons.Default.CalendarToday, null) } })
                ProTextField(h, {}, "Hora", trailingIcon = { IconButton({ tD.show() }) { Icon(Icons.Default.AccessTime, null) } })
                ProTextField(e, { e = it }, "Especialista")
                ProTextField(c, { c = it }, "Lugar / Centro")
                Spacer(Modifier.height(8.dp))
                Button(onClick = { if(f.isNotEmpty()) onGuardar(f,h,e,c) }, Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Black)) { Text("GUARDAR") }
            }
        }
    }
}

// 5. DIÁLOGO REGISTRO (REDISEÑADO A FONDO - FICHA PROFESIONAL)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogoNuevoRegistro(onDismiss: () -> Unit, onGuardar: (com.example.hamparo.data.model.TipoMedicion, String, String, String, String, String) -> Unit) {
    // 1. ORDENAMOS ALFABÉTICAMENTE LOS TIPOS
    val tiposOrdenados = remember { com.example.hamparo.data.model.TipoMedicion.values().sortedBy { it.label } }

    var tipo by remember { mutableStateOf(com.example.hamparo.data.model.TipoMedicion.TENSION) }
    var f by remember { mutableStateOf("") }; var h by remember { mutableStateOf("") }
    var v1 by remember { mutableStateOf("") }; var v2 by remember { mutableStateOf("") }; var n by remember { mutableStateOf("") }

    val ctx = LocalContext.current
    val cal = Calendar.getInstance()

    LaunchedEffect(Unit) {
        f = "%02d/%02d/%d".format(cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH)+1, cal.get(Calendar.YEAR))
        h = "%02d:%02d".format(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
    }

    val dateD = android.app.DatePickerDialog(ctx, { _, y, m, d -> f = "%02d/%02d/%d".format(d, m + 1, y) }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
    val timeD = TimePickerDialog(ctx, { _, hour, min -> h = "%02d:%02d".format(hour, min) }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(2.dp, Color.Black)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // CABECERA
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Nuevo Registro", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, null) }
                }

                // SELECTOR DE TIPO (Chips de Alto Contraste)
                Text("Tipo de Medición", style = MaterialTheme.typography.labelMedium, color = Color.Gray, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    tiposOrdenados.forEach { t ->
                        val selected = tipo == t
                        FilterChip(
                            selected = selected,
                            enabled = true,
                            onClick = { tipo = t; v1=""; v2="" },
                            label = {
                                Text(
                                    t.label,
                                    fontWeight = if(selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if(selected) Color.White else Color.Black
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color.Black, // ⚫ Fondo Negro seleccionado
                                selectedLabelColor = Color.White,
                                containerColor = Color.White,
                                labelColor = Color.Black
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = if(selected) Color.Black else Color.LightGray,
                                borderWidth = 1.dp,
                                enabled = true,
                                selected = selected
                            ),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }

                HorizontalDivider(color = Color.LightGray.copy(0.3f))

                // FECHA Y HORA
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.weight(1f)) {
                        ProTextField(f, {}, "Fecha", trailingIcon = { IconButton({ dateD.show() }) { Icon(Icons.Default.DateRange, null) } })
                    }
                    Box(Modifier.weight(1f)) {
                        ProTextField(h, {}, "Hora", trailingIcon = { IconButton({ timeD.show() }) { Icon(Icons.Default.Schedule, null) } })
                    }
                }

                // VALORES
                if (tipo == com.example.hamparo.data.model.TipoMedicion.TENSION) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ProTextField(v1, { v1 = it }, "Sistólica (Alta)", Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        ProTextField(v2, { v2 = it }, "Diastólica (Baja)", Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    }
                } else {
                    ProTextField(
                        value = v1,
                        onValueChange = { v1 = it },
                        label = "Valor (${tipo.unidad})",
                        keyboardOptions = KeyboardOptions(keyboardType = if(tipo.unidad.isEmpty()) KeyboardType.Text else KeyboardType.Decimal),
                        placeholder = "Introducir dato"
                    )
                }

                // NOTAS
                ProTextField(n, { n = it }, "Notas Adicionales", singleLine = false, placeholder = "Observaciones...")

                Spacer(Modifier.height(8.dp))

                // BOTÓN GUARDAR
                Button(
                    onClick = { if (v1.isNotEmpty()) onGuardar(tipo, v1, v2, f, h, n) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black), // Botón Negro sólido
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("GUARDAR REGISTRO", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

// --- 6. DIÁLOGO CURA ---

@Composable
fun DialogoNuevaCura(onDismiss: () -> Unit, onGuardar: (String, String, Int, String, String, String?) -> Unit) {
    var tipo by remember { mutableStateOf("Úlcera (UPP)") }; var zona by remember { mutableStateOf("") }
    var dolor by remember { mutableFloatStateOf(0f) }; var estado by remember { mutableStateOf("") }
    var mat by remember { mutableStateOf("") }; var foto by remember { mutableStateOf<String?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { if (it != null) foto = it.toString() }
    val tipos = listOf("Arañazo", "Dermatitis", "Herida Qx", "Micótica", "Quemadura", "Traumática", "Úlcera (UPP)")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(2.dp, Color.Black),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Registrar Cura", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                Column {
                    Text("Tipo de Herida", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Row(Modifier.horizontalScroll(rememberScrollState())) {
                        tipos.forEach { t ->
                            FilterChip(
                                selected = tipo == t,
                                enabled = true, // ✅ AÑADIDO PARA EVITAR ERRORES
                                onClick = { tipo = t },
                                label = { Text(t) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color.Black, selectedLabelColor = Color.White),
                                border = FilterChipDefaults.filterChipBorder(enabled = true, selected = tipo == t, borderColor = if(tipo==t) Color.Black else Color.LightGray),
                                modifier = Modifier.padding(end=4.dp)
                            )
                        }
                    }
                }

                ProTextField(zona, { zona = it }, "Zona (Ej: Talón)")
                ProTextField(estado, { estado = it }, "Estado (Ej: Roja/Necrosis)")

                Column {
                    Text("Nivel de Dolor: ${dolor.toInt()}/10", fontWeight = FontWeight.Bold)
                    Slider(value = dolor, onValueChange = { dolor = it }, valueRange = 0f..10f, steps = 9, colors = SliderDefaults.colors(thumbColor = Color.Black, activeTrackColor = Color.Black))
                }

                ProTextField(mat, { mat = it }, "Materiales Usados", singleLine = false)

                OutlinedButton(
                    onClick = { launcher.launch("image/*") },
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)
                ) {
                    Icon(Icons.Default.CameraAlt, null); Spacer(Modifier.width(8.dp)); Text(if (foto == null) "ADJUNTAR FOTO" else "FOTO OK ✅")
                }

                Button(onClick = { if (zona.isNotEmpty()) onGuardar(tipo, zona, dolor.toInt(), estado, mat, foto) }, Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Black)) { Text("GUARDAR") }
            }
        }
    }
}