import com.modrinth.minotaur.Minotaur
import com.modrinth.minotaur.ModrinthExtension
import io.papermc.hangarpublishplugin.model.Platforms
import net.kyori.indra.licenser.spotless.IndraSpotlessLicenserPlugin
import java.util.*
import kotlin.io.path.createFile
import kotlin.io.path.writeText

plugins {
    /*`maven-publish`
    signing*/
    id("io.papermc.hangar-publish-plugin") version "0.1.4"
    id("com.gradleup.shadow") version "8.3.6" apply false
    id("net.kyori.indra.licenser.spotless") version "3.1.3"
    id("com.modrinth.minotaur") version "2.+" apply false
    id("com.vanniktech.maven.publish") version "0.35.0"
    java
}

group = Version.PROJECT_GROUP
version = Version.PROJECT_VERSION

subprojects {
    apply {
        plugin("java")
        plugin<IndraSpotlessLicenserPlugin>()
        plugin<com.vanniktech.maven.publish.MavenPublishPlugin>()
    }
    indraSpotlessLicenser {
        newLine(true)
    }
    java {
        toolchain.languageVersion = JavaLanguageVersion.of(21)
    }
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://jitpack.io")
        maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
        maven("https://central.sonatype.com/repository/maven-snapshots/")
    }

    tasks {
        mavenPublishing {
            publishToMavenCentral(automaticRelease = true)
            signAllPublications()

            coordinates(
                rootProject.group.toString().lowercase(Locale.getDefault()),
                project.name,
                project.publishingVersion()
            )
            pom {
                name.set(project.name)
                description.set(project.description)
                url.set("https://github.com/JvstvsHD/necrify")

                developers {
                    developer {
                        name.set("JvstvsHD")
                    }
                }

                licenses {
                    license {
                        name.set("GNU General Public License v3.0")
                        url.set("https://www.gnu.org/licenses/gpl-3.0.en.html")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/JvstvsHD/necrify.git")
                    url.set("https://github.com/JvstvsHD/necrify/tree/main")
                }
            }
        }

        gradle.projectsEvaluated {
            javadoc {
                (options as StandardJavadocDocletOptions).tags(
                    "apiNote:a:API Note",
                    "implSpec:a:Implementation Requirements",
                    "implNote:a:Implementation Note"
                )
            }
        }
    }
    if (project.name == "necrify-paper" || project.name == "necrify-velocity") {
        apply {
            plugin<Minotaur>()
        }
        afterEvaluate {
            configure<ModrinthExtension> {
                syncBodyFrom = rootProject.file("README.md").readText()
                token.set(System.getenv("MODRINTH_TOKEN"))
                projectId.set("necrify")
                versionNumber.set(buildVersion())
                versionType.set(if (!rootProject.isRelease) "beta" else "release")
                uploadFile.set(tasks.findByName("shadowJar"))
                gameVersions.addAll(property("gameVersions").toString().split(",").map { it.trim() })
                loaders.add(property("loader").toString())
                if (!rootProject.isRelease) {
                    changelog.set(changelogMessage())
                } else {
                    changelog.set("Changes will be provided shortly.\nComplete changelog can be found on GitHub: https://www.github.com/JvstvsHD/necrify/releases/tag/v${rootProject.version}")
                }
            }
        }
    }
}

hangarPublish {
    publications.register("necrify") {
        version.set(buildVersion())
        channel.set(if (!rootProject.isRelease) "Snapshot" else "Release")
        id.set("necrify")
        apiKey.set(System.getenv("HANGAR_API_TOKEN") ?: "")
        if (!rootProject.isRelease) {
            changelog.set(changelogMessage())
        } else {
            changelog.set("Changes will be provided shortly.\nComplete changelog can be found on GitHub: https://www.github.com/JvstvsHD/necrify/releases/tag/v${rootProject.version}")
        }
        platforms {
            register(Platforms.PAPER) {
                jar.set((project(":necrify-paper").tasks.getByName("shadowJar") as Jar).archiveFile)
                val versions: List<String> = (property("paperVersion") as String)
                    .split(",")
                    .map { it.trim() }
                platformVersions.set(versions)
            }

            register(Platforms.VELOCITY) {
                jar.set((project(":necrify-velocity").tasks.getByName("shadowJar") as Jar).archiveFile)
                val versions: List<String> = (property("velocityVersion") as String)
                    .split(",")
                    .map { it.trim() }
                platformVersions.set(versions)
            }
        }
    }
}

tasks {
    register<Javadoc>("alljavadoc") {
        title = "Necrify " + buildVersion()
        val sjd = options as StandardJavadocDocletOptions
        sjd.tags(
            "apiNote:a:API Note",
            "implSpec:a:Implementation Requirements",
            "implNote:a:Implementation Note"
        )
        sjd.bottom("© 2025 JvstvsHD | <a href=\"https://docs.jvstvshd.de/necrify/\">Tutorials & further documenation</a> | <a href=\"https://github.com/JvstvsHD/necrify\">Github</a> | <a href=\"https://jd.jvstvshd.de/necrify/\">Return to version list</a>")
        val projects = rootProject.allprojects
        setSource(projects.map { project -> project.sourceSets.main.get().allJava })
        classpath = files(projects.map { project -> project.sourceSets.main.get().compileClasspath })
        val destinationDirectory =
            file("${rootProject.layout.buildDirectory.get()}/docs/javadoc/${rootProject.version}")
        setDestinationDir(destinationDirectory)
        doLast {
            copy {
                from(destinationDirectory)
                into(file("${rootProject.layout.buildDirectory.get()}/docs/javadoc/latest"))
            }
            //version file is used by Jenkins
            val versionFile =
                kotlin.io.path.Path("${rootProject.layout.buildDirectory.get()}/docs/javadoc/version.txt").createFile()
            versionFile.writeText(rootProject.version.toString())
        }
    }

    register<Task>("printSnapshotVersions") {
        doLast {
            subprojects.flatMap { subproject -> subproject.configurations.matching { it.isCanBeResolved } }
                .flatMap { it.resolvedConfiguration.resolvedArtifacts }
                .toSet()
                .forEach { artifact ->
                    val id = artifact.moduleVersion.id
                    if (id.version.endsWith("-SNAPSHOT") || id.version.contains("SNAPSHOT")) {
                        println("${id.group}:${id.name}:${id.version}")
                    }
                }
        }
    }
}