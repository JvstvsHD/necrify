import org.gradle.api.Project

object Version {
    const val PROJECT_VERSION = "1.2.1"
    const val PROJECT_GROUP = "de.jvstvshd.necrify"
}

fun Project.buildNumber(): String? {
    if (hasProperty("buildnumber")) {
        return property("buildnumber").toString()
    }
    return System.getenv("GITHUB_RUN_NUMBER")
}

fun Project.publishingVersion(): String {
    val branch = git.currentBranch()
    return if (branch == "master" || branch.startsWith("dev/")) {
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
    "[https://github.com/JvstvsHD/necrify/commit/${latestCommitHash()}](${latestCommitHashShort()}: ${latestCommitMessage()}"
}

val Project.isRelease: Boolean
    get() = !version.toString().contains("-")

val Project.isSnapshot: Boolean
    get() = version.toString().endsWith("-SNAPSHOT")