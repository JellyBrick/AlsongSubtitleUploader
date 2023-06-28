package be.zvz.alsonguploader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

interface MPFile<out T : Any> {
    // on JS this will be a file name, on other platforms it will be a file path
    val path: String
    val platformFile: T
}

data class JvmFile(
    override val path: String,
    override val platformFile: File,
) : MPFile<File>

@Composable
fun FilePicker(
    show: Boolean,
    initialDirectory: String? = null,
    fileExtensions: List<String> = emptyList(),
    onFileSelected: (MPFile<Any>?) -> Unit,
) {
    val scope = rememberCoroutineScope()
    LaunchedEffect(show) {
        if (show) {
            scope.launch(Dispatchers.Default) {
                val fileFilter = if (fileExtensions.isNotEmpty()) {
                    fileExtensions.joinToString(",")
                } else {
                    ""
                }

                val initialDir = initialDirectory ?: System.getProperty("user.dir")
                val filePath = FileChooser.chooseFile(
                    initialDirectory = initialDir,
                    fileExtensions = fileFilter,
                )
                withContext(Dispatchers.Main) {
                    if (filePath != null) {
                        onFileSelected(JvmFile(filePath, File(filePath)))
                    } else {
                        onFileSelected(null)
                    }
                }
            }
        }
    }
}

@Composable
fun DirectoryPicker(
    show: Boolean,
    initialDirectory: String? = null,
    onFileSelected: (String?) -> Unit,
) {
    val scope = rememberCoroutineScope()
    LaunchedEffect(show) {
        if (show) {
            scope.launch(Dispatchers.Default) {
                val initialDir = initialDirectory ?: System.getProperty("user.dir")
                val fileChosen = FileChooser.chooseDirectory(initialDir)
                withContext(Dispatchers.Main) {
                    onFileSelected(fileChosen)
                }
            }
        }
    }
}
