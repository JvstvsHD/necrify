plugins {
    java
    `java-library`
    id("com.gradleup.shadow")
    id("net.minecrell.plugin-yml.paper") version "0.6.0"
    id("io.papermc.hangar-publish-plugin")
}

version = rootProject.version
description = "Paper plugin implementing the Necrify API used for help enforcing mutes for velocity-based applications"

dependencies {
    compileOnly(libs.paper.api)
    api(projects.necrifyCommon)
}

tasks {
    jar {
        archiveFileName.set("Necrify-Paper-${project.buildVersion()}.jar")
    }
    shadowJar {
        archiveFileName.set("Necrify-Paper-${project.buildVersion()}.jar")
        dependencies {
            include(project(":necrify-common"))
            include(project(":necrify-api"))
        }
    }
    build {
        dependsOn(shadowJar)
    }
    /*
     This is a hacky workaround to exclude all those dependencies from being loaded by paper that are in this project
     (plugin-common, api). Maybe I should be taken to the International Criminal Court for this
     */
    generatePaperPluginDescription {
        val field = librariesRootComponent.get()::class.java.getDeclaredField("dependencies")
        field.isAccessible = true
        val set = field.get(librariesRootComponent.get()) as LinkedHashSet<DependencyResult>
        val configuration = project(":necrify-common").configurations.getByName("runtimeClasspath")
        val resolutionResult = configuration.incoming.resolutionResult
        set.clear()
        set.addAll(
            resolutionResult.allDependencies
                .filter { it.from.id is ProjectComponentIdentifier }
                .filter { it.requested is ModuleComponentSelector },
        )
    }
}

java {
    withSourcesJar()
    withJavadocJar()
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

paper {
    main = "de.jvstvshd.necrify.paper.NecrifyPaperJavaPlugin"
    name = "necrify-paper"
    version = rootProject.version.toString()
    description = "A paper plugin complementing the Necrify plugin for velocity for imposing mutes."
    apiVersion = "1.20"
    bootstrapper = "de.jvstvshd.necrify.paper.NecrifyPaperPluginBootstrap"
    loader = "de.jvstvshd.necrify.paper.NecrifyPaperPluginLoader"
    generateLibrariesJson = true
}
