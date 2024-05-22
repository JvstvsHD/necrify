plugins {
    java
}

version = rootProject.version
description = "An convenient API to handle various punishments on Velocity."

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(libs.velocity.api)
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