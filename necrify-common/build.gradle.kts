plugins {
    java
    `java-library`
    id("dev.vankka.dependencydownload.plugin") version "1.3.1"
    id("net.kyori.blossom") version "2.1.0"
}

version = rootProject.version
description = "Common project for all plugin implementations of the necrify-api"

sourceSets {
    main {
        blossom {
            javaSources {
                property("version", project.version.toString())
                property("gitCommit", git.latestCommitHashShort())
                property("buildNumber", project.buildNumber() ?: "-1")
            }
        }
    }
}

dependencies {
    api(projects.necrifyApi)
    api(libs.caffeine)
    api(libs.bundles.jackson)
    runtimeDownload(libs.bundles.database.drivers) {
        exclude(group = "org.slf4j")
    }
    //compileOnly(libs.bundles.database.drivers)
    api(libs.bundles.database.helper) {
        exclude(group = "org.slf4j")
    }
    api(libs.bundles.cloud) {
        exclude(group = "net.kyori")
    }
    compileOnly(libs.brigadier)
    annotationProcessor(libs.cloud.annotations)
    compileOnly(libs.slf4j.api)
    compileOnly("com.google.code.gson:gson:2.12.1")
    compileOnly(libs.bundles.adventure)
    api(libs.adventure.text.feature.pagination) {
        exclude(group = "net.kyori")
    }
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

tasks {
    build {
        dependsOn(getByName("generateJavaTemplates"))
    }
    javadoc {
        dependsOn(generateRuntimeDownloadResourceForRuntimeDownload)
    }
    jar {
        dependsOn(generateRuntimeDownloadResourceForRuntimeDownload)
    }

    generateRuntimeDownloadResourceForRuntimeDownload {
        val prefix: (String) -> String = { "de.jvstvshd.necrify.lib.$it" }
        relocate("com.mysql", prefix("mysql"))
        relocate("org.mariadb", prefix("mariadb"))
        relocate("org.postgresql", prefix("postgresql"))
    }
}

java {
    withSourcesJar()
    withJavadocJar()
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}