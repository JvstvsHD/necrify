import org.gradle.api.Project
import java.io.ByteArrayOutputStream

class Version(val project: Project) {

    //https://docs.papermc.io/misc/hangar-publishing
    fun executeGitCommand(vararg command: String): String {
        val byteOut = ByteArrayOutputStream()
        project.exec {
            commandLine = listOf("git", *command)
            standardOutput = byteOut
        }
        return byteOut.toString(Charsets.UTF_8.name()).trim()
    }

    fun latestCommitMessage(): String {
        return executeGitCommand("log", "-1", "--pretty=%B")
    }

    fun latestCommitHash(): String {
        return executeGitCommand("rev-parse", "HEAD")
    }

    fun latestCommitHashShort(): String {
        return executeGitCommand("rev-parse", "--short", "HEAD")
    }

    fun buildNumber(): String? {
        if (project.hasProperty("buildnumber")) {
            return project.property("buildnumber").toString()
        }
        return System.getenv("GITHUB_RUN_NUMBER")
    }

    val versionString: String = project.version as String
    val isRelease: Boolean = !versionString.contains("-")
    val suffixedVersion: String = if (project.isSnapshot) versionString +
            if (project.hasProperty("buildnumber")) {
                "-" + project.property("buildnumber") as String
            } else {
                val githubRunNumber = System.getenv("GITHUB_RUN_NUMBER")
                if (githubRunNumber != null) "-$githubRunNumber" else ""
            } else versionString
}

fun Project.buildVersion() = Version(this).suffixedVersion

fun Project.changelogMessage() = with(Version(this)) {
    "https://github.com/JvstvsHD/necrify/commit/${latestCommitHash()}: ${latestCommitMessage()}"
}

fun Project.isRelease() = Version(this).isRelease

val Project.isSnapshot: Boolean
    get() = version.toString().endsWith("-SNAPSHOT")