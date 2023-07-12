package be.zvz.alsonguploader.srt

import org.apache.any23.encoding.TikaEncodingDetector
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.Scanner

class SubtitleFile {
    private val subtitles: MutableList<Subtitle>

    /* Create a new SubtitleFile. */
    constructor() {
        subtitles = mutableListOf()
    }

    /* Load an existing SubtitleFile from a File. */
    constructor(file: File) {
        subtitles = mutableListOf()
        val encoding = file.inputStream().use { Charset.forName(TikaEncodingDetector().guessEncoding(it)) }
        file.inputStream().bufferedReader(encoding).use {
            val scanner = Scanner(it)

            if (scanner.hasNextLine()) {
                scanner.nextLine()
            }

            while (scanner.hasNextLine()) {
                /* We assign our own ID's, ignore the ID given in the file. */

                /* Read the Timestamps from the file. */
                val timestamps = scanner.nextLine().split(" --> ")
                if (timestamps.size != 2) throw InvalidTimestampFormatException()
                val startTime = Timestamp(timestamps[0])
                val endTime = Timestamp(timestamps[1])
                val subtitle = Subtitle(startTime, endTime)
                var line: String = scanner.nextLine()
                var beforeLine: String? = null
                while (scanner.hasNextLine()) {
                    if (beforeLine != null && beforeLine.isEmpty() && line.toLongOrNull() != null) break
                    subtitle.addLine(line)
                    beforeLine = line
                    line = scanner.nextLine()
                }
                subtitles.add(subtitle)
            }
        }
    }

    fun addSubtitle(subtitle: Subtitle) {
        subtitles.add(subtitle)
    }

    fun clearSubtitles() {
        subtitles.clear()
    }

    fun removeSubtitle(subtitle: Subtitle) {
        subtitles.remove(subtitle)
    }

    fun removeSubtitle(index: Int) {
        subtitles.removeAt(index)
    }

    fun getSubtitle(index: Int): Subtitle {
        return subtitles[index]
    }

    fun getSubtitles(): List<Subtitle> {
        return subtitles
    }

    fun compile(): String {
        val string = StringBuilder()

        /* Subtitle indexes start at 1 */
        var index = 1
        for (subtitle in subtitles) {
            string.append(subtitle.compile(index))
            index++
        }
        return string.toString()
    }

    @Throws(IOException::class)
    fun save(file: File) {
        FileOutputStream(file).use {
            it.write(compile().toByteArray(StandardCharsets.UTF_8))
        }
    }
}
