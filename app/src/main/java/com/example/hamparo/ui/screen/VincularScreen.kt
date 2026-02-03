package com.example.hamparo.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.hamparo.ui.VincularViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VincularScreen(
    // Inyectamos el ViewModel automáticamente con Hilt
    viewModel: VincularViewModel = hiltViewModel(),
    // Callback para navegar cuando todo salga bien (ej. ir al Home)
    onVinculacionExitosa: () -> Unit
) {
    // "Escuchamos" el estado del ViewModel. Si cambia, la pantalla se redibuja sola.
    val state by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current // Para ocultar teclado

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "Vincular Familiar",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Introduce el código de 6 dígitos proporcionado por la residencia.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(32.dp))

        // --- CAMPO DE TEXTO ---
        OutlinedTextField(
            value = state.codigoInput,
            onValueChange = { viewModel.onCodigoChanged(it) },
            label = { Text("Código de Vinculación") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number, // Teclado numérico
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(
                onSearch = {
                    focusManager.clearFocus() // Ocultar teclado
                    viewModel.buscarPaciente()
                }
            ),
            trailingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Buscar")
            }
        )

        // --- MENSAJE DE ERROR ---
        if (state.error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = state.error!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- BOTÓN BUSCAR ---
        Button(
            onClick = {
                focusManager.clearFocus()
                viewModel.buscarPaciente()
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = !state.isLoading // Deshabilitar si está cargando
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Buscar Paciente")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- RESULTADO: TARJETA DEL PACIENTE ---
        // Solo se muestra si pacienteEncontrado NO es null
        state.pacienteEncontrado?.let { paciente ->

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(60.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = paciente.nombre, // Asegúrate que tu modelo tiene "nombre"
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Habitación: ${paciente.habitacion}",
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Botón para confirmar que ES este el familiar
                    Button(
                        onClick = {
                            onVinculacionExitosa()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Sí, es mi familiar")
                    }
                }
            }
        }
    }
}