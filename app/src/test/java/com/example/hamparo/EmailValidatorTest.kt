package com.example.hamparo

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Prueba Unitaria (Unit Test).
 * Verifica que la lógica de validación funciona sin abrir la App.
 */
class EmailValidatorTest {

    // Simula tu lógica de validación
    private fun esEmailValido(email: String): Boolean {
        // Una validación simple: que no esté vacío y tenga arroba
        return email.isNotEmpty() && email.contains("@") && email.contains(".")
    }

    @Test
    fun email_vacio_retorna_falso() {
        val resultado = esEmailValido("")
        assertFalse(resultado)
    }

    @Test
    fun email_correcto_retorna_verdadero() {
        val resultado = esEmailValido("usuario@prueba.com")
        assertTrue(resultado)
    }
}