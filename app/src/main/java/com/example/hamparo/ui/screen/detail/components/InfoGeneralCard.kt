package com.example.hamparo.ui.screen.detail.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hamparo.data.model.Paciente

@Composable
fun InfoGeneralCard(paciente: Paciente, onEditRequest: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // CABECERA CON FOTO Y NOMBRE
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Foto / Inicial
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(50.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        val inicial = if (paciente.nombre.isNotEmpty()) paciente.nombre.take(1) else "?"
                        Text(inicial, fontSize = 24.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Nombre y Edad
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${paciente.nombre} ${paciente.apellidos}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "NHC: ${paciente.nhc} | Hab: ${paciente.habitacion}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                // Botón Editar Rápido
                IconButton(onClick = onEditRequest) {
                    Icon(Icons.Default.Edit, "Editar", tint = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(12.dp))

            // DATOS RÁPIDOS (Alergias y Dieta)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                // Alergias
                if (paciente.alergias.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, tint = Color(0xFFD32F2F), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Alergias: ${paciente.alergias.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFD32F2F),
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Text("Sin alergias conocidas", style = MaterialTheme.typography.bodySmall, color = Color(0xFF388E3C))
                }
            }

            Spacer(Modifier.height(4.dp))

            // Dieta
            Row {
                Text("Dieta: ", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                Text(paciente.tipoDieta.label, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}