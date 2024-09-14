import io.papermc.hangarpublishplugin.model.Platforms
import net.kyori.indra.licenser.spotless.IndraSpotlessLicenserPlugin
import java.util.*

plugins {
    `maven-publish`
    signing
    id("io.papermc.hangar-publish-plugin") version "0.1.2"
    id("com.gradleup.shadow") version "8.3.1" apply false
    id("net.kyori.indra.licenser.spotless") version "2.2.0"
    java
}

group = "de.jvstvshd.necrify"
version = "1.2.1-SNAPSHOT"

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
        (options as StandardJavadocDocletOptions).tags(
            "apiNote:a:API Note",
            "implSpec:a:Implementation Requirements",
            "implNote:a:Implementation Note"
        )
        setDestinationDir(file("${layout.buildDirectory.get()}/docs/javadoc"))
        val projects = rootProject.allprojects
        setSource(projects.map { project -> project.sourceSets.main.get().allJava })
        classpath = files(projects.map { project -> project.sourceSets.main.get().compileClasspath })
    }
}