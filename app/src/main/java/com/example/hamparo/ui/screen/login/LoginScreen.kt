package com.example.hamparo.ui.screen.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.hamparo.ui.navigation.AppScreens
import com.example.hamparo.ui.utils.BiometricAuth

@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: LoginViewModel = hiltViewModel()
) {
    // --- ESTADOS UI ---
    var selectedRole by remember { mutableStateOf<String?>(null) }
    var isRegistering by remember { mutableStateOf(false) }

    val loginExitoso by viewModel.loginExitoso.collectAsState()
    // Recuperamos los datos completos del paciente (JSON) que preparó el ViewModel
    val pacienteJson by viewModel.pacienteJson.collectAsState()

    val context = LocalContext.current


    // NAVEGACIÓN AUTOMÁTICA INTELIGENTE

    LaunchedEffect(loginExitoso) {
        if (loginExitoso) {
            val prefs = context.getSharedPreferences("HamparoPrefs", android.content.Context.MODE_PRIVATE)
            val rol = prefs.getString("ROL", "PACIENTE")
            val emailFamiliar = prefs.getString("FAMILIAR_EMAIL", "") ?: ""

            when (rol) {
                "CUIDADOR" -> {
                    // El cuidador (Ana) va a su panel de gestión global
                    navController.navigate(AppScreens.AdminHome.route) { popUpTo(0) }
                }
                "FAMILIAR" -> {
                    // Si entra como familiar con código, va DIRECTO al detalle
                    if (!pacienteJson.isNullOrEmpty()) {
                        val jsonEncoded = android.net.Uri.encode(pacienteJson)
                        navController.navigate("patient_detail/$jsonEncoded/$emailFamiliar") {
                            popUpTo(0)
                        }
                    } else {
                        // Si falla algo, vuelta atrás
                        navController.navigate(AppScreens.Welcome.route) { popUpTo(0) }
                    }
                }
                else -> {
                    // El paciente (Alfonso) va a su pantalla simplificada (modo abuelo)
                    navController.navigate(AppScreens.PatientHome.route) { popUpTo(0) }
                }
            }
        }
    }

    // UI PRINCIPAL
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // LOGO
        Icon(
            imageVector = Icons.Default.HealthAndSafety,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            "Hamparo",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(40.dp))

        // --- FASE 1: MENÚ DE SELECCIÓN DE ROL ---
        if (selectedRole == null) {
            Text(
                "¿Quién eres?",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(24.dp))

            // 1. PROFESIONALES (RESPONSABLE)
            RoleCard(
                title = "Soy Responsable / Cuidador",
                subtitle = "Gestionar pacientes y medicación",
                icon = Icons.Default.MedicalServices,
                color = MaterialTheme.colorScheme.primaryContainer,
                onClick = {
                    viewModel.limpiarEstados()
                    selectedRole = "ADMIN"
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2. FAMILIARES (VISITA/COLABORADOR)
            RoleCard(
                title = "Soy Familiar / Visita",
                subtitle = "Consultar estado y ayudar",
                icon = Icons.Default.Favorite,
                color = MaterialTheme.colorScheme.tertiaryContainer,
                onClick = {
                    viewModel.limpiarEstados()
                    selectedRole = "FAMILY"
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. PACIENTES
            RoleCard(
                title = "Soy Paciente",
                subtitle = "Ver mis recordatorios",
                icon = Icons.Default.Elderly,
                color = MaterialTheme.colorScheme.secondaryContainer,
                onClick = {
                    viewModel.limpiarEstados()
                    selectedRole = "PATIENT"
                }
            )
        }
        // --- FASE 2: FORMULARIOS DE ACCESO ---
        else {
            // Botón VOLVER al menú
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.limpiarEstados()
                        selectedRole = null
                    }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cambiar perfil", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Renderizado condicional del formulario según el rol
            when (selectedRole) {
                "ADMIN" -> AdminForm(
                    isRegistering = isRegistering,
                    onToggleRegister = { isRegistering = !isRegistering },
                    viewModel = viewModel
                )
                "PATIENT" -> PatientSimpleLogin(viewModel = viewModel)
                "FAMILY" -> FamilyLoginForm(viewModel = viewModel)
            }
        }
    }
}

// COMPONENTES AUXILIARES Y FORMULARIOS

@Composable
fun RoleCard(title: String, subtitle: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .padding(end = 12.dp),
                tint = Color.Black.copy(alpha = 0.7f)
            )
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

// --- FORMULARIO ADMIN (Responsable) ---
@Composable
fun AdminForm(isRegistering: Boolean, onToggleRegister: () -> Unit, viewModel: LoginViewModel) {
    val email by viewModel.email.collectAsState()
    val clave by viewModel.clave.collectAsState()
    val nombre by viewModel.nombreProfesional.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.mensajeError.collectAsState()

    val focusManager = LocalFocusManager.current

    Text(
        if (isRegistering) "Registro Responsable" else "Acceso Responsable",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
    )
    Text(
        if (isRegistering) "Crea tu cuenta de gestión profesional" else "Inicia sesión para gestionar",
        color = Color.Gray
    )

    Spacer(modifier = Modifier.height(20.dp))

    // CAMPO NOMBRE (Solo en Registro)
    AnimatedVisibility(visible = isRegistering) {
        Column {
            OutlinedTextField(
                value = nombre,
                onValueChange = { viewModel.onNombreProfesionalChange(it) },
                label = { Text("Nombre y Apellidos") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Person, null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    // CAMPO EMAIL
    OutlinedTextField(
        value = email,
        onValueChange = { viewModel.onEmailChange(it) },
        label = { Text("Correo Electrónico") },
        modifier = Modifier.fillMaxWidth(),
        leadingIcon = { Icon(Icons.Default.Email, null) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            capitalization = KeyboardCapitalization.None,
            imeAction = ImeAction.Next
        ),
        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
    )

    Spacer(modifier = Modifier.height(12.dp))

    // CAMPO CONTRASEÑA
    PasswordInput(
        text = clave,
        onTextChanged = { viewModel.onClaveChange(it) },
        onDone = {
            focusManager.clearFocus()
            if (isRegistering) viewModel.onRegistroClick() else viewModel.onLoginClick()
        }
    )

    if (error != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(error!!, color = MaterialTheme.colorScheme.error)
    }

    Spacer(modifier = Modifier.height(24.dp))

    if (isLoading) {
        CircularProgressIndicator()
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    focusManager.clearFocus()
                    if (isRegistering) viewModel.onRegistroClick() else viewModel.onLoginClick()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (isRegistering) "CREAR CUENTA" else "ENTRAR")
            }

            if (!isRegistering) {
                val context = LocalContext.current
                FilledTonalIconButton(
                    onClick = {
                        BiometricAuth.authenticate(
                            context = context,
                            onSuccess = { viewModel.onLoginBiometricoExitoso("CUIDADOR") }
                        )
                    },
                    modifier = Modifier.size(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Fingerprint, contentDescription = "Huella")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (isRegistering) "¿Ya tienes cuenta? " else "¿Eres nuevo? ")
            Text(
                if (isRegistering) "Inicia Sesión" else "Regístrate aquí",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onToggleRegister() }
            )
        }
    }
}

// 🔥 PACIENTE: MODO SEGURO (EMAIL + CÓDIGO) CON ACTIVACIÓN

@Composable
fun PatientSimpleLogin(viewModel: LoginViewModel) {
    // Estados del formulario
    val codigo by viewModel.clave.collectAsState()
    val emailPaciente by viewModel.emailPaciente.collectAsState()
    val nombrePaciente by viewModel.nombrePaciente.collectAsState()

    val error by viewModel.mensajeError.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Switch para cambiar entre "Entrar" y "Activar Cuenta"
    var isActivationMode by remember { mutableStateOf(false) } // false = Entrar, true = Activar

    val focusManager = LocalFocusManager.current

    Column(horizontalAlignment = Alignment.CenterHorizontally) {

        // --- TÍTULO ---
        Text(
            text = if (isActivationMode) "Activar Cuenta" else "Acceso Paciente",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = if (isActivationMode) "Configura tus datos por primera vez" else "Consulta tu medicación",
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --- CAMPO NOMBRE (Solo visible en modo ACTIVAR) ---
        AnimatedVisibility(visible = isActivationMode) {
            Column {
                OutlinedTextField(
                    value = nombrePaciente,
                    onValueChange = { viewModel.onNombrePacienteChange(it) },
                    label = { Text("Nombre y Apellidos") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Person, null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // --- CAMPO EMAIL ---
        OutlinedTextField(
            value = emailPaciente,
            onValueChange = { viewModel.onEmailPacienteChange(it) },
            label = { Text("Correo Electrónico") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Email, null) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                capitalization = KeyboardCapitalization.None,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- CAMPO CÓDIGO ---
        PasswordInput(
            text = codigo,
            onTextChanged = { viewModel.onClaveChange(it) },
            label = "Código de Vinculación",
            keyboardType = KeyboardType.NumberPassword,
            onDone = {
                focusManager.clearFocus()
                if (isActivationMode) viewModel.onActivarPacienteClick() else viewModel.onLoginClick()
            }
        )

        if (error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(error!!, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(30.dp))

        // --- BOTÓN PRINCIPAL ---
        if (isLoading) CircularProgressIndicator()
        else {
            Button(
                onClick = {
                    focusManager.clearFocus()
                    if (isActivationMode) viewModel.onActivarPacienteClick() else viewModel.onLoginClick()
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isActivationMode) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (isActivationMode) "ACTIVAR Y ENTRAR" else "ENTRAR",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- TOGGLE / ENLACE PARA CAMBIAR MODO ---
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (isActivationMode) "¿Ya tienes cuenta? " else "¿Es tu primera vez? ")
            Text(
                text = if (isActivationMode) "Entra aquí" else "Activa tu cuenta aquí",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable {
                    isActivationMode = !isActivationMode
                    viewModel.limpiarErrores()
                }
            )
        }
    }
}

// -------------------------------------------------------------------------
// 🔥 FAMILIAR: FORMULARIO MODIFICADO (LOGIN VS REGISTRO)
// -------------------------------------------------------------------------
@Composable
fun FamilyLoginForm(viewModel: LoginViewModel) {
    // Estado para cambiar entre "Entrar" y "Registrarme"
    var isRegistering by remember { mutableStateOf(false) }

    val codigo by viewModel.clave.collectAsState()
    val nombre by viewModel.nombreProfesional.collectAsState()
    val emailFamiliar by viewModel.emailFamiliar.collectAsState()

    val error by viewModel.mensajeError.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val focusManager = LocalFocusManager.current

    Text(
        text = if (isRegistering) "Registro Familiar" else "Acceso Familiar",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
    )
    Text(
        text = if (isRegistering) "Regístrate para vincularte al paciente" else "Inicia sesión para consultar",
        color = Color.Gray
    )

    Spacer(modifier = Modifier.height(24.dp))

    // CAMPO NOMBRE (Solo visible si me estoy registrando)
    AnimatedVisibility(visible = isRegistering) {
        Column {
            OutlinedTextField(
                value = nombre,
                onValueChange = { viewModel.onNombreProfesionalChange(it) },
                label = { Text("Tu Nombre y Parentesco") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Person, null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // CAMPO EMAIL (Siempre visible)
    OutlinedTextField(
        value = emailFamiliar,
        onValueChange = { viewModel.onEmailFamiliarChange(it) },
        label = { Text("Correo Electrónico") },
        modifier = Modifier.fillMaxWidth(),
        leadingIcon = { Icon(Icons.Default.Email, null) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            capitalization = KeyboardCapitalization.None,
            imeAction = ImeAction.Next
        ),
        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
    )

    Spacer(modifier = Modifier.height(16.dp))

    // CAMPO CÓDIGO (Siempre visible)
    PasswordInput(
        text = codigo,
        onTextChanged = { viewModel.onClaveChange(it) },
        label = "Código del Paciente",
        keyboardType = KeyboardType.NumberPassword,
        onDone = {
            focusManager.clearFocus()
            // Llamamos a la función correcta según el estado
            if (isRegistering) viewModel.onRegistroFamiliarClick() else viewModel.onLoginFamiliarClick()
        }
    )

    if (error != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(error!!, color = MaterialTheme.colorScheme.error)
    }

    Spacer(modifier = Modifier.height(24.dp))

    if (isLoading) {
        CircularProgressIndicator()
    } else {
        Button(
            onClick = {
                focusManager.clearFocus()
                // Llamamos a la función correcta según el estado
                if (isRegistering) viewModel.onRegistroFamiliarClick() else viewModel.onLoginFamiliarClick()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(if (isRegistering) "VINCULARME AHORA" else "ENTRAR")
        }

        // BIOMETRÍA (Solo visible en modo Login)
        if (!isRegistering) {
            Spacer(modifier = Modifier.height(16.dp))
            val context = LocalContext.current
            FilledTonalIconButton(
                onClick = {
                    BiometricAuth.authenticate(
                        context = context,
                        onSuccess = { viewModel.onLoginBiometricoExitoso("FAMILIAR") }
                    )
                },
                modifier = Modifier.size(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Icon(Icons.Default.Fingerprint, contentDescription = "Huella Familiar")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // TOGGLE: CAMBIAR ENTRE REGISTRO Y LOGIN
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (isRegistering) "¿Ya estás vinculado? " else "¿Eres nuevo? ")
            Text(
                text = if (isRegistering) "Inicia Sesión" else "Regístrate aquí",
                color = MaterialTheme.colorScheme.tertiary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable {
                    isRegistering = !isRegistering
                    viewModel.limpiarErrores()
                }
            )
        }
    }
}

// COMPONENTE REUTILIZABLE
@Composable
fun PasswordInput(
    text: String,
    onTextChanged: (String) -> Unit,
    label: String = "Contraseña",
    keyboardType: KeyboardType = KeyboardType.Password,
    onDone: () -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = text,
        onValueChange = onTextChanged,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        leadingIcon = { Icon(Icons.Default.Lock, null) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        trailingIcon = {
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(
                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = if (passwordVisible) "Ocultar" else "Mostrar"
                )
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() })
    )
}