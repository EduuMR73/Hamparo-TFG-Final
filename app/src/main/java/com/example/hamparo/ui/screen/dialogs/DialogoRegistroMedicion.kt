package com.example.hamparo.ui.screen.detail.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.hamparo.data.model.TipoMedicion

@Composable
fun DialogoRegistroMedicion(
    tipo: TipoMedicion,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit // valor1, valor2
) {
    var valor1 by remember { mutableStateOf("") }
    var valor2 by remember { mutableStateOf("") } // Solo para Tensión

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // TÍTULO
                Text(
                    text = "Registrar ${tipo.label}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))

                // CAMPOS DE TEXTO
                if (tipo == TipoMedicion.TENSION) {
                    // CASO ESPECIAL: TENSIÓN (Necesita 2 valores)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = valor1,
                            onValueChange = { if (it.length <= 3) valor1 = it },
                            label = { Text("Sistólica (Alta)") },
                            placeholder = { Text("120") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = valor2,
                            onValueChange = { if (it.length <= 3) valor2 = it },
                            label = { Text("Diastólica (Baja)") },
                            placeholder = { Text("80") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                } else {
                    // CASO NORMAL: UN SOLO VALOR
                    OutlinedTextField(
                        value = valor1,
                        onValueChange = { valor1 = it },
                        label = { Text("Valor en ${tipo.unidad}") },
                        placeholder = { Text("Ej: 98") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // BOTONES
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (valor1.isNotEmpty()) {
                                onConfirm(valor1, valor2)
                            }
                        },
                        enabled = valor1.isNotEmpty()
                    ) {
                        Text("Guardar")
                    }
                }
            }
        }
    }
}