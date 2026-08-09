package io.jadu.promptpong.data.ai

/** The Android model bundle. */
object ModelSpec {
    const val ID = "google-gemma-3-270m-onnx"
    const val DISPLAY_NAME = "Gemma 3 270M"
    const val ZIP_FILE_NAME = "gemma-3-270m-onnx.zip"

    const val DOWNLOAD_URL =
        "https://github.com/Rohit-554/MindKit/releases/download/gemma-3-270m-onnx-v1/gemma-3-270m-onnx.zip"

    /** Checked after extraction. */
    val REQUIRED_FILES = listOf(
        "genai_config.json",
        "tokenizer.json",
        "tokenizer_config.json",
    )
}
