import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform") version "1.8.22"

    id("org.jetbrains.compose") version "1.4.1"
}

group = "be.zvz.alsonguploader"
val isDev = false
val versionNumber = "1.0.0"
version = "${versionNumber}${if (isDev) "-SNAPSHOT" else ""}"

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    maven("https://jitpack.io")
}

compose {
    kotlinCompilerPlugin.set("org.jetbrains.compose.compiler:compiler:1.4.7")
    kotlinCompilerPluginArgs.add("suppressKotlinVersionCompatibilityCheck=1.8.22")
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = JavaVersion.VERSION_17.toString()
            }
        }
        withJava()
    }
    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation("com.github.JellyBrick:alsong-kt:2.0.7")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.7.1")
                implementation("net.sf.javamusictag:jid3lib:0.5.4")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
                implementation("io.github.pdvrieze.xmlutil:serialization-jvm:0.86.0")
                implementation("ws.schild:jave-all-deps:3.3.1")
                implementation("commons-io:commons-io:2.13.0")
            }
        }
        val jvmTest by getting
    }
}

compose.desktop {
    application {
        mainClass = "be.zvz.alsonguploader.MainKt"
        nativeDistributions {
            modules("java.instrument", "java.scripting", "java.sql", "jdk.unsupported")
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "AlsongUploader"
            packageVersion = versionNumber
        }
    }
}