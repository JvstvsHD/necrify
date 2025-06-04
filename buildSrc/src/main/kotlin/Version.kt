import org.gradle.api.Project

object Version {
    const val PROJECT_VERSION = "1.3.0-SNAPSHOT"
    const val PROJECT_GROUP = "de.jvstvshd.necrify"
}

fun Project.buildNumber(): String? {
    if (hasProperty("buildnumber")) {
        return property("buildnumber").toString()
    }
    return System.getenv("BUILD_NUMBER") ?: System.getenv("GITHUB_RUN_NUMBER") //Jenkins and Github
}

fun Project.publishingVersion(): String {
    val branch = git.currentBranch()
    return if (branch == "master" || branch.startsWith("dev")) {
        version.toString()
    } else "${branch.replace('/', '-')}-SNAPSHOT"
}

fun Project.buildVersion(): String {
    val versionString: String = project.version as String
    if (!project.isSnapshot) {
        return versionString
    }
    val buildNum = project.buildNumber()
    return if (buildNum != null) {
        "$versionString-$buildNum"
    } else {
        versionString
    }
}

fun Project.changelogMessage() = with(git) {
    "[${latestCommitHashShort()}](https://github.com/JvstvsHD/necrify/commit/${latestCommitHash()}): ${latestCommitMessage()}"
}

val Project.isRelease: Boolean
    get() = !version.toString().contains("-")

val Project.isSnapshot: Boolean
    get() = version.toString().endsWith("-SNAPSHOT")