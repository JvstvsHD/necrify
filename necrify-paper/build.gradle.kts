import io.papermc.hangarpublishplugin.model.Platforms
import java.io.ByteArrayOutputStream

plugins {
    java
    `java-library`
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("net.minecrell.plugin-yml.paper") version "0.6.0"
    id("io.papermc.hangar-publish-plugin")
}

group = "de.jvstvshd.necrify"
version = rootProject.version

dependencies {
    compileOnly(libs.paper.api)
    api(projects.necrifyCommon)
}

tasks {
    shadowJar {
        archiveFileName.set("${rootProject.name}-Paper-${project.version}.jar")
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

//https://docs.papermc.io/misc/hangar-publishing
fun executeGitCommand(vararg command: String): String {
    val byteOut = ByteArrayOutputStream()
    exec {
        commandLine = listOf("git", *command)
        standardOutput = byteOut
    }
    return byteOut.toString(Charsets.UTF_8.name()).trim()
}

fun latestCommitMessage(): String {
    return executeGitCommand("log", "-1", "--pretty=%B")
}

val versionString: String = version as String
val isRelease: Boolean = !versionString.contains('-')

val suffixedVersion: String = "$versionString-Paper" + if (project.hasProperty("buildnumber")) {
    "-" + project.property("buildnumber") as String
} else {
    val githubRunNumber = System.getenv("GITHUB_RUN_NUMBER")
    versionString + if (githubRunNumber != null) "-$githubRunNumber" else ""
}

val changelogContent: String = latestCommitMessage()

hangarPublish {
    publications.register("necrify-paper") {
        version.set(suffixedVersion)
        channel.set(if (!isRelease) "Snapshot" else "Release")
        id.set("necrify")
        apiKey.set(System.getenv("HANGAR_API_TOKEN"))
        if (!isRelease) {
            changelog.set(changelogContent)
        }
        platforms {
            register(Platforms.PAPER) {
                jar.set(tasks.shadowJar.flatMap { it.archiveFile })
                val versions: List<String> = (property("paperVersion") as String)
                    .split(",")
                    .map { it.trim() }
                platformVersions.set(versions)
            }
        }
    }
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

paper {
    main = "de.jvstvshd.necrify.paper.NecrifyPaperPlugin"
    name = "necrify-paper"
    version = rootProject.version.toString()
    description = "A paper plugin complementing the Necrify plugin for velocity for imposing mutes."
    apiVersion = "1.20"
    bootstrapper = "de.jvstvshd.necrify.paper.NecrifyPaperPluginBootstrap"
    loader = "de.jvstvshd.necrify.paper.NecrifyPaperPluginLoader"
    generateLibrariesJson = true
}
