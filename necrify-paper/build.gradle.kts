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
    api(libs.bundles.jackson)
}

tasks {
    shadowJar {
        archiveFileName.set("Necrify-Paper-${project.buildVersion()}.jar")
        dependencies {
            include(project(":necrify-common"))
            include(project(":necrify-api"))
            //Since 1.21.3, jackson databind is also loaded through paper but with an older version. In order to mitigate
            //those issues, all jackson dependencies are relocated to a different package but are now included directly
            //in the shadow jar. This increases the size of the jar from ~185KB to ~2.7MB.
            //https://forums.papermc.io/threads/conflicting-jackson-databind-versions-starting-in-1-21-3.1537/#post-4360
            include { it.moduleGroup.startsWith("com.fasterxml.jackson") }
            exclude { it.moduleGroup != "com.fasterxml.jackson.core" &&
                    (it.moduleGroup == "de.jvstvshd.necrify.common" || it.moduleGroup == "de.jvstvshd.necrify.api") }

        }
        relocate("com.fasterxml.jackson", "de.jvstvshd.necrify.lib.jackson")
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
                .filter { it.requested is ModuleComponentSelector }
                //https://forums.papermc.io/threads/conflicting-jackson-databind-versions-starting-in-1-21-3.1537/#post-4360
                //jackson libraries need to be included directly
                .filterNot { it.toString().contains("com.fasterxml.jackson") },
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
    apiVersion = "1.21"
    bootstrapper = "de.jvstvshd.necrify.paper.NecrifyPaperPluginBootstrap"
    loader = "de.jvstvshd.necrify.paper.NecrifyPaperPluginLoader"
    generateLibrariesJson = true
}
