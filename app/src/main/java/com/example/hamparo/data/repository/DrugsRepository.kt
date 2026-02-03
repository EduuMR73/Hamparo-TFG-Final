package com.example.hamparo.data.repository

import com.example.hamparo.data.local.InfoMedicamento
import com.example.hamparo.data.network.CimaNetwork
import com.example.hamparo.data.network.MedicamentoResumen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DrugsRepository @Inject constructor() {

    // 1. BUSCAR LISTA (Por nombre)
    suspend fun buscarPorNombre(query: String): List<MedicamentoResumen> {
        return withContext(Dispatchers.IO) {
            try {
                val respuesta = CimaNetwork.api.buscarPorNombre(query)
                respuesta.resultados ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    // 2. BUSCAR DETALLE (Por Código Nacional o NRegistro)
    suspend fun obtenerDetalleMedicamento(codigo: String, esNRegistro: Boolean = false): InfoMedicamento? {
        return withContext(Dispatchers.IO) {
            try {
                // Si es un escaneo sucio (no es NRegistro directo) y detectamos el código largo problemático:
                if (!esNRegistro && codigo.contains("8436531244385")) {
                    // Forzamos la búsqueda por nombre, ignorando el código de barras roto
                    val busqueda = CimaNetwork.api.buscarPorNombre("PARACETAMOL ABAMED")
                    if (!busqueda.resultados.isNullOrEmpty()) {
                        // ¡Encontrado! Llamamos a esta misma función pero con el ID bueno (recursividad)
                        return@withContext obtenerDetalleMedicamento(busqueda.resultados[0].nregistro, esNRegistro = true)
                    }
                }

                val respuesta = if (esNRegistro) {
                    CimaNetwork.api.getMedicamentoPorNRegistro(codigo)
                } else {
                    // Limpiamos CN si es necesario
                    val cnLimpio = extraerCN(codigo) ?: codigo
                    CimaNetwork.api.getMedicamento(cnLimpio)
                }

                if (respuesta.nombre != null) {
                    // APLICAMOS LA INTELIGENCIA CIENTÍFICA (ATC)
                    val atcCode = respuesta.atcs?.firstOrNull()?.codigo ?: ""
                    val (usoUtil, consejo) = interpretarPorATC(atcCode, respuesta.nombre)

                    // Extraemos el PDF (Prospecto)
                    val linkPdf = respuesta.docs?.find { it.tipo == 2 }?.url

                    // Devolvemos el objeto ya procesado.
                    InfoMedicamento(
                        nombre = respuesta.nombre,
                        dosis = extraerDosis(respuesta.nombre),
                        tipo = linkPdf ?: "", // <--- Aquí va el PDF oculto
                        uso = usoUtil,
                        advertencia = consejo
                    )
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun interpretarPorATC(atc: String, nombre: String): Pair<String, String> {
        val code = atc.uppercase()
        val nombreUp = nombre.uppercase()
        return when {
            code.startsWith("A02") -> Pair("Antiácido / Protector de estómago.", "Suele tomarse en ayunas (ej: Omeprazol) o tras comidas (ej: Almax).")
            code.startsWith("A10") -> Pair("Antidiabético / Insulina.", "Controlar niveles de azúcar. Rotar zona de inyección si es insulina.")
            code.startsWith("A11") -> Pair("Vitaminas / Suplemento.", "Seguir pauta médica.")
            code.startsWith("B01") -> Pair("Antitrombótico / Anticoagulante.", "⚠️ Controlar sangrados y hematomas. Dosis estricta (ej: Sintrom, Adiro).")
            code.startsWith("C01") -> Pair("Terapia cardíaca.", "Vigilar pulso y tensión.")
            code.startsWith("C03") -> Pair("Diurético (Para orinar).", "Tomar por la mañana para no levantarse de noche. Vigilar potasio.")
            code.startsWith("C07") -> Pair("Betabloqueante (Corazón/Tensión).", "No suspender de golpe. Controlar pulsaciones.")
            code.startsWith("C08") || code.startsWith("C09") -> Pair("Antihipertensivo (Para la Tensión).", "Tomar siempre a la misma hora para mantener la presión estable.")
            code.startsWith("C10") -> Pair("Hipolipemiante (Para el Colesterol).", "Suele tomarse por la noche/cena. Evitar pomelo.")
            code.startsWith("D") -> Pair("Uso Dermatológico (Piel).", "Aplicar sobre piel limpia. Lavar manos antes y después.")
            code.startsWith("G04") -> Pair("Urología (Próstata/Incontinencia).", "Puede cambiar el color de la orina o causar sequedad de boca.")
            code.startsWith("H03") -> Pair("Tiroides.", "Tomar en ayunas estricta, esperar 30min antes de desayunar.")
            code.startsWith("J01") -> Pair("Antibiótico.", "⚠️ OBLIGATORIO terminar la caja completa aunque se sienta bien.")
            code.startsWith("J02") -> Pair("Antimicótico (Hongos).", "Seguir el tratamiento el tiempo indicado.")
            code.startsWith("L01") -> Pair("Antineoplásico (Quimioterapia).", "⚠️ MEDICAMENTO PELIGROSO. Uso hospitalario o bajo estricto control oncológico.")
            code.startsWith("L04") -> Pair("Inmunosupresor.", "Baja las defensas. Evitar contacto con gente enferma.")
            code.startsWith("M01") -> Pair("Antiinflamatorio (AINE).", "⚠️ Tomar SIEMPRE con comida para proteger el estómago.")
            code.startsWith("M02") -> Pair("Dolor articular (Tópico).", "Uso externo. No aplicar sobre heridas abiertas.")
            code.startsWith("M04") -> Pair("Para la Gota (Ácido úrico).", "Beber mucha agua durante el tratamiento.")
            code.startsWith("M05") -> Pair("Para los Huesos (Osteoporosis).", "Si es oral: tomar de pie, no tumbarse hasta pasada 1 hora.")
            code.startsWith("N02") -> Pair("Analgésico (Para el dolor).", "Usar según dolor. Precaución con opiáceos (Tramadol) y somnolencia.")
            code.startsWith("N05") -> Pair("Psicoléptico (Ansiedad/Dormir).", "⚠️ Produce sueño y disminuye reflejos. Precaución al conducir o levantarse.")
            code.startsWith("N06") -> Pair("Psicoanaléptico (Antidepresivo/Demencia).", "El efecto puede tardar semanas en notarse. No dejar de golpe.")
            code.startsWith("R03") -> Pair("Para Asma o EPOC.", "Si es inhalador: enjuagar la boca con agua tras su uso.")
            code.startsWith("R05") -> Pair("Para la tos o resfriado.", "Beber abundantes líquidos.")
            code.startsWith("R06") -> Pair("Antihistamínico (Alergias).", "Puede causar sueño. Evitar alcohol.")
            "PARACETAMOL" in nombreUp -> Pair("Para fiebre y dolor leve.", "Respetar intervalo entre tomas.")
            else -> Pair("Medicamento Registrado (ATC: $atc).", "Consulte el prospecto (Botón PDF) o a su farmacéutico.")
        }
    }

    private fun extraerDosis(nombre: String): String {
        val regex = Regex("\\d+([.,]\\d+)?\\s?(MG|G|ML|MCG)")
        return regex.find(nombre.uppercase())?.value ?: "Ver envase"
    }

    private fun extraerCN(codigo: String): String? {
        val indice = codigo.indexOf("847000")
        if (indice != -1 && codigo.length >= indice + 12) return codigo.substring(indice + 6, indice + 12)
        if (codigo.length == 13) return codigo.substring(6, 12)
        if (codigo.length in 6..7) return codigo.take(6)
        return null
    }
}