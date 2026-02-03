package com.example.hamparo.data.repository

import com.example.hamparo.data.model.Alerta
import com.example.hamparo.data.model.Medicamento
import com.example.hamparo.data.model.Medicion
import com.example.hamparo.data.model.Paciente
import com.example.hamparo.data.model.PautaMedica
import com.example.hamparo.data.model.Usuario
import kotlinx.coroutines.flow.Flow

interface FirestoreRepository {
    // --- USUARIOS (CUIDADORES Y FAMILIARES) ---
    suspend fun getUsuario(uid: String): Usuario?

    suspend fun getUsuarioPorId(uid: String): Usuario?

    // --- MEDICAMENTOS (CRUD BÁSICO) ---
    suspend fun guardarMedicamento(medicamento: Medicamento): Result<Boolean>

    // Por defecto es false (ver activas), pero si pasas true se vera el historial
    fun obtenerMedicamentos(grupoId: String, esHistorial: Boolean = false): Flow<List<Medicamento>>

    suspend fun borrarMedicamento(medicamentoId: String): Result<Boolean>
    suspend fun actualizarStock(medicamentoId: String, nuevoStock: String): Result<Boolean>
    suspend fun registrarToma(medicamentoId: String, nuevoStock: String, fechaToma: Long): Result<Boolean>
    suspend fun actualizarTomaMedicamento(idMedicamento: String, nuevoStock: String, ultimaToma: Long): Result<Boolean>

    // --- MEDICIONES Y ALERTAS ---
    suspend fun guardarMedicion(medicion: Medicion): Result<Boolean>

    fun obtenerHistorial(grupoId: String, esHistorial: Boolean = false): Flow<List<Medicion>>

    suspend fun borrarMedicion(medicionId: String): Result<Boolean>

    suspend fun marcarMedicionComoArchivada(medicionId: String, archivada: Boolean)

    suspend fun marcarMedicionComoLeida(medicionId: String)

    // --- GRUPOS ---
    suspend fun buscarGrupoPorCodigo(codigo: String): String?

    // --- TOKENS (Notificaciones) ---
    suspend fun guardarTokenCuidador(email: String, token: String): Result<Boolean>
    suspend fun obtenerTokenCuidador(email: String): String?

    fun actualizarTokenCuidador(emailCuidador: String, nuevoToken: String)

    // --- GESTIÓN DE PACIENTES ---
    suspend fun guardarPaciente(paciente: Paciente): Result<Boolean>
    suspend fun actualizarPaciente(paciente: Paciente): Result<Boolean>
    fun obtenerPacientes(adminId: String): Flow<List<Paciente>>
    fun obtenerPacientePorCodigo(codigo: String): Flow<Paciente?>
    suspend fun buscarPacientePorCodigo(codigo: String): Paciente?

    suspend fun obtenerPacientePorId(pacienteId: String): Paciente?

    suspend fun actualizarDatosPaciente(pacienteId: String, nombre: String, email: String): Boolean

    suspend fun actualizarPinPaciente(pacienteId: String, nuevoPin: String): Boolean

    suspend fun actualizarPauta(pacienteId: String, pauta: PautaMedica)

    suspend fun anadirPautaSegura(pacienteId: String, nuevaPauta: PautaMedica)

    // --- AUDITORÍA Y VINCULACIÓN ---
    suspend fun registrarAccesoFamiliar(pacienteId: String, nombreFamiliar: String, emailFamiliar: String): Result<Boolean>

    suspend fun vincularPacienteAUsuario(userId: String, pacienteId: String): Boolean

    suspend fun agregarFamiliarAPaciente(pacienteId: String, email: String, nombre: String): Boolean

    // --- BASE DE DATOS GLOBAL DE MEDICAMENTOS ---
    suspend fun buscarMedicamentoGlobal(codigo: String): String?
    suspend fun guardarMedicamentoGlobal(codigo: String, nombre: String)

    fun obtenerAlertasEnTiempoReal(pacienteId: String, esHistorial: Boolean = false): Flow<List<Alerta>>

    suspend fun enviarAlerta(alerta: Alerta)
    suspend fun marcarAlertaComoLeida(pacienteId: String, alertaId: String)

    suspend fun marcarAlertaComoArchivada(pacienteId: String, alertaId: String, archivada: Boolean)
    suspend fun borrarAlerta(pacienteId: String, alertaId: String)
}