package com.demo.healthtracker

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun formatDateTime(instant: Instant): String {
    return DateTimeFormatter
        .ofPattern("MMM dd, yyyy - hh:mm a")
        .withZone(ZoneId.systemDefault())
        .format(instant)
}

fun String.capitalize(): String {
    return this.split(' ').joinToString(" ") { word ->
        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }
}

@SuppressLint("NewApi")
fun getDurationText(startTime: Instant, endTime: Instant): String {
    val duration = Duration.between(startTime, endTime)
    val hours = duration.toHours()
    val minutes = duration.toMinutesPart()
    return "${hours}h ${minutes}m"
}

fun formatTime(instant: Instant): String {
    return DateTimeFormatter
        .ofPattern("hh:mm a")
        .withZone(ZoneId.systemDefault())
        .format(instant)
}

@RequiresApi(Build.VERSION_CODES.S)
fun formatDuration(start: Instant, end: Instant): String {
    val duration = Duration.between(start, end)
    val hours = duration.toHours()
    val minutes = duration.toMinutesPart()
    return if (hours > 0) {
        "${hours}h ${minutes}m"
    } else {
        "${minutes}m"
    }
}
