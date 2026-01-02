package com.example.hamparo.ui.screen.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Elderly
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext // <--- 1. NUEVO IMPORT
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.hamparo.ui.navigation.AppScreens
import com.example.hamparo.ui.utils.BiometricAuth // <--- 2. NUEVO IMPORT (Tu archivo de seguridad)

@Composable
fun LoginScreen(navController: NavController) {

    // <--- 3. NECESITAMOS EL CONTEXTO AQUÍ
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Título de bienvenida
        Text(
            text = "Bienvenido a\nHamparo",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Por favor, selecciona tu perfil",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Opción 1: Soy Paciente (Botón Gigante) -> SIN CAMBIOS (Entra directo)
        RoleCard(
            text = "Soy Paciente",
            icon = Icons.Default.Elderly,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            onClick = { navController.navigate(AppScreens.PatientHome.route) }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Opción 2: Soy Cuidador (Botón Secundario) -> CON SEGURIDAD
        RoleCard(
            text = "Soy Cuidador",
            icon = Icons.Default.HealthAndSafety,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            onClick = {
                // <--- 4. AQUÍ ESTÁ LA MAGIA 🔒
                BiometricAuth.authenticate(context) {
                    // Solo navegamos si pone la huella bien
                    navController.navigate(AppScreens.AdminHome.route)
                }
            }
        )
    }
}

// Componente reutilizable (PUNTOS EXTRA RA3.b) -> ESTO SE QUEDA IGUAL
@Composable
fun RoleCard(
    text: String,
    icon: ImageVector,
    containerColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}