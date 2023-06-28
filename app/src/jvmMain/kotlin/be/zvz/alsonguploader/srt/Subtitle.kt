package be.zvz.alsonguploader.srt

/* Create a new Subtitle with the given start and end times. */
data class Subtitle(var startTime: Timestamp, var endTime: Timestamp) {
    val lines = mutableListOf<String>()

    fun clearLines() {
        lines.clear()
    }

    fun addLine(line: String) {
        lines.add(line)
    }

    fun removeLine(line: String) {
        lines.remove(line)
    }

    fun removeLine(index: Int) {
        lines.removeAt(index)
    }

    /* Compiles subtitle into a string with the given subtitle index. */
    fun compile(index: Int): String {
        val subtitle = StringBuilder(index.toString())
            .append("\n")
            .append(startTime.compile())
            .append(" --> ")
            .append(endTime.compile())
            .append("\n")

        lines.forEach { line ->
            subtitle.append(line).append("\n")
        }

        subtitle.append("\n")

        return subtitle.toString()
    }

    companion object {
        fun formatLine(line: String): String {
            /* Replace CRLF with LF for neatness. */
            return line.replace("\r\n", "\n")
                /* Empty line marks the end of a subtitle, replace it with a space.  */
                .replace("\n\n", "\n \n")
        }
    }
}
