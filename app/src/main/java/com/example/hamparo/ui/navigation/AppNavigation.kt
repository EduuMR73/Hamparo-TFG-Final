package com.example.hamparo.ui.navigation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.example.hamparo.data.model.Paciente
import com.example.hamparo.ui.screen.LinkScreen
import com.example.hamparo.ui.screen.admin.AdminHomeScreen
import com.example.hamparo.ui.screen.detail.PatientDetailScreen
import com.example.hamparo.ui.screen.drugs.DrugsScreen
import com.example.hamparo.ui.screen.drugs.PatientDrugsScreen
import com.example.hamparo.ui.screen.login.LoginScreen
import com.example.hamparo.ui.screen.login.WelcomeScreen
import com.example.hamparo.ui.screen.patient.PatientHomeScreen
import com.example.hamparo.ui.screen.patient.PatientViewModel
import com.google.gson.Gson

@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: String //
) {

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // 1. BIENVENIDA
        composable(route = AppScreens.Welcome.route) {
            WelcomeScreen(navController)
        }

        // 2. LOGIN
        composable(route = AppScreens.Login.route) {
            LoginScreen(navController)
        }

        // 3. HOME ADMIN (CUIDADOR)
        composable(route = AppScreens.AdminHome.route) {
            AdminHomeScreen(navController)
        }

        // 4. GRAFO DEL PACIENTE (Modo Simplificado)
        navigation(startDestination = AppScreens.PatientHome.route, route = "patient_graph") {
            composable(route = AppScreens.PatientHome.route) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry("patient_graph")
                }
                val sharedViewModel = hiltViewModel<PatientViewModel>(parentEntry)
                PatientHomeScreen(navController, viewModel = sharedViewModel)
            }

            composable(route = "patient_medicines_full") { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry("patient_graph")
                }
                val sharedViewModel = hiltViewModel<PatientViewModel>(parentEntry)
                PatientDrugsScreen(navController, viewModel = sharedViewModel)
            }
        }

        // 5. VINCULACIÓN
        composable(route = AppScreens.Link.route) {
            LinkScreen(navController)
        }

        // 6. DICCIONARIO
        composable(route = AppScreens.DrugsDictionary.route) {
            DrugsScreen(navController)
        }

        // 7. DETALLE PACIENTE (Usado por CUIDADOR desde la lista)
        composable(
            route = "patient_detail/{pacienteJson}/{email}",
            arguments = listOf(
                navArgument("pacienteJson") { type = NavType.StringType },
                navArgument("email") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val json = backStackEntry.arguments?.getString("pacienteJson")
            val emailExtraido = backStackEntry.arguments?.getString("email") ?: ""

            val paciente = try {
                Gson().fromJson(json, Paciente::class.java)
            } catch (e: Exception) {
                Paciente()
            }

            PatientDetailScreen(
                pacienteInicial = paciente,
                emailLogueado = emailExtraido,
                onBack = { navController.popBackStack() }
            )
        }

        // 🔥 8. DASHBOARD FAMILIAR
        composable(
            route = "dashboard_screen/{pacienteId}",
            arguments = listOf(
                navArgument("pacienteId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val pacienteId = backStackEntry.arguments?.getString("pacienteId") ?: ""
            val context = LocalContext.current // Necesario solo aquí para leer datos extra del familiar

            // Recuperamos datos guardados en el LoginViewModel (solo visuales)
            val prefs = context.getSharedPreferences("HamparoPrefs", Context.MODE_PRIVATE)
            val emailFamiliar = prefs.getString("FAMILIAR_EMAIL", "") ?: ""
            val nombrePaciente = prefs.getString("PACIENTE_NOMBRE", "Tu Familiar") ?: "Paciente"

            // Creamos el placeholder para que la UI cargue inmediatamente
            val pacientePlaceholder = Paciente(
                id = pacienteId,
                nombre = nombrePaciente
            )

            PatientDetailScreen(
                pacienteInicial = pacientePlaceholder,
                emailLogueado = emailFamiliar,
                onBack = {
                    // Si el familiar da "Atrás", cerramos sesión y vamos al inicio
                    navController.navigate(AppScreens.Welcome.route) {
                        popUpTo(0) // Borra todo el historial
                    }
                }
            )
        }
    }
}