import org.gradle.api.Project
import java.io.ByteArrayOutputStream

//https://docs.papermc.io/misc/hangar-publishing
class Git(private val project: Project) {

    fun latestCommitMessage(): String {
        return project.git("log", "-1", "--pretty=%B")
    }

    fun latestCommitHash(): String {
        return project.git("rev-parse", "HEAD")
    }

    fun latestCommitHashShort(): String {
        return project.git("rev-parse", "--short", "HEAD")
    }

    fun currentBranch(): String {
        return project.git("rev-parse", "--abbrev-ref", "HEAD")
    }
}

fun Project.git(vararg command: String): String {
    val byteOut = ByteArrayOutputStream()
    project.exec {
        commandLine = listOf("git", *command)
        standardOutput = byteOut
        errorOutput = System.err
    }
    return byteOut.toString(Charsets.UTF_8.name()).trim()
}

val Project.git: Git
    get() = Git(this)
