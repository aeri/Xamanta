import com.google.protobuf.gradle.id
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.protobuf)
    alias(libs.plugins.kotlin.serialization)
}

android {
    val keystorePropertiesFile = rootProject.file("sign/keystore.properties")
    val keystoreProperties = keystorePropertiesFile.takeIf { it.isFile }?.let { file ->
        Properties().apply { file.inputStream().use { load(it) } }
    }

    signingConfigs {

        keystoreProperties?.let { properties ->
            create("release") {
                storeFile = rootProject.file("sign/xamanta.jks")
                storePassword = properties["storePassword"] as String
                keyAlias = properties["keyAlias"] as String
                keyPassword = properties["keyPassword"] as String
            }
        }
    }
    namespace = "cat.naval.xamanta"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "cat.naval.xamanta"
        minSdk = 21
        targetSdk = 36
        versionCode = 8
        versionName = "1.0.0-beta.1"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.findByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    lint {
        abortOnError = true
        checkReleaseBuilds = true
        error += listOf(
            "NewApi",
            "InlinedApi",
            "UnsafeProtectedBroadcastReceiver",
            "ExportedReceiver",
        )
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${libs.versions.protoc.get()}"
    }
    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:${libs.versions.grpc.get()}"
        }
        create("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:${libs.versions.grpcKotlin.get()}:jdk8@jar"
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                id("java") {
                    option("lite")
                }
            }
            task.plugins {
                id("grpc") {
                    option("lite")
                }
                id("grpckt") {
                    option("lite")
                }
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.material)
    implementation(libs.grpc.okhttp)
    implementation(libs.grpc.protobuf.lite)
    implementation(libs.grpc.stub)
    implementation(libs.grpc.kotlin.stub)
    implementation(libs.javax.annotation)
    testImplementation(libs.junit)
}
