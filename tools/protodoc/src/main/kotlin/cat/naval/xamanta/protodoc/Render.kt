package cat.naval.xamanta.protodoc

import com.google.protobuf.DescriptorProtos.DescriptorProto
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type
import com.google.protobuf.DescriptorProtos.FileDescriptorProto

private val ATTRIBUTION = """
> Portions of this documentation are copied or adapted from the Android Management API reference
> (https://developers.google.com/android/management/reference/rest/v1/), (c) Google LLC, licensed
> under the Creative Commons Attribution 4.0 License
> (https://creativecommons.org/licenses/by/4.0/). Descriptions of fields and enum values that have
> an Android Management API counterpart are reproduced verbatim from its discovery document; the
> rest describe Xamanta's own surface and say so. Xamanta is not affiliated with, endorsed by, or
> sponsored by Google. Android is a trademark of Google LLC.
""".trimIndent()

class Renderer(private val file: FileDescriptorProto) {
    private val source = SourceInfo(file)
    private val sections = sectionsOf(file, source)
    private val mapEntries = HashMap<String, DescriptorProto>()
    private val headings = HashMap<String, String>()
    private val known = HashSet<String>()
    val warnings = ArrayList<String>()

    init {
        indexTypes(file.messageTypeList, ".${file.getPackage()}")
        file.enumTypeList.forEach { known += ".${file.getPackage()}.${it.name}" }
        resolveHeadings()
    }

    private fun indexTypes(messages: List<DescriptorProto>, scope: String) {
        for (m in messages) {
            val full = "$scope.${m.name}"
            if (m.options.mapEntry) mapEntries[full] = m else known += full
            indexTypes(m.nestedTypeList, full)
            m.enumTypeList.forEach { known += "$full.${it.name}" }
        }
    }

    private fun resolveHeadings() {
        val prefix = ".${file.getPackage()}."
        val bySimple = known.groupBy { it.substringAfterLast('.') }
        for (full in known) {
            val simple = full.substringAfterLast('.')
            headings[full] = if (bySimple.getValue(simple).size == 1) simple else full.removePrefix(prefix)
        }
    }

    private fun resourceSection(): Section? = sections.firstOrNull { it is MessageSection }

    private fun fullNameOf(section: Section): String {
        val parts = ArrayList<String>()
        var messages: List<DescriptorProto> = file.messageTypeList
        val path = section.path
        var i = 0
        while (i < path.size) {
            val descends = if (i == 0) path[i] == FILE_MESSAGE else path[i] == MESSAGE_NESTED
            if (descends) {
                val m = messages[path[i + 1]]
                parts += m.name
                messages = m.nestedTypeList
            } else {
                parts += section.name
            }
            i += 2
        }
        return ".${file.getPackage()}." + parts.joinToString(".")
    }

    private fun headingFor(section: Section): String = headings[fullNameOf(section)] ?: section.name

    private fun anchor(text: String): String =
        text.lowercase().replace(Regex("[^a-z0-9 -]"), "").replace(' ', '-')

    private fun link(typeName: String): String {
        val heading = headings[typeName]
        if (heading == null) {
            warnings += "unresolved type reference $typeName"
            return typeName.substringAfterLast('.')
        }
        return "[$heading](#${anchor(heading)})"
    }

    private fun shortName(typeName: String): String =
        headings[typeName] ?: typeName.substringAfterLast('.')

    private fun scalar(type: Type): String = when (type) {
        Type.TYPE_STRING -> "string"
        Type.TYPE_BOOL -> "boolean"
        Type.TYPE_INT32, Type.TYPE_SINT32, Type.TYPE_SFIXED32 -> "integer"
        Type.TYPE_UINT32, Type.TYPE_FIXED32 -> "integer (uint32 format)"
        Type.TYPE_INT64, Type.TYPE_SINT64, Type.TYPE_SFIXED64 -> "string (int64 format)"
        Type.TYPE_UINT64, Type.TYPE_FIXED64 -> "string (uint64 format)"
        Type.TYPE_FLOAT, Type.TYPE_DOUBLE -> "number"
        Type.TYPE_BYTES -> "string (bytes format)"
        else -> type.name
    }

    private fun mapEntryOf(field: FieldDescriptorProto): DescriptorProto? =
        if (field.type == Type.TYPE_MESSAGE) mapEntries[field.typeName] else null

    private fun singleJson(field: FieldDescriptorProto, indent: String): String = when (field.type) {
        Type.TYPE_MESSAGE -> "{\n$indent  object (${shortName(field.typeName)})\n$indent}"
        Type.TYPE_ENUM -> "enum (${shortName(field.typeName)})"
        else -> scalar(field.type)
    }

    private fun jsonValue(field: FieldDescriptorProto, indent: String): String {
        val entry = mapEntryOf(field)
        if (entry != null) {
            val k = scalar(entry.getField(0).type)
            val v = entry.getField(1).let {
                if (it.type == Type.TYPE_MESSAGE) "object (${shortName(it.typeName)})" else scalar(it.type)
            }
            return "{\n$indent  $k: $v,\n$indent  ...\n$indent}"
        }
        if (field.label != Label.LABEL_REPEATED) return singleJson(field, indent)
        return "[\n$indent  ${singleJson(field, "$indent  ")}\n$indent]"
    }

    private fun tableType(field: FieldDescriptorProto): String {
        val entry = mapEntryOf(field)
        if (entry != null) {
            val k = scalar(entry.getField(0).type)
            val v = entry.getField(1).let {
                if (it.type == Type.TYPE_MESSAGE) "object (${link(it.typeName)})" else scalar(it.type)
            }
            return "`map (key: $k, value: $v)`"
        }
        return when (field.type) {
            Type.TYPE_MESSAGE -> "`object (`${link(field.typeName)}`)`"
            Type.TYPE_ENUM -> "`enum (`${link(field.typeName)}`)`"
            else -> "`${scalar(field.type)}`"
        }
    }

    private fun fieldLabel(field: FieldDescriptorProto): String =
        if (field.label == Label.LABEL_REPEATED && mapEntryOf(field) == null) "${field.jsonName}[]"
        else field.jsonName

    private fun cell(text: String): String {
        if (text.isBlank()) return ""
        return text.split(Regex("\n[ \t]*\n"))
            .joinToString("<br><br>") { paragraph ->
                paragraph.lines().joinToString(" ") { it.trim() }.trim()
            }
            .replace("|", "\\|")
            .trim()
    }

    fun render(): String = buildString {
        val packageDoc = source.comment(listOf(FILE_PACKAGE))
        val title = packageDoc.lineSequence().firstOrNull()?.trim().orEmpty()
            .ifBlank { file.name.removeSuffix(".proto").replaceFirstChar { it.uppercase() } }
        val preamble = packageDoc.lineSequence().drop(1).joinToString("\n").trim()

        appendLine("<!-- Generated by :tools:protodoc from app/src/main/proto/${file.name}.")
        appendLine("     Edit the doc comments in that proto, then run ./gradlew generateReferenceDocs. -->")
        appendLine()
        appendLine("# $title")
        appendLine()
        appendLine(ATTRIBUTION)
        appendLine()
        if (preamble.isNotEmpty()) {
            appendLine(preamble)
            appendLine()
        }
        val resource = resourceSection()
        for (section in sections) {
            when (section) {
                is MessageSection -> renderMessage(section, section === resource)
                is EnumSection -> renderEnum(section)
                is ServiceSection -> renderService(section)
            }
        }
    }

    private fun StringBuilder.renderMessage(section: MessageSection, isResource: Boolean) {
        val heading = headingFor(section)
        appendLine(if (isResource) "## Resource: $heading" else "## $heading")
        appendLine()
        val doc = source.comment(section.path)
        if (doc.isBlank()) {
            warnings += "message ${section.name} has no doc comment"
        } else {
            appendLine(doc)
            appendLine()
        }

        if (section.descriptor.fieldList.isEmpty()) {
            appendLine("This message has no fields.")
            appendLine()
        } else {
            appendLine("### JSON representation")
            appendLine()
            appendLine("```json")
            appendLine("{")
            append(jsonBody(section))
            appendLine("}")
            appendLine("```")
            appendLine()
            appendLine("### Fields")
            appendLine()
            appendLine("| Field | Type | Description |")
            appendLine("|---|---|---|")
            section.descriptor.fieldList.forEachIndexed { i, f ->
                val fieldDoc = source.comment(section.path + listOf(MESSAGE_FIELD, i))
                if (fieldDoc.isBlank()) warnings += "field ${section.name}.${f.name} has no doc comment"
                appendLine("| `${fieldLabel(f)}` | ${tableType(f)} | ${cell(fieldDoc)} |")
            }
            appendLine()
        }

        val reserved = reservedOf(section.descriptor)
        if (reserved.isNotEmpty()) {
            appendLine("**Reserved:** $reserved. These numbers are burned and must never be reused.")
            appendLine()
        }
    }

    private fun jsonBody(section: MessageSection): String = buildString {
        val fields = section.descriptor.fieldList
        val plain = fields.filter { !it.hasOneofIndex() || it.proto3Optional }
        val blocks = ArrayList<String>()
        if (plain.isNotEmpty()) {
            blocks += plain.joinToString(",\n") { "  \"${it.jsonName}\": ${jsonValue(it, "  ")}" }
        }
        section.descriptor.oneofDeclList.forEachIndexed { index, oneof ->
            val members = fields.filter { it.hasOneofIndex() && it.oneofIndex == index && !it.proto3Optional }
            if (members.isEmpty()) return@forEachIndexed
            blocks += buildString {
                appendLine("  // Union field ${oneof.name} can be only one of the following:")
                append(members.joinToString(",\n") { "  \"${it.jsonName}\": ${jsonValue(it, "  ")}" })
                appendLine()
                append("  // End of list of possible types for union field ${oneof.name}.")
            }
        }
        append(blocks.joinToString(",\n\n"))
        appendLine()
    }

    private fun reservedOf(message: DescriptorProto): String {
        val ranges = message.reservedRangeList.map {
            if (it.end - it.start == 1) "${it.start}" else "${it.start}-${it.end - 1}"
        }
        val names = message.reservedNameList.map { "`$it`" }
        return (ranges + names).joinToString(", ")
    }

    private fun StringBuilder.renderEnum(section: EnumSection) {
        appendLine("## ${headingFor(section)}")
        appendLine()
        val doc = source.comment(section.path)
        if (doc.isNotBlank()) {
            appendLine(doc)
            appendLine()
        }
        appendLine("### Enums")
        appendLine()
        appendLine("| Value | Description |")
        appendLine("|---|---|")
        section.descriptor.valueList.forEachIndexed { i, v ->
            val valueDoc = source.comment(section.path + listOf(ENUM_VALUE, i))
            if (valueDoc.isBlank()) warnings += "enum value ${section.name}.${v.name} has no doc comment"
            appendLine("| `${v.name}` | ${cell(valueDoc)} |")
        }
        appendLine()
    }

    private fun StringBuilder.renderService(section: ServiceSection) {
        val serviceDoc = source.comment(section.path)
        if (serviceDoc.isBlank()) warnings += "service ${section.name} has no doc comment"
        section.descriptor.methodList.forEachIndexed { i, m ->
            appendLine("## Method: ${section.name}.${m.name}")
            appendLine()
            if (i == 0 && serviceDoc.isNotBlank()) {
                appendLine(serviceDoc)
                appendLine()
            }
            val methodDoc = source.comment(section.path + listOf(SERVICE_METHOD, i))
            if (methodDoc.isBlank()) {
                warnings += "method ${section.name}.${m.name} has no doc comment"
            } else {
                appendLine(methodDoc)
                appendLine()
            }
            appendLine("### gRPC signature")
            appendLine()
            appendLine("```proto")
            val input = (if (m.clientStreaming) "stream " else "") + m.inputType.substringAfterLast('.')
            val output = (if (m.serverStreaming) "stream " else "") + m.outputType.substringAfterLast('.')
            appendLine("rpc ${m.name}($input) returns ($output);")
            appendLine("```")
            appendLine()
            appendLine("| Direction | Message | Streaming |")
            appendLine("|---|---|---|")
            appendLine("| Device to server | ${link(m.inputType)} | ${if (m.clientStreaming) "yes" else "no"} |")
            appendLine("| Server to device | ${link(m.outputType)} | ${if (m.serverStreaming) "yes" else "no"} |")
            appendLine()
        }
    }
}
