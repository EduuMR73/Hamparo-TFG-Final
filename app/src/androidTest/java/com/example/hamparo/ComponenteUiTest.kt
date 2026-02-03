package com.example.hamparo

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Prueba de Interfaz (UI Test).
 * Verifica que los elementos visuales se renderizan correctamente.
 */
@RunWith(AndroidJUnit4::class)
class ComponenteUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun verificarBotonVisible() {
        // 1. Cargamos un botón de prueba (simulando un componente de tu app)
        composeRule.setContent {
            Button(onClick = { }) {
                Text(text = "Entrar en Hamparo")
            }
        }

        // 2. Le decimos al test: "Busca un texto que diga 'Entrar en Hamparo' y asegúrate de que se ve"
        composeRule.onNodeWithText("Entrar en Hamparo").assertIsDisplayed()
    }
}