plugins {
    application
    kotlin("jvm") version "2.3.0"
    id("com.gradleup.shadow") version "9.3.1"
}

group = "kr.chabun"
version = "1.0-SNAPSHOT"

kotlin {
    jvmToolchain(25)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Logging
    implementation("io.klogging:klogging:0.11.7")
    implementation("io.klogging:slf4j-klogging:0.11.7")

    // Minestom
    implementation("net.minestom:minestom:2026.01.08-1.21.11")

    // Ktor
    implementation("io.ktor:ktor-server-core:3.3.3")
    implementation("io.ktor:ktor-server-cio:3.3.3")

    testImplementation(kotlin("test"))
}

tasks {
    jar {
        manifest {
            attributes["Main-Class"] = "MainKt"
        }
    }

    build {
        dependsOn(shadowJar)
    }

    named<JavaExec>("run") {
        workingDir = layout.projectDirectory.dir("run").asFile

        mainClass = "MainKt"

        doFirst {
            workingDir.mkdirs()
        }
    }

    shadowJar {
        mergeServiceFiles()
        archiveClassifier.set("") // Prevent the -all suffix on the shadowjar file.
        dependsOn("distTar", "distZip")
    }

    test {
        useJUnitPlatform()
    }
}