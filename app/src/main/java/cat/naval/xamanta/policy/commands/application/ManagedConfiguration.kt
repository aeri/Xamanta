package cat.naval.xamanta.policy.commands.application

import android.os.Bundle
import android.os.Parcelable
import cat.naval.xamanta.models.ManagedProperty

private const val MAX_DEPTH = 8

object ManagedConfiguration {

    fun validate(properties: List<ManagedProperty>, depth: Int = 1): String? {
        val seen = mutableSetOf<String>()
        properties.forEach { property ->
            val key = property.key
            if (key.isBlank()) return "managed configuration has a blank key"
            if (!seen.add(key)) return "duplicate managed configuration key '$key'"

            val values = property.valueCount()
            if (values != 1) {
                return "managed configuration key '$key' must carry exactly one value, found $values"
            }
            val nested = property.bundleValue?.let { listOf(it) } ?: property.bundleArrayValue
            if (nested != null) {
                if (depth >= MAX_DEPTH) {
                    return "managed configuration key '$key' nests deeper than $MAX_DEPTH levels"
                }
                nested.forEach { inner -> validate(inner, depth + 1)?.let { return it } }
            }
        }
        return null
    }

    fun toBundle(properties: List<ManagedProperty>): Bundle {
        validate(properties)?.let { throw IllegalArgumentException(it) }
        return build(properties)
    }

    @Suppress("DEPRECATION")
    fun contentEquals(left: Bundle, right: Bundle): Boolean {
        if (left.size() != right.size()) return false
        return left.keySet().all { key ->
            right.containsKey(key) && valuesEqual(left.get(key), right.get(key))
        }
    }

    private fun ManagedProperty.valueCount(): Int = listOf(
        stringValue,
        boolValue,
        intValue,
        stringListValue,
        bundleValue,
        bundleArrayValue,
    ).count { it != null }

    private fun build(properties: List<ManagedProperty>): Bundle = Bundle().apply {
        properties.forEach { property ->
            val key = property.key
            when {
                property.stringValue != null -> putString(key, property.stringValue)
                property.boolValue != null -> putBoolean(key, property.boolValue)
                property.intValue != null -> putInt(key, property.intValue)
                property.stringListValue != null ->
                    putStringArray(key, property.stringListValue.toTypedArray())

                property.bundleValue != null -> putBundle(key, build(property.bundleValue))
                property.bundleArrayValue != null -> putParcelableArray(
                    key,
                    property.bundleArrayValue.map { build(it) }.toTypedArray<Parcelable>(),
                )
            }
        }
    }

    private fun valuesEqual(left: Any?, right: Any?): Boolean = when {
        left is Bundle && right is Bundle -> contentEquals(left, right)
        left is Array<*> && right is Array<*> ->
            left.size == right.size && left.indices.all { valuesEqual(left[it], right[it]) }

        else -> left == right
    }
}
