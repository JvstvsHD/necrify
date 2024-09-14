plugins {
    kotlin("jvm") version "2.0.20"
    `kotlin-dsl`
}

group = "de.jvstvshd.necrify"
version = "1.2.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}