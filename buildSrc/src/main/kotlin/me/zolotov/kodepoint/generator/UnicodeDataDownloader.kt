package me.zolotov.kodepoint.generator

import java.net.URI
import java.nio.file.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.exists
import kotlin.io.path.outputStream

object UnicodeDataDownloader {
    fun ensureUnicodeFilesDownloaded(dataDir: Path, dataFileNames: List<String>) {
        for (fileName in dataFileNames) {
            val url = "$UNICODE_BASE_URL/$fileName"
            val targetFile = dataDir.resolve(fileName)
            if (targetFile.exists()) {
                println("  Using $fileName from cache")
            } else {
                println("  Downloading $fileName...")
                downloadFile(url, targetFile)
            }
        }
    }

    private fun downloadFile(url: String, target: Path) {
        val uri = URI(url)
        uri.toURL().openStream().use { input ->
            target.createParentDirectories().outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }
}
