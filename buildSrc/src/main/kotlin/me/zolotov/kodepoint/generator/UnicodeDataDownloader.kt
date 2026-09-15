package me.zolotov.kodepoint.generator

import java.net.URI
import java.nio.file.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.exists
import kotlin.io.path.outputStream

object UnicodeDataDownloader {
    fun ensureUnicodeFilesDownloaded(cacheDir: Path, version: UnicodeVersion, dataFileNames: List<String>): Path {
        val dataDir = cacheDir.resolve("unicode-data-$version")
        for (fileName in dataFileNames) {
            val url = "${version.ucdBaseUrl}/$fileName"
            val targetFile = dataDir.resolve(fileName)
            if (targetFile.exists()) {
                println("  Using $fileName from cache")
            } else {
                println("  Downloading $fileName...")
                downloadFile(url, targetFile)
            }
        }
        return dataDir
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
