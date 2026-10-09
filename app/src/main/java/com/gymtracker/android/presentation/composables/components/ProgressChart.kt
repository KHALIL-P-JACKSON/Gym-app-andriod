package com.gymtracker.android.presentation.composables.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gymtracker.android.domain.model.WeightUnit
import com.gymtracker.android.domain.service.ProgressCalculator
import com.gymtracker.android.presentation.Formatters
import com.gymtracker.android.presentation.composables.previews.PreviewData
import com.gymtracker.android.presentation.composables.theme.GymTrackerTheme

@Composable
fun ProgressChart(history: List<ProgressCalculator.Performance>, unit: WeightUnit) {
    val accent = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outlineVariant
    val values = history.map { unit.fromPounds(it.bestSet.estimatedOneRepMax) }
    val minimum = (values.minOrNull() ?: 0.0) * 0.9
    val maximum = (values.maxOrNull() ?: 1.0) * 1.1
    val range = (maximum - minimum).coerceAtLeast(1.0)
    TrackerCard {
        Text("Estimated 1RM Over Time", style = MaterialTheme.typography.titleMedium)
        Text("${Formatters.number(minimum)} – ${Formatters.number(maximum)} ${unit.label}", style = MaterialTheme.typography.labelMedium)
        Canvas(Modifier.fillMaxWidth().height(220.dp).semantics {
            contentDescription = "Estimated one rep max chart. Values by date are listed below."
        }) {
            val padding = 12.dp.toPx()
            val width = size.width - padding * 2
            val height = size.height - padding * 2
            for (line in 0..4) {
                val y = padding + height * line / 4
                drawLine(grid, Offset(padding, y), Offset(padding + width, y), 1.dp.toPx())
            }
            val start = history.firstOrNull()?.date?.toEpochMilli() ?: 0L
            val dateRange = ((history.lastOrNull()?.date?.toEpochMilli() ?: start) - start).coerceAtLeast(1L)
            val points = history.mapIndexed { index, performance ->
                val x = if (history.size == 1) padding + width / 2
                    else padding + width * ((performance.date.toEpochMilli() - start).toDouble() / dateRange).toFloat()
                val y = padding + height * (1.0 - (values[index] - minimum) / range).toFloat()
                Offset(x, y)
            }
            val path = Path()
            points.forEachIndexed { index, point ->
                if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
            }
            drawPath(path, accent, style = Stroke(width = 3.dp.toPx()))
            points.forEach { drawCircle(accent, 4.dp.toPx(), it) }
        }
        history.forEach { performance ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(Formatters.date(performance.date))
                Text(Formatters.weight(performance.bestSet.estimatedOneRepMax, unit))
            }
        }
    }
}
@Preview(showBackground = true)
@Composable
private fun ProgressChartPreview() { GymTrackerTheme { ProgressChart(
    ProgressCalculator.history(PreviewData.data.workouts, PreviewData.bench.id), WeightUnit.LB
) } }
