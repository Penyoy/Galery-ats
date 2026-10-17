package com.ats.galery.util

import java.util.Calendar

private val MONTHS_ID = arrayOf(
    "Jan", "Feb", "Mar", "Apr", "Mei", "Jun",
    "Jul", "Agu", "Sep", "Okt", "Nov", "Des"
)

/** Label hari berbahasa Indonesia: "Hari ini", "Kemarin", "12 Okt". */
fun formatDayLabel(millis: Long): String {
    val target = Calendar.getInstance().apply { timeInMillis = millis }
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

    return when {
        sameDay(target, today) -> "Hari ini"
        sameDay(target, yesterday) -> "Kemarin"
        target.get(Calendar.YEAR) == today.get(Calendar.YEAR) ->
            "${target.get(Calendar.DAY_OF_MONTH)} ${MONTHS_ID[target.get(Calendar.MONTH)]}"
        else ->
            "${target.get(Calendar.DAY_OF_MONTH)} ${MONTHS_ID[target.get(Calendar.MONTH)]} ${target.get(Calendar.YEAR)}"
    }
}

fun formatFullDate(millis: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = millis }
    val time = "%02d:%02d".format(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
    return "${cal.get(Calendar.DAY_OF_MONTH)} ${MONTHS_ID[cal.get(Calendar.MONTH)]} ${cal.get(Calendar.YEAR)} • $time"
}

fun startOfDay(millis: Long): Long =
    Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

/** "12:05" atau "1:02:30" untuk durasi video. */
fun formatDuration(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val totalSeconds = ms / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

fun formatSize(bytes: Long): String {
    if (bytes <= 0L) return "-"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unit = 0
    while (value >= 1024.0 && unit < units.lastIndex) {
        value /= 1024.0
        unit++
    }
    return if (unit == 0) "${value.toLong()} ${units[unit]}"
    else "%.1f %s".format(value, units[unit])
}

private fun sameDay(a: Calendar, b: Calendar): Boolean =
    a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
        a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
