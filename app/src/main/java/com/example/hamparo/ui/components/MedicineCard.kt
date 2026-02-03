package com.example.hamparo.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hamparo.data.local.InfoMedicamento

@Composable
fun MedicineCard(
    info: InfoMedicamento,
    onDownloadPdf: ((String) -> Unit)? = null
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)), // Azul médico claro
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(24.dp)) {
            // CABECERA: Icono + Nombre + DOSIS
            Row(verticalAlignment = Alignment.Top) {
                Surface(
                    color = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(50.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.MedicalServices, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        text = info.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))

                    // Dosis detectada
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Dosis detectada: ",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        Text(
                            text = info.dosis,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Divider(Modifier.padding(vertical = 16.dp), color = Color.White)

            // SECCIÓN: ¿PARA QUÉ SIRVE?
            InfoRow(Icons.Default.Info, " ¿PARA QUÉ SIRVE?", info.uso)

            Spacer(Modifier.height(12.dp))

            // SECCIÓN: ADVERTENCIAS
            InfoRow(Icons.Default.Warning, " CONSEJO / ADVERTENCIA:", info.advertencia, isWarning = true)

            // BOTÓN DE DESCARGA
            if (onDownloadPdf != null && info.tipo.startsWith("http")) {
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = { onDownloadPdf(info.tipo) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("DESCARGAR PROSPECTO OFICIAL")
                }
            }
        }
    }
}

// Subcomponente para las filas de texto
@Composable
fun InfoRow(icon: ImageVector, title: String, body: String, isWarning: Boolean = false) {
    val color = if (isWarning) Color(0xFFD32F2F) else MaterialTheme.colorScheme.primary
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
        }
        Spacer(Modifier.height(4.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, lineHeight = 20.sp)
    }
}