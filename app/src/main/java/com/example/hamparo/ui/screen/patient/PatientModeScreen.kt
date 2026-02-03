package com.example.hamparo.ui.screen.patient

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.hamparo.ui.screen.detail.PatientDetailViewModel
import kotlinx.coroutines.delay

@Composable
fun PatientModeScreen(
    viewModel: PatientDetailViewModel = hiltViewModel()
) {
    // Estados para animación del botón
    var isPressed by remember { mutableStateOf(false) }
    var mensajeEnviado by remember { mutableStateOf(false) }

    // Colores de emergencia
    val colorFondo = Color(0xFFFFEBEE) // Rojo muy clarito
    val colorBoton = Color(0xFFD32F2F) // Rojo intenso

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorFondo),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = "BOTÓN DE AYUDA",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Pulse el botón rojo si necesita asistencia",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // --- EL BOTÓN GIGANTE SOS ---
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(280.dp) // Tamaño enorme para accesibilidad
                    .scale(if (isPressed) 0.95f else 1f) // Efecto de pulsación
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFFFF5252), colorBoton)
                        )
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null // Quitamos el efecto ripple estándar
                    ) {
                        // ⚡ ACCIÓN AL PULSAR
                        isPressed = true
                        viewModel.enviarAlertaSOS() // <--- LLAMADA A TU FUNCIÓN
                        mensajeEnviado = true
                    }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "SOS",
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (mensajeEnviado) {
                        Text(
                            text = "¡ENVIADO!",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Resetear la animación del botón
            LaunchedEffect(isPressed) {
                if (isPressed) {
                    delay(200)
                    isPressed = false
                }
            }

            // Resetear el mensaje de "Enviado" después de 3 segundos
            LaunchedEffect(mensajeEnviado) {
                if (mensajeEnviado) {
                    delay(3000)
                    mensajeEnviado = false
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Botón secundario para Voz
            Button(
                onClick = { /* Lógica de reconocimiento de voz */ },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                elevation = ButtonDefaults.buttonElevation(4.dp)
            ) {
                Icon(Icons.Default.Mic, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Pedir ayuda por voz", color = Color.Black, fontSize = 18.sp)
            }
        }
    }
}