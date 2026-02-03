package com.example.hamparo.ui.utils

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast

object PDFDownloader {

    /**
     * Descarga un PDF desde una URL asegurando un nombre de archivo limpio.
     */
    fun descargar(context: Context, url: String, nombreMedicamento: String) {
        try {
            // 1. Limpieza de nombre
            val nombreLimpio = nombreMedicamento
                .replace(" ", "_")
                .replace("/", "-")
                .replace(Regex("[^a-zA-Z0-9_\\-]"), "")
                .take(60)

            val nombreFichero = "$nombreLimpio.pdf"

            // 2. Configurar la petición
            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle(nombreMedicamento) // Título de la notificación
                .setDescription("Descargando prospecto oficial...") // 👇 ¡AQUÍ ESTABA EL ERROR! (setDescription)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, nombreFichero)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            // 3. Ejecutar
            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            manager.enqueue(request)

            Toast.makeText(context, "Guardando: $nombreFichero", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Toast.makeText(context, "Error al iniciar descarga: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }
}