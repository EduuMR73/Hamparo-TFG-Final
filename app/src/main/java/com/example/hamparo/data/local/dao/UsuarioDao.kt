package com.example.hamparo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.hamparo.data.local.entities.UsuarioEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioDao {

    // Insertar usuario. Si ya existe (mismo ID), lo reemplaza.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsuario(usuario: UsuarioEntity)

    // Obtener todos los usuarios. Devuelve un "Flow".
    // "Flow" notifica a la UI automáticamente si hay cambios
    @Query("SELECT * FROM usuarios")
    fun getAllUsuarios(): Flow<List<UsuarioEntity>>

    // Obtener un usuario por ID
    @Query("SELECT * FROM usuarios WHERE id = :id")
    suspend fun getUsuarioById(id: Int): UsuarioEntity?
}