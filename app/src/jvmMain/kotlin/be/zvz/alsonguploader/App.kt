package be.zvz.alsonguploader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import be.zvz.alsong.Alsong
import be.zvz.alsong.dto.LyricLookup
import be.zvz.alsong.dto.LyricUploadResult
import be.zvz.alsong.dto.UserData
import be.zvz.alsong.exception.InvalidDataReceivedException
import be.zvz.alsonguploader.srt.SubtitleFile
import com.github.kittinunf.fuel.core.FuelManager
import org.apache.commons.io.FilenameUtils
import org.farng.mp3.MP3File
import ws.schild.jave.MultimediaObject
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.TreeMap
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlin.system.exitProcess

object App {
    private val alsong = Alsong(
        fuelManager = FuelManager().apply {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun getAcceptedIssuers(): Array<X509Certificate>? = null
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = Unit
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) = Unit
            })

            socketFactory = SSLContext.getInstance("SSL").apply {
                init(null, trustAllCerts, SecureRandom())
            }.socketFactory

            hostnameVerifier = HostnameVerifier { _, _ -> true }
        },
    )
    private fun getHash(f: File): String {
        val mp3 = MP3File()
        val start = mp3.getMp3StartByte(f)
        val data = ByteArray(163840)
        FileInputStream(f).use { fis ->
            fis.channel.position(start)
            fis.read(data, 0, 163840)
        }
        val md5 = MessageDigest.getInstance("MD5")
        md5.update(data)
        val md5enc = md5.digest()
        val sb = StringBuilder()
        for (b in md5enc) {
            sb.append(((b.toInt() and 0xff) + 0x100).toString(16).substring(1))
        }
        return sb.toString()
    }

    @Composable
    fun main() = AppTheme {
        var currentScreen: Screen by remember { mutableStateOf(Screen.FilePicker) }
        var userName by remember { mutableStateOf("") }
        var title by remember { mutableStateOf("") }
        var artist by remember { mutableStateOf("") }
        var album by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(false) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = "알송 가사 업로더") },
                )
            },
        ) {
            when (currentScreen) {
                Screen.FilePicker -> filePickerScreen(
                    onFilesSelected = { mp3File, metadata, srtFile, isModifying, originalLyric ->
                        // Perform file validation or any other logic
                        currentScreen = Screen.Upload(mp3File, metadata, srtFile, isModifying, originalLyric)
                    },
                )
                is Screen.Upload -> {
                    val uploadScreen = currentScreen as Screen.Upload
                    title = if (uploadScreen.isModifying) {
                        uploadScreen.originalLyric?.title ?: uploadScreen.metadata.title
                    } else {
                        uploadScreen.metadata.title
                    }
                    artist = if (uploadScreen.isModifying) {
                        uploadScreen.originalLyric?.artist ?: uploadScreen.metadata.artist
                    } else {
                        uploadScreen.metadata.artist
                    }
                    album = if (uploadScreen.isModifying) {
                        uploadScreen.originalLyric?.albumName ?: uploadScreen.metadata.album
                    } else {
                        uploadScreen.metadata.album
                    }
                    uploadScreen(
                        isModifying = uploadScreen.isModifying,
                        getUserName = { userName },
                        onUserNameChange = { userName = it },
                        getTitle = { title },
                        onTitleChange = { title = it },
                        getArtist = { artist },
                        onArtistChange = { artist = it },
                        getAlbum = { album },
                        onAlbumChange = { album = it },
                        onUpload = { isNotOriginalAuthor ->
                            isLoading = true
                            val lyrics: Map<Long, List<String>> = TreeMap<Long, List<String>>().apply {
                                SubtitleFile(File(uploadScreen.srtFile)).getSubtitles().forEach {
                                    val time = it.startTime.hours * 3600000L +
                                        it.startTime.minutes * 60000L +
                                        it.startTime.seconds * 1000L +
                                        it.startTime.milliseconds
                                    val srtLines = it.lines.take(3)

                                    put(time, srtLines)
                                }
                            }

                            val uploadResponse = alsong.uploadLyric(
                                isModifying = isNotOriginalAuthor,
                                lyric = lyrics,
                                md5 = uploadScreen.metadata.hash,
                                registerData = if (isNotOriginalAuthor) {
                                    uploadScreen.originalLyric?.let {
                                        return@let UserData(
                                            firstName = it.registerFirstName ?: "",
                                            firstEmail = it.registerFirstEmail ?: "",
                                            firstUrl = it.registerFirstUrl ?: "",
                                            firstPhone = it.registerFirstPhone ?: "",
                                            firstComment = it.registerFirstComment ?: "",
                                            name = userName,
                                            email = "",
                                            url = "",
                                            phone = "",
                                            comment = "",
                                        )
                                    } ?: UserData(
                                        firstName = userName,
                                    )
                                } else {
                                    UserData(
                                        firstName = userName,
                                    )
                                },
                                fileName = FilenameUtils.getName(uploadScreen.mp3File),
                                title = title,
                                artist = artist,
                                album = album,
                                playtime = uploadScreen.metadata.playtime,
                                originalLyricId = (
                                    if (isNotOriginalAuthor) {
                                        uploadScreen.originalLyric?.infoId
                                    } else {
                                        -1
                                    }
                                    ) ?: -1,
                            )
                            currentScreen = Screen.UploadResult(uploadResponse)
                        },
                        isLoading = isLoading,
                    )
                }
                is Screen.UploadResult -> {
                    val uploadResultScreen = currentScreen as Screen.UploadResult
                    val resultString = when (
                        val code = uploadResultScreen.result.body.uploadLyricResponse.uploadLyricResult
                    ) {
                        "NotEnough" -> "타이틀, 아티스트, 앨범을 모두 입력해주세요."
                        "ModifySuccessed" -> "가사 수정 성공. 관리자의 검토를 거친 후 반영됩니다."
                        "RegistSuccessed" -> "가사 등록 성공. 관리자의 검토를 거친 후 반영됩니다."
                        else -> "알 수 없는 오류: $code"
                    }
                    uploadResultScreen(
                        uploadResult = resultString,
                        onExit = { exitProcess(0) },
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    fun filePickerScreen(
        onFilesSelected: (
            mp3File: String,
            metadata: Metadata,
            srtFile: String,
            isModifying: Boolean,
            originalLyric: LyricLookup?,
        ) -> Unit,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            var showMp3FilePicker by remember { mutableStateOf(false) }
            var showSrtFilePicker by remember { mutableStateOf(false) }

            var mp3File by remember { mutableStateOf("") }
            var metadata: Metadata? by remember { mutableStateOf(null) }
            var srtFile by remember { mutableStateOf("") }

            var showEmptyAlertDialog by remember { mutableStateOf(false) }
            var showDuplicatedAlertDialog by remember { mutableStateOf(false) }

            var isLoading by remember { mutableStateOf(false) }
            var originalLyric by remember { mutableStateOf<LyricLookup?>(null) }

            FilePicker(showMp3FilePicker, fileExtensions = listOf("mp3")) { path ->
                showMp3FilePicker = false
                mp3File = path?.path ?: ""
            }
            FilePicker(showSrtFilePicker, fileExtensions = listOf("srt")) { path ->
                showSrtFilePicker = false
                srtFile = path?.path ?: ""
            }

            Text(text = "MP3: $mp3File")
            Text(text = "SRT: $srtFile")

            Button(
                onClick = {
                    showMp3FilePicker = true
                },
            ) {
                Text(text = "MP3 선택")
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (showEmptyAlertDialog) {
                AlertDialog(
                    onDismissRequest = {},
                    title = { Text(text = "오류", color = Color.Red, style = TextStyle(fontWeight = FontWeight.Bold)) },
                    confirmButton = {
                        Button(onClick = {
                            showEmptyAlertDialog = false
                        }) {
                            Text("확인")
                        }
                    },
                    text = { Text("MP3 파일 / SRT 파일이 입력되지 않았습니다.") },
                )
            }

            if (showDuplicatedAlertDialog) {
                AlertDialog(
                    onDismissRequest = {},
                    title = { Text(text = "주의", color = Color.Yellow, style = TextStyle(fontWeight = FontWeight.Bold)) },
                    confirmButton = {
                        Button(onClick = {
                            showDuplicatedAlertDialog = false
                            onFilesSelected(mp3File, metadata!!, srtFile, true, originalLyric)
                        }) {
                            Text("확인")
                        }
                    },
                    dismissButton = {
                        Button(onClick = {
                            showDuplicatedAlertDialog = false
                        }) {
                            Text("취소")
                        }
                    },
                    text = { Text("해당 파일에 가사가 존재합니다. 진행하시겠습니까?") },
                )
            }

            if (isLoading) {
                CircularProgressIndicator()
            }

            Button(
                onClick = {
                    showSrtFilePicker = true
                },
            ) {
                Text(text = "SRT 선택")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (mp3File.isNotEmpty() && srtFile.isNotEmpty()) {
                        isLoading = true
                        val mp3FileObject = File(mp3File)
                        // get duration from mp3 with jave2 with MultiMediaInfo
                        val multimediaObject = MultimediaObject(mp3FileObject)
                        val info = multimediaObject.info
                        val duration = info.duration
                        val mp3Hash = getHash(mp3FileObject)
                        if (
                            try {
                                val lyric = alsong.getLyricByHash(mp3Hash)
                                metadata = Metadata(
                                    lyric.artist,
                                    lyric.title,
                                    lyric.albumName,
                                    mp3Hash,
                                    duration,
                                )
                                originalLyric = lyric
                                isLoading = false
                                true
                            } catch (e: InvalidDataReceivedException) {
                                false
                            }
                        ) {
                            showDuplicatedAlertDialog = true
                        } else {
                            val mp3 = MP3File(mp3FileObject)
                            metadata = if (mp3.hasID3v1Tag()) {
                                Metadata(
                                    mp3.iD3v1Tag.artist,
                                    mp3.iD3v1Tag.title,
                                    mp3.iD3v1Tag.album,
                                    mp3Hash,
                                    duration,
                                )
                            } else if (mp3.hasID3v2Tag()) {
                                Metadata(
                                    mp3.iD3v2Tag.leadArtist,
                                    mp3.iD3v2Tag.songTitle,
                                    mp3.iD3v2Tag.albumTitle,
                                    mp3Hash,
                                    duration,
                                )
                            } else {
                                Metadata(
                                    "",
                                    "",
                                    "",
                                    mp3Hash,
                                    duration,
                                )
                            }
                            onFilesSelected(mp3File, metadata!!, srtFile, false, null)
                        }
                    } else {
                        showDuplicatedAlertDialog = true
                    }
                },
            ) {
                Text(text = "진행")
            }
        }
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    fun uploadScreen(
        isModifying: Boolean,
        getUserName: () -> String,
        onUserNameChange: (String) -> Unit,
        getTitle: () -> String,
        onTitleChange: (String) -> Unit,
        getArtist: () -> String,
        onArtistChange: (String) -> Unit,
        getAlbum: () -> String,
        onAlbumChange: (String) -> Unit,
        onUpload: (Boolean) -> Unit,
        isLoading: Boolean,
    ) {
        var showEmptyAlertDialog by remember { mutableStateOf(false) }
        val isNotOriginalAuthor = remember { mutableStateOf(isModifying) }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = isNotOriginalAuthor.value,
                    onCheckedChange = { isNotOriginalAuthor.value = it },
                    enabled = isModifying,
                )
                Text(
                    modifier = Modifier.padding(start = 2.dp),
                    text = "가사의 최초 등록자를 기존 가사 등록자로 설정",
                    style = TextStyle(fontWeight = FontWeight.Bold),
                    color = if (isModifying) Color.Unspecified else Color.Gray,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            TextField(
                value = getUserName(),
                onValueChange = onUserNameChange,
                label = { Text(text = "내 닉네임 입력") },
            )
            Spacer(modifier = Modifier.height(16.dp))
            TextField(
                value = getTitle(),
                onValueChange = onTitleChange,
                label = { Text(text = "제목 입력") },
            )
            Spacer(modifier = Modifier.height(16.dp))
            TextField(
                value = getArtist(),
                onValueChange = onArtistChange,
                label = { Text(text = "아티스트 입력") },
            )
            Spacer(modifier = Modifier.height(16.dp))
            TextField(
                value = getAlbum(),
                onValueChange = onAlbumChange,
                label = { Text(text = "앨범 입력") },
            )

            if (showEmptyAlertDialog) {
                AlertDialog(
                    onDismissRequest = {},
                    title = { Text(text = "오류", color = Color.Red, style = TextStyle(fontWeight = FontWeight.Bold)) },
                    confirmButton = {
                        Button(onClick = {
                            showEmptyAlertDialog = false
                        }) {
                            Text("확인")
                        }
                    },
                    text = { Text("제목, 아티스트, 앨범을 모두 입력해주세요.") },
                )
            }

            Button(
                onClick = {
                    if (getTitle().isEmpty() || getArtist().isEmpty() || getAlbum().isEmpty()) {
                        showEmptyAlertDialog = true
                    } else {
                        onUpload(isNotOriginalAuthor.value)
                    }
                },
                enabled = !isLoading,
            ) {
                if (isLoading) {
                    CircularProgressIndicator()
                } else {
                    Text(text = "업로드")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    @Composable
    fun uploadResultScreen(uploadResult: String, onExit: () -> Unit) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = uploadResult)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onExit,
            ) {
                Text(text = "Exit")
            }
        }
    }

    sealed class Screen {
        object FilePicker : Screen()
        data class Upload(
            val mp3File: String,
            val metadata: Metadata,
            val srtFile: String,
            val isModifying: Boolean,
            val originalLyric: LyricLookup? = null,
        ) : Screen()
        data class UploadResult(val result: LyricUploadResult) : Screen()
    }

    data class Metadata(
        val artist: String,
        val title: String,
        val album: String,
        val hash: String,
        val playtime: Long,
    )
}
