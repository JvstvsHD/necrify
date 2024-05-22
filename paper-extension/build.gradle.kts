plugins {
    java
    `java-library`
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("net.minecrell.plugin-yml.bukkit") version "0.5.3"
}

group = "de.jvstvshd.necrify"
version = rootProject.version

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.karuslabs.com/repository/chimera-releases/")
    maven("https://libraries.minecraft.net/")
}

dependencies {
    compileOnly(libs.paper.api)
    api(libs.brigadier)
    api(projects.pluginCommon)
}

tasks {
    shadowJar {
        archiveFileName.set("${rootProject.name}-Paper-${project.version}.jar")
    }
    build {
        dependsOn(shadowJar)
    }
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

bukkit {
    main = "de.jvstvshd.necrify.paper.NecrifyPaperPlugin"
    name = "Necrify paper-extension"
    version = rootProject.version.toString()
    description = "A paper plugin complementing the Necrify plugin for velocity for imposing mutes."
    apiVersion = "1.19"
}