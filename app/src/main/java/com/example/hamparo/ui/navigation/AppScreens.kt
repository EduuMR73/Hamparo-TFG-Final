package com.example.hamparo.ui.navigation

sealed class AppScreens(val route: String) {
    // 0. Pantalla de Carga
    object Splash : AppScreens("splash_screen")

    // 1. Pantalla de Bienvenida
    object Welcome : AppScreens("welcome_screen")

    // 2. Pantalla de Login
    object Login : AppScreens("login_screen")

    // 3. Pantalla de Vinculación
    object Link : AppScreens("link_screen")

    // 4. Pantallas Principales
    object AdminHome : AppScreens("admin_home_screen")
    object PatientHome : AppScreens("patient_home_screen")

    // 5. Diccionario
    object DrugsDictionary : AppScreens("drugs_screen")
}