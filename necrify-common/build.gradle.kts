plugins {
    java
    `java-library`
    id("dev.vankka.dependencydownload.plugin") version "1.3.1"
}

group = "de.jvstvshd.necrify"
version = rootProject.version

dependencies {
    api(projects.necrifyApi)
    api(libs.bundles.jackson)
    runtimeDownload(libs.bundles.database) {
        exclude(group = "org.slf4j", module = "slf4j-api")
    }
    api(libs.sadu)
    api(libs.sadu.queries)
    api(libs.bundles.cloud)
    compileOnly(libs.cloud.brigadier)
    compileOnly(libs.brigadier)
    annotationProcessor(libs.cloud.annotations)
    compileOnly(libs.slf4j.api)
    compileOnly("com.google.code.gson:gson:2.10.1")
    compileOnly(libs.bundles.adventure)
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

tasks {
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

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}