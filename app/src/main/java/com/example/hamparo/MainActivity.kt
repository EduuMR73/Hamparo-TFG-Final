package com.example.hamparo

import android.os.Bundle
// CAMBIO IMPORTANTE: Cambio ComponentActivity por FragmentActivity
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.hamparo.ui.navigation.AppScreens
import com.example.hamparo.ui.screen.login.LoginScreen
import com.example.hamparo.ui.screen.patient.PatientHomeScreen
import com.example.hamparo.ui.screens.admin.AdminHomeScreen
import com.example.hamparo.ui.theme.HamparoTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
// AHORA HEREDO DE FragmentActivity (Necesario para huella y diálogos antiguos)
class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 👇👇👇 AÑADIDO: CREAMOS EL CANAL AL INICIAR LA APP 👇👇👇
        crearCanalNotificaciones()

        setContent {
            HamparoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Configuro el controlador de navegación central
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = AppScreens.Login.route
                    ) {
                        // Pantalla 1: Login
                        composable(route = AppScreens.Login.route) {
                            LoginScreen(navController)
                        }

                        // Pantalla 2: Home del Paciente
                        composable(route = AppScreens.PatientHome.route) {
                            PatientHomeScreen(navController)
                        }

                        // Pantalla 3: Home del Admin
                        composable(route = AppScreens.AdminHome.route) {
                            AdminHomeScreen(navController)
                        }
                    }
                }
            }
        }
    }

    // --- FUNCIÓN AUXILIAR PARA CONFIGURAR EL CANAL DE NOTIFICACIONES ---
    private fun crearCanalNotificaciones() {
        // Solo necesario en Android 8.0 (Oreo) o superior
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val name = "Recordatorios de Medicinas"
            val descriptionText = "Canal para avisos de pastillas"
            val importance = android.app.NotificationManager.IMPORTANCE_HIGH // ¡IMPORTANTE! Para que suene fuerte

            // ID del canal: "canal_medicinas" (Debe coincidir con el AlarmReceiver)
            val channel = android.app.NotificationChannel("canal_medicinas", name, importance).apply {
                description = descriptionText
            }

            // Registramos el canal en el sistema
            val notificationManager: android.app.NotificationManager =
                getSystemService(android.app.NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}