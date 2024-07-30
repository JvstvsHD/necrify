import io.papermc.hangarpublishplugin.model.Platforms

plugins {
    java
    `java-library`
    id("io.github.goooler.shadow") version "8.1.8"
    id("xyz.jpenilla.run-velocity") version "2.3.0"
    id("io.papermc.hangar-publish-plugin")
}

version = rootProject.version
description = "A plugin handling all your needs for punishments on Velocity, based on the necrify-api."

repositories {
    mavenCentral()
}

dependencies {
    api(projects.necrifyCommon)
    api(libs.cloud.velocity)
    annotationProcessor(libs.velocity.api)
    compileOnly(libs.velocity.api)
    compileOnly(libs.luckperms.api)
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
    }

    runVelocity {
        // Configure the Velocity version for our task.
        // This is the only required configuration besides applying the plugin.
        // Your plugin's jar (or shadowJar if present) will be used automatically.
        velocityVersion("3.3.0-SNAPSHOT")
    }
    shadowJar {
        archiveFileName.set("${rootProject.name}-Velocity-${project.version}.jar")
        archiveBaseName.set("necrify")
        dependencies {
            //Do not relocate sqlite since it loads some native libraries
            val prefix: (String) -> String = { "de.jvstvshd.necrify.lib.$it" }
            relocate("com.fasterxml.jackson", prefix("jackson"))
            relocate("com.github.benmanes.caffeine", prefix("caffeine"))
            relocate("com.google.errorprone", prefix("google.errorprone"))
            relocate("com.google.protobuf", prefix("google.protobuf"))
            relocate("com.mysql", prefix("mysql"))
            relocate("com.sun.jna", "sun.jna")
            relocate("com.zaxxer.hikari", prefix("hikari"))
            relocate("de.chojo.sadu", prefix("sadu"))
            relocate("google", prefix("google"))
            relocate("io.leangen.geantyref", prefix("geantyref"))
            relocate("org.apache.commons", prefix("commons"))
            relocate("org.checkerframework", prefix("checkerframework"))
            relocate("org.incendo.cloud", prefix("cloud"))
            relocate("org.intellij.lang.annotations", prefix("intellij.lang.annotations"))
            relocate("org.jetbrains.annotations", prefix("jetbrains.annotations"))
            relocate("org.mariadb", prefix("mariadb"))
            relocate("org.postgresql", prefix("postgresql"))
            relocate("org.yaml.snakeyaml", prefix("snakeyaml"))
            relocate("sun.jna", prefix("sun.jna"))
            relocate("waffle", prefix("waffle"))
        }
    }
    build {
        dependsOn(shadowJar)
    }
}

hangarPublish {
    publications.register("necrify-paper") {
        val pluginVersion = project.version as String
        version.set(pluginVersion)
        channel.set(if (pluginVersion.contains("-")) "Snapshot" else "Release")
        id.set("necrify")
        apiKey.set(System.getenv("HANGAR_API_TOKEN"))
        platforms {
            register(Platforms.PAPER) {
                jar.set(tasks.jar.flatMap { it.archiveFile })
                val versions: List<String> = (property("paperVersion") as String)
                    .split(",")
                    .map { it.trim() }
                platformVersions.set(versions)
            }
        }
    }
}

java {
    withSourcesJar()
    withJavadocJar()
}
