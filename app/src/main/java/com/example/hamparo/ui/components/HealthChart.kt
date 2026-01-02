package com.example.hamparo.ui.components

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.example.hamparo.data.local.entities.MedicionEntity
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.core.component.shape.LineComponent
import com.patrykandpatrick.vico.core.component.shape.Shapes
import com.patrykandpatrick.vico.core.entry.entryModelOf

@Composable
fun HealthChart(
    data: List<MedicionEntity>,
    modifier: Modifier = Modifier
) {
    // 1. Convertimos los datos de la BD a datos del gráfico.
    // Usamos toTypedArray() que es más seguro para evitar conflictos de tipos.
    val chartEntryModel = entryModelOf(*data.map { it.valor1 }.toTypedArray())

    // 2. Definimos el estilo de las barras
    val columnStyle = LineComponent(
        color = MaterialTheme.colorScheme.primary.toArgb(), // .toArgb() es la forma correcta
        thicknessDp = 16f,
        shape = Shapes.roundedCornerShape(allPercent = 25)
    )

    // 3. Dibujamos el gráfico
    Chart(
        chart = columnChart(
            columns = listOf(columnStyle)
        ),
        model = chartEntryModel,
        startAxis = rememberStartAxis(),
        bottomAxis = rememberBottomAxis(),
        modifier = modifier
            .height(200.dp)
            .padding(16.dp)
    )
}