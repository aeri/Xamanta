package cat.naval.xamanta.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private const val ISO_PATTERN = "yyyy-MM-dd'T'HH:mm:ss'Z'"

internal fun isoNow(): String = isoOf(System.currentTimeMillis())

internal fun isoOf(millis: Long): String = SimpleDateFormat(ISO_PATTERN, Locale.US)
    .apply { timeZone = TimeZone.getTimeZone("UTC") }
    .format(Date(millis))

internal fun parseIsoOrNull(value: String?): Long? {
    if (value.isNullOrBlank()) return null
    return runCatching {
        SimpleDateFormat(ISO_PATTERN, Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .parse(value)?.time
    }.getOrNull()
}
