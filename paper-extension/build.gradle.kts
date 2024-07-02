plugins {
    java
    `java-library`
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("net.minecrell.plugin-yml.paper") version "0.6.0"
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

paper {
    main = "de.jvstvshd.necrify.paper.NecrifyPaperPlugin"
    name = "necrify-paper"
    version = rootProject.version.toString()
    description = "A paper plugin complementing the Necrify plugin for velocity for imposing mutes."
    apiVersion = "1.20"
    bootstrapper = "de.jvstvshd.necrify.paper.NecrifyPaperPluginBootstrap"
    loader = "de.jvstvshd.necrify.paper.NecrifyPaperPluginLoader"
}
