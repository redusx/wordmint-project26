package com.example.wordstore.ui.format

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.wordstore.R
import com.example.wordstore.domain.scheduling.Rating
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.roundToInt

enum class IntervalUnit { MINUTES, HOURS, DAYS, MONTHS, YEARS }

data class IntervalParts(val value: Int, val unit: IntervalUnit) {
    companion object {
        private const val MINUTE = 60_000L
        private const val HOUR = 60 * MINUTE
        private const val DAY = 24 * HOUR

        /** Rounds first and picks the unit after, so a value never reads "60 min" or "24 h". */
        fun of(millis: Long): IntervalParts = when {
            millis < HOUR - MINUTE / 2 -> IntervalParts((millis.toDouble() / MINUTE).roundToInt().coerceAtLeast(1), IntervalUnit.MINUTES)
            millis < DAY - HOUR / 2 -> IntervalParts((millis.toDouble() / HOUR).roundToInt(), IntervalUnit.HOURS)
            millis < 30 * DAY - DAY / 2 -> IntervalParts((millis.toDouble() / DAY).roundToInt(), IntervalUnit.DAYS)
            millis < 365 * DAY -> IntervalParts((millis.toDouble() / (30.44 * DAY)).roundToInt().coerceAtLeast(1), IntervalUnit.MONTHS)
            else -> IntervalParts((millis.toDouble() / (365.25 * DAY)).roundToInt().coerceAtLeast(1), IntervalUnit.YEARS)
        }
    }
}

/** A compact interval for tight spaces, such as "10 min" or "3 d". */
@Composable
fun shortInterval(millis: Long): String {
    val parts = IntervalParts.of(millis)
    val res = when (parts.unit) {
        IntervalUnit.MINUTES -> R.string.interval_minutes
        IntervalUnit.HOURS -> R.string.interval_hours
        IntervalUnit.DAYS -> R.string.interval_days
        IntervalUnit.MONTHS -> R.string.interval_months
        IntervalUnit.YEARS -> R.string.interval_years
    }
    return stringResource(res, parts.value)
}

/** Time until [dueAt], or "now" once it has passed. */
@Composable
fun dueText(dueAt: Long, nowMillis: Long): String =
    if (dueAt <= nowMillis) stringResource(R.string.due_now) else stringResource(R.string.due_in, shortInterval(dueAt - nowMillis))

@Composable
fun ratingLabel(rating: Rating): String = stringResource(
    when (rating) {
        Rating.AGAIN -> R.string.rating_again
        Rating.HARD -> R.string.rating_hard
        Rating.GOOD -> R.string.rating_good
        Rating.EASY -> R.string.rating_easy
    }
)

@Composable
fun todayHeading(millis: Long): String {
    val locale = LocalConfiguration.current.locales[0]
    val pattern = DateFormat.getBestDateTimePattern(locale, "EEEEdMMMM")
    return DateTimeFormatter.ofPattern(pattern, locale).format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))
}

@Composable
fun dateTimeText(millis: Long): String {
    val locale = LocalConfiguration.current.locales[0]
    return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        .withLocale(locale)
        .format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))
}

@Composable
fun dayText(epochDay: Long): String {
    val locale = LocalConfiguration.current.locales[0]
    val pattern = DateFormat.getBestDateTimePattern(locale, "EEEdMMM")
    return DateTimeFormatter.ofPattern(pattern, locale).format(LocalDate.ofEpochDay(epochDay))
}

@Composable
fun weekdayInitial(epochDay: Long): String {
    val locale = LocalConfiguration.current.locales[0]
    return DateTimeFormatter.ofPattern("EEEEE", locale).format(LocalDate.ofEpochDay(epochDay))
}

/** A share from 0 to 1 in the current locale: "85%" in English, "%85" in Turkish. */
@Composable
fun percentText(share: Double): String =
    NumberFormat.getPercentInstance(LocalConfiguration.current.locales[0]).format(share)

@Composable
fun clockText(hour: Int, minute: Int): String {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val pattern = DateFormat.getBestDateTimePattern(locale, if (DateFormat.is24HourFormat(context)) "Hm" else "hm")
    return DateTimeFormatter.ofPattern(pattern, locale).format(java.time.LocalTime.of(hour, minute))
}

@Composable
fun durationText(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = (totalSeconds / 60).toInt()
    val seconds = (totalSeconds % 60).toInt()
    return if (minutes > 0) {
        stringResource(R.string.duration_minutes_seconds, minutes, seconds)
    } else {
        stringResource(R.string.duration_seconds, seconds)
    }
}
