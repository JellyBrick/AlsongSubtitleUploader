package be.zvz.alsonguploader

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.swing.JFileChooser
import javax.swing.UIManager
import javax.swing.filechooser.FileNameExtensionFilter

internal object FileChooser {
    private enum class CallType {
        FILE,
        DIRECTORY,
    }

    suspend fun chooseFile(
        initialDirectory: String = System.getProperty("user.dir"),
        fileExtensions: String = "",
    ): String? {
        return chooseFile(CallType.FILE, initialDirectory, fileExtensions)
    }

    suspend fun chooseDirectory(
        initialDirectory: String = System.getProperty("user.dir"),
    ): String? {
        return chooseFile(CallType.DIRECTORY, initialDirectory)
    }

    private suspend fun chooseFile(
        type: CallType,
        initialDirectory: String,
        fileExtensions: String = "",
    ): String? {
        return kotlin.runCatching { chooseFileSwing(type, initialDirectory, fileExtensions) }
            .onFailure { swingException ->
                println("A call to chooseDirectorySwing failed ${swingException.message}")
            }
            .getOrNull()
    }

    private suspend fun chooseFileSwing(
        type: CallType,
        initialDirectory: String,
        fileExtension: String,
    ) = withContext(Dispatchers.IO) {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())

        val chooser = when (type) {
            CallType.FILE -> {
                JFileChooser(initialDirectory).apply {
                    fileSelectionMode = JFileChooser.FILES_ONLY
                    isAcceptAllFileFilterUsed = false
                    isVisible = true
                    addChoosableFileFilter(FileNameExtensionFilter(fileExtension, fileExtension))
                }
            }

            CallType.DIRECTORY -> {
                JFileChooser(initialDirectory).apply {
                    fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                    isVisible = true
                }
            }
        }

        when (val code = chooser.showOpenDialog(null)) {
            JFileChooser.APPROVE_OPTION -> chooser.selectedFile.absolutePath
            JFileChooser.CANCEL_OPTION -> null
            JFileChooser.ERROR_OPTION -> error("An error occurred while executing JFileChooser::showOpenDialog")
            else -> error("Unknown return code '$code' from JFileChooser::showOpenDialog")
        }
    }
}
