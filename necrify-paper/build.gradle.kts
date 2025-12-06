plugins {
    java
    `java-library`
    id("com.gradleup.shadow")
    id("de.eldoria.plugin-yml.paper") version "0.8.0"
    id("io.papermc.hangar-publish-plugin")
}

version = rootProject.version
description = "Paper plugin implementing the Necrify API used for help enforcing mutes for velocity-based applications"

dependencies {
    compileOnly(libs.paper.api)
    api(projects.necrifyCommon)
    api(libs.bundles.jackson)

    //workaround, so that only the real dependencies are downloaded by paper and not the common module, which already
    //is included in the JAR
    configurations {
        //API configuration is not resolvable, so the following code after this would fail
        val resolvedApi by creating {
            extendsFrom(configurations.api.get())
            isCanBeResolved = true
            isCanBeConsumed = true
        }

        resolvedApi.incoming.resolutionResult.allDependencies
            .filterIsInstance<ResolvedDependencyResult>()
            .flatMap { it.selected.dependencies }
            .filter { it.from.id is ProjectComponentIdentifier }
            .filter { it.requested is ModuleComponentSelector }
            .forEach {
                with(it.requested as ModuleComponentSelector) {
                    library("${group}:${module}:${version}")
                }
            }
    }
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
            exclude {
                it.moduleGroup != "com.fasterxml.jackson.core" &&
                        (it.moduleGroup == "de.jvstvshd.necrify.common" || it.moduleGroup == "de.jvstvshd.necrify.api")
            }

        }
        relocate("com.fasterxml.jackson", "de.jvstvshd.necrify.lib.jackson")
    }
    build {
        dependsOn(shadowJar)
    }
    generatePaperPluginDescription {
        useGoogleMavenCentralProxy()
    }
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
