import com.apehum.replayaudio.VersionResolver
import me.modmuss50.mpp.ReleaseType

plugins {
    id("dev.isxander.modstitch.base") version "0.8.4"
    id("me.modmuss50.mod-publish-plugin") version "1.1.0"
}

val platform = stonecutter.current.project.substringAfter('-')
val mcVersion = stonecutter.current.version
val isFabric = platform == "fabric"
val isNeoforge = platform == "neoforge"

version = "${property("mod_version")}+$mcVersion"
group = property("maven_group") as String

base {
    archivesName = "${property("archives_base_name")}-$platform"
}

modstitch {
    minecraftVersion = property("minecraft_version") as String

    loom {
        fabricLoaderVersion = property("deps.fabric_loader_version") as String

        configureLoom {
            runs.all { runDir = "../../run" }
        }
    }

    moddevgradle {
        neoForgeVersion = property("deps.neoforge") as? String

        defaultRuns(true, false)

        configureNeoForge {
            runs.all { gameDirectory = file("../../run") }
        }
    }

    metadata {
        modId = "replaymodaudiorender"
        modVersion = project.version.toString()
        modName = "ReplayModAudioRender"
        modDescription = "ReplayMod addon for rendering audio using loopback device."

        replacementProperties.apply {
            put("fabric_loader_version", property("deps.fabric_loader_version") as String)
            put("minecraft_version_dependency", property("minecraft_version_dependency") as String)
            (findProperty("minecraft_version_range") as? String)?.let { put("minecraft_version_range", it) }
            put("fabric_api_dependency_name", if (stonecutter.eval(mcVersion, ">=26.1")) "fabric-api" else "fabric")
        }
    }

    mixin {
        addMixinsToModManifest = true

        configs.register("replaymodaudiorender")
    }
}

dependencies {
    modstitch.loom {
        modstitchModImplementation("net.fabricmc.fabric-api:fabric-api:${property("deps.fabric_api")}")
        modstitchModImplementation("maven.modrinth:replaymod:${property("deps.replaymod")}")
    }

    modstitch.moddevgradle {
        modstitchModImplementation("curse.maven:reforgedplay-mod-1018692:${property("deps.reforgedplay_file")}")
        // these libraries are jij in reforgedplay, so we need to manually specify them
        compileOnly("org.apache.commons:commons-exec:1.3")
        compileOnly("com.github.ReplayMod:lwjgl-utils:27dcd66")
    }
}

stonecutter {
    // reforgedplay keeps these libraries unshaded
    replacements.string(isNeoforge) {
        replace("com.replaymod.lib.de.johni0702.minecraft", "de.johni0702.minecraft")
    }
    replacements.string(isNeoforge) {
        replace("com.replaymod.lib.org.apache.commons.exec", "org.apache.commons.exec")
    }

    replacements.string(current.parsed >= "1.18.2") {
        replace("tryOpenDevice", "openDeviceOrFallback")
    }

    replacements.string(current.parsed >= "1.18.2") {
        replace("device", "currentDevice")
    }

    replacements.string(current.parsed >= "1.18.2") {
        replace("getDevice", "getCurrentDevice")
    }
}

val outputJarTask = modstitch.finalJarTask

tasks {
    jar {
        from(rootProject.file("LICENSE")) {
            rename { "${it}_${project.property("archives_base_name")}" }
        }
    }

    val copyToRoot =
        register<Copy>("copyToRoot") {
            dependsOn(outputJarTask)
            from(outputJarTask.map { it.archiveFile.get() })
            into(rootProject.layout.buildDirectory.dir("libs"))
        }

    build {
        dependsOn(copyToRoot)
    }
}

publishMods {
    changelog =
        rootProject.layout.projectDirectory
            .file("changelog.md")
            .asFile
            .readText()
    type = ReleaseType.BETA
    modLoaders.add(platform)

    val loaderDisplayName =
        when (platform) {
            "fabric" -> "Fabric"
            "neoforge" -> "NeoForge"
            else -> throw IllegalStateException("Unsupported platform $platform")
        }

    displayName = "[$loaderDisplayName $mcVersion] ReplayModAudioRender ${property("mod_version")}"
    file = outputJarTask.flatMap { it.archiveFile }

    val modrinthToken =
        providers
            .gradleProperty("modrinth_token")
            .orElse(providers.environmentVariable("MODRINTH_TOKEN").orElse(""))
            .orNull
            ?.takeIf { it.isNotBlank() }

    val curseforgeToken =
        providers
            .gradleProperty("curseforge_token")
            .orElse(providers.environmentVariable("CURSEFORGE_TOKEN").orElse(""))
            .orNull
            ?.takeIf { it.isNotBlank() }

    val minecraftVersions =
        VersionResolver
            .getMinecraftVersionsInRange("release", property("minecraft_version_dependency") as String)
            .get()
            .map { it.id }

    if (isFabric) {
        modrinth {
            projectId = "JNgb4oIM"
            accessToken = modrinthToken
            requires("replaymod")
            this.minecraftVersions.addAll(minecraftVersions)
        }
    }

    if (isNeoforge) {
        curseforge {
            projectId = "1597682"
            accessToken = curseforgeToken
            requires("reforgedplay-mod")
            this.minecraftVersions.addAll(minecraftVersions)
        }
    }

    val dryRunProperty =
        providers
            .gradleProperty("dry_run")
            .getOrElse("false")
            .toBoolean()

    dryRun = modrinthToken == null || curseforgeToken == null || dryRunProperty
}
