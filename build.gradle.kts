import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("fabric-loom") version "1.11-SNAPSHOT"
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
    sourceCompatibility = JavaVersion.toVersion(javaRelease)
    targetCompatibility = JavaVersion.toVersion(javaRelease)
    withSourcesJar()
}

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net/")
}

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    //? if MC >= 26.1 {
    /*mappings(loom.officialMojangMappings())*/
    //?} else {
    mappings("net.fabricmc:yarn:${property("yarn_mappings")}:v2")
    //?}
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
