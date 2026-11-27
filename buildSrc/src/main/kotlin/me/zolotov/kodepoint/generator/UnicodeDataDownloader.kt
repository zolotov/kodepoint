package me.zolotov.kodepoint.generator

import java.net.URI
import java.nio.file.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.exists
import kotlin.io.path.outputStream

object UnicodeDataDownloader {
    fun ensureUnicodeFilesDownloaded(cacheDir: Path, version: UnicodeVersion, dataFileNames: List<String>): Path =
        ensureFilesDownloaded(cacheDir.resolve("unicode-data-$version"), version.ucdBaseUrl, dataFileNames)

    /**
     * Downloads the emoji data files that live outside the UCD, under `Public/emoji/<major.minor>/`
     * (for example `emoji-test.txt`), into their own per-version cache directory.
     */
    fun ensureEmojiFilesDownloaded(cacheDir: Path, version: UnicodeVersion, dataFileNames: List<String>): Path =
        ensureFilesDownloaded(cacheDir.resolve("emoji-data-${version.emojiVersion}"), version.emojiBaseUrl, dataFileNames)

    private fun ensureFilesDownloaded(dataDir: Path, baseUrl: String, dataFileNames: List<String>): Path {
        for (fileName in dataFileNames) {
            val url = "$baseUrl/$fileName"
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
