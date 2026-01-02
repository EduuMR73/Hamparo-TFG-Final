package com.example.hamparo.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medicamentos")
data class MedicamentoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val nombre: String,       // Ej: "Paracetamol"
    val dosis: String,        // Ej: "1 pastilla"
    val frecuenciaHoras: Int, // Ej: 8 (cada 8 horas)
    val stock: Int,           // Ej: 20 (quedan 20 pastillas)
    val usuarioId: Int        // Ej: 1 (Pertenece al Abuelo)
)