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
    val rol: UserRole, // Usamos el Enum que creamos arriba

    @ColumnInfo(name = "edad")
    val edad: Int, // Útil para justificar "informes demográficos" si hiciera falta

    @ColumnInfo(name = "foto_uri")
    val fotoUri: String? = null // Para la foto de perfil (RA1.d Personalización)
)