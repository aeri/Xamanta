package cat.naval.xamanta.util

import android.util.Log
import kotlinx.serialization.json.Json

val json = Json { ignoreUnknownKeys = true }

inline fun <reified T> decodeJsonOrNull(tag: String, describe: String, raw: String?): T? =
    raw?.let { text ->
        runCatching { json.decodeFromString<T>(text) }
            .onFailure { Log.e(tag, "unreadable JSON in $describe: ${it.message}") }
            .getOrNull()
    }
