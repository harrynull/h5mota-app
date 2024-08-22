package tech.harrynull.h5mota.utils

import android.content.Context
import android.util.Log
import com.kdownloader.KDownloader
import tech.harrynull.h5mota.api.downloadUrl
import tech.harrynull.h5mota.models.Tower
import java.io.File
import java.nio.charset.Charset
import java.util.zip.ZipFile

class DownloadManager(private val context: Context) {
    private fun downloadRoot() = context.filesDir.resolve("towers")

    fun saveRoot() = context.filesDir.resolve("saves")
    
    fun download(tower: Tower, onProgress: (Int) -> Unit, onCompleted: () -> Unit) {
        val kDownloader = KDownloader.create(context)
        kDownloader.enqueue(
            kDownloader.newRequestBuilder(
                url = tower.downloadUrl(),
                dirPath = downloadRoot().absolutePath,
                fileName = "${tower.name}.zip"
            ).build(),
            onProgress = onProgress,
            onCompleted = {
                val downloadedZip = downloadRoot().resolve("${tower.name}.zip")
                // unzip
                ZipFile(downloadedZip, Charset.forName("GBK")).use { zip ->
                    val totalFileCount = zip.size()
                    zip.entries().asSequence().forEachIndexed { index, entry ->
                        val dest = downloadRoot().resolve(entry.name)
                        Log.i(
                            "DownloadManager",
                            "Unzipping (d?=${entry.isDirectory}) ${entry.name} to ${dest.absolutePath}"
                        )
                        onProgress(index * 100 / totalFileCount)
                        if (entry.isDirectory) {
                            dest.mkdirs()
                            return@forEachIndexed
                        }
                        zip.getInputStream(entry).use { input ->
                            dest.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                }
                // rm zip
                downloadedZip.delete()
                onCompleted()
            }
        )

    }

    fun getDownloadedPath(tower: Tower): File {
        return downloadRoot().resolve(tower.name).resolve("index.html")
    }

    fun downloaded(tower: Tower): Boolean {
        return getDownloadedPath(tower).exists()
    }

    fun getAllDownloaded(): List<String> {
        return downloadRoot().list()?.toList() ?: emptyList()
    }
}