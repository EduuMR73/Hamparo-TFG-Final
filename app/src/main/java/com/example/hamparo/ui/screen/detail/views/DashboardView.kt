package com.example.hamparo.ui.screen.detail.views

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.example.hamparo.data.model.Paciente
import com.example.hamparo.data.model.Usuario
import com.example.hamparo.ui.screen.detail.DetailView
import com.example.hamparo.ui.screen.detail.PatientDetailViewModel
import com.example.hamparo.ui.screen.detail.components.CardAuditoriaFamiliares
import com.example.hamparo.ui.screen.detail.components.InfoGeneralCard
import com.google.firebase.auth.FirebaseAuth

@Composable
fun DashboardView(
    paciente: Paciente,
    cuidador: Usuario?,
    onNavigate: (DetailView) -> Unit,
    viewModel: PatientDetailViewModel? = null
) {
    val context = LocalContext.current

    // --- CAPTURA DE ESTADOS ---
    val alertasNoLeidas by viewModel?.alertasNoLeidas?.collectAsState() ?: remember { mutableIntStateOf(0) }
    val medicionesNoLeidas by viewModel?.medicionesNoLeidas?.collectAsState() ?: remember { mutableIntStateOf(0) }

    // ESTRATEGIA DE RESCATE TOTAL
    val emailDesdeVM by viewModel?.usuarioActualEmail?.collectAsState() ?: remember { mutableStateOf("") }
    val emailFirebase = remember { FirebaseAuth.getInstance().currentUser?.email ?: "" }
    val emailPrefs = remember {
        val prefs = context.getSharedPreferences("HamparoPrefs", android.content.Context.MODE_PRIVATE)
        prefs.getString("EMAIL", "") ?: ""
    }

    val emailFinal = when {
        emailDesdeVM.isNotBlank() -> emailDesdeVM
        emailFirebase.isNotBlank() -> emailFirebase
        else -> emailPrefs
    }

    LaunchedEffect(emailFinal) {
        Log.d("HAMPARO_DEBUG", "Email Detectado: '$emailFinal'")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        InfoGeneralCard(paciente, onEditRequest = { onNavigate(DetailView.FICHA) })

        // Fila 1: Medicación y Citas
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            DashboardCard(
                title = "Medicación",
                subtitle = if (paciente.medicacionActual.isNotEmpty()) "${paciente.medicacionActual.size} activas" else "Sin pautas",
                icon = Icons.Default.Medication,
                color = Color(0xFFE8F5E9),
                iconColor = Color(0xFF2E7D32),
                modifier = Modifier.weight(1f)
            ) { onNavigate(DetailView.MEDICACION) }

            DashboardCard(
                title = "Citas",
                subtitle = if (paciente.citasMedicas.isNotEmpty()) "Próxima: ${paciente.citasMedicas.last().fecha}" else "Sin visitas",
                icon = Icons.Default.CalendarToday,
                color = Color(0xFFFFF3E0),
                iconColor = Color(0xFFEF6C00),
                modifier = Modifier.weight(1f)
            ) { onNavigate(DetailView.CITAS) }
        }

        // Fila 2: Alertas e Historial
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            DashboardCard(
                title = "Alertas SOS",
                subtitle = if (alertasNoLeidas > 0) "¡$alertasNoLeidas avisos!" else "Buzón al día",
                icon = Icons.Default.NotificationsActive,
                color = Color(0xFFFFEBEE),
                iconColor = Color(0xFFC62828),
                modifier = Modifier.weight(1f),
                badgeCount = alertasNoLeidas
            ) { onNavigate(DetailView.ALERTAS) }

            DashboardCard(
                title = "Historial",
                subtitle = "Ver registros",
                icon = Icons.Default.Timeline,
                color = Color(0xFFF3E5F5),
                iconColor = Color(0xFF6A1B9A),
                modifier = Modifier.weight(1f),
                badgeCount = medicionesNoLeidas,
                badgeColor = Color(0xFF2196F3)
            ) { onNavigate(DetailView.HISTORIAL) }
        }

        // 🔥 FILA 3: CURAS Y CONSTANTES (Nuevo botón añadido aquí)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Botón Curas
            DashboardCard(
                title = "Curas",
                subtitle = "Evolución",
                icon = Icons.Default.Healing,
                color = Color(0xFFE0F7FA),
                iconColor = Color(0xFF006064),
                modifier = Modifier.weight(1f)
            ) { onNavigate(DetailView.CURAS) }

            // 👇 NUEVO BOTÓN: CONSTANTES VITALES
            DashboardCard(
                title = "Constantes",
                subtitle = "Tensión, Peso...",
                icon = Icons.Default.MonitorHeart, // Icono de corazón/monitor
                color = Color(0xFFFFEBEE), // Fondo rosado suave
                iconColor = Color(0xFFE91E63), // Icono rosa fuerte
                modifier = Modifier.weight(1f)
            ) { onNavigate(DetailView.CONSTANTES) }
        }

        Text(
            text = "AUDITORÍA DE ACCESOS",
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )

        CardAuditoriaFamiliares(
            familiares = paciente.familiaresVinculados,
            cuidador = cuidador,
            usuarioLogueadoEmail = emailFinal
        )

        Spacer(Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    iconColor: Color,
    modifier: Modifier = Modifier,
    badgeCount: Int = 0,
    badgeColor: Color = Color.Red,
    onClick: () -> Unit
) {
    Box(modifier = modifier) {
        Card(
            onClick = onClick,
            modifier = Modifier.height(140.dp).fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = color,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = iconColor)
                    }
                }
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
        }
        if (badgeCount > 0) {
            Surface(
                color = badgeColor,
                shape = CircleShape,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(24.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (badgeCount > 9) "9+" else badgeCount.toString(),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}