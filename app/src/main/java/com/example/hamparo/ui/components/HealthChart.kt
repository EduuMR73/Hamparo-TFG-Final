package com.example.hamparo.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.hamparo.data.model.Medicion

@Composable
fun HealthChart(
    datos: List<Medicion>,
    titulo: String,
    unidad: String,
    modifier: Modifier = Modifier
) {
    // 1. Convertimos los datos para asegurarnos de que se pueden pintar
    val datosValidos = remember(datos) {
        datos.filter { it.valor1.toFloatOrNull() != null }
    }

    // Detectamos si es Tensión (tiene valor2)
    val esTension = remember(datosValidos) {
        datosValidos.any { it.valor2.toFloatOrNull() != null }
    }

    Card(
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("📈", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = titulo,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (datosValidos.isNotEmpty())
                            "Último: ${datosValidos.last().valor1} $unidad"
                        else "Sin datos válidos",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (datosValidos.size < 2) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Se necesitan más registros para gráfica", color = Color.LightGray)
                }
            } else {
                ChartCanvas(datosValidos, esTension)
            }
        }
    }
}

@Composable
fun ChartCanvas(datos: List<Medicion>, esTension: Boolean) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = Color(0xFFE91E63)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .padding(start = 10.dp, end = 10.dp, bottom = 20.dp)
    ) {
        val width = size.width
        val height = size.height

        val maxVal1 = datos.maxOfOrNull { it.valor1.toFloatOrNull() ?: 0f } ?: 100f
        val maxVal2 = if(esTension) datos.maxOfOrNull { it.valor2.toFloatOrNull() ?: 0f } ?: 0f else 0f
        val maxGlobal = maxOf(maxVal1, maxVal2) * 1.2f

        val spacingX = width / (datos.size - 1).coerceAtLeast(1)

        val textPaint = Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = 30f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }

        // --- 1. REJILLA ---
        drawLine(Color.LightGray.copy(0.3f), Offset(0f, height), Offset(width, height), 2f)

        // --- 2. FUNCIÓN DE DIBUJO ---
        fun drawLinePath(extractor: (Medicion) -> Float, color: Color, fill: Boolean) {
            val path = Path()
            val fillPath = Path()

            datos.forEachIndexed { index, item ->
                val valor = extractor(item)
                val x = index * spacingX
                val y = height - (valor / maxGlobal) * height

                if (index == 0) {
                    path.moveTo(x, y)
                    fillPath.moveTo(x, height)
                    fillPath.lineTo(x, y)
                } else {
                    path.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
                if (index == datos.size - 1) {
                    fillPath.lineTo(x, height)
                    fillPath.close()
                }

                drawCircle(Color.White, 8f, Offset(x, y))
                drawCircle(color, 5f, Offset(x, y))
            }

            if (fill) {
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(color.copy(alpha = 0.2f), Color.Transparent),
                        startY = 0f,
                        endY = height
                    )
                )
            }

            drawPath(
                path = path,
                color = color,
                style = Stroke(
                    width = 5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )
        }

        drawLinePath({ it.valor1.toFloatOrNull() ?: 0f }, primaryColor, fill = !esTension)

        if (esTension) {
            drawLinePath({ it.valor2.toFloatOrNull() ?: 0f }, secondaryColor, fill = false)
        }

        // --- 3. FECHAS ---
        datos.forEachIndexed { index, item ->
            val x = index * spacingX
            if (datos.size < 6 || index % (datos.size/4) == 0) {
                val fechaCorta = item.fecha.take(5)
                drawContext.canvas.nativeCanvas.drawText(
                    fechaCorta, x, height + 45f, textPaint
                )
            }
        }
    }
}