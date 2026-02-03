package com.example.hamparo.ui.screen.login

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hamparo.data.repository.AuthRepository
import com.example.hamparo.data.repository.FirestoreRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val firestoreRepository: FirestoreRepository,
    private val firebaseAuth: FirebaseAuth,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // --- ESTADOS UI ---
    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email

    private val _clave = MutableStateFlow("")
    val clave: StateFlow<String> = _clave

    private val _nombreProfesional = MutableStateFlow("")
    val nombreProfesional: StateFlow<String> = _nombreProfesional

    private val _emailFamiliar = MutableStateFlow("")
    val emailFamiliar: StateFlow<String> = _emailFamiliar

    // Email específico para el LOGIN DE PACIENTE (Seguridad)
    private val _emailPaciente = MutableStateFlow("")
    val emailPaciente: StateFlow<String> = _emailPaciente

    private val _nombrePaciente = MutableStateFlow("")
    val nombrePaciente: StateFlow<String> = _nombrePaciente

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _mensajeError = MutableStateFlow<String?>(null)
    val mensajeError: StateFlow<String?> = _mensajeError

    private val _loginExitoso = MutableStateFlow(false)
    val loginExitoso: StateFlow<Boolean> = _loginExitoso

    // Estado para pasar el paciente completo a la UI (Json)
    private val _pacienteJson = MutableStateFlow<String?>(null)
    val pacienteJson: StateFlow<String?> = _pacienteJson

    // --- EVENTOS (Inputs) ---
    fun onEmailChange(t: String) { _email.value = t }
    fun onClaveChange(t: String) { _clave.value = t }
    fun onNombreProfesionalChange(t: String) { _nombreProfesional.value = t }
    fun onNombrePacienteChange(t: String) { _nombrePaciente.value = t }
    fun onEmailFamiliarChange(t: String) { _emailFamiliar.value = t }
    fun onEmailPacienteChange(t: String) { _emailPaciente.value = t }

    fun limpiarErrores() { _mensajeError.value = null }

    fun limpiarEstados() {
        _email.value = ""
        _clave.value = ""
        _nombreProfesional.value = ""
        _nombrePaciente.value = ""
        _emailFamiliar.value = ""
        _emailPaciente.value = ""
        _mensajeError.value = null
        _isLoading.value = false
        _pacienteJson.value = null
    }

    // 1. LOGIN GENÉRICO (Admin y Paciente)

    fun onLoginClick() {
        _mensajeError.value = null
        val inputCodigo = _clave.value.trim()

        // --- LÓGICA PACIENTE ---
        if (_email.value.isBlank()) {
            val inputEmailPaciente = _emailPaciente.value.trim().lowercase()

            if (inputEmailPaciente.isBlank()) {
                _mensajeError.value = "Introduce tu correo electrónico."
                return
            }
            if (inputCodigo.isBlank()) {
                _mensajeError.value = "Escribe tu código de vinculación."
                return
            }

            viewModelScope.launch {
                _isLoading.value = true

                // Buscamos al dueño del código
                val pacienteEncontrado = firestoreRepository.buscarPacientePorCodigo(inputCodigo)

                if (pacienteEncontrado != null) {
                    // SEGURIDAD: Comprobamos si el email coincide
                    val emailReal = pacienteEncontrado.email.lowercase()

                    if (emailReal == inputEmailPaciente) {
                        // ¡COINCIDEN! Es Alfonso de verdad.
                        guardarEmailEnDisco(inputEmailPaciente)
                        guardarSesionLocal("PACIENTE", pacienteEncontrado.adminId, pacienteEncontrado.nombre)

                        val prefs = context.getSharedPreferences("HamparoPrefs", Context.MODE_PRIVATE)
                        prefs.edit()
                            .putString("PACIENTE_ID", pacienteEncontrado.id)
                            .putString("CODIGO_VINCULACION", inputCodigo)
                            .putBoolean("TIENE_PACIENTES", true)
                            .apply()

                        _pacienteJson.value = Gson().toJson(pacienteEncontrado)
                        _loginExitoso.value = true
                    } else {
                        _mensajeError.value = "El correo o el código son incorrectos."
                    }
                } else {
                    _mensajeError.value = "Código incorrecto."
                }
                _isLoading.value = false
            }
            return
        }

        // --- LÓGICA CUIDADOR ---
        if (_clave.value.isBlank()) {
            _mensajeError.value = "Falta contraseña"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            val result = authRepository.loginUsuario(_email.value, _clave.value)
            if (result.isSuccess) {

                // Obtenemos el token actual del dispositivo para notificaciones PUSH
                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val token = task.result
                        Log.d("HamparoLogin", "Token FCM obtenido: $token")

                        // Llamamos a la función del repositorio para actualizarlo en Firestore
                        // NOTA: Asegúrate de haber añadido 'actualizarTokenCuidador' en FirestoreRepository
                        firestoreRepository.actualizarTokenCuidador(_email.value, token)
                    } else {
                        Log.e("HamparoLogin", "Error obteniendo token FCM", task.exception)
                    }
                }

                guardarEmailEnDisco(_email.value)
                activarHuellaParaFuturo(rol = "CUIDADOR", identificador = _email.value)

                val prefs = context.getSharedPreferences("HamparoSeguridad", Context.MODE_PRIVATE)
                prefs.edit().putString("SAVED_EMAIL", _email.value)
                    .putString("SAVED_PASS", _clave.value).apply()

                guardarSesionLocal("CUIDADOR", null, null)
                _loginExitoso.value = true
            } else {
                _mensajeError.value = "Credenciales incorrectas"
            }
            _isLoading.value = false
        }
    }

    // 2. ACTIVACIÓN PACIENTE (Primer acceso)

    fun onActivarPacienteClick() {
        val inputCodigo = _clave.value.trim()
        val inputEmail = _emailPaciente.value.trim().lowercase()
        val inputNombre = _nombrePaciente.value.trim()

        if (inputCodigo.isBlank() || inputEmail.isBlank() || inputNombre.isBlank()) {
            _mensajeError.value = "Rellena todos los campos."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            val paciente = firestoreRepository.buscarPacientePorCodigo(inputCodigo)

            if (paciente != null) {
                if (!paciente.email.isNullOrBlank() && paciente.email != inputEmail) {
                    _mensajeError.value = "Este código ya está en uso."
                } else {
                    val resultado = firestoreRepository.actualizarDatosPaciente(paciente.id, inputNombre, inputEmail)
                    if (resultado) {
                        onLoginClick()
                    } else {
                        _mensajeError.value = "Error al activar."
                    }
                }
            } else {
                _mensajeError.value = "Código inválido."
            }
            _isLoading.value = false
        }
    }

    // 3. FAMILIARES (REGISTRO vs LOGIN)

    // A) REGISTRO FAMILIAR: Pide Nombre + Email + Código
    fun onRegistroFamiliarClick() {
        _mensajeError.value = null
        val codigoPaciente = _clave.value.trim()
        val miNombre = _nombreProfesional.value.trim()
        val miEmail = _emailFamiliar.value.trim().lowercase()

        if (miNombre.isBlank()) { _mensajeError.value = "Falta tu nombre"; return }
        if (miEmail.isBlank()) { _mensajeError.value = "Falta tu email"; return }
        if (codigoPaciente.isBlank()) { _mensajeError.value = "Falta el código del paciente"; return }

        viewModelScope.launch {
            _isLoading.value = true
            val paciente = firestoreRepository.buscarPacientePorCodigo(codigoPaciente)

            if (paciente != null) {
                // Comprobamos si ya existe ese email en la lista para no duplicar
                val yaExiste = paciente.familiaresVinculados.any { it.email.lowercase() == miEmail }

                if (yaExiste) {
                    _mensajeError.value = "Este email ya está registrado. Por favor, inicia sesión."
                } else {
                    // REGISTRAMOS EL VÍNCULO EN FIREBASE
                    firestoreRepository.registrarAccesoFamiliar(
                        pacienteId = paciente.id,
                        nombreFamiliar = miNombre,
                        emailFamiliar = miEmail
                    )

                    // Iniciamos sesión directamente
                    iniciarSesionFamiliarLocal(paciente, miNombre, miEmail, codigoPaciente)
                }
            } else {
                _mensajeError.value = "Código de paciente no válido."
            }
            _isLoading.value = false
        }
    }

    // B) LOGIN FAMILIAR: Pide Email + Código (Verifica si existe)
    fun onLoginFamiliarClick() {
        _mensajeError.value = null
        val codigoPaciente = _clave.value.trim()
        val miEmail = _emailFamiliar.value.trim().lowercase()

        if (miEmail.isBlank()) { _mensajeError.value = "Introduce tu email"; return }
        if (codigoPaciente.isBlank()) { _mensajeError.value = "Falta el código del paciente"; return }

        viewModelScope.launch {
            _isLoading.value = true
            val paciente = firestoreRepository.buscarPacientePorCodigo(codigoPaciente)

            if (paciente != null) {
                // VERIFICACIÓN: ¿Está este email en la lista de familiares del paciente?
                val familiarEncontrado = paciente.familiaresVinculados.find { it.email.lowercase() == miEmail }

                if (familiarEncontrado != null) {
                    // ¡EXISTE! Entramos usando el nombre que ya estaba registrado
                    iniciarSesionFamiliarLocal(paciente, familiarEncontrado.nombre, miEmail, codigoPaciente)
                } else {
                    _mensajeError.value = "No constas como familiar. Regístrate primero."
                }
            } else {
                _mensajeError.value = "Código incorrecto."
            }
            _isLoading.value = false
        }
    }

    // Función auxiliar para no repetir código de guardado en prefs
    private fun iniciarSesionFamiliarLocal(paciente: com.example.hamparo.data.model.Paciente, nombreFamiliar: String, emailFamiliar: String, codigo: String) {
        guardarEmailEnDisco(emailFamiliar)

        val prefs = context.getSharedPreferences("HamparoPrefs", Context.MODE_PRIVATE)
        with(prefs.edit()) {
            putString("ROL", "FAMILIAR")
            putString("FAMILIAR_EMAIL", emailFamiliar)
            putString("FAMILIAR_NOMBRE", nombreFamiliar)
            putString("PACIENTE_ID", paciente.id)
            putString("PACIENTE_NOMBRE", paciente.nombre)
            putBoolean("TIENE_PACIENTES", true)
            putString("CODIGO_VINCULACION", codigo)
            apply()
        }

        activarHuellaParaFuturo("FAMILIAR", codigo)
        context.getSharedPreferences("HamparoSeguridad", Context.MODE_PRIVATE).edit()
            .putString("SAVED_CODIGO_FAMILIAR", codigo)
            .putString("SAVED_EMAIL_FAMILIAR", emailFamiliar)
            .putString("SAVED_NOMBRE_FAMILIAR", nombreFamiliar)
            .apply()

        _pacienteJson.value = Gson().toJson(paciente)
        _loginExitoso.value = true
    }

    // 4. OTROS (REGISTRO ADMIN / BIOMETRÍA)


    fun onRegistroClick() {
        if (_email.value.isBlank() || _clave.value.length < 6) {
            _mensajeError.value = "Email inválido o contraseña corta"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            val result = authRepository.registrarUsuario(_email.value, _clave.value, _nombreProfesional.value)
            if (result.isSuccess) {
                guardarEmailEnDisco(_email.value)
                activarHuellaParaFuturo("CUIDADOR", _email.value)
                guardarSesionLocal("CUIDADOR", null, _nombreProfesional.value)
                _loginExitoso.value = true
            } else {
                _mensajeError.value = "Error: ${result.exceptionOrNull()?.message}"
            }
            _isLoading.value = false
        }
    }

    fun onLoginBiometricoExitoso(rolIntentado: String) {
        val prefsSeguridad = context.getSharedPreferences("HamparoSeguridad", Context.MODE_PRIVATE)
        val huellaActivada = prefsSeguridad.getBoolean("HUELLA_ACTIVADA_$rolIntentado", false)

        if (!huellaActivada) {
            _mensajeError.value = "Inicia sesión manualmente la primera vez."
            return
        }

        if (rolIntentado == "FAMILIAR") {
            val savedCodigo = prefsSeguridad.getString("SAVED_CODIGO_FAMILIAR", "") ?: ""
            val savedEmail = prefsSeguridad.getString("SAVED_EMAIL_FAMILIAR", "") ?: ""

            // Para login biométrico usamos el login simple (verifica si sigue existiendo)
            if (savedCodigo.isNotEmpty() && savedEmail.isNotEmpty()) {
                _emailFamiliar.value = savedEmail
                _clave.value = savedCodigo
                onLoginFamiliarClick()
            } else {
                _mensajeError.value = "Datos caducados. Entra manualmente."
            }
            return
        }

        if (rolIntentado == "CUIDADOR") {
            val savedEmail = prefsSeguridad.getString("SAVED_EMAIL", "") ?: ""
            val savedPass = prefsSeguridad.getString("SAVED_PASS", "") ?: ""

            if (savedEmail.isNotEmpty() && savedPass.isNotEmpty()) {
                _email.value = savedEmail
                _clave.value = savedPass
                onLoginClick()
            } else {
                _mensajeError.value = "Datos no encontrados."
            }
        }
    }

    private fun guardarEmailEnDisco(email: String) {
        val prefs = context.getSharedPreferences("HamparoPrefs", Context.MODE_PRIVATE)
        prefs.edit().putString("EMAIL", email.trim().lowercase()).apply()
    }

    private fun activarHuellaParaFuturo(rol: String, identificador: String) {
        val prefs = context.getSharedPreferences("HamparoSeguridad", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("HUELLA_ACTIVADA_$rol", true).putString("ID_USUARIO_$rol", identificador).apply()
    }

    private fun guardarSesionLocal(rol: String, grupoId: String?, nombrePaciente: String?) {
        val prefs = context.getSharedPreferences("HamparoPrefs", Context.MODE_PRIVATE)
        with(prefs.edit()) {
            putString("ROL", rol)
            putBoolean("IS_LOGGED_IN", true)
            if (grupoId != null) putString("GRUPO_ID", grupoId)
            if (nombrePaciente != null) putString("PACIENTE_NOMBRE", nombrePaciente)
            apply()
        }
    }

    fun onBiometricoError(msg: String) { _mensajeError.value = msg }
}