package com.example.hamparo.data.local

data class InfoMedicamento(
    val nombre: String = "",
    val dosis: String = "",
    val tipo: String = "",       // Comp, Sobre, Inyec...
    val uso: String = "",        // Uso corto (Ej: "Dolor")
    val advertencia: String = "", // Advertencia corta

    val descripcion: String = "",   // Descripción larga
    val advertencias: String = "",  // Texto largo de advertencias
    val prospectoUrl: String = "",  // Link al PDF
    val codigoNacional: String = "" // Código CN
)

object MedicineDatabase {

    // MAPA REAL DE MEDICAMENTOS DE ESPAÑA (Datos del CIMA/AEMPS)
    // Clave = Código de Barras (EAN) o Código Nacional (CN)
    val baseDeDatos = mapOf(

        // --- PRUEBAS REALES  ---
        "8470008855661" to InfoMedicamento("Paracetamol", "1g", "Comp.", "Dolor y fiebre", "Espaciar tomas 8h"),

        // --- SALUD MENTAL (Psicofármacos) ---
        "665589" to InfoMedicamento("Quetiapina", "25 mg", "Comp.", "Agitación o dormir", "Puede dar somnolencia"),
        "832758" to InfoMedicamento("Alprazolam", "0.5 mg", "Comp.", "Ansiedad (Trankimazin)", "Puede causar dependencia"),
        "8470007700654" to InfoMedicamento("Lorazepam (Orfidal)", "1 mg", "Comp.", "Ansiedad fuerte", "Riesgo de caídas en mayores"),
        "770065" to InfoMedicamento("Lorazepam (Orfidal)", "1 mg", "Comp.", "Ansiedad fuerte", "Riesgo de caídas en mayores"),
        "8470006500996" to InfoMedicamento("Lormetazepam (Noctamid)", "1 mg", "Comp.", "Para dormir", "Tomar justo antes de acostarse"),
        "988097" to InfoMedicamento("Tranxilium", "50 mg", "Inyec.", "Ansiedad aguda", "Solo uso profesional/Urgencia"),
        "673805" to InfoMedicamento("Olanzapina Flas", "10 mg", "Bucodispersable", "Trastornos del ánimo", "Vigilar azúcar y peso"),
        "818997" to InfoMedicamento("Rivotril", "0.5 mg", "Comp.", "Epilepsia/Nervios", "No suspender de golpe"),
        "8470006812549" to InfoMedicamento("Diazepam (Valium)", "2 mg", "Comp.", "Relajante muscular", "No mezclar con alcohol"),
        "8470007017363" to InfoMedicamento("Abilify (Aripiprazol)", "400 mg", "Inyec.", "Antipsicótico Depot", "Uso mensual. Conservar bien."),
        "729599" to InfoMedicamento("Pregabalina (Lyrica)", "50 mg", "Cáps.", "Dolor neuropático/Ansiedad", "Puede marear al principio"),

        // --- ANTIBIÓTICOS E INFECCIONES ---
        "697876" to InfoMedicamento("Augmentine (Amox/Clav)", "875/125 mg", "Comp.", "Infección (Antibiótico)", "Terminar la caja completa"),
        "8470006956212" to InfoMedicamento("Ciprofloxacino", "500 mg", "Comp.", "Infección orina/respiratoria", "Evitar el sol directo"),
        "725928" to InfoMedicamento("Zinnat (Cefuroxima)", "500 mg", "Comp.", "Infección", "Tomar con alimentos"),
        "694799" to InfoMedicamento("Monurol (Fosfomicina)", "3 g", "Sobre", "Infección orina", "Tomar preferiblemente noche"),
        "8470006567333" to InfoMedicamento("Fosfocina", "500 mg", "Cáps.", "Infección", "Tomar con agua"),
        "8470007006671" to InfoMedicamento("Furantoina", "50 mg", "Comp.", "Cistitis (Orina)", "Puede teñir la orina"),
        "694890" to InfoMedicamento("Levofloxacino", "500 mg", "Comp.", "Infección pulmón/orina", "Separar de antiácidos"),
        "8470007267591" to InfoMedicamento("Aciclovir", "200 mg", "Comp.", "Herpes/Virus", "Beber mucha agua"),
        "8470006402689" to InfoMedicamento("Dalacin (Clindamicina)", "300 mg", "Cáps.", "Infección piel/boca", "No tumbarse justo después"),
        "8470009919016" to InfoMedicamento("Septrin Forte", "160/800 mg", "Comp.", "Infección resistente", "Beber líquidos abundantes"),

        // --- CARDIO Y TENSIÓN ---
        "706317" to InfoMedicamento("Captopril", "25 mg", "Comp.", "Tensión alta (Urgencia)", "Poner bajo lengua si urge"),
        "605873" to InfoMedicamento("Sintrom", "4 mg", "Comp.", "Anticoagulante", "⚠️ DOSIS CRÍTICA. REVISAR PAUTA"),
        "853895" to InfoMedicamento("Enalapril", "20 mg", "Comp.", "Tensión alta", "Vigilar tos seca"),
        "8470006892558" to InfoMedicamento("Losartan/HCTZ", "50/12.5 mg", "Comp.", "Tensión + Diurético", "Tomar por la mañana"),

        // --- DOLOR E INFLAMACIÓN ---
        "689941" to InfoMedicamento("Palexia (Tapentadol)", "25 mg", "Comp.", "Dolor crónico intenso", "Opiáceo. Puede estreñir"),
        "662457" to InfoMedicamento("Zaldiar (Tramadol/Parac)", "37.5/325", "Comp.", "Dolor moderado", "Máximo 8 al día"),
        "627067" to InfoMedicamento("Prednisona", "30 mg", "Comp.", "Antiinflamatorio potente", "Tomar con comida. No dejar de golpe"),
        "790725" to InfoMedicamento("Deflazacort (Zamene)", "30 mg", "Comp.", "Corticoides", "Vigilar hinchazón"),

        // --- OTROS (Estómago, Hierro...) ---
        "8470009465902" to InfoMedicamento("Motilium", "10 mg", "Comp.", "Náuseas y vómitos", "Tomar antes de comer"),
        "8470007586234" to InfoMedicamento("Fluidasa", "Solución", "Jarabe", "Mocos/Respiratorio", "Agitar antes de usar"),
        "8470006514733" to InfoMedicamento("Ferbisol", "100 mg", "Cáps.", "Hierro (Anemia)", "Mejor con zumo de naranja"),
        "776773" to InfoMedicamento("Ferplex", "40 mg", "Vial", "Hierro bebible", "Separar de lácteos")
    )

    fun buscar(codigo: String): InfoMedicamento? {
        return baseDeDatos[codigo] ?: baseDeDatos.entries.find { codigo.contains(it.key) }?.value
    }
}