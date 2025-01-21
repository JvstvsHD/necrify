import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.nio.file.Path
import kotlin.io.path.createFile
import kotlin.io.path.exists
import kotlin.io.path.writeText

object Documentation {

    fun buildJavadocIndexFile(path: Path, version: String) {
        val document = if (!path.exists()) {
            if (!path.parent.exists()) path.parent.createFile()
            path.createFile()
            generateNewIndexFile(path)
        } else Jsoup.parse(path.toFile())
        val ul = document.getElementsByTag("ul").first() ?: document.body().appendElement("ul")
        val children = ul.children()
        if (children.find { child -> child.attr("version") == version } == null) {
            val html = "<a href='https://docs.jvstvshd.de/necrify/javadoc/$version'>Necrify $version</a>"
            ul.appendElement("li").html(html).attr("version", version)
        }
        path.writeText(document.outerHtml())
    }

    fun generateNewIndexFile(path: Path): Document {
        val document =
            Jsoup.parse("<html><head><title>Necrify Javadoc Index</title></head><body><h1>List of Necrify Javadocs:</h1><ul></ul></body></html>")
        path.toFile().writeText(document.outerHtml())
        return document
    }
}