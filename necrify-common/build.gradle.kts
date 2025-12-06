plugins {
    java
    `java-library`
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
    api(libs.bundles.database.drivers) {
        exclude(group = "org.slf4j")
    }
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
    api(libs.adventure.text.feature.pagination)
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

tasks {
    build {
        dependsOn(getByName("generateJavaTemplates"))
    }

    test {
        useJUnitPlatform()
    }
}