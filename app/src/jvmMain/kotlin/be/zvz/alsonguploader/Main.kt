package be.zvz.alsonguploader

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        title = "알송 가사 업로더",
        icon = painterResource("favicon.ico"),
        onCloseRequest = ::exitApplication,
    ) {
        App.main()
    }
}
