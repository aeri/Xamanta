import javax.inject.Inject
import org.gradle.process.ExecOperations

plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

dependencies {
    implementation(libs.protobuf.java)
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("cat.naval.xamanta.protodoc.MainKt")
}

// protoc is a native executable published as a classified Maven artifact. The protobuf Gradle
// plugin resolves the same coordinate for :app; resolving it again here keeps this module
// independent of that plugin's internal layout.
val protocTool: Configuration by configurations.creating { isTransitive = false }

fun protocClassifier(): String {
    val os = System.getProperty("os.name").lowercase()
    val arch = when (val a = System.getProperty("os.arch").lowercase()) {
        "x86_64", "amd64" -> "x86_64"
        "aarch64", "arm64" -> "aarch_64"
        else -> a
    }
    val platform = when {
        os.contains("win") -> "windows"
        os.contains("mac") || os.contains("darwin") -> "osx"
        else -> "linux"
    }
    return "$platform-$arch"
}

dependencies {
    protocTool("com.google.protobuf:protoc:${libs.versions.protoc.get()}:${protocClassifier()}@exe")
}

abstract class DescriptorSetTask : DefaultTask() {
    @get:InputFiles abstract val protocArtifact: ConfigurableFileCollection
    @get:InputDirectory abstract val protoDir: DirectoryProperty
    @get:OutputFile abstract val descriptorSet: RegularFileProperty
    @get:Inject abstract val execOps: ExecOperations

    @TaskAction
    fun generate() {
        val dir = protoDir.get().asFile
        val protos = dir.listFiles { f -> f.isFile && f.name.endsWith(".proto") }
            ?.sortedBy { it.name }
            ?: error("no protos in $dir")
        require(protos.isNotEmpty()) { "no protos in $dir" }

        val out = descriptorSet.get().asFile
        out.parentFile.mkdirs()

        val protoc = File(out.parentFile, if (System.getProperty("os.name").lowercase().contains("win")) "protoc.exe" else "protoc")
        protocArtifact.singleFile.copyTo(protoc, overwrite = true)
        protoc.setExecutable(true)

        execOps.exec {
            executable = protoc.absolutePath
            args("--include_source_info")
            args("--descriptor_set_out=${out.absolutePath}")
            args("--proto_path=${dir.absolutePath}")
            args(protos.map { it.name })
        }
    }
}

val protoDescriptorSet by tasks.registering(DescriptorSetTask::class) {
    description = "Compiles the protos to a descriptor set carrying their doc comments."
    protocArtifact.from(protocTool)
    protoDir.set(rootProject.layout.projectDirectory.dir("app/src/main/proto"))
    descriptorSet.set(layout.buildDirectory.file("protodoc/descriptors.pb"))
}

val referenceDir = rootProject.layout.projectDirectory.dir("reference")

val generateReferenceDocs by tasks.registering(JavaExec::class) {
    group = "documentation"
    description = "Regenerates reference/*.md from the doc comments in the protos."
    dependsOn(protoDescriptorSet)
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("cat.naval.xamanta.protodoc.MainKt")
    argumentProviders.add {
        listOf(
            protoDescriptorSet.get().descriptorSet.get().asFile.absolutePath,
            referenceDir.asFile.absolutePath,
        )
    }
}

val checkReferenceDocs by tasks.registering(JavaExec::class) {
    group = "verification"
    description = "Fails if reference/*.md is out of date with the protos."
    dependsOn(protoDescriptorSet)
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("cat.naval.xamanta.protodoc.MainKt")
    argumentProviders.add {
        listOf(
            protoDescriptorSet.get().descriptorSet.get().asFile.absolutePath,
            referenceDir.asFile.absolutePath,
            "--check",
        )
    }
}

tasks.named("check") { dependsOn(checkReferenceDocs) }
