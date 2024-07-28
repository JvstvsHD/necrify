plugins {
    java
    `java-library`
}

group = "de.jvstvshd.necrify"
version = rootProject.version

dependencies {
    api(projects.necrifyApi)
    api(libs.bundles.jackson)
    api(libs.bundles.database) {
        exclude(group = "org.slf4j", module = "slf4j-api")
    }
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

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}