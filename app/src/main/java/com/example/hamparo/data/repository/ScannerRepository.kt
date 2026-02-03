package com.example.hamparo.data.repository

import com.example.hamparo.data.local.InfoMedicamento
import com.example.hamparo.data.local.MedicineDatabase
import com.example.hamparo.data.network.CimaNetwork
import com.example.hamparo.data.network.MedicamentoResumen
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScannerRepository @Inject constructor(
    private val db: FirebaseFirestore
) {

    /**
     * Busca el medicamento siguiendo una jerarquía de fuentes.
     * Prioriza la velocidad (Local/Firebase) y luego la precisión (CIMA).
     */
    suspend fun buscarMedicamento(codigoBruto: String, grupoId: String?): InfoMedicamento? {
        return withContext(Dispatchers.IO) {

            // 1. CHEQUEO DE EMERGENCIA / CASOS HARCODIADOS
            if (codigoBruto.contains("8436531244385")) {
                return@withContext buscarPorNombreForzado("PARACETAMOL ABAMED")
            }

            // 2. BUSCAR EN MEMORIA DE GRUPO (FIREBASE)
            if (!grupoId.isNullOrEmpty()) {
                val memoria = buscarEnFirebaseGrupo(codigoBruto, grupoId)
                if (memoria != null) return@withContext memoria
            }

            // 3. EXTRACCIÓN Y LIMPIEZA DEL CÓDIGO NACIONAL (CN)
            val cnParaBusqueda = extraerCN(codigoBruto)

            // 4. INTENTO EN BASE DE DATOS LOCAL
            if (cnParaBusqueda != null) {
                val infoLocal = MedicineDatabase.buscar(cnParaBusqueda)
                if (infoLocal != null) return@withContext infoLocal
            }

            // 5. CONSULTA A LA API OFICIAL (CIMA)
            if (cnParaBusqueda != null) {
                try {
                    val respuesta = CimaNetwork.api.getMedicamento(cnParaBusqueda)
                    if (respuesta.nombre != null) {
                        return@withContext procesarRespuestaCima(respuesta)
                    }
                } catch (e: Exception) {
                    // Log error si fuera necesario para depuración
                }
            }

            // Si nada funciona, devolvemos null para que la UI maneje el "No encontrado"
            return@withContext null
        }
    }

    suspend fun aprenderCodigo(grupoId: String, codigoBruto: String, nombre: String, dosis: String) {
        withContext(Dispatchers.IO) {
            try {
                val codigoId = codigoBruto.replace(Regex("[^a-zA-Z0-9]"), "")
                val datos = hashMapOf(
                    "codigo" to codigoBruto,
                    "nombre" to nombre.uppercase(),
                    "dosis" to dosis.uppercase(),
                    "actualizado" to System.currentTimeMillis()
                )
                db.collection("grupos")
                    .document(grupoId)
                    .collection("codigos_custom")
                    .document(codigoId)
                    .set(datos)
                    .await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun buscarCandidatosPorNombre(query: String): List<MedicamentoResumen> {
        return withContext(Dispatchers.IO) {
            try {
                val respuesta = CimaNetwork.api.buscarPorNombre(query)
                respuesta.resultados ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    suspend fun obtenerDetalleOficial(nregistro: String): InfoMedicamento? {
        return withContext(Dispatchers.IO) {
            try {
                val respuesta = CimaNetwork.api.getMedicamentoPorNRegistro(nregistro)
                if (respuesta.nombre != null) {
                    return@withContext procesarRespuestaCima(respuesta)
                }
                null
            } catch (e: Exception) {
                null
            }
        }
    }

    // --- FUNCIONES INTERNAS DE PROCESAMIENTO ---

    private suspend fun buscarEnFirebaseGrupo(codigoBruto: String, grupoId: String): InfoMedicamento? {
        return try {
            val codigoId = codigoBruto.replace(Regex("[^a-zA-Z0-9]"), "")
            val doc = db.collection("grupos")
                .document(grupoId)
                .collection("codigos_custom")
                .document(codigoId)
                .get().await()

            if (doc.exists()) {
                InfoMedicamento(
                    nombre = doc.getString("nombre") ?: "",
                    dosis = doc.getString("dosis") ?: "",
                    tipo = "Aprendido",
                    uso = "Registrado manualmente por tu equipo de cuidado.",
                    advertencia = "Dato verificado por la comunidad Hamparo."
                )
            } else null
        } catch (e: Exception) { null }
    }

    private suspend fun buscarPorNombreForzado(nombre: String): InfoMedicamento? {
        return try {
            val busqueda = CimaNetwork.api.buscarPorNombre(nombre)
            if (!busqueda.resultados.isNullOrEmpty()) {
                val detalle = CimaNetwork.api.getMedicamentoPorNRegistro(busqueda.resultados[0].nregistro)
                procesarRespuestaCima(detalle)
            } else null
        } catch (e: Exception) { null }
    }

    private fun procesarRespuestaCima(respuesta: com.example.hamparo.data.network.CimaMedicamento): InfoMedicamento {
        val atcCode = respuesta.atcs?.firstOrNull()?.codigo ?: ""
        val (usoUtil, consejo) = interpretarPorATC(atcCode, respuesta.nombre ?: "")
        val linkPdf = respuesta.docs?.find { it.tipo == 2 }?.url

        return InfoMedicamento(
            nombre = respuesta.nombre?.split(",")?.firstOrNull()?.trim() ?: "Desconocido", // Limpiamos nombres largos
            dosis = extraerDosisDelNombre(respuesta.nombre ?: ""),
            tipo = linkPdf ?: "Farmacia",
            uso = usoUtil,
            advertencia = consejo
        )
    }

    private fun interpretarPorATC(atc: String, nombre: String): Pair<String, String> {
        val code = atc.uppercase()
        val n = nombre.uppercase()
        return when {
            code.startsWith("A02") -> "Antiácido / Protector" to "Tomar preferiblemente en ayunas."
            code.startsWith("A10") -> "Control de Glucosa" to "Administrar según pauta glucémica."
            code.startsWith("B01") -> "Anticoagulante" to "⚠️ Vigilar aparición de hematomas espontáneos."
            code.startsWith("C09") || code.startsWith("C08") -> "Control de Tensión" to "Vigilar mareos al levantarse."
            code.startsWith("J01") -> "Antibiótico" to "⚠️ Respetar estrictamente los horarios y completar el ciclo."
            code.startsWith("M01") -> "Antiinflamatorio" to "⚠️ Tomar siempre con el estómago lleno."
            code.startsWith("N02") || "PARACETAMOL" in n -> "Analgésico / Antitérmico" to "No superar la dosis diaria recomendada."
            code.startsWith("N05") -> "Ansiolítico / Hipnótico" to "⚠️ Puede causar somnolencia y riesgo de caídas."
            else -> "Medicamento General" to "Siga las instrucciones del facultativo."
        }
    }

    private fun extraerCN(codigo: String): String? {
        // Formato estándar EAN-13 de farmacia en España (847000XXXXXXC)
        val indiceStandard = codigo.indexOf("847000")
        if (indiceStandard != -1 && codigo.length >= indiceStandard + 12) {
            return codigo.substring(indiceStandard + 6, indiceStandard + 12)
        }
        // Caso de códigos de 13 dígitos que empiezan por 847 (sin los tres ceros)
        if (codigo.length == 13 && codigo.startsWith("847")) {
            return codigo.substring(6, 12)
        }
        // Si el código ya es directamente el CN
        if (codigo.length in 6..7) return codigo.take(6)

        return null
    }

    private fun extraerDosisDelNombre(nombre: String): String {
        val regex = Regex("\\d+([.,]\\d+)?\\s?(MG|G|ML|MCG)")
        return regex.find(nombre.uppercase())?.value ?: "Consultar envase"
    }
}