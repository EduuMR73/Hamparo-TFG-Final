package com.example.hamparo.ui.screen

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.hamparo.ui.navigation.AppScreens
import com.example.hamparo.ui.screen.login.LoginViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkScreen(
    navController: NavController,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("HamparoPrefs", Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()
    val db = FirebaseFirestore.getInstance()

    // Estados
    var codigoGrupo by remember { mutableStateOf("") }
    var nombreFamiliar by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var hayError by remember { mutableStateOf(false) }

    val colorGestion = Color(0xFF673AB7) // Morado

    // Escáner QR
    val barcodeLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            codigoGrupo = result.contents
            hayError = false
            Toast.makeText(context, "Código leído correctamente", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        containerColor = Color(0xFFF5F5F5),
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icono Cabecera
            Box(
                modifier = Modifier.size(100.dp).background(colorGestion.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.FamilyRestroom, null, tint = colorGestion, modifier = Modifier.size(50.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Gestión Familiar", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Únete a la unidad para gestionar juntos.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)

            Spacer(modifier = Modifier.height(32.dp))

            Card(colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(modifier = Modifier.padding(24.dp)) {

                    // CAMPO CÓDIGO
                    Text("Código de Unidad (6 cifras)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = if(hayError) Color.Red else colorGestion)
                    OutlinedTextField(
                        value = codigoGrupo,
                        onValueChange = {
                            if (it.length <= 6) codigoGrupo = it
                            hayError = false
                        },
                        placeholder = { Text("Ej: 593825") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = hayError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        trailingIcon = {
                            if (hayError) {
                                Icon(Icons.Default.Error, "Error", tint = Color.Red)
                            } else {
                                IconButton(onClick = {
                                    val options = ScanOptions()
                                    options.setPrompt("Escanea el código QR de la Unidad")
                                    options.setBeepEnabled(true)
                                    options.setOrientationLocked(false)
                                    barcodeLauncher.launch(options)
                                }) {
                                    Icon(Icons.Default.QrCodeScanner, contentDescription = "QR", tint = colorGestion)
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // CAMPO NOMBRE
                    Text("Tu Nombre", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = if(hayError) Color.Red else colorGestion)
                    OutlinedTextField(
                        value = nombreFamiliar,
                        onValueChange = {
                            nombreFamiliar = it
                            hayError = false
                        },
                        placeholder = { Text("Ej: Hermano Juan") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = hayError
                    )

                    if (hayError) {
                        Text("⚠️ Revisa los datos (Código incorrecto o campos vacíos)", color = Color.Red, style = MaterialTheme.typography.bodySmall)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // BOTÓN ENTRAR
                    Button(
                        onClick = {
                            if (codigoGrupo.length < 4 || nombreFamiliar.isBlank()) {
                                hayError = true
                                Toast.makeText(context, "Faltan datos", Toast.LENGTH_SHORT).show()
                            } else {
                                isLoading = true
                                scope.launch {
                                    try {
                                        // 1. Buscamos el grupo en Firebase
                                        val query = db.collection("grupos")
                                            .whereEqualTo("codigoAcceso", codigoGrupo)
                                            .get()
                                            .await()

                                        if (!query.isEmpty) {
                                            // 2. Si existe, guardamos y entramos
                                            val grupo = query.documents[0]

                                            prefs.edit()
                                                .putString("ROL", "FAMILIAR")
                                                .putBoolean("IS_LOGGED_IN", true)
                                                .putString("NOMBRE_USUARIO", nombreFamiliar)
                                                .putString("GRUPO_ID", grupo.id)
                                                .apply()

                                            Toast.makeText(context, "¡Conexión Exitosa!", Toast.LENGTH_SHORT).show()

                                            navController.navigate(AppScreens.AdminHome.route) {
                                                popUpTo(0) { inclusive = true }
                                            }
                                        } else {
                                            hayError = true
                                            Toast.makeText(context, "El código no existe", Toast.LENGTH_LONG).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Error de conexión: Verifica tu internet", Toast.LENGTH_LONG).show()
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if(hayError) Color.Gray else colorGestion),
                        enabled = !isLoading
                    ) {
                        if (isLoading) CircularProgressIndicator(color = Color.White) else Text("ENTRAR AL PANEL")
                    }
                }
            }
        }
    }
}