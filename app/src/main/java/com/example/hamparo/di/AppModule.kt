package com.example.hamparo.di

import android.content.Context
import androidx.room.Room
import com.example.hamparo.data.local.HamparoDatabase
import com.example.hamparo.data.local.dao.MedicamentoDao
import com.example.hamparo.data.local.dao.MedicionDao
import com.example.hamparo.data.local.dao.UsuarioDao
import com.example.hamparo.data.network.NotificationAPI
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // 1. BASE DE DATOS (ROOM)

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): HamparoDatabase {
        return Room.databaseBuilder(
            context,
            HamparoDatabase::class.java,
            "hamparo_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideUsuarioDao(db: HamparoDatabase): UsuarioDao = db.usuarioDao()

    @Provides
    fun provideMedicionDao(db: HamparoDatabase): MedicionDao = db.medicionDao()

    @Provides
    fun provideMedicamentoDao(db: HamparoDatabase): MedicamentoDao = db.medicamentoDao()


    // 2. RED (RETROFIT / NOTIFICACIONES)

    @Provides
    @Singleton
    fun provideNotificationAPI(): NotificationAPI {
        return Retrofit.Builder()
            .baseUrl("https://fcm.googleapis.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NotificationAPI::class.java)
    }

}