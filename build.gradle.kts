import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("fabric-loom") version "1.16.2"
    id("org.jetbrains.kotlin.jvm") version "2.3.21"
    id("dev.kikugie.stonecutter")
}

val mcVersion = stonecutter.current.version
val modVersion: String by project
val modGroup: String by project
val modId: String by project
val javaRelease = (property("java_release") as String).toInt()

version = "$modVersion+mc$mcVersion"
group = modGroup
base.archivesName.set(modId)

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(javaRelease))
    }
    withSourcesJar()
}

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net/")
}

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    if ((property("minecraft_version") as String).startsWith("26.")) {
        // MC 26.x: Mojang stopped obfuscating client.jar; intermediary 0.0.0 is the
        // no-op stub from Fabric meta API (https://meta.fabricmc.net/v2/versions/intermediary/26.1.2).
        mappings("net.fabricmc:intermediary:0.0.0:v2")
    } else {
        mappings(loom.officialMojangMappings())
    }
    modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_version")}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${property("flk_version")}")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

loom {
    runs {
        register("gametest") {
            server()
            name = "Game Test Server"
            vmArg("-Dfabric-api.gametest")
            vmArg("-Dfabric-api.gametest.report-file=${layout.buildDirectory.get()}/gametest-report.xml")
            runDir = "build/gametest"
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(javaRelease)
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(javaRelease.toString()))
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}
