plugins {
    kotlin("jvm") version "2.0.20"
    `kotlin-dsl`
}

/*group = Version.PROJECT_GROUP
version = Version.PROJECT_VERSION*/

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.11.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}