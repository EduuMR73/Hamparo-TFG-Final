package com.example.hamparo.ui.navigation

// Definimos las rutas de la aplicación de forma segura
sealed class AppScreens(val route: String) {
    object Login : AppScreens("login_screen")
    object PatientHome : AppScreens("patient_home_screen")
    object AdminHome : AppScreens("admin_home_screen")
}