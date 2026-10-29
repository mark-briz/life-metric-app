package com.markbriz.lifemetric.domain

import com.markbriz.lifemetric.data.BloodPressureReading

/** Text alternatives for charts (WCAG 1.1.1), read by TalkBack in place of the drawing. */

fun describeBloodPressureChart(readings: List<BloodPressureReading>): String {
    val ordered = chronologicalBp(readings)
    if (ordered.isEmpty()) return "Blood pressure chart. No readings."
    val latest = ordered.last()
    val noun = if (ordered.size == 1) "reading" else "readings"
    return "Blood pressure, last ${ordered.size} $noun. " +
        "Systolic ${ordered.minOf { it.systolic }} to ${ordered.maxOf { it.systolic }}, latest ${latest.systolic}. " +
        "Diastolic ${ordered.minOf { it.diastolic }} to ${ordered.maxOf { it.diastolic }}, latest ${latest.diastolic}."
}

fun describeWeightChart(model: WeightChartModel, goalLb: Double?, useKg: Boolean): String {
    if (model.points.isEmpty()) return "Weight chart. No readings in this range."
    val lows = model.points.map { it.weight }
    val latestTrend = model.trendByDate[model.points.last().date]
    return buildString {
        append("Weight, ${shortDateLabel(model.startDate)} to ${shortDateLabel(model.endDate)}. ")
        append("Daily lows ${formatWeightWithUnit(lows.min(), useKg)} to ${formatWeightWithUnit(lows.max(), useKg)}.")
        if (latestTrend != null) append(" 7-day trend latest ${formatWeightWithUnit(latestTrend, useKg)}.")
        if (goalLb != null) append(" Goal ${formatWeightWithUnit(goalLb, useKg)}.")
    }
}

fun describeWeekBars(data: List<WeekBucket>, title: String, valueOf: (WeekBucket) -> Int): String {
    if (data.isEmpty()) return "$title chart. No weeks to show."
    val weeks = data.joinToString(", ") { "${it.label}: ${valueOf(it)}" }
    return "$title per week, last ${data.size} weeks. $weeks. Total ${data.sumOf(valueOf)}."
}
