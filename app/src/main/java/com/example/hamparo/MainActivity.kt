package com.example.hamparo

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.rememberNavController
import com.example.hamparo.ui.navigation.AppNavigation
import com.example.hamparo.ui.navigation.AppScreens
import com.example.hamparo.ui.theme.HamparoTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        crearCanalesNotificaciones()
        solicitarPermisosNotificacion()

        setContent {
            HamparoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    // --- ESTADOS PARA CONTROLAR EL SPLASH SCREEN ---
                    var showSplash by remember { mutableStateOf(true) }
                    var startDestination by remember { mutableStateOf(AppScreens.Welcome.route) }

                    // --- LÓGICA DE DECISIÓN (Se ejecuta 1 sola vez al abrir la app) ---
                    LaunchedEffect(key1 = true) {
                        val prefs = getSharedPreferences("HamparoPrefs", Context.MODE_PRIVATE)
                        val isLoggedIn = prefs.getBoolean("IS_LOGGED_IN", false)
                        val rol = prefs.getString("ROL", null)
                        val pacienteId = prefs.getString("PACIENTE_ID", "")

                        // Decidimos la ruta basándonos en los datos guardados
                        startDestination = if (isLoggedIn && rol != null) {
                            when (rol) {
                                "CUIDADOR" -> AppScreens.AdminHome.route
                                "FAMILIAR" -> {
                                    if (!pacienteId.isNullOrEmpty()) "dashboard_screen/$pacienteId"
                                    else AppScreens.Welcome.route
                                }
                                "PACIENTE" -> "patient_graph"
                                else -> AppScreens.Welcome.route
                            }
                        } else {
                            AppScreens.Welcome.route
                        }

                        // Pequeña pausa (1.5 seg) para que se vea el logo y no dé un "flash" feo
                        delay(1500)
                        showSplash = false
                    }

                    // --- INTERCAMBIO DE PANTALLAS ---
                    if (showSplash) {
                        SplashScreen() // Muestra esto mientras carga
                    } else {
                        // Cuando termina de cargar, lanza la navegación con la ruta YA DECIDIDA
                        AppNavigation(navController = navController, startDestination = startDestination)
                    }
                }
            }
        }
    }

    // --- DISEÑO DE LA PANTALLA DE CARGA (SPLASH) ---
    @Composable
    fun SplashScreen() {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.primary), // Color corporativo de fondo
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                Text(
                    text = "HAMPARO",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Indicador de carga
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    }

    private fun solicitarPermisosNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }

    private fun crearCanalesNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (notificationManager.getNotificationChannel("canal_medicinas") == null) {
                val channel = NotificationChannel("canal_medicinas", "Recordatorios Medicinas", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Avisos para tomar medicación"
                    enableVibration(true)
                    val audioAttributes = AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build()
                    setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), audioAttributes)
                }
                notificationManager.createNotificationChannel(channel)
            }

            if (notificationManager.getNotificationChannel("canal_alertas_urgentes") == null) {
                val channelUrgente = NotificationChannel("canal_alertas_urgentes", "Alertas SOS", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Alertas de emergencia"
                    enableVibration(true)
                    val audioAttributes = AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build()
                    setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), audioAttributes)
                }
                notificationManager.createNotificationChannel(channelUrgente)
            }
        }
    }
}