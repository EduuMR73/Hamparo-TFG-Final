package com.example.hamparo.ui.screen.detail.views

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.hamparo.data.model.*
import com.example.hamparo.ui.screen.detail.PatientDetailViewModel
import com.example.hamparo.ui.screen.detail.components.*
import java.util.Calendar

@Composable
fun FichaTecnicaView(
    paciente: Paciente,
    viewModel: PatientDetailViewModel,
    isEditing: Boolean,
    onAddFamiliar: () -> Unit,
    onEditFamiliar: (FamiliarVinculado) -> Unit,
    onEditFoto: () -> Unit
) {
    var tabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Datos Personales", "Valoración Clínica")

    Column(Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = tabIndex,
            containerColor = Color.White,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = tabIndex == index,
                    onClick = { tabIndex = index },
                    text = { Text(title, fontWeight = if (tabIndex == index) FontWeight.Bold else FontWeight.Normal) },
                    icon = { Icon(if (index == 0) Icons.Default.Person else Icons.Default.MedicalServices, null) }
                )
            }
        }
        when (tabIndex) {
            0 -> if (isEditing) TabPersonalEditar(paciente, viewModel, onAddFamiliar, onEditFamiliar, onEditFoto) else TabPersonalLectura(paciente)
            1 -> if (isEditing) TabClinicaEditar(paciente, viewModel) else TabClinicaLectura(paciente)
        }
    }
}

// PESTAÑAS LECTURA Y EDICIÓN


@Composable
fun TabPersonalLectura(paciente: Paciente) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

        Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(70.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        if (paciente.fotoUrl.isNullOrEmpty()) {
                            Text(paciente.nombre.take(1), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        } else {
                            AsyncImage(model = paciente.fotoUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape))
                        }
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("${paciente.nombre} ${paciente.apellidos}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MeetingRoom, null, Modifier.size(16.dp), tint = Color.Gray)
                        Spacer(Modifier.width(4.dp))
                        Text(if (paciente.habitacion.isNotEmpty()) "Hab: ${paciente.habitacion}" else "Sin asignar", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        DetailCard("Datos Demográficos") {
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DatoLectura("DNI / NIF", paciente.dni)
                    DatoLectura("Estado Civil", paciente.estadoCivil.label)
                    DatoLectura("Género", paciente.genero.label)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DatoLectura("Fecha Nac.", paciente.fechaNacimiento)
                    DatoLectura("SIP / SS", paciente.sip)
                    DatoLectura("NHC Interno", paciente.nhc)
                }
            }
            Spacer(Modifier.height(8.dp))
            DatoLectura("Dirección Empadronamiento", paciente.direccion)
        }

        //  NO MOSTRAMOS EL PIN AQUÍ PARA NO ENSUCIAR LA VISTA DE LECTURA

        DetailCard("Personas de Contacto") {
            if (paciente.familiaresVinculados.isEmpty()) {
                Text("No hay familiares vinculados.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
            } else {
                paciente.familiaresVinculados.forEach { familiar ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Phone, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(if(familiar.parentesco.isNotEmpty()) "${familiar.nombre} (${familiar.parentesco})" else familiar.nombre, fontWeight = FontWeight.Bold)
                            Text(familiar.telefono.ifEmpty { familiar.email }, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f))
                }
            }
        }
        Spacer(Modifier.height(60.dp))
    }
}

@Composable
fun TabPersonalEditar(
    paciente: Paciente,
    viewModel: PatientDetailViewModel,
    onAddFamiliar: () -> Unit,
    onEditFamiliar: (FamiliarVinculado) -> Unit,
    onEditFoto: () -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).background(Color.White).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

        Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                // FOTO EDITABLE
                Box(contentAlignment = Alignment.BottomEnd) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(70.dp).clickable { onEditFoto() }) {
                        Box(contentAlignment = Alignment.Center) {
                            if (paciente.fotoUrl.isNullOrEmpty()) {
                                Text(paciente.nombre.take(1), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            } else {
                                AsyncImage(model = paciente.fotoUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape))
                            }
                        }
                    }
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.PhotoCamera, null, tint = Color.White, modifier = Modifier.padding(4.dp))
                    }
                }

                Spacer(Modifier.width(16.dp))

                Column {
                    OutlinedTextField(
                        value = paciente.nombre, onValueChange = { viewModel.actualizarDato(paciente.copy(nombre = it)) },
                        label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next, capitalization = KeyboardCapitalization.Words)
                    )
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = paciente.apellidos, onValueChange = { viewModel.actualizarDato(paciente.copy(apellidos = it)) },
                        label = { Text("Apellidos") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next, capitalization = KeyboardCapitalization.Words)
                    )
                }
            }
            OutlinedTextField(
                value = paciente.habitacion, onValueChange = { viewModel.actualizarDato(paciente.copy(habitacion = it)) },
                label = { Text("Habitación / Cama") }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
        }

        // 🔥🔥 NUEVA SECCIÓN: SEGURIDAD (PIN) 🔥🔥
        var passwordVisible by remember { mutableStateOf(false) }
        DetailCard("Seguridad de Acceso") {
            OutlinedTextField(
                value = paciente.pinDesbloqueo,
                onValueChange = { if(it.length <= 6) viewModel.actualizarDato(paciente.copy(pinDesbloqueo = it)) },
                label = { Text("PIN de Salida (Modo Abuelo)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                trailingIcon = {
                    val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, contentDescription = null)
                    }
                },
                supportingText = { Text("Código para salir de la pantalla del paciente (Defecto: 1234)") }
            )
        }

        DetailCard("Administrativo") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = paciente.dni, onValueChange = { viewModel.actualizarDato(paciente.copy(dni = it)) },
                    label = { Text("DNI") }, modifier = Modifier.weight(1f), singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
                OutlinedTextField(
                    value = paciente.sip, onValueChange = { viewModel.actualizarDato(paciente.copy(sip = it)) },
                    label = { Text("SIP") }, modifier = Modifier.weight(1f), singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next, keyboardType = KeyboardType.Number)
                )
            }
            Spacer(Modifier.height(8.dp))

            // ✅ USO DEL HELPER LOCAL PARA EVITAR ERRORES
            FichaDatePicker(
                label = "Fecha Nacimiento",
                fechaActual = paciente.fechaNacimiento,
                onFechaSeleccionada = { viewModel.actualizarDato(paciente.copy(fechaNacimiento = it)) }
            )

            Spacer(Modifier.height(8.dp))
            // ✅ USO DEL HELPER LOCAL PARA DROPDOWNS
            FichaDropdown("Género", paciente.genero.label, Genero.entries.map { it.label }) { label -> viewModel.actualizarDato(paciente.copy(genero = Genero.entries.find { it.label == label } ?: Genero.OTRO)) }
            Spacer(Modifier.height(8.dp))
            FichaDropdown("Estado Civil", paciente.estadoCivil.label, EstadoCivil.entries.map { it.label }) { label -> viewModel.actualizarDato(paciente.copy(estadoCivil = EstadoCivil.entries.find { it.label == label } ?: EstadoCivil.NO_CONST)) }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = paciente.direccion, onValueChange = { viewModel.actualizarDato(paciente.copy(direccion = it)) },
                label = { Text("Dirección / Provincia") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
            )
        }

        DetailCard("Gestión Contactos") {
            if (paciente.familiaresVinculados.isEmpty()) {
                Text("Sin contactos", color = Color.Gray, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
            } else {
                paciente.familiaresVinculados.forEach { fam ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(fam.nombre, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${fam.parentesco} • ${fam.telefono}", style = MaterialTheme.typography.bodySmall)
                        }
                        Row {
                            IconButton(onClick = { onEditFamiliar(fam) }) { Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) }
                            IconButton(onClick = {
                                val nuevaLista = paciente.familiaresVinculados.filter { it.id != fam.id }
                                viewModel.actualizarDato(paciente.copy(familiaresVinculados = nuevaLista))
                            }) { Icon(Icons.Default.Delete, null, tint = Color.Red, modifier = Modifier.size(20.dp)) }
                        }
                    }
                    HorizontalDivider()
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = onAddFamiliar, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("AÑADIR FAMILIAR")
            }
        }
        Spacer(Modifier.height(60.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TabClinicaLectura(paciente: Paciente) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

        if (paciente.tieneHipertension || paciente.tieneDiabetes || paciente.riesgoCaidas || paciente.riesgoUlceras || paciente.incapacitadoLegal || paciente.alergias.isNotEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("⚠️ ALERTAS E INCIDENCIAS", fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (paciente.alergias.isNotEmpty()) RiskBadge("ALERGIAS", Color(0xFFD32F2F), Color.White)
                        if (paciente.incapacitadoLegal) RiskBadge("INCAPACITADO", Color(0xFFC62828), Color.White)
                        if (paciente.polimedicado) RiskBadge("POLIMEDICADO", Color(0xFF1565C0), Color.White)
                        if (paciente.tieneDiabetes) RiskBadge("DIABETES", Color(0xFF1976D2), Color.White)
                        if (paciente.riesgoCaidas) RiskBadge("RIESGO CAÍDA", Color(0xFFFBC02D), Color.Black)
                        if (paciente.riesgoUlceras) RiskBadge("RIESGO ÚLCERAS", Color(0xFF7B1FA2), Color.White)
                        if (paciente.fuma) RiskBadge("FUMADOR", Color.Gray, Color.White)
                        if (paciente.bebeAlcohol) RiskBadge("ALCOHOL", Color.Gray, Color.White)
                    }
                    if (paciente.alergias.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text("Alergias: ${paciente.alergias.joinToString()}", style = MaterialTheme.typography.bodySmall)
                    }
                    if (paciente.riesgoUlceras && paciente.descripcionUlceras.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text("Úlceras: ${paciente.descripcionUlceras}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF7B1FA2))
                    }
                }
            }
        }

        DetailCard("Capacidades Sensoriales") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                DatoFisicoGrande(Icons.Default.Visibility, paciente.vision.label, "Visión")
                DatoFisicoGrande(Icons.AutoMirrored.Filled.VolumeUp, paciente.audicion.label, "Audición")
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Face, null, tint = Color.Gray)
                Spacer(Modifier.width(8.dp))
                DatoLectura("Estado Bucal", paciente.dentadura.label)
            }
        }

        DetailCard("Movilidad y Físico") {
            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    DatoLectura("Movilidad", paciente.tipoMovilidad.label)
                    DatoLectura("Dependencia", paciente.gradoDependencia)
                }
                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    BoxDatoFisico("Peso", "${paciente.peso} kg")
                    BoxDatoFisico("Altura", "${paciente.altura} cm")
                    val p = paciente.peso.toDoubleOrNull()
                    val a = paciente.altura.toDoubleOrNull()
                    if (p != null && a != null && a > 0) {
                        val imc = p / ((a/100)*(a/100))
                        BoxDatoFisico("IMC", "%.1f".format(imc))
                    }
                }
            }
        }

        DetailCard("Cuidados y Atención") {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().background(Color(0xFFE3F2FD), RoundedCornerShape(8.dp)).padding(12.dp)) {
                Icon(Icons.Default.Groups, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("GRUPO / COMEDOR", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Text(paciente.tipoGrupo.label, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("DIETA", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(paciente.tipoDieta.label, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text("INCONTINENCIA", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(paciente.tipoIncontinencia.label, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            Text("Registros Obligatorios:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(if(paciente.controlAbsorbentes) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, null, tint = if(paciente.controlAbsorbentes) Color(0xFF2E7D32) else Color.LightGray)
                    Spacer(Modifier.width(8.dp))
                    Text("Control Pañal", style = MaterialTheme.typography.bodyMedium)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(if(paciente.controlDeposiciones) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, null, tint = if(paciente.controlDeposiciones) Color(0xFF2E7D32) else Color.LightGray)
                    Spacer(Modifier.width(8.dp))
                    Text("Control Deposiciones", style = MaterialTheme.typography.bodyMedium)
                }
            }

            if (paciente.usaOxigeno) {
                Spacer(Modifier.height(16.dp))
                Surface(color = Color(0xFFE3F2FD), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Text("PRECISA OXIGENOTERAPIA", Modifier.padding(8.dp), fontWeight = FontWeight.Bold, color = Color(0xFF1565C0), textAlign = TextAlign.Center)
                }
            }
        }

        if (paciente.observaciones.isNotEmpty()) {
            DetailCard("Observaciones / Vacunación") {
                Text(paciente.observaciones, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(Modifier.height(60.dp))
    }
}

@Composable
fun TabClinicaEditar(paciente: Paciente, viewModel: PatientDetailViewModel) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).background(Color.White).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        DetailCard("Diagnóstico") {
            OutlinedTextField(
                value = paciente.diagnostico,
                onValueChange = { viewModel.actualizarDato(paciente.copy(diagnostico = it)) },
                label = { Text("Diagnóstico Principal") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )
        }
        DetailCard("Sensorial y Bucal") {
            FichaDropdown("Visión", paciente.vision.label, TipoVision.entries.map { it.label }) { l -> viewModel.actualizarDato(paciente.copy(vision = TipoVision.entries.find { it.label == l }!!)) }
            Spacer(Modifier.height(8.dp))
            FichaDropdown("Audición", paciente.audicion.label, TipoAudicion.entries.map { it.label }) { l -> viewModel.actualizarDato(paciente.copy(audicion = TipoAudicion.entries.find { it.label == l }!!)) }
            Spacer(Modifier.height(8.dp))
            FichaDropdown("Dentadura", paciente.dentadura.label, TipoDentadura.entries.map { it.label }) { l -> viewModel.actualizarDato(paciente.copy(dentadura = TipoDentadura.entries.find { it.label == l }!!)) }
        }
        DetailCard("Físico y Movilidad") {
            FichaDropdown("Movilidad", paciente.tipoMovilidad.label, TipoMovilidad.entries.map { it.label }) { l -> viewModel.actualizarDato(paciente.copy(tipoMovilidad = TipoMovilidad.entries.find { it.label == l }!!)) }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = paciente.peso,
                    onValueChange = { viewModel.actualizarDato(paciente.copy(peso = it)) },
                    label = { Text("Peso (kg)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
                )
                OutlinedTextField(
                    value = paciente.altura,
                    onValueChange = { viewModel.actualizarDato(paciente.copy(altura = it)) },
                    label = { Text("Altura (cm)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = paciente.gradoDependencia,
                onValueChange = { viewModel.actualizarDato(paciente.copy(gradoDependencia = it)) },
                label = { Text("Grado Dependencia") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
        }
        DetailCard("Estado y Riesgos") {
            OutlinedTextField(
                value = paciente.alergias.joinToString(", "),
                onValueChange = { viewModel.actualizarDato(paciente.copy(alergias = it.split(",").map { s -> s.trim() }.filter { s -> s.isNotEmpty() })) },
                label = { Text("Alergias") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    CheckItem("Hipertensión", paciente.tieneHipertension) { viewModel.actualizarDato(paciente.copy(tieneHipertension = it)) }
                    CheckItem("Diabetes", paciente.tieneDiabetes) { viewModel.actualizarDato(paciente.copy(tieneDiabetes = it)) }
                    CheckItem("Polimedicado", paciente.polimedicado) { viewModel.actualizarDato(paciente.copy(polimedicado = it)) }
                    CheckItem("Fumador", paciente.fuma) { viewModel.actualizarDato(paciente.copy(fuma = it)) }
                }
                Column(Modifier.weight(1f)) {
                    CheckItem("Riesgo Caídas", paciente.riesgoCaidas) { viewModel.actualizarDato(paciente.copy(riesgoCaidas = it)) }
                    CheckItem("Riesgo Úlceras", paciente.riesgoUlceras) { viewModel.actualizarDato(paciente.copy(riesgoUlceras = it)) }
                    CheckItem("Incapacitado", paciente.incapacitadoLegal) { viewModel.actualizarDato(paciente.copy(incapacitadoLegal = it)) }
                    CheckItem("Alcohol", paciente.bebeAlcohol) { viewModel.actualizarDato(paciente.copy(bebeAlcohol = it)) }
                }
            }
            if (paciente.riesgoUlceras) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = paciente.descripcionUlceras,
                    onValueChange = { viewModel.actualizarDato(paciente.copy(descripcionUlceras = it)) },
                    label = { Text("Descripción / Ubicación Úlceras") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
            }
        }
        DetailCard("Cuidados Diarios") {
            FichaDropdown("Grupo / Comedor", paciente.tipoGrupo.label, TipoGrupo.entries.map { it.label }) { l -> viewModel.actualizarDato(paciente.copy(tipoGrupo = TipoGrupo.entries.find { it.label == l }!!)) }
            Spacer(Modifier.height(8.dp))
            FichaDropdown("Dieta", paciente.tipoDieta.label, TipoDieta.entries.map { it.label }) { l -> viewModel.actualizarDato(paciente.copy(tipoDieta = TipoDieta.entries.find { it.label == l }!!)) }
            Spacer(Modifier.height(8.dp))
            FichaDropdown("Incontinencia", paciente.tipoIncontinencia.label, TipoIncontinencia.entries.map { it.label }) { l -> viewModel.actualizarDato(paciente.copy(tipoIncontinencia = TipoIncontinencia.entries.find { it.label == l }!!)) }
            Spacer(Modifier.height(8.dp))
            Text("Registros Necesarios:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Row {
                Column(Modifier.weight(1f)) { CheckItem("Control Absorbentes", paciente.controlAbsorbentes) { viewModel.actualizarDato(paciente.copy(controlAbsorbentes = it)) } }
                Column(Modifier.weight(1f)) { CheckItem("Control Deposiciones", paciente.controlDeposiciones) { viewModel.actualizarDato(paciente.copy(controlDeposiciones = it)) } }
            }
            CheckItem("Precisa Oxigenoterapia", paciente.usaOxigeno) { viewModel.actualizarDato(paciente.copy(usaOxigeno = it)) }
        }
        DetailCard("Observaciones") {
            OutlinedTextField(value = paciente.observaciones, onValueChange = { viewModel.actualizarDato(paciente.copy(observaciones = it)) }, label = { Text("Observaciones / Vacunas / Hábitos") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
        }
        Spacer(Modifier.height(60.dp))
    }
}

// COMPONENTES DE AYUDA LOCALES

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FichaDropdown(
    label: String,
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun FichaDatePicker(
    label: String,
    fechaActual: String,
    onFechaSeleccionada: (String) -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    if (fechaActual.isNotEmpty()) {
        try {
            val parts = fechaActual.split("/")
            if (parts.size == 3) {
                calendar.set(parts[2].toInt(), parts[1].toInt() - 1, parts[0].toInt())
            }
        } catch (e: Exception) { }
    } else {
        calendar.set(Calendar.YEAR, 1950)
    }

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val fechaFormateada = "%02d/%02d/%d".format(dayOfMonth, month + 1, year)
            onFechaSeleccionada(fechaFormateada)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    OutlinedTextField(
        value = fechaActual,
        onValueChange = {},
        label = { Text(label) },
        readOnly = true,
        enabled = false,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { datePickerDialog.show() },
        colors = OutlinedTextFieldDefaults.colors(
            disabledTextColor = MaterialTheme.colorScheme.onSurface,
            disabledBorderColor = MaterialTheme.colorScheme.outline,
            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        trailingIcon = {
            Icon(Icons.Default.CalendarToday, null)
        }
    )
}