package com.example.hamparo.di

import com.example.hamparo.data.repository.AuthRepository
import com.example.hamparo.data.repository.AuthRepositoryImpl
import com.example.hamparo.data.repository.FirestoreRepository
import com.example.hamparo.data.repository.FirestoreRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindFirestoreRepository(
        firestoreRepositoryImpl: FirestoreRepositoryImpl
    ): FirestoreRepository
}