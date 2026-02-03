package com.example.hamparo.data.model

import java.util.UUID

// --- ENUMS PROPIOS ---
enum class TipoGrupo(val label: String) {
    COMEDOR_GENERAL("Comedor General"),
    COMEDOR_ASISTIDO("Comedor Asistido (Ayuda)"),
    EN_HABITACION("En Habitación"),
    MESA_ESPECIAL("Mesa Especial / Vigilada")
}

enum class Genero(val label: String) { HOMBRE("Hombre"), MUJER("Mujer"), OTRO("Otro / No especificar") }
enum class EstadoCivil(val label: String) { SOLTERO("Soltero/a"), CASADO("Casado/a"), VIUDO("Viudo/a"), DIVORCIADO("Divorciado/a"), NO_CONST("No consta") }
enum class TipoVision(val label: String) { NORMAL("Normal"), GAFAS("Usa Gafas"), CATARATAS("Cataratas"), CEGUERA("Ceguera Parc./Total") }
enum class TipoAudicion(val label: String) { NORMAL("Normal"), HIPOACUSIA("Hipoacusia (Sordera)"), AUDIFONO("Usa Audífono") }
enum class TipoDentadura(val label: String) { PROPIA("Propia / Sana"), PARCIAL("Prótesis Parcial"), COMPLETA("Prótesis Total (Postiza)"), EDENTULO("Sin piezas / Edéntulo") }
enum class TipoMovilidad(val label: String) { AUTONOMO("Autónomo"), BASTON("Usa Bastón/Muleta"), ANDADOR("Usa Andador"), SILLA("Silla de Ruedas"), ENCAMADO("Encamado") }
enum class TipoDieta(val label: String) { NORMAL("Normal / Basal"), DIABETICA("Diabética"), TRITURADA("Triturada / Disfagia"), ASTRINGENTE("Astringente"), HIPOSODICA("Hiposódica (Sin sal)") }
enum class TipoIncontinencia(val label: String) { NINGUNA("Continente"), URINARIA("Urinaria"), FECAL("Fecal"), DOBLE("Doble / Mixta") }

// --- CLASES AUXILIARES ---

data class PautaMedica(
    val id: String = UUID.randomUUID().toString(),
    val nombreMedicamento: String = "",
    val dosis: String = "",
    val cantidad: Float = 1f,
    val fechaInicio: Long = System.currentTimeMillis(),
    val stock: Int = 0,

    val tipoFrecuencia: TipoFrecuencia = TipoFrecuencia.INTERVALO,

    val cadaCuantasHoras: Int = 0,
    val horaInicio: String = "08:00",
    val indicacion: String = "",
    val descripcionApi: String = "",
    val advertencias: String = "",
    val prospectoUrl: String = "",
    val codigoNacional: String = "",
    val archivada: Boolean = false,
    val tomas: List<String> = emptyList(),
    val ultimaToma: Long = 0L,
    val activo: Boolean = true,
    var tomadaHoy: Boolean = false
) {
    val horarioDetalle: String
        get() = when (tipoFrecuencia) {
            TipoFrecuencia.INTERVALO -> "Cada $cadaCuantasHoras h (desde $horaInicio)"
            TipoFrecuencia.SI_PRECISA -> "Si precisa: ${indicacion.ifEmpty { "Generico" }}"
            else -> "Pauta manual"
        }

    val tipo: TipoPauta
        get() = when (tipoFrecuencia) {
            TipoFrecuencia.INTERVALO -> TipoPauta.VARIABLE
            TipoFrecuencia.SI_PRECISA -> TipoPauta.SI_PRECISA
            else -> TipoPauta.FIJA
        }
}

data class FamiliarVinculado(
    val id: String = UUID.randomUUID().toString(),
    val nombre: String = "",
    val parentesco: String = "",
    val telefono: String = "",
    val email: String = "",
    val fechaVinculacion: Long = System.currentTimeMillis()
)

data class Paciente(
    val id: String = "",
    val adminId: String = "",
    val email: String = "",
    val nhc: String = "",
    val codigoVinculacion: String = "",
    val pinDesbloqueo: String = "1234",
    val activo: Boolean = true,
    val fechaIngreso: Long = System.currentTimeMillis(),
    val asignadoA: String = "",
    val nombre: String = "",
    val apellidos: String = "",
    val dni: String = "",
    val sip: String = "",
    val fechaNacimiento: String = "",
    val genero: Genero = Genero.HOMBRE,
    val grupoSanguineo: String = "",
    val estadoCivil: EstadoCivil = EstadoCivil.NO_CONST,
    val direccion: String = "",
    val fotoUrl: String? = null,
    val habitacion: String = "",
    val diagnostico: String = "",
    val gradoDependencia: String = "",
    val peso: String = "",
    val altura: String = "",
    val tipoMovilidad: TipoMovilidad = TipoMovilidad.AUTONOMO,
    val vision: TipoVision = TipoVision.NORMAL,
    val audicion: TipoAudicion = TipoAudicion.NORMAL,
    val dentadura: TipoDentadura = TipoDentadura.PROPIA,
    val alergias: List<String> = emptyList(),
    val tipoDieta: TipoDieta = TipoDieta.NORMAL,
    val tipoGrupo: TipoGrupo = TipoGrupo.COMEDOR_GENERAL,
    val tipoIncontinencia: TipoIncontinencia = TipoIncontinencia.NINGUNA,
    val controlAbsorbentes: Boolean = false,
    val controlDeposiciones: Boolean = false,
    val usaOxigeno: Boolean = false,
    val riesgoCaidas: Boolean = false,
    val riesgoUlceras: Boolean = false,
    val descripcionUlceras: String = "",
    val tieneDiabetes: Boolean = false,
    val tieneHipertension: Boolean = false,
    val fuma: Boolean = false,
    val bebeAlcohol: Boolean = false,
    val incapacitadoLegal: Boolean = false,
    val polimedicado: Boolean = false,
    val observaciones: String = "",
    val nombreFamiliar: String = "",
    val telefonoFamiliar: String = "",

    val medicacionActual: List<PautaMedica> = emptyList(),
    val citasMedicas: List<CitaMedica> = emptyList(),
    val familiaresVinculados: List<FamiliarVinculado> = emptyList(),
    val historialCuras: List<Cura> = emptyList(),
    val historialMedico: List<Medicion> = emptyList()
)