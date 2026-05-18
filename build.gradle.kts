import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File

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

val isMojangNamed = (property("minecraft_version") as String).startsWith("26.")

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    if (isMojangNamed) {
        // MC 26.x: client.jar ships in source-level (Mojang) names already.
        // Identity tiny v2 stub (official == named); noIntermediateMappings()
        // below tells Loom to skip the intermediary remapping step entirely.
        mappings(files("${rootProject.projectDir}/libs/identity-mappings-26.1.2.jar"))
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
    accessWidenerPath = rootProject.file("src/main/resources/dndweapons.accesswidener")
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

if (isMojangNamed) {
    // MC 26.x: source names are already human-readable; remapping sources JAR
    // is a no-op and fails because the identity stub has no class entries.
    tasks.named("remapSourcesJar") {
        enabled = false
    }

    // Loom writes fabric.defaultModDistributionNamespace=official in launch.cfg because
    // our identity mappings stub has 'official' as the source namespace. But the remapped
    // dependency JARs are in 'named' namespace. Patch launch.cfg after configureLaunch so
    // Fabric Loader accepts ClassTweakers that declare 'named'.
    fun patchLaunchCfg() {
        val cfg = file("${projectDir}/.gradle/loom-cache/launch.cfg")
        if (cfg.exists()) {
            var text = cfg.readText()
            // Remap deps are in 'named' namespace (even with identity stub).
            // fabric.runtimeMappingNamespace controls the ClassTweaker namespace check.
            // fabric.defaultModDistributionNamespace controls how mods are distributed.
            var changed = false
            if ("fabric.defaultModDistributionNamespace=official" in text) {
                text = text.replace(
                    "fabric.defaultModDistributionNamespace=official",
                    "fabric.defaultModDistributionNamespace=named"
                )
                changed = true
            }
            if ("fabric.runtimeMappingNamespace" !in text) {
                // Insert after fabric.defaultModDistributionNamespace line
                text = text.replace(
                    "fabric.defaultModDistributionNamespace=named",
                    "fabric.defaultModDistributionNamespace=named\n\tfabric.runtimeMappingNamespace=named"
                )
                changed = true
            }
            if (changed) {
                cfg.writeText(text)
                logger.lifecycle("[dnd-weapons] Patched launch.cfg for MC 26.x: runtimeMappingNamespace=named, defaultModDistributionNamespace=named")
            }
        }
    }

    tasks.named("configureLaunch") {
        doLast { patchLaunchCfg() }
    }

    // Patch before any run task in case configureLaunch regenerates the file
    afterEvaluate {
        tasks.matching { it.name.startsWith("run") }.configureEach {
            doFirst { patchLaunchCfg() }
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

// Ensure the `test` source set is materialized in every subproject so that
// Stonecutter's configureSources() iterates it alongside `main` and stamps
// per-version chiseledSrc/test directories. The kotlin("jvm") plugin creates
// `test` lazily; touching it here forces creation before Stonecutter walks
// the SourceSetContainer. Without this, Stonecutter's SourceSetContainer
// iteration may run before the test source set is realized, and version
// gates in src/test/kotlin/** would not be processed per version.
sourceSets {
    named("test") {
        // Force realization. No additional config needed — kotlin("jvm")
        // already wires src/test/kotlin and src/test/java.
    }
}

// Per-version overlay resources under versions/<mc>/src/main/resources/ may
// share filenames with the shared src/main/resources/ tree (e.g. recipe JSONs
// that need a per-version ingredient format because vanilla MC's recipe codec
// changed across versions). The overlay MUST win over the shared file.
//
// Stonecutter wires the per-version `src/main/resources` directory into the
// `main` source set FIRST (it is the project's own srcDir for `:<mc>`), then
// the shared `<root>/src/main/resources` is appended later by the central
// build script. With Gradle's default Copy iteration that means the shared
// file is the LAST one written for any conflicting path. We need the OVERLAY
// to win, so we use DuplicatesStrategy.EXCLUDE which keeps the FIRST entry
// (the per-version overlay) and ignores subsequent duplicates.
tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// sourcesJar packages all main resources; it hits the same per-version vs shared
// collision and needs the same first-wins strategy so the overlay's per-version
// recipe JSON is shipped in the sources artifact, not the shared one.
tasks.withType<org.gradle.api.tasks.bundling.Jar>().configureEach {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// ===== Phase 6: Wiki generator =====

tasks.register<JavaExec>("generateWiki") {
    group = "wiki"
    description = "Generate the player-facing Markdown wiki under build/wiki/."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.dndweapons.codegen.WikiGen")
    val out = layout.buildDirectory.dir("wiki").get().asFile
    val hand = rootProject.layout.projectDirectory.dir("wiki/handwritten").asFile
    val sha = try {
        providers.exec {
            commandLine("git", "rev-parse", "--short", "HEAD")
        }.standardOutput.asText.get().trim()
    } catch (_: Exception) {
        "local"
    }
    args(out.absolutePath, modVersion, sha, hand.absolutePath)
    dependsOn("classes")
    doFirst {
        out.mkdirs()
        if (!hand.exists()) {
            throw GradleException("Missing handwritten wiki dir: $hand")
        }
    }
}

tasks.register("publishWiki") {
    group = "wiki"
    description = "Sync build/wiki/ to <repo>.wiki.git (push). Auth via env WIKI_PUBLISH_TOKEN."
    dependsOn("generateWiki")
    doLast {
        val wikiDir = layout.buildDirectory.dir("wiki").get().asFile
        val cloneDir = layout.buildDirectory.dir("wiki-repo").get().asFile
        val token: String? = System.getenv("WIKI_PUBLISH_TOKEN")
        val url = if (!token.isNullOrBlank()) {
            "https://x-access-token:$token@github.com/MisterVitoPro/dndcraft-weapons.wiki.git"
        } else {
            "git@github.com:MisterVitoPro/dndcraft-weapons.wiki.git"
        }
        val dryRun = project.findProperty("dryRun")?.toString().toBoolean()

        logger.lifecycle("publishWiki: source = $wikiDir")
        logger.lifecycle("publishWiki: target = $url")
        logger.lifecycle("publishWiki: dryRun = $dryRun")

        if (cloneDir.exists()) cloneDir.deleteRecursively()
        cloneDir.parentFile.mkdirs()

        providers.exec {
            commandLine("git", "clone", "--depth", "1", url, cloneDir.absolutePath)
        }.result.get()

        // Wipe everything except .git
        cloneDir.listFiles()?.forEach { f ->
            if (f.name != ".git") {
                if (f.isDirectory) f.deleteRecursively() else f.delete()
            }
        }

        // Copy generated + handwritten wiki output into the clone
        wikiDir.listFiles()?.forEach { f ->
            val dest = File(cloneDir, f.name)
            if (f.isDirectory) f.copyRecursively(dest, overwrite = true)
            else f.copyTo(dest, overwrite = true)
        }

        providers.exec {
            workingDir = cloneDir
            commandLine("git", "add", ".")
        }.result.get()

        // Check whether there is anything to commit
        val status = providers.exec {
            workingDir = cloneDir
            commandLine("git", "status", "--porcelain")
        }.standardOutput.asText.get().trim()
        if (status.isEmpty()) {
            logger.lifecycle("publishWiki: no changes to publish, exiting clean.")
            return@doLast
        }

        val msg = "Wiki: sync from ${project.version}"
        providers.exec {
            workingDir = cloneDir
            commandLine("git", "-c", "user.email=wiki-bot@dndcraft-weapons", "-c", "user.name=Wiki Bot", "commit", "-m", msg)
        }.result.get()

        if (dryRun) {
            logger.lifecycle("publishWiki: dry-run mode, NOT pushing.")
            return@doLast
        }
        providers.exec {
            workingDir = cloneDir
            commandLine("git", "push", "origin", "HEAD")
        }.result.get()
        logger.lifecycle("publishWiki: pushed to $url")
    }
}
