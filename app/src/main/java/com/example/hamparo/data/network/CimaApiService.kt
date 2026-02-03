package com.example.hamparo.data.network

import com.google.gson.annotations.SerializedName
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// 1. LOS MOLDES DE DATOS

data class CimaMedicamento(
    val nregistro: String?,
    val nombre: String?,
    val pactivos: String?,
    val labtitular: String?,
    val docs: List<CimaDocumento>?,
    val atcs: List<CimaAtc>?
)

// Clase para leer el código ATC
data class CimaAtc(
    val codigo: String?, // Ej: "N02BE01"
    val nombre: String?  // Ej: "Paracetamol"
)

data class CimaDocumento(
    val tipo: Int,
    val url: String?
)

data class MedicamentoResumen(
    val nregistro: String,
    val nombre: String,
    val labtitular: String?
)

data class CimaListResponse(
    val total: Int,
    @SerializedName("resultados") val resultados: List<MedicamentoResumen>?
)

// 2. INTERFAZ

interface CimaApiService {
    @GET("medicamento")
    suspend fun getMedicamento(@Query("cn") cn: String): CimaMedicamento

    @GET("medicamento")
    suspend fun getMedicamentoPorNRegistro(@Query("nregistro") nregistro: String): CimaMedicamento

    @GET("medicamentos")
    suspend fun buscarPorNombre(@Query("nombre") nombre: String): CimaListResponse
}

// 3. SINGLETON

object CimaNetwork {
    private const val BASE_URL = "https://cima.aemps.es/cima/rest/"

    val api: CimaApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CimaApiService::class.java)
    }
}