package com.example.hamparo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.hamparo.data.local.entities.MedicionEntity
import com.example.hamparo.data.local.entities.MedicionType
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicionDao {

    @Insert
    suspend fun insertMedicion(medicion: MedicionEntity)

    // Obtener todo el historial de un usuario ordenado por fecha (el más reciente primero)
    @Query("SELECT * FROM mediciones WHERE usuarioId = :userId ORDER BY fecha_hora DESC")
    fun getHistorialCompleto(userId: Int): Flow<List<MedicionEntity>>

    // LA QUERY MAESTRA PARA EL RA5 (Gráficos)
    // Nos permite pedir: "Dame solo la Tensión Arterial de este usuario para la gráfica"
    @Query("SELECT * FROM mediciones WHERE usuarioId = :userId AND tipo_medicion = :tipo ORDER BY fecha_hora ASC")
    fun getMedicionesPorTipo(userId: Int, tipo: MedicionType): Flow<List<MedicionEntity>>

    // Para estadísticas rápidas (RA5.d Cálculos y totales)
    // Cuenta cuántas mediciones hay de un tipo
    @Query("SELECT COUNT(*) FROM mediciones WHERE usuarioId = :userId AND tipo_medicion = :tipo")
    suspend fun countMediciones(userId: Int, tipo: MedicionType): Int
}