// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    // He decidido poner las versiones "hardcoded" (explícitas) en lugar de usar
    // los alias del catálogo para solucionar el conflicto de versiones.

    // Versiones base de Android
    id("com.android.application") version "8.2.0" apply false
    id("com.android.library") version "8.2.0" apply false

    // AQUÍ ESTABA EL PROBLEMA: Fuerzo manualmente la versión 1.9.20 de Kotlin
    // para asegurar la compatibilidad estricta con la versión de KSP que necesito.
    id("org.jetbrains.kotlin.android") version "1.9.20" apply false

    // Plugin para Hilt (Inyección de Dependencias)
    id("com.google.dagger.hilt.android") version "2.48.1" apply false

    // Plugin KSP (Kotlin Symbol Processing)
    // Esta versión específica (1.9.20-1.0.14) requiere obligatoriamente Kotlin 1.9.20
    id("com.google.devtools.ksp") version "1.9.20-1.0.14" apply false
    alias(libs.plugins.google.gms.google.services) apply false
}