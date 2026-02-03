package com.example.hamparo.ui.screen.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hamparo.data.model.FamiliarVinculado
import com.example.hamparo.data.model.Usuario

@Composable
fun CardAuditoriaFamiliares(
    familiares: List<FamiliarVinculado>,
    cuidador: Usuario?,
    usuarioLogueadoEmail: String
) {
    val nombreRaw = cuidador?.nombre ?: ""
    val emailRaw = cuidador?.email ?: ""

    // Lógica para detectar si es el usuario demo "Ana" o carga inicial
    val activarModoAna = nombreRaw.isBlank() ||
            emailRaw.contains("anita", ignoreCase = true) ||
            nombreRaw.equals("Usuario", ignoreCase = true) ||
            nombreRaw.equals("Cuidador", ignoreCase = true) ||
            nombreRaw.equals("Cargando...", ignoreCase = true) ||
            cuidador == null

    val nombreMostrar = if (activarModoAna) "Ana Barrios Busto" else "$nombreRaw ${cuidador?.apellidos ?: ""}".trim()
    val emailMostrar = if (activarModoAna) "anita@prueba.es" else emailRaw

    // Limpieza de email para comparación segura
    val emailLogueadoLimpio = usuarioLogueadoEmail.trim().lowercase()
    val esAdminLogueado = emailMostrar.trim().lowercase() == emailLogueadoLimpio

    val inicial = if (nombreMostrar.isNotEmpty()) nombreMostrar.take(1).uppercase() else "A"

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // CABECERA
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.VerifiedUser, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Permisos y Accesos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ================= SECCIÓN ADMINISTRADOR =================
            Text(
                "ADMINISTRADOR (Propietario)",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            val backgroundAdmin = if (esAdminLogueado) Color(0xFFE3F2FD) else Color.Transparent

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(backgroundAdmin, RoundedCornerShape(8.dp))
                    .padding(12.dp), // Padding igualado para todos
                verticalAlignment = Alignment.CenterVertically // Centrado Vertical Externo
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = inicial,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center // Centrado Vertical Interno del Texto
                ) {
                    Text(
                        text = nombreMostrar,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                        // Eliminado lineHeight para evitar desplazamiento vertical extraño
                    )
                    Text(
                        text = emailMostrar,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                if (esAdminLogueado) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Conectado",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ================= SECCIÓN FAMILIARES =================
            if (familiares.isNotEmpty()) {
                Text(
                    "FAMILIARES VINCULADOS (Login)",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                familiares.forEach { familiar ->
                    val esFamiliarLogueado = familiar.email.trim().lowercase() == emailLogueadoLimpio
                    val backgroundFamiliar = if (esFamiliarLogueado) Color(0xFFE3F2FD) else Color.Transparent

                    if (familiar != familiares.first()) {
                        HorizontalDivider(
                            color = Color.LightGray.copy(alpha = 0.2f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(backgroundFamiliar, RoundedCornerShape(8.dp))
                            .padding(12.dp), // IGUALAMOS PADDING A 12dp para alinear con Ana
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = familiar.nombre.take(1).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = familiar.nombre,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.Black
                            )
                            Text(
                                text = familiar.email.ifEmpty { familiar.telefono },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }

                        if (esFamiliarLogueado) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Conectado",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}