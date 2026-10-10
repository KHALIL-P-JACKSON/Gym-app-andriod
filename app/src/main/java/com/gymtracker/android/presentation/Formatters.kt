package com.gymtracker.android.presentation

import com.gymtracker.android.domain.model.WeightUnit
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object Formatters {
    fun number(value: Double): String {
        val text = String.format(Locale.getDefault(), "%.2f", value)
        val decimal = java.text.DecimalFormatSymbols.getInstance().decimalSeparator
        return text.trimEnd('0').trimEnd(decimal)
    }
    fun weight(pounds: Double, unit: WeightUnit) = "${number(unit.fromPounds(pounds))} ${unit.label}"
    fun date(instant: Instant): String =
        DateTimeFormatter.ofPattern("MMM d, yyyy").withZone(ZoneId.systemDefault()).format(instant)
    fun greeting(): String = "Welcome back \nMy Love"
}
