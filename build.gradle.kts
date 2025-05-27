import com.modrinth.minotaur.Minotaur
import com.modrinth.minotaur.ModrinthExtension
import io.papermc.hangarpublishplugin.model.Platforms
import net.kyori.indra.licenser.spotless.IndraSpotlessLicenserPlugin
import java.util.*

plugins {
    `maven-publish`
    signing
    id("io.papermc.hangar-publish-plugin") version "0.1.2"
    id("com.gradleup.shadow") version "8.3.6" apply false
    id("net.kyori.indra.licenser.spotless") version "3.1.3"
    id("com.modrinth.minotaur") version "2.+" apply false
    java
}

group = Version.PROJECT_GROUP
version = Version.PROJECT_VERSION

subprojects {
    apply {
        plugin<MavenPublishPlugin>()
        plugin<SigningPlugin>()
        plugin("java")
        plugin<IndraSpotlessLicenserPlugin>()
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
    }
    tasks {
        gradle.projectsEvaluated {
            javadoc {
                (options as StandardJavadocDocletOptions).tags(
                    "apiNote:a:API Note",
                    "implSpec:a:Implementation Requirements",
                    "implNote:a:Implementation Note"
                )
            }
            signing {
                val signingKey = findProperty("signingKey")?.toString() ?: System.getenv("SIGNING_KEY")
                val signingPassword = findProperty("signingPassword")?.toString() ?: System.getenv("SIGNING_PASSWORD")
                if (signingKey != null && signingPassword != null) {
                    useInMemoryPgpKeys(signingKey, signingPassword)
                }
                sign(publishing.publications)
            }

            publishing {
                repositories {
                    maven(
                        if (project.publishingVersion().endsWith("-SNAPSHOT"))
                            "https://s01.oss.sonatype.org/content/repositories/snapshots/" else "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
                    ) {
                        name = "ossrh"
                        credentials {
                            username =
                                project.findProperty("sonatypeUsername") as String?
                                    ?: System.getenv("SONATYPE_USERNAME")
                            password =
                                project.findProperty("sonatypePassword") as String?
                                    ?: System.getenv("SONATYPE_PASSWORD")
                        }
                    }
                }
                publications {
                    create<MavenPublication>(rootProject.name) {
                        from(this@subprojects.components["java"])
                        groupId = rootProject.group.toString().lowercase(Locale.getDefault())
                        artifactId = project.name
                        version = project.publishingVersion()

                        pom {
                            name.set(project.name)
                            description.set(project.description)
                            url.set("https://github.com/JvstvsHD/necrify")
                            packaging = "jar"

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
                }
            }
        }
    }
    if (project.name == "necrify-paper" || project.name == "necrify-velocity") {
        apply {
            plugin<Minotaur>()
        }
        afterEvaluate {
            configure<ModrinthExtension> {
                syncBodyFrom = rootProject.file("README.md").path
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
        //"""e
        //            <a href=\"https://jd.jvstvshd.de/necrify/\">Return to version list</a>
        //            """.trimMargin()
        val projects = rootProject.allprojects
        setSource(projects.map { project -> project.sourceSets.main.get().allJava })
        classpath = files(projects.map { project -> project.sourceSets.main.get().compileClasspath })
        val destinationDirectory =
            file("${rootProject.layout.buildDirectory.get()}/docs/javadoc/${rootProject.version}")
        setDestinationDir(destinationDirectory)
        doLast {
            /*try {
                Documentation.buildJavadocIndexFile(file("${rootProject.layout.buildDirectory.get()}/docs/javadoc/index.html").toPath(), rootProject.version.toString())
            } catch (e: Exception) {
                logger.error("Failed to build Javadoc index file", e)
            }*/
            copy {
                from(destinationDirectory)
                into(file("${rootProject.layout.buildDirectory.get()}/docs/javadoc/latest"))
            }
        }
    }
}