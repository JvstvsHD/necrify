import org.cadixdev.gradle.licenser.Licenser
import java.util.*

plugins {
    `maven-publish`
    signing
    id("org.cadixdev.licenser") version "0.6.1"
}

group = "de.jvstvshd.necrify"
version = "1.2.0-SNAPSHOT"

subprojects {
    apply {
        plugin<Licenser>()
        plugin<MavenPublishPlugin>()
        plugin<SigningPlugin>()
    }

    license {
        header(rootProject.file("HEADER.txt"))
        include("**/*.java")
        newLine(true)
    }

    repositories {
        mavenCentral()
        maven("https://nexus.velocitypowered.com/repository/maven-public/")
        maven("https://papermc.io/repo/repository/maven-public/")
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
                        artifactId = "necrify-${project.name}"
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

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    sourceCompatibility = "17"
    targetCompatibility = "17"
}