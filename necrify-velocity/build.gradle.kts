plugins {
    java
    `java-library`
    id("io.github.goooler.shadow")
    id("xyz.jpenilla.run-velocity") version "2.3.0"
    id("io.papermc.hangar-publish-plugin")
    id("dev.vankka.dependencydownload.plugin") version "1.3.1"
}

version = rootProject.version
description = "A plugin handling all your needs for punishments on Velocity, based on the necrify-api."

repositories {
    mavenCentral()
}

dependencies {
    api(projects.necrifyApi)
    compileOnly(libs.bundles.database)
    api(projects.necrifyCommon)
    api(libs.cloud.velocity)
    api(libs.minecraftdependencydownload.velocity)
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
    javadoc {
        dependsOn(generateRuntimeDownloadResourceForRuntimeDownload)
    }
    compileJava {
        options.encoding = "UTF-8"
    }

    runVelocity {
        // Configure the Velocity version for our task.
        // This is the only required configuration besides applying the plugin.
        // Your plugin's jar (or shadowJar if present) will be used automatically.
        velocityVersion("3.3.0-SNAPSHOT")
    }

    jar {
        dependsOn(generateRuntimeDownloadResourceForRuntimeDownload)
        finalizedBy(shadowJar)
    }

    shadowJar {
        archiveFileName.set("Necrify-Velocity-${project.buildVersion()}.jar")
        dependencies {
            val prefix: (String) -> String = { "de.jvstvshd.necrify.lib.$it" }
            relocate("com.fasterxml.jackson", prefix("jackson"))
            relocate("com.google.errorprone", prefix("google.errorprone"))
            relocate("com.google.protobuf", prefix("google.protobuf"))
            relocate("com.mysql", prefix("mysql"))
            relocate("com.sun.jna", "sun.jna")
            relocate("com.zaxxer.hikari", prefix("hikari"))
            relocate("de.chojo.sadu", prefix("sadu"))
            relocate("dev.vankka", prefix("vankka"))
            relocate("google", prefix("google"))
            relocate("io.leangen.geantyref", prefix("geantyref"))
            relocate("me.lucko.jarrelocator", prefix("lucko.jarrelocator"))
            relocate("org.objectweb.asm", prefix("objectweb.asm"))
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

java {
    withSourcesJar()
    withJavadocJar()
}
