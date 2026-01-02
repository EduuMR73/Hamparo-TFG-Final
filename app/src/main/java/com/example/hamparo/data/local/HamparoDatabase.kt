package com.example.hamparo.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.hamparo.data.local.dao.MedicionDao
import com.example.hamparo.data.local.dao.UsuarioDao
import com.example.hamparo.data.local.dao.MedicamentoDao // <--- NUEVO IMPORT
import com.example.hamparo.data.local.entities.MedicamentoEntity
import com.example.hamparo.data.local.entities.MedicionEntity
import com.example.hamparo.data.local.entities.UsuarioEntity

@Database(
    entities = [UsuarioEntity::class, MedicionEntity::class, MedicamentoEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class HamparoDatabase : RoomDatabase() {

    abstract fun usuarioDao(): UsuarioDao
    abstract fun medicionDao(): MedicionDao
    abstract fun medicamentoDao(): MedicamentoDao // <--- DESCOMENTADO
}