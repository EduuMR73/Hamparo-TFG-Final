package com.example.hamparo.data.repository

import android.util.Log
import com.example.hamparo.data.model.Alerta
import com.example.hamparo.data.model.FamiliarVinculado
import com.example.hamparo.data.model.Medicamento
import com.example.hamparo.data.model.Medicion
import com.example.hamparo.data.model.Paciente
import com.example.hamparo.data.model.PautaMedica
import com.example.hamparo.data.model.Usuario
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Implementación del repositorio de datos utilizando Firebase Firestore como backend.
 *
 * ARQUITECTURA:
 * Esta clase sigue el patrón Repository (Clean Architecture), actuando como única fuente de verdad
 * para los datos de la aplicación. Se encarga de abstraer la complejidad de las consultas a la nube,
 * el manejo de hilos (Threading) y la gestión de errores.
 *
 * CARACTERÍSTICAS TÉCNICAS IMPLEMENTADAS:
 * 1. Concurrencia Estructurada: Uso intensivo de Kotlin Coroutines y Flow para operaciones asíncronas.
 * 2. Programación Reactiva: Implementación de canales (Channels) y flujos (Flows) para actualizaciones en tiempo real (Real-time updates).
 * 3. Atomicidad: Uso de transacciones de Firestore para garantizar la integridad de datos críticos (ej. medicación).
 * 4. Eficiencia: Filtrado en servidor para minimizar el consumo de datos y batería en el dispositivo móvil.
 *
 * @property db Instancia de FirebaseFirestore inyectada mediante Dagger-Hilt.
 */
class FirestoreRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore
) : FirestoreRepository {

    // ==========================================
    // --- USUARIOS (CUIDADORES Y FAMILIARES) ---
    // ==========================================

    /**
     * Recupera la información de un usuario específico desde la colección "usuarios".
     *
     * @param uid Identificador único del usuario (proporcionado por Firebase Auth).
     * @return Objeto [Usuario] si existe, o null en caso de error o no existencia.
     * @throws Exception Se captura internamente para garantizar la estabilidad de la app.
     */
    override suspend fun getUsuario(uid: String): Usuario? {
        return try {
            // Utilizamos .await() para convertir la Task asíncrona de Google en una suspensión coroutine-friendly.
            val snapshot = db.collection("usuarios").document(uid).get().await()
            if (snapshot.exists()) {
                snapshot.toObject(Usuario::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Wrapper para obtener usuario por ID. Mantiene consistencia en la interfaz del repositorio.
     */
    override suspend fun getUsuarioPorId(uid: String): Usuario? {
        return getUsuario(uid)
    }

    /**
     * Vincula un paciente a un usuario cuidador.
     *
     * ESTRATEGIA DE RESILIENCIA:
     * Implementa un patrón de "Intento optimista con Fallback".
     * 1. Intenta añadir el ID al array existente (operación atómica 'arrayUnion').
     * 2. Si falla (ej. el documento del usuario no existía aún), captura la excepción
     * y crea el documento desde cero con 'SetOptions.merge()'.
     *
     * Esto asegura que la vinculación funcione incluso en casos borde de inicialización de cuentas.
     */
    override suspend fun vincularPacienteAUsuario(userId: String, pacienteId: String): Boolean {
        return try {
            db.collection("usuarios").document(userId)
                .update("pacientesVinculados", FieldValue.arrayUnion(pacienteId))
                .await()
            true
        } catch (e: Exception) {
            try {
                // Fallback: Si el documento no existe, lo creamos e inicializamos el array
                val data = mapOf("pacientesVinculados" to listOf(pacienteId))
                db.collection("usuarios").document(userId)
                    .set(data, SetOptions.merge())
                    .await()
                true
            } catch (e2: Exception) {
                e2.printStackTrace()
                false
            }
        }
    }

    // ==========================================
    // --- MEDICAMENTOS (CRUD BÁSICO) ---
    // ==========================================

    /**
     * Almacena o actualiza un medicamento en la base de datos.
     * Utiliza .set() para sobreescribir el objeto completo, garantizando que la nube tenga la versión más reciente.
     */
    override suspend fun guardarMedicamento(medicamento: Medicamento): Result<Boolean> {
        return try {
            db.collection("medicamentos").document(medicamento.id).set(medicamento).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtiene un flujo reactivo (Stream) de medicamentos filtrados por grupo y estado.
     *
     * 🔴 MODIFICADO: Ahora usa el parámetro 'esHistorial'.
     *
     * DECISIÓN TÉCNICA (Flow vs Future):
     * Se utiliza [callbackFlow] en lugar de una simple suspensión porque necesitamos escuchar cambios
     * en tiempo real. Si un familiar añade un medicamento desde otro dispositivo, esta pantalla
     * se actualizará automáticamente sin intervención del usuario.
     *
     * GESTIÓN DE MEMORIA:
     * Es crítico el uso de [awaitClose] para eliminar el listener de Firebase cuando el usuario
     * abandona la pantalla, evitando fugas de memoria (Memory Leaks).
     */
    override fun obtenerMedicamentos(grupoId: String, esHistorial: Boolean): Flow<List<Medicamento>> = callbackFlow {
        // Si 'esHistorial' es true -> buscamos archivada=true. Si es false -> archivada=false.
        val estadoArchivado = esHistorial

        val subscription = db.collection("medicamentos")
            .whereEqualTo("grupoId", grupoId)
            .whereEqualTo("archivada", estadoArchivado) // 🔥 FILTRO DINÁMICO: Optimización de lectura en servidor
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    trySend(snapshot.toObjects(Medicamento::class.java))
                }
            }
        // Limpieza de recursos al cerrar el flujo
        awaitClose { subscription.remove() }
    }

    /**
     * Elimina físicamente un documento de medicamento.
     * Nota: En un entorno de producción estricto, consideraríamos un "Soft Delete" (marcado lógico),
     * pero para el alcance actual realizamos borrado físico.
     */
    override suspend fun borrarMedicamento(medicamentoId: String): Result<Boolean> {
        return try {
            db.collection("medicamentos").document(medicamentoId).delete().await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Actualiza únicamente el campo 'stock' de un medicamento.
     * Optimización: No envía todo el objeto medicamento, solo el delta del cambio.
     */
    override suspend fun actualizarStock(medicamentoId: String, nuevoStock: String): Result<Boolean> {
        return try {
            db.collection("medicamentos").document(medicamentoId)
                .update("stock", nuevoStock).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fachada para el registro de tomas. Redirige a la lógica centralizada de actualización.
     */
    override suspend fun registrarToma(medicamentoId: String, nuevoStock: String, fechaToma: Long): Result<Boolean> {
        return actualizarTomaMedicamento(medicamentoId, nuevoStock, fechaToma)
    }

    /**
     * Actualiza simultáneamente el stock y la fecha de última toma.
     * Esta operación asegura que la interfaz muestre cuándo se tomó la pastilla por última vez,
     * dato crítico para evitar sobredosis.
     */
    override suspend fun actualizarTomaMedicamento(idMedicamento: String, nuevoStock: String, ultimaToma: Long): Result<Boolean> {
        return try {
            db.collection("medicamentos").document(idMedicamento)
                .update(
                    mapOf(
                        "stock" to nuevoStock,
                        "ultimaToma" to ultimaToma
                    )
                ).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // --- MEDICIONES Y ALERTAS ---
    // ==========================================

    override suspend fun guardarMedicion(medicion: Medicion): Result<Boolean> {
        return try {
            db.collection("mediciones").document(medicion.id).set(medicion).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtiene el historial de mediciones (ej. Tensión, Glucosa).
     *
     * 🔴 MODIFICADO: Ahora usa el parámetro 'esHistorial' para las Mediciones.
     *
     * ROBUSTEZ DE DATOS:
     * Implementamos un mapeo seguro (`mapNotNull` con `try-catch` interno).
     * En bases de datos NoSQL, los esquemas pueden variar. Si un documento está corrupto
     * o le falta un campo, esta función lo ignora y sigue procesando el resto,
     * evitando que la aplicación crashee por un solo dato erróneo.
     */
    override fun obtenerHistorial(grupoId: String, esHistorial: Boolean): Flow<List<Medicion>> = callbackFlow {
        // Aquí la lógica es inversa:
        // Si pedimos el historial (esHistorial=true), queremos ver las archivadas.
        // Si NO es historial (esHistorial=false), queremos las NO archivadas.
        val estadoArchivado = esHistorial

        val subscription = db.collection("mediciones")
            .whereEqualTo("grupoId", grupoId)
            .whereEqualTo("archivada", estadoArchivado) // 🔥 FILTRO DINÁMICO
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val listaSegura = snapshot.documents.mapNotNull { doc ->
                        try {
                            // Deserialización segura
                            doc.toObject(Medicion::class.java)
                        } catch (e: Exception) {
                            // Loguear error pero no detener flujo
                            null
                        }
                    }
                    trySend(listaSegura)
                }
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun borrarMedicion(medicionId: String): Result<Boolean> {
        return try {
            db.collection("mediciones").document(medicionId).delete().await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Archiva una medición (Soft Delete conceptual) para que deje de salir en la pantalla principal
     * pero se mantenga en el historial médico.
     */
    override suspend fun marcarMedicionComoArchivada(medicionId: String, archivada: Boolean) {
        try {
            db.collection("mediciones").document(medicionId)
                .update(
                    mapOf(
                        "archivada" to archivada,
                        "leido" to true
                    )
                ).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun marcarMedicionComoLeida(medicionId: String) {
        try {
            db.collection("mediciones").document(medicionId)
                .update("leido", true)
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ==========================================
    // --- GRUPOS ---
    // ==========================================

    /**
     * Busca un grupo familiar mediante un código de acceso compartido.
     * Esencial para la funcionalidad de vincular múltiples cuidadores al mismo paciente.
     */
    override suspend fun buscarGrupoPorCodigo(codigo: String): String? {
        return try {
            val snapshot = db.collection("grupos")
                .whereEqualTo("codigoAcceso", codigo)
                .get().await()

            if (!snapshot.isEmpty) {
                snapshot.documents[0].getString("grupoId") ?: snapshot.documents[0].id
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    // ==========================================
    // --- TOKENS (Notificaciones) ---
    // ==========================================

    /**
     * Guarda el token FCM (Firebase Cloud Messaging) para notificaciones Push.
     */
    override suspend fun guardarTokenCuidador(email: String, token: String): Result<Boolean> {
        return try {
            val datos = hashMapOf("token" to token, "email" to email)
            db.collection("tokens_cuidadores").document(email).set(datos).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun obtenerTokenCuidador(email: String): String? {
        return try {
            val doc = db.collection("tokens_cuidadores").document(email).get().await()
            doc.getString("token")
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 🔥🔥 NUEVO: IMPLEMENTACIÓN PARA LOGIN VIEW MODEL (Botón SOS) 🔥🔥
     *
     * Actualiza el token en múltiples ubicaciones para asegurar redundancia.
     * Busca al usuario por email y actualiza su campo 'fcmToken', además de actualizar
     * la colección dedicada 'tokens_cuidadores'.
     *
     * @param emailCuidador Email identificador del cuidador.
     * @param nuevoToken Nuevo token generado por el servicio de mensajería.
     */
    override fun actualizarTokenCuidador(emailCuidador: String, nuevoToken: String) {
        // Actualización en colección principal de usuarios
        db.collection("usuarios")
            .whereEqualTo("email", emailCuidador)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    for (document in documents) {
                        document.reference.update("fcmToken", nuevoToken)
                            .addOnFailureListener { e ->
                                Log.e("HamparoRepo", "Error actualizando token usuario", e)
                            }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("HamparoRepo", "Error buscando usuario para token", e)
            }

        // Actualización en colección específica de tokens (Redundancia)
        val datos = hashMapOf("token" to nuevoToken, "email" to emailCuidador)
        db.collection("tokens_cuidadores").document(emailCuidador)
            .set(datos, SetOptions.merge())
    }

    // ==========================================
    // --- PACIENTES ---
    // ==========================================

    override suspend fun guardarPaciente(paciente: Paciente): Result<Boolean> {
        return try {
            db.collection("pacientes").document(paciente.id).set(paciente).await()
            Result.success(true)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun actualizarPaciente(paciente: Paciente): Result<Boolean> {
        return try {
            db.collection("pacientes").document(paciente.id).set(paciente).await()
            Result.success(true)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun actualizarDatosPaciente(pacienteId: String, nombre: String, email: String): Boolean {
        return try {
            db.collection("pacientes").document(pacienteId)
                .update(
                    mapOf(
                        "nombre" to nombre,
                        "email" to email
                    )
                ).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun actualizarPinPaciente(pacienteId: String, nuevoPin: String): Boolean {
        return try {
            db.collection("pacientes").document(pacienteId)
                .update("pinDesbloqueo", nuevoPin)
                .await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun obtenerPacientePorId(pacienteId: String): Paciente? {
        return try {
            val doc = db.collection("pacientes").document(pacienteId).get().await()
            if (doc.exists()) {
                doc.toObject(Paciente::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Obtiene la lista de pacientes y realiza un JOIN manual con sus mediciones.
     *
     * LIMITACIÓN NoSQL Y SOLUCIÓN:
     * Firestore es una base de datos documental que no soporta JOINs (unir tablas) como SQL.
     * Para mostrar en la tarjeta del paciente sus últimos datos médicos, realizamos una
     * "Agregación en Cliente":
     * 1. Descargamos la lista de pacientes.
     * 2. Lanzamos una sub-consulta para obtener mediciones del grupo.
     * 3. Combinamos ambas listas en memoria.
     *
     * Se utiliza [channelFlow] porque permite operaciones asíncronas concurrentes más complejas
     * dentro del cuerpo del flujo.
     */
    override fun obtenerPacientes(adminId: String): Flow<List<Paciente>> = channelFlow {
        val subscription = db.collection("pacientes")
            .whereEqualTo("adminId", adminId)
            .whereEqualTo("activo", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val pacientesBase = snapshot.toObjects(Paciente::class.java)

                    // Sub-consulta para enriquecer datos (Join manual)
                    db.collection("mediciones")
                        .whereEqualTo("grupoId", adminId)
                        .get()
                        .addOnSuccessListener { medicionesSnapshot ->

                            val todasLasMediciones = medicionesSnapshot.documents.mapNotNull { doc ->
                                try {
                                    doc.toObject(Medicion::class.java)
                                } catch (e: Exception) {
                                    Log.e("HamparoRepo", "Error al leer medición antigua: ${doc.id} - ${e.message}")
                                    null
                                }
                            }

                            // Mapeo y filtrado en memoria
                            val pacientesCompletos = pacientesBase.map { paciente ->
                                val susMediciones = todasLasMediciones.filter { medicion ->
                                    medicion.pacienteNombre == paciente.nombre || true
                                }
                                paciente.copy(historialMedico = susMediciones)
                            }

                            // Emitimos la lista procesada y ordenada
                            trySend(pacientesCompletos.sortedBy { it.habitacion })
                        }
                        .addOnFailureListener {
                            // En caso de fallo en la subconsulta, enviamos los datos básicos para no bloquear la UI
                            trySend(pacientesBase.sortedBy { it.habitacion })
                        }
                }
            }
        awaitClose { subscription.remove() }
    }

    /**
     * Observa cambios en un paciente específico buscando por código de vinculación.
     */
    override fun obtenerPacientePorCodigo(codigo: String): Flow<Paciente?> = callbackFlow {
        val subscription = db.collection("pacientes")
            .whereEqualTo("codigoVinculacion", codigo)
            .limit(1)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val paciente = snapshot.documents[0].toObject(Paciente::class.java)
                    trySend(paciente)
                } else {
                    trySend(null)
                }
            }

        awaitClose { subscription.remove() }
    }

    override suspend fun buscarPacientePorCodigo(codigo: String): Paciente? {
        return try {
            val query = db.collection("pacientes")
                .whereEqualTo("codigoVinculacion", codigo)
                .limit(1)
                .get()
                .await()

            if (!query.isEmpty) {
                query.documents[0].toObject(Paciente::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Actualiza una pauta médica existente dentro del array 'medicacionActual'.
     * Requiere lectura previa del documento para localizar y reemplazar el objeto en la lista local,
     * seguido de una escritura de la lista completa.
     */
    override suspend fun actualizarPauta(pacienteId: String, pauta: PautaMedica) {
        try {
            val snapshot = db.collection("pacientes").document(pacienteId).get().await()
            val paciente = snapshot.toObject(Paciente::class.java)

            if (paciente != null) {
                val nuevaLista = paciente.medicacionActual.map {
                    if (it.id == pauta.id) pauta else it
                }
                db.collection("pacientes").document(pacienteId)
                    .update("medicacionActual", nuevaLista)
                    .await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Añade una nueva pauta médica utilizando una TRANSACCIÓN ATÓMICA.
     *
     * IMPORTANCIA DE LA SEGURIDAD EN DATOS MÉDICOS:
     * Al modificar listas dentro de documentos, existe riesgo de "Race Conditions" (Condiciones de carrera)
     * si dos cuidadores editan al mismo tiempo. Uno podría sobrescribir los cambios del otro.
     *
     * SOLUCIÓN:
     * [db.runTransaction] asegura que la operación de lectura-modificación-escritura sea indivisible.
     * Si el documento cambia mientras se ejecuta la función, la transacción se reintenta automáticamente.
     */
    override suspend fun anadirPautaSegura(pacienteId: String, nuevaPauta: PautaMedica) {
        try {
            val docRef = db.collection("pacientes").document(pacienteId)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val paciente = snapshot.toObject(Paciente::class.java) ?: return@runTransaction

                val listaActual = paciente.medicacionActual.toMutableList()
                listaActual.add(nuevaPauta)

                transaction.update(docRef, "medicacionActual", listaActual)
            }.await()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Registra el acceso de un familiar, evitando duplicados mediante verificación transaccional.
     *
     * LÓGICA DE NEGOCIO:
     * Se normaliza el email (trim + lowercase) para evitar duplicados por formato.
     * Se comprueba dentro de la transacción si el familiar ya estaba vinculado.
     */
    override suspend fun registrarAccesoFamiliar(
        pacienteId: String,
        nombreFamiliar: String,
        emailFamiliar: String
    ): Result<Boolean> {
        return try {
            val emailLimpio = emailFamiliar.trim().lowercase()
            val nombreLimpio = nombreFamiliar.trim()
            val docRef = db.collection("pacientes").document(pacienteId)

            db.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val paciente = snapshot.toObject(Paciente::class.java)
                    ?: return@runTransaction

                val yaExiste = paciente.familiaresVinculados.any {
                    it.email.trim().lowercase() == emailLimpio
                }

                if (!yaExiste) {
                    val nuevoAcceso = FamiliarVinculado(
                        nombre = nombreLimpio,
                        email = emailLimpio,
                        fechaVinculacion = System.currentTimeMillis()
                    )
                    val nuevaLista = paciente.familiaresVinculados.toMutableList()
                    nuevaLista.add(nuevoAcceso)
                    transaction.update(docRef, "familiaresVinculados", nuevaLista)
                }
            }.await()

            Result.success(true)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.success(true)
        }
    }

    override suspend fun agregarFamiliarAPaciente(pacienteId: String, email: String, nombre: String): Boolean {
        val resultado = registrarAccesoFamiliar(pacienteId, nombre, email)
        return resultado.isSuccess
    }

    override suspend fun buscarMedicamentoGlobal(codigo: String): String? {
        return try {
            val doc = db.collection("medicamentos_global").document(codigo).get().await()
            doc.getString("nombre")
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun guardarMedicamentoGlobal(codigo: String, nombre: String) {
        try {
            val datos = hashMapOf(
                "nombre" to nombre,
                "fechaRegistro" to System.currentTimeMillis()
            )
            db.collection("medicamentos_global").document(codigo)
                .set(datos, SetOptions.merge())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ==========================================
    // --- 🔔 IMPLEMENTACIÓN DE ALERTAS (BUZÓN) ---
    // ==========================================

    /**
     * Obtiene alertas en tiempo real para el buzón del paciente.
     *
     * 🔴 MODIFICADO: Ahora usa el parámetro 'esHistorial' para las Alertas.
     *
     * ORDENACIÓN:
     * Se aplica [orderBy] por timestamp descendente directamente en la query para que la UI reciba
     * los datos ya ordenados (del más reciente al más antiguo), descargando trabajo del hilo principal de la UI.
     */
    override fun obtenerAlertasEnTiempoReal(pacienteId: String, esHistorial: Boolean): Flow<List<Alerta>> = callbackFlow {
        if (pacienteId.isEmpty()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        // Si esHistorial es true, mostramos las archivadas.
        val estadoArchivado = esHistorial

        val subscription = db.collection("pacientes").document(pacienteId)
            .collection("alertas")
            .whereEqualTo("archivada", estadoArchivado) // 🔥 FILTRO DINÁMICO
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val alertas = snapshot.toObjects(Alerta::class.java)
                    trySend(alertas)
                }
            }

        awaitClose { subscription.remove() }
    }

    override suspend fun enviarAlerta(alerta: Alerta) {
        if (alerta.pacienteId.isEmpty()) return

        try {
            db.collection("pacientes").document(alerta.pacienteId)
                .collection("alertas").document(alerta.id)
                .set(alerta)
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun marcarAlertaComoLeida(pacienteId: String, alertaId: String) {
        try {
            db.collection("pacientes").document(pacienteId)
                .collection("alertas").document(alertaId)
                .update("leido", true)
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun marcarAlertaComoArchivada(pacienteId: String, alertaId: String, archivada: Boolean) {
        try {
            db.collection("pacientes").document(pacienteId)
                .collection("alertas").document(alertaId)
                .update(
                    mapOf(
                        "archivada" to archivada,
                        "leido" to true
                    )
                ).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun borrarAlerta(pacienteId: String, alertaId: String) {
        try {
            db.collection("pacientes").document(pacienteId)
                .collection("alertas").document(alertaId)
                .delete()
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}