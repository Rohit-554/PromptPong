package io.jadu.promptpong.data.ai

import io.jadu.promptpong.domain.port.ModelDelivery
import io.jadu.promptpong.domain.port.ModelSetupProgress
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

private const val CONNECT_TIMEOUT_MILLIS = 15_000
private const val READ_TIMEOUT_MILLIS = 30 * 60 * 1_000
private const val BUFFER_SIZE = 32 * 1024

/** Downloads and unpacks the ONNX model bundle on Android. */
class AndroidModelDelivery(
    private val storage: AndroidModelStorage,
) : ModelDelivery {

    override val requiresDownload: Boolean = true

    // First ask the storage keeper whether the Android model is already here.
    /* override suspend fun isReady(): Boolean = storage.isModelReady() */

    // This is the delivery quest. Download, unpack, check, then save the model.
    /* override fun setUp(): Flow<ModelSetupProgress> = flow {
        try {
            if (storage.isModelReady()) {
                emit(ModelSetupProgress.Ready)
                return@flow
            }

            val zip = storage.tempZipFile()
            download(ModelSpec.DOWNLOAD_URL, zip)
            extract(zip, storage.tempExtractDirectory())

            emit(ModelSetupProgress.Verifying)
            verifyExtractedBundle()
            storage.promoteExtractedModel()
            storage.clearTemp()

            emit(ModelSetupProgress.Ready)
        } catch (error: Throwable) {
            storage.clearTemp()
            emit(ModelSetupProgress.Failed(error.message ?: "Model setup failed"))
        }
    }.flowOn(Dispatchers.IO) */

    // The model is on the way. Bring this back to show download progress in the app.
    /* private suspend fun FlowCollector<ModelSetupProgress>.download(url: String, destination: File) {
        destination.delete()

        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = CONNECT_TIMEOUT_MILLIS
            readTimeout = READ_TIMEOUT_MILLIS
            requestMethod = "GET"
            setRequestProperty("Accept-Encoding", "identity")
        }

        try {
            val status = connection.responseCode
            require(status in 200..299) { "Download failed with HTTP $status" }
            val total = connection.contentLengthLong.takeIf { it > 0L }

            connection.inputStream.use { input ->
                FileOutputStream(destination).use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var downloaded = 0L
                    var lastReported = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        downloaded += count
                        if (downloaded - lastReported >= 1_000_000) {
                            lastReported = downloaded
                            emit(ModelSetupProgress.Downloading(downloaded, total))
                        }
                    }
                    require(downloaded > 0L) { "Downloaded model bundle is empty" }
                    emit(ModelSetupProgress.Downloading(downloaded, total))
                }
            }
        } catch (error: Throwable) {
            destination.delete()
            throw error
        } finally {
            connection.disconnect()
        }
    } */

    // A zip file is just a suitcase. Carefully unpack it before the AI can use it.
    /* private suspend fun FlowCollector<ModelSetupProgress>.extract(zip: File, destination: File) {
        destination.deleteRecursively()
        destination.mkdirs()
        val root = destination.canonicalFile
        val totalFiles = ZipFile(zip).use { archive ->
            archive.entries().asSequence().count { !it.isDirectory }
        }

        var extracted = 0
        ZipInputStream(FileInputStream(zip)).use { input ->
            var entry = input.nextEntry
            while (entry != null) {
                if (entry.isDirectory) {
                    File(destination, entry.name).canonicalFile.mkdirs()
                } else {
                    val outputFile = File(destination, entry.name).canonicalFile
                    require(
                        outputFile.path == root.path ||
                            outputFile.path.startsWith(root.path + File.separator),
                    ) { "Unsafe zip entry: ${entry?.name}" }

                    outputFile.parentFile?.mkdirs()
                    FileOutputStream(outputFile).use { output -> input.copyTo(output) }
                    extracted += 1
                    emit(ModelSetupProgress.Extracting(extracted, totalFiles))
                }
                input.closeEntry()
                entry = input.nextEntry
            }
        }
    } */

    /** Checks the required files before promoting the bundle. */
    // Last safety check. Make sure this bundle has the files an ONNX model needs.
    /* private fun verifyExtractedBundle() {
        val extracted = storage.tempExtractDirectory()
        val root = extracted.listFiles()?.singleOrNull()?.takeIf { it.isDirectory } ?: extracted
        val missing = ModelSpec.REQUIRED_FILES.filterNot { File(root, it).isFile }
        require(missing.isEmpty()) {
            "The model bundle is missing: ${missing.joinToString()}"
        }
    } */
}
