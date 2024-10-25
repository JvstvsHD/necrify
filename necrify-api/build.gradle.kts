plugins {
    java
    `java-library`
}

version = rootProject.version
description = "An convenient API to handle various punishments on Velocity."

repositories {
    mavenCentral()
}

dependencies {
    api(libs.jetbrains.annotations)
    api(libs.eventbus)
    compileOnly(libs.slf4j.api)
    compileOnly(libs.bundles.adventure)
    testImplementation(libs.slf4j.api)
    testImplementation("ch.qos.logback:logback-classic:1.5.12")
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

java {
    withJavadocJar()
    withSourcesJar()
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}