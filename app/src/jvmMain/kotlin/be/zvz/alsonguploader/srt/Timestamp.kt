package be.zvz.alsonguploader.srt

class Timestamp {
    val hours: Long
    val minutes: Long
    val seconds: Long
    val milliseconds: Long

    /* Create a new timestamp at the given time. */
    constructor(hours: Long, minutes: Long, seconds: Long, milliseconds: Long) {
        this.hours = hours
        this.minutes = minutes
        this.seconds = seconds
        this.milliseconds = milliseconds
    }

    /* Create a new timestamp from the given string.
     * Uses the SRT timestamp format:
     * hours:minutes:seconds,milliseconds
     * eg. 00:00:28,400 */
    constructor(time: String) {
        val topParts = time.split(",").dropLastWhile { it.isEmpty() }.toTypedArray()
        if (topParts.size != 2) throw InvalidTimestampFormatException()
        val parts = topParts[0].split(":").dropLastWhile { it.isEmpty() }.toTypedArray()
        if (parts.size != 3) throw InvalidTimestampFormatException()
        hours = parts[0].toLong()
        minutes = parts[1].toLong()
        seconds = parts[2].toLong()
        milliseconds = topParts[1].toLong()
    }

    /* Compiles the timestamp to an SRT timestamp. */
    fun compile(): String {
        return String.format("%02d", hours) + ":" + String.format("%02d", minutes) + ":" + String.format(
            "%02d",
            seconds,
        ) + "," + String.format("%03d", milliseconds)
    }
}
