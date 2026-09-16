package cat.naval.xamanta.protodoc

import com.google.protobuf.DescriptorProtos.DescriptorProto
import com.google.protobuf.DescriptorProtos.EnumDescriptorProto
import com.google.protobuf.DescriptorProtos.FileDescriptorProto
import com.google.protobuf.DescriptorProtos.ServiceDescriptorProto

const val FILE_PACKAGE = 2
const val FILE_MESSAGE = 4
const val FILE_ENUM = 5
const val FILE_SERVICE = 6
const val MESSAGE_FIELD = 2
const val MESSAGE_NESTED = 3
const val MESSAGE_ENUM = 4
const val ENUM_VALUE = 2
const val SERVICE_METHOD = 2

class SourceInfo(file: FileDescriptorProto) {
    private val comments = HashMap<List<Int>, String>()
    private val lines = HashMap<List<Int>, Int>()

    init {
        for (loc in file.sourceCodeInfo.locationList) {
            val path = loc.pathList.toList()
            if (loc.spanCount > 0) lines.putIfAbsent(path, loc.getSpan(0))
            val raw = loc.leadingComments.ifBlank { loc.trailingComments }
            if (raw.isNotBlank()) comments.putIfAbsent(path, clean(raw))
        }
    }

    fun comment(path: List<Int>): String = comments[path].orEmpty()

    fun line(path: List<Int>): Int = lines[path] ?: Int.MAX_VALUE

    private fun clean(raw: String): String =
        raw.lines().joinToString("\n") { it.removePrefix(" ") }.trim()
}

sealed interface Section {
    val name: String
    val path: List<Int>
}

class MessageSection(
    override val name: String,
    override val path: List<Int>,
    val descriptor: DescriptorProto,
) : Section

class EnumSection(
    override val name: String,
    override val path: List<Int>,
    val descriptor: EnumDescriptorProto,
) : Section

class ServiceSection(
    override val name: String,
    override val path: List<Int>,
    val descriptor: ServiceDescriptorProto,
) : Section

fun sectionsOf(file: FileDescriptorProto, source: SourceInfo): List<Section> {
    val top = ArrayList<Section>()
    file.messageTypeList.forEachIndexed { i, m ->
        top += collectMessage(m, listOf(FILE_MESSAGE, i), source)
    }
    file.enumTypeList.forEachIndexed { i, e ->
        top += EnumSection(e.name, listOf(FILE_ENUM, i), e)
    }
    file.serviceList.forEachIndexed { i, s ->
        top += ServiceSection(s.name, listOf(FILE_SERVICE, i), s)
    }
    return top.sortedBy { source.line(it.path) }
}

private fun collectMessage(
    message: DescriptorProto,
    path: List<Int>,
    source: SourceInfo,
): List<Section> {
    if (message.options.mapEntry) return emptyList()
    val nested = ArrayList<Section>()
    message.nestedTypeList.forEachIndexed { i, m ->
        nested += collectMessage(m, path + listOf(MESSAGE_NESTED, i), source)
    }
    message.enumTypeList.forEachIndexed { i, e ->
        nested += EnumSection(e.name, path + listOf(MESSAGE_ENUM, i), e)
    }
    return listOf(MessageSection(message.name, path, message)) +
        nested.sortedBy { source.line(it.path) }
}
