package cat.naval.xamanta.proto

internal inline fun <reified E : Enum<E>> Enum<*>.asModelOrNull(): E? {
    val protoName = name
    return enumValues<E>().firstOrNull { it.name == protoName }
}

internal inline fun <reified E : Enum<E>> Enum<*>.asModel(default: E): E =
    asModelOrNull<E>() ?: default
