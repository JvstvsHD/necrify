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

    val versionString: String = project.version as String
    val isRelease: Boolean = !versionString.contains('-')

    val suffixedVersion: String = versionString + if (project.hasProperty("buildnumber")) {
        "-" + project.property("buildnumber") as String
    } else {
        val githubRunNumber = System.getenv("GITHUB_RUN_NUMBER")
        if (githubRunNumber != null) "-$githubRunNumber" else ""
    }
}

fun Project.buildVersion() = Version(this).suffixedVersion

fun Project.latestGitCommitMessage() = Version(this).latestCommitMessage()

fun Project.isRelease() = Version(this).isRelease