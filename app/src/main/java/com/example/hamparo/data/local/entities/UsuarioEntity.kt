package com.example.hamparo.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "usuarios")
data class UsuarioEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "nombre_completo")
    val nombre: String,

    @ColumnInfo(name = "rol_usuario")
    val rol: UserRole,

    @ColumnInfo(name = "edad")
    val edad: Int,

    @ColumnInfo(name = "foto_uri")
    val fotoUri: String? = null // Para la foto de perfil
)