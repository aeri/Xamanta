package cat.naval.xamanta.protodoc

import com.google.protobuf.DescriptorProtos.FileDescriptorSet
import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.size < 2) {
        System.err.println("usage: protodoc <descriptor-set> <output-dir> [--check]")
        exitProcess(2)
    }
    val descriptorSet = File(args[0])
    val outputDir = File(args[1])
    val check = args.contains("--check")

    val set = descriptorSet.inputStream().buffered().use { FileDescriptorSet.parseFrom(it) }
    val pages = set.fileList.associate { file ->
        val renderer = Renderer(file)
        val page = Page(
            fileName = file.name.removeSuffix(".proto") + ".md",
            content = renderer.render(),
            warnings = renderer.warnings,
        )
        file.name to page
    }

    val undocumented = pages.values.sumOf { it.warnings.size }
    pages.values.forEach { page ->
        page.warnings.forEach { System.err.println("protodoc: ${page.fileName}: $it") }
    }

    if (check) {
        val stale = pages.values.filter { page ->
            val existing = File(outputDir, page.fileName)
            !existing.isFile || existing.readText() != page.content
        }
        if (stale.isNotEmpty()) {
            System.err.println(
                "protodoc: out of date: ${stale.joinToString(", ") { it.fileName }}. " +
                    "Run ./gradlew generateReferenceDocs and commit the result."
            )
            exitProcess(1)
        }
        println("protodoc: reference is up to date (${pages.size} pages, $undocumented undocumented elements)")
        return
    }

    outputDir.mkdirs()
    pages.values.forEach { page ->
        File(outputDir, page.fileName).writeText(page.content)
        println("protodoc: wrote ${page.fileName}")
    }
    println("protodoc: $undocumented undocumented elements")
}

class Page(val fileName: String, val content: String, val warnings: List<String>)
