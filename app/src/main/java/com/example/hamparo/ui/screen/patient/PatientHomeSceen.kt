package com.example.hamparo.ui.screen.patient

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning // <--- IMPORT AÑADIDO
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.hamparo.data.local.entities.MedicamentoEntity
import com.example.hamparo.data.local.entities.MedicionType
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientHomeScreen(
    navController: NavController,
    viewModel: PatientViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val listaMedicinas by viewModel.medicinas.collectAsState()

    // --- ESTADOS PARA EL MENSAJE GIGANTE ---
    var mostrarDialogo by remember { mutableStateOf(false) }
    var tituloDialogo by remember { mutableStateOf("") }
    var mensajeDialogo by remember { mutableStateOf("") }

    // --- LÓGICA DE VOZ "CEREBRO COMPLETO" ---
    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            var texto = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0)?.lowercase()?.trim() ?: ""

            // 1. LIMPIEZA DE NÚMEROS
            texto = texto.replace(" y ", " ")
                .replace("mil", "1000").replace("doscientos", "2").replace("trescientos", "3").replace("cuatrocientos", "4")
                .replace("quinientos", "5").replace("seiscientos", "6").replace("setecientos", "7")
                .replace("ochocientos", "8").replace("novecientos", "9").replace("ciento", "1")
                .replace("noventa", "90").replace("ochenta", "80").replace("setenta", "70").replace("sesenta", "60")
                .replace("cincuenta", "50").replace("cuarenta", "40").replace("treinta", "30").replace("veinte", "20").replace("diez", "10")
                .replace("once", "11").replace("doce", "12").replace("trece", "13").replace("catorce", "14").replace("quince", "15")
                .replace("uno", "1").replace("dos", "2").replace("tres", "3").replace("cuatro", "4").replace("cinco", "5")
                .replace("seis", "6").replace("siete", "7").replace("ocho", "8").replace("nueve", "9")

            // 🚨 NIVEL 1: EMERGENCIA (PRIORIDAD MÁXIMA)
            if (texto.contains("socorro") || texto.contains("ayuda") || texto.contains("emergencia") || texto.contains("llamar") || texto.contains("malo")) {

                // A) AVISO AL ABUELO (VISUAL)
                tituloDialogo = "🚨 ¡ALERTA ENVIADA!"
                mensajeDialogo = "Has pedido ayuda diciendo: '$texto'\n\n" +
                        "📞\n" +
                        "Estamos avisando a tu cuidador.\n\n" +
                        "Mantén la calma."
                mostrarDialogo = true

                // B) AVISO AL CUIDADOR (BASE DE DATOS)
                // Guardamos una "medición" de tipo ALERTA. Valor 1.0f = Activa.
                viewModel.registrarSaludPorVoz(MedicionType.ALERTA, 1.0f)
            }

            // SI NO ES EMERGENCIA, SEGUIMOS...
            else {
                val numeroEncontrado = texto.filter { it.isDigit() }.toFloatOrNull()
                val esOxigeno = texto.contains("oxígeno") || texto.contains("oxigeno") || texto.contains("saturación")
                val esPulso = texto.contains("pulso") || texto.contains("ritmo") || texto.contains("latidos")

                // 🩺 NIVEL 2: REGISTRAR SALUD
                if (numeroEncontrado != null && (esOxigeno || esPulso)) {
                    var valorFinal = numeroEncontrado
                    if (valorFinal > 200 && valorFinal < 1000) valorFinal = valorFinal / 10
                    if (valorFinal > 1000) valorFinal = valorFinal.toString().take(3).toFloat()

                    if (esOxigeno) {
                        if (valorFinal > 100) valorFinal = 100f
                        viewModel.registrarSaludPorVoz(MedicionType.OXIGENO, valorFinal)
                        tituloDialogo = "✅ ANOTADO"
                        mensajeDialogo = "OXÍGENO: ${valorFinal.toInt()}%"
                        mostrarDialogo = true
                    } else {
                        viewModel.registrarSaludPorVoz(MedicionType.PULSO, valorFinal)
                        tituloDialogo = "✅ ANOTADO"
                        mensajeDialogo = "PULSO: ${valorFinal.toInt()} ppm"
                        mostrarDialogo = true
                    }
                }

                // 💊 NIVEL 3: BUSCADOR DE MEDICINAS Y AYUDA
                else {
                    val medicinaEncontrada = listaMedicinas.find { medicina ->
                        texto.contains(medicina.nombre.lowercase())
                    }

                    if (medicinaEncontrada != null) {
                        tituloDialogo = "💊 ${medicinaEncontrada.nombre.uppercase()}"
                        mensajeDialogo = "• Dosis: ${medicinaEncontrada.dosis}\n" +
                                "• Cada: ${medicinaEncontrada.frecuenciaHoras} horas\n" +
                                "• Te quedan: ${medicinaEncontrada.stock}"
                        mostrarDialogo = true
                    }
                    else {
                        val esUnaPalabra = texto.split(" ").size < 2
                        if (esUnaPalabra && texto.length > 2) {
                            tituloDialogo = "📅 HOY NO TE TOCA"
                            mensajeDialogo = "Has preguntado por: '$texto'\n\n" +
                                    "Tranquilo, esa medicina no aparece en tu horario de ahora."
                        } else {
                            tituloDialogo = "🤔 NO TE ENTENDÍ"
                            mensajeDialogo = "Intenta decir:\n" +
                                    "• '¡Socorro!' (Emergencia)\n" +
                                    "• 'Paracetamol' (Medicina)\n" +
                                    "• '98 de oxígeno' (Salud)"
                        }
                        mostrarDialogo = true
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("MIS PASTILLAS", fontWeight = FontWeight.Black, fontSize = 24.sp) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Salir", modifier = Modifier.size(32.dp))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            LargeFloatingActionButton(
                onClick = {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Dime: 'Tengo 98 de oxígeno' o '¿Paracetamol?'")
                    }
                    try {
                        voiceLauncher.launch(intent)
                    } catch (e: Exception) {
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            tituloDialogo = "⚠️ MODO SIMULACIÓN"
                            mensajeDialogo = "El micrófono no funciona en el emulador.\nSimulando: 'Oxígeno 98'"
                            mostrarDialogo = true
                            viewModel.registrarSaludPorVoz(MedicionType.OXIGENO, 98f)
                        }, 1000)
                    }
                },
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Mic, contentDescription = "Hablar", modifier = Modifier.size(48.dp))
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = "Hola Abuelo 👋",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Toca el botón verde o usa el micrófono.",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 👇👇👇 BOTÓN DE PÁNICO MANUAL (SIMULAR VOZ) 👇👇👇
            // Esto permite probar la alerta en dispositivos reales sin grabar audio
            Button(
                onClick = {
                    // 1. Simulamos el guardado en BD como si fuera voz
                    viewModel.registrarSaludPorVoz(MedicionType.ALERTA, 1.0f)

                    // 2. Mostramos el feedback visual
                    tituloDialogo = "🚨 ¡ALERTA ENVIADA!"
                    mensajeDialogo = "Has pulsado el botón de pánico.\n\n" +
                            "📞\n" +
                            "Avisando al cuidador..."
                    mostrarDialogo = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp) // Separación con la lista
                    .height(56.dp), // Botón grande para dedos torpes
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("SOS (Simular Voz)", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
            }
            // 👆👆👆 FIN DEL BOTÓN DE PÁNICO 👆👆👆

            if (listaMedicinas.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("¡Qué bien!\nNo tienes nada pendiente.", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center, color = Color.Gray)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    items(listaMedicinas) { medicina ->
                        GrandfatherCard(medicina = medicina, onTomarClick = { viewModel.tomarMedicina(medicina) })
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

    // 👇 VENTANA GIGANTE (POP-UP) 👇
    if (mostrarDialogo) {
        AlertDialog(
            onDismissRequest = { mostrarDialogo = false },
            title = {
                Text(
                    text = tituloDialogo,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    text = mensajeDialogo,
                    style = MaterialTheme.typography.headlineSmall,
                    fontSize = 22.sp,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = { mostrarDialogo = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("¡ENTENDIDO!", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFFFFF9C4), // Fondo amarillo claro
            shape = RoundedCornerShape(24.dp)
        )
    }
}

// --- TARJETA DE MEDICINA ---
@Composable
fun GrandfatherCard(
    medicina: MedicamentoEntity,
    onTomarClick: () -> Unit
) {
    val context = LocalContext.current
    Card(
        elevation = CardDefaults.cardElevation(8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = medicina.nombre.uppercase(), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, color = Color.Black, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Text(text = medicina.dosis, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(12.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Schedule, contentDescription = null, tint = Color.DarkGray, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Cada ${medicina.frecuenciaHoras} horas", style = MaterialTheme.typography.titleLarge, color = Color.DarkGray)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    onTomarClick()
                    Toast.makeText(context, "¡Apuntado!", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = "YA ME LA TOMÉ", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}