package com.example.hamparo.ui.screen.drugs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.hamparo.data.model.PautaMedica
import com.example.hamparo.data.model.TipoPauta
import com.example.hamparo.ui.screen.patient.PatientViewModel

// Datos auxiliares para los colores de las tarjetas
data class EstiloMedicamento(
    val colorFondo: Color,
    val colorTexto: Color,
    val icono: androidx.compose.ui.graphics.vector.ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDrugsScreen(
    navController: NavController,
    viewModel: PatientViewModel = hiltViewModel()
) {
    // Obtenemos TODAS las pautas
    val pautasMedicas by viewModel.pautas.collectAsState()

    // Mostramos la pastilla SI: (Tiene Stock > 0) O (Ya se ha tomado hoy)
    val pautasVisibles = remember(pautasMedicas) {
        pautasMedicas.filter { pauta ->
            // CORREGIDO: pauta.stock ya es Int
            val stockNum = pauta.stock
            stockNum > 0 || pauta.tomadaHoy
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Calendario Diario", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFFF5F5F5)
    ) { padding ->
        if (pautasVisibles.isEmpty()) {
            // Estado Vacío
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Medication, null, tint = Color.Gray, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No tienes medicinas pendientes", fontSize = 20.sp, color = Color.Gray)
                }
            }
        } else {
            // Lista Filtrada
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        "Toda tu medicación de hoy:",
                        fontSize = 18.sp,
                        color = Color.DarkGray,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                items(pautasVisibles) { pauta ->
                    ItemMedicamentoCalendario(
                        pauta = pauta,
                        onTomar = { viewModel.tomarPauta(pauta) }
                    )
                }

                item { Spacer(Modifier.height(40.dp)) }
            }
        }
    }
}

@Composable
fun ItemMedicamentoCalendario(pauta: PautaMedica, onTomar: () -> Unit) {

    // 1. Detectamos si ya se ha tomado hoy
    val estaTomada = pauta.tomadaHoy

    // CORREGIDO: pauta.stock ya es Int
    val stockActual = pauta.stock
    val esStockBajo = stockActual <= 2 && !estaTomada

    // 2. Definimos colores base según el tipo
    val estiloBase = when(pauta.tipo) {
        TipoPauta.FIJA -> EstiloMedicamento(Color(0xFFE8F5E9), Color(0xFF2E7D32), Icons.Default.Restaurant)
        TipoPauta.VARIABLE -> EstiloMedicamento(Color(0xFFE3F2FD), Color(0xFF1565C0), Icons.Default.AccessTime)
        TipoPauta.SI_PRECISA -> EstiloMedicamento(Color(0xFFFFF3E0), Color(0xFFE65100), Icons.Default.Healing)
    }

    // 3. Colores finales (Verde si tomada)
    val colorFondo = if (estaTomada) Color(0xFFEEEEEE) else Color.White
    val colorBoton = if (estaTomada) Color(0xFF4CAF50) else estiloBase.colorTexto
    val textoBoton = if (estaTomada) "COMPLETADO" else "TOMAR"
    val alphaGeneral = if (estaTomada) 0.6f else 1f

    val nombreVisual = remember(pauta.nombreMedicamento) {
        val palabras = pauta.nombreMedicamento.split(" ")
        if (palabras.size > 2) "${palabras[0]} ${palabras[1]}..." else pauta.nombreMedicamento
    }

    Card(
        elevation = CardDefaults.cardElevation(if (estaTomada) 0.dp else 4.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colorFondo),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Cabecera: Icono y Nombre
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = if (estaTomada) Color.LightGray else estiloBase.colorFondo,
                    shape = CircleShape,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (estaTomada) Icons.Default.Check else estiloBase.icono,
                            contentDescription = null,
                            tint = if (estaTomada) Color.White else estiloBase.colorTexto,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = nombreVisual.uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = Color.Black.copy(alpha = alphaGeneral)
                    )
                    Text(
                        text = pauta.dosis,
                        color = estiloBase.colorTexto.copy(alpha = alphaGeneral),
                        fontWeight = FontWeight.Bold
                    )

                    if (!estaTomada) {
                        Spacer(modifier = Modifier.height(4.dp))
                        AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    "Quedan: $stockActual",
                                    fontSize = 12.sp,
                                    fontWeight = if(esStockBajo) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (esStockBajo) Color(0xFFFFEBEE) else Color.Transparent,
                                labelColor = if (esStockBajo) Color.Red else Color.Gray
                            ),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (esStockBajo) Color.Red else Color.LightGray
                            ),
                            modifier = Modifier.height(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Botón de Acción
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Info Horario (Izquierda)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if(estaTomada) "Listo por hoy" else pauta.horarioDetalle,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if(estaTomada) Color(0xFF4CAF50) else Color.DarkGray
                    )
                }

                // Botón (Derecha)
                Button(
                    onClick = onTomar,
                    enabled = !estaTomada,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorBoton,
                        disabledContainerColor = Color(0xFFA5D6A7)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 0.dp)
                ) {
                    Text(text = textoBoton)
                }
            }
        }
    }
}