package com.example.hamparo.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(
    tableName = "mediciones",
    // Vinculamos la medición a un usuario. Si se borra el usuario, se borran sus datos (CASCADE)
    foreignKeys = [
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["id"],
            childColumns = ["usuarioId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class MedicionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "usuarioId")
    val usuarioId: Int, // Clave foránea

    @ColumnInfo(name = "tipo_medicion")
    val tipo: MedicionType,

    @ColumnInfo(name = "valor_principal")
    val valor1: Float, // Ej: 120 (Sistólica) o 90 (Peso)

    @ColumnInfo(name = "valor_secundario")
    val valor2: Float? = null, // Ej: 80 (Diastólica). Null si es peso o glucosa.

    @ColumnInfo(name = "fecha_hora")
    val timestamp: Long = System.currentTimeMillis() // Para ordenar cronológicamente
)