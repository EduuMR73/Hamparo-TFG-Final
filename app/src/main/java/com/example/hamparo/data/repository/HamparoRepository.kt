package com.example.hamparo.data.repository

import com.example.hamparo.data.local.dao.MedicionDao
import com.example.hamparo.data.local.dao.UsuarioDao
import com.example.hamparo.data.local.dao.MedicamentoDao
import com.example.hamparo.data.local.entities.MedicionEntity
import com.example.hamparo.data.local.entities.UsuarioEntity
import com.example.hamparo.data.local.entities.MedicamentoEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HamparoRepository @Inject constructor(
    private val usuarioDao: UsuarioDao,
    private val medicionDao: MedicionDao,
    private val medicamentoDao: MedicamentoDao
) {

    // --- Funciones para Usuarios ---
    suspend fun crearUsuario(usuario: UsuarioEntity) = usuarioDao.insertUsuario(usuario)

    // --- Funciones para Mediciones (Salud) ---
    suspend fun guardarMedicion(medicion: MedicionEntity) {
        medicionDao.insertMedicion(medicion)
    }

    // Devuelve todas las mediciones para hacer GRÁFICOS (RA5)
    fun obtenerHistorial(usuarioId: Int) = medicionDao.getHistorialCompleto(usuarioId)

    // --- FUNCIONES DE MEDICAMENTOS (Inventario) ---

    // 1. Función para guardar (o editar si pasamos ID)
    suspend fun guardarMedicamento(nombre: String, dosis: String, frecuencia: Int, stock: Int, usuarioId: Int, id: Int = 0) {
        val nuevaMedicina = MedicamentoEntity(
            id = id,
            nombre = nombre,
            dosis = dosis,
            frecuenciaHoras = frecuencia,
            stock = stock,
            usuarioId = usuarioId
        )
        medicamentoDao.insertMedicamento(nuevaMedicina)
    }

    // 2. Función para leer
    fun obtenerInventario(usuarioId: Int) = medicamentoDao.getMedicamentos(usuarioId)

    // 3. Función para borrar
    suspend fun borrarMedicamento(medicina: MedicamentoEntity) {
        medicamentoDao.deleteMedicamento(medicina)
    }

    // 4. Función para restar stock (NUEVA) 👇
    // Llama a la consulta SQL optimizada del DAO
    suspend fun restarStock(idMedicina: Int) {
        medicamentoDao.bajarStock(idMedicina)
    }
}