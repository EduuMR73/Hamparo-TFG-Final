package com.example.hamparo.di

import android.content.Context
import androidx.room.Room
import com.example.hamparo.data.local.HamparoDatabase
import com.example.hamparo.data.local.dao.MedicamentoDao // <--- IMPORT NUEVO
import com.example.hamparo.data.local.dao.MedicionDao
import com.example.hamparo.data.local.dao.UsuarioDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class) // Este módulo vivirá tanto como la app
object AppModule {

    // 1. Proveemos la Base de Datos
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): HamparoDatabase {
        return Room.databaseBuilder(
            context,
            HamparoDatabase::class.java,
            "hamparo_database" // Nombre del archivo físico de la BD
        )
            .fallbackToDestructiveMigration() // Si cambiamos la BD, borra la anterior (útil en desarrollo)
            .build()
    }

    // 2. Proveemos los DAOs individualmente (Así el Repositorio no necesita saber de la BD entera)

    @Provides
    fun provideUsuarioDao(db: HamparoDatabase): UsuarioDao = db.usuarioDao()

    @Provides
    fun provideMedicionDao(db: HamparoDatabase): MedicionDao = db.medicionDao()

    // --- NUEVO: Proveedor para el DAO de Medicamentos ---
    @Provides
    fun provideMedicamentoDao(db: HamparoDatabase): MedicamentoDao {
        return db.medicamentoDao()
    }
}