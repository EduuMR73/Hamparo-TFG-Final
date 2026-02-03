package com.example.hamparo.ui.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import com.example.hamparo.data.model.Medicion
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Motor de generación de informes y exportación de datos en formato PDF.
 *
 * ARQUITECTURA:
 * Definido como un Singleton (`object`) para garantizar una única instancia de utilidad en memoria,
 * evitando la sobrecarga de instanciación recurrente (Stateless Utility Pattern).
 *
 * CARACTERÍSTICAS TÉCNICAS:
 * 1. Renderizado Nativo: Utiliza la API de `android.graphics.pdf` y `Canvas` para dibujar vectores y texto
 * sin dependencias de terceros, reduciendo el tamaño del APK y mejorando el rendimiento.
 * 2. Scoped Storage (Seguridad): Implementa la lógica de persistencia adaptativa para Android 10+ (API 29),
 * utilizando `ContentResolver` y `MediaStore` en lugar de acceso directo al sistema de archivos.
 * 3. Procesamiento de Datos: Realiza cálculos estadísticos en tiempo de ejecución (promedios, agregaciones)
 * utilizando programación funcional sobre colecciones.
 */
object ReportGenerator {

    /**
     * Genera un documento PDF físico a partir de un conjunto de datos clínicos.
     *
     * @param context Contexto de la aplicación necesario para acceder al ContentResolver y Resources.
     * @param datos Lista tipada de objetos [Medicion] que constituyen el cuerpo del informe.
     * @param tipoNombre Categoría del informe (ej. "Tensión", "Glucosa") para el encabezado.
     * @param nombreCompleto Identificador del paciente asociado a los datos (Protección de datos).
     *
     * ESTRATEGIA DE RENDERIZADO:
     * Se sigue un enfoque procedimental de dibujo sobre Canvas (Paint & Draw), lo que otorga
     * control total sobre el posicionamiento de píxeles y garantiza una visualización idéntica
     * en cualquier dispositivo.
     */
    // Añadimos 'nombreCompleto' como parámetro obligatorio
    fun generarInformePDF(
        context: Context,
        datos: List<Medicion>,
        tipoNombre: String,
        nombreCompleto: String
    ) {
        // Validación defensiva: Evita el consumo de recursos de I/O si no hay carga útil.
        if (datos.isEmpty()) {
            Toast.makeText(context, "No hay datos para generar el informe", Toast.LENGTH_SHORT).show()
            return
        }

        // Inicialización del motor de documentos nativo de Android
        val pdfDocument = PdfDocument()
        val paint = Paint()

        // 1. Configuración de la página (Formato A4 estándar: 595x842 puntos)
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        // --- CABECERA (Tipografía y Jerarquía Visual) ---
        paint.color = Color.BLACK
        paint.textSize = 24f
        paint.isFakeBoldText = true // Renderizado de fuente en negrita sintética
        canvas.drawText("Informe de $tipoNombre", 50f, 60f, paint)

        paint.textSize = 14f
        paint.isFakeBoldText = false
        val fechaHoy = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())

        // Usamos el nombre completo que nos llega
        // Inyección de metadatos del paciente en el documento impreso
        canvas.drawText("Paciente: $nombreCompleto", 50f, 90f, paint)

        canvas.drawText("Fecha de emisión: $fechaHoy", 50f, 110f, paint)
        canvas.drawText("App: Hamparo - Cuidado de Mayores", 50f, 130f, paint)

        // --- ESTADÍSTICAS (Análisis de datos en cliente) ---
        // Uso de programación funcional (mapNotNull + average) para cálculo robusto evitando NullPointerExceptions
        var textoResumen = "Total registros: ${datos.size}"
        val media = datos.mapNotNull { it.valor1.replace(",",".").toDoubleOrNull() }.average()

        // Validación aritmética para evitar imprimir "NaN" si los datos no son numéricos
        if (!media.isNaN()) {
            val mediaFormateada = String.format("%.2f", media)
            textoResumen += " | Promedio: $mediaFormateada"
        }

        paint.color = Color.DKGRAY
        canvas.drawText(textoResumen, 50f, 160f, paint)

        // --- LÍNEA SEPARADORA (UI/UX en documento) ---
        paint.strokeWidth = 2f
        paint.color = Color.LTGRAY
        canvas.drawLine(50f, 180f, 545f, 180f, paint)

        // --- TABLA DE DATOS ---
        paint.color = Color.BLACK
        paint.textSize = 14f
        var y = 210f

        // Encabezados de tabla
        paint.isFakeBoldText = true
        canvas.drawText("FECHA", 50f, y, paint)
        canvas.drawText("HORA", 150f, y, paint)
        canvas.drawText("VALOR", 250f, y, paint)
        canvas.drawText("NOTAS", 350f, y, paint)

        y += 30f
        paint.isFakeBoldText = false
        paint.textSize = 12f

        // LIMITACIÓN DE DISEÑO:
        // Se toman los primeros 25 registros para asegurar que caben en una única página (MVP).
        // En versiones futuras se implementará lógica de paginación dinámica.
        val datosParaImprimir = datos.take(25)

        for (item in datosParaImprimir) {
            canvas.drawText(item.fecha, 50f, y, paint)
            canvas.drawText(item.hora, 150f, y, paint)

            // Formateo condicional según el tipo de dato (presión arterial vs escalar simple)
            val valorTexto = if (item.valor2.isNotEmpty()) "${item.valor1} / ${item.valor2}" else item.valor1
            canvas.drawText(valorTexto, 250f, y, paint)

            // Truncado de texto para evitar desbordamiento horizontal en el Canvas
            val notaCorta = if (item.notas.length > 20) item.notas.take(20) + "..." else item.notas
            canvas.drawText(notaCorta, 350f, y, paint)

            y += 25f
        }

        // Pie de página (Watermark)
        paint.color = Color.GRAY
        paint.textSize = 10f
        canvas.drawText("Documento generado automáticamente por Hamparo App", 50f, 800f, paint)

        // Finalización del renderizado de la página actual
        pdfDocument.finishPage(page)

        // --- GUARDADO Y PERSISTENCIA ---
        // Normalización de nombre de archivo para compatibilidad con sistemas de ficheros (evitar caracteres ilegales)
        val nombreLimpio = nombreCompleto.replace(" ", "_").replace(".", "")

        // En vez de un número raro, ponemos la fecha y hora legible
        // Timestamp human-readable para facilitar la organización de archivos al usuario
        val fechaHoraArchivo = SimpleDateFormat("dd-MM-yyyy_HH-mm", Locale.getDefault()).format(Date())
        val nombreFichero = "${tipoNombre}_${nombreLimpio}_$fechaHoraArchivo.pdf"
        var outputStream: OutputStream? = null

        try {
            // ESTRATEGIA DE ALMACENAMIENTO ADAPTATIVO (Scoped Storage):
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // API 29+ (Android 10 y superior):
                // Uso de MediaStore y ContentResolver. No se requieren permisos de WRITE_EXTERNAL_STORAGE
                // explícitos en el Manifest para escribir en carpetas públicas propias.
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, nombreFichero)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

                if (uri != null) {
                    outputStream = resolver.openOutputStream(uri)
                }
            } else {
                // API < 29 (Legacy):
                // Acceso directo al sistema de archivos. Requiere gestión clásica de permisos.
                val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), nombreFichero)
                outputStream = FileOutputStream(file)
            }

            // Escritura del buffer binario en el stream
            if (outputStream != null) {
                pdfDocument.writeTo(outputStream)
                Toast.makeText(context, "✅ Guardado: $nombreFichero", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "❌ Error al crear el archivo", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            // GESTIÓN DE MEMORIA:
            // Es crítico cerrar los flujos (Streams) y liberar la instancia del documento nativo
            // en el bloque 'finally' para evitar fugas de memoria (Memory Leaks) y corrupción de archivos.
            outputStream?.close()
            pdfDocument.close()
        }
    }
}