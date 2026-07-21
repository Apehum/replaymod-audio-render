pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        maven("https://maven.neoforged.net/releases") { name = "NeoForged" }
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("dev.kikugie.stonecutter") version "0.9"
}

stonecutter {
    centralScript = "build.gradle.kts"

    create(rootProject) {
        fun mc(
            mcVersion: String,
            vararg loaders: String,
        ) = loaders.forEach { version("$mcVersion-$it", mcVersion) }

        mc("1.14.4", "fabric")
        mc("1.18.2", "fabric")
        mc("1.19.3", "fabric")
        mc("26.1.1", "fabric")
        mc("26.2", "fabric")

        mc("1.21.1", "neoforge")

        vcsVersion = "1.19.3-fabric"
    }
}

rootProject.name = "replaymodaudiorender"
