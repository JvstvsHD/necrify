import io.papermc.hangarpublishplugin.model.Platforms
import org.cadixdev.gradle.licenser.Licenser
import java.io.ByteArrayOutputStream
import java.util.*

plugins {
    `maven-publish`
    signing
    id("org.cadixdev.licenser") version "0.6.1"
    id("io.papermc.hangar-publish-plugin") version "0.1.2"
    java
}

group = "de.jvstvshd.necrify"
version = "1.2.0-SNAPSHOT"

subprojects {
    apply {
        plugin<Licenser>()
        plugin<MavenPublishPlugin>()
        plugin<SigningPlugin>()
        plugin("java")
    }

    license {
        header(rootProject.file("HEADER.txt"))
        include("**/*.java")
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
                        if (project.version.toString().endsWith("-SNAPSHOT"))
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
                        version = project.version.toString()

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
        channel.set(if (!isRelease()) "Snapshot" else "Release")
        id.set("necrify")
        apiKey.set("5eb868d8-6dbf-4d5e-92be-6cf2c0126818.cfb7e524-0b1f-48c9-9ac4-8f31f0fa1f80")
        //apiKey.set(System.getenv("HANGAR_API_TOKEN"))
        if (!isRelease()) {
            changelog.set(latestGitCommitMessage())
        }
        platforms {
            register(Platforms.PAPER) {
                jar.set(project(":necrify-paper").tasks.jar.flatMap { it.archiveFile })
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
        setDestinationDir(file("${layout.buildDirectory.get()}/docs/javadoc"))
        val projects = rootProject.allprojects
        setSource(projects.map { project -> project.sourceSets.main.get().allJava })
        classpath = files(projects.map { project -> project.sourceSets.main.get().compileClasspath })
    }
}