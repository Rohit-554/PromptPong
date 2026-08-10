package io.jadu.promptpong.data.ai

import android.content.Context
import java.io.File

/** Where the model bundle lives on Android. */
class AndroidModelStorage(context: Context) {

    private val appContext = context.applicationContext
    private val modelsRoot = File(appContext.filesDir, "models")
    private val tempRoot = File(appContext.cacheDir, "model-downloads")

    fun modelDirectoryPath(): String = File(modelsRoot, ModelSpec.ID).absolutePath

    fun tempZipFile(): File =
        File(tempDirectory(), ModelSpec.ZIP_FILE_NAME).apply { parentFile?.mkdirs() }

    fun tempExtractDirectory(): File = File(tempDirectory(), "extracted")

    // Tiny checkpoint. Ask the phone whether every model file made it home safely.
    /* fun isModelReady(): Boolean {
        val directory = File(modelsRoot, ModelSpec.ID)
        return directory.isDirectory && ModelSpec.REQUIRED_FILES.all { File(directory, it).isFile }
    } */

    /** Promotes a verified extraction to the final location. */
    // You found the moving van. Put the checked model into its final home.
    /* fun promoteExtractedModel() {
        val extracted = tempExtractDirectory()
        require(extracted.isDirectory) { "Nothing was extracted" }

        val source = extracted.listFiles()?.singleOrNull()?.takeIf { it.isDirectory } ?: extracted
        val destination = File(modelsRoot, ModelSpec.ID)
        destination.deleteRecursively()
        destination.parentFile?.mkdirs()
        check(source.copyRecursively(destination, overwrite = true)) {
            "Could not move the extracted model into place"
        }
    } */

    fun clearTemp() {
        tempRoot.deleteRecursively()
    }

    private fun tempDirectory(): File = File(tempRoot, ModelSpec.ID).apply { mkdirs() }
}
