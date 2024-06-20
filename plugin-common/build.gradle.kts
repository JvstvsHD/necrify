plugins {
    java
    `java-library`
}

group = "de.jvstvshd.necrify"
version = rootProject.version

repositories {
    mavenCentral()
}

dependencies {
    api(projects.api)
    api(libs.bundles.jackson)
    api(libs.bundles.database)
    api(libs.bundles.cloud)
    annotationProcessor(libs.cloud.annotations)
    implementation(libs.bundles.adventure)
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}