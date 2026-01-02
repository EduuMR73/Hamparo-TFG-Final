package com.example.hamparo.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.hamparo.data.local.entities.MedicamentoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicamentoDao {
    // Guardar o Actualizar medicina
    // Si el ID ya existe, lo reemplaza (UPDATE). Si no, lo crea (INSERT).
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicamento(medicamento: MedicamentoEntity)

    // Obtener todas las medicinas de un usuario específico
    @Query("SELECT * FROM medicamentos WHERE usuarioId = :userId")
    fun getMedicamentos(userId: Int): Flow<List<MedicamentoEntity>>

    // Eliminar una medicina concreta
    @Delete
    suspend fun deleteMedicamento(medicamento: MedicamentoEntity)

    // 👇 AÑADIDO: Resta 1 al stock directamente
    // Esta consulta es inteligente: solo resta si el stock es mayor que 0.
    // Así evitamos tener "-1 pastillas".
    @Query("UPDATE medicamentos SET stock = stock - 1 WHERE id = :id AND stock > 0")
    suspend fun bajarStock(id: Int)
}