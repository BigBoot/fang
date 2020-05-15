package de.bigboot.ggtools.fang.utils

private fun pluralize(str: String, quantity: Long) = when (quantity) {
    1L -> "$quantity $str"
    else -> "$quantity ${str}s"
}

@Suppress("MagicNumber")
fun Long.milliSecondsToTimespan(milliSecondPrecision: Boolean = false): String {
    val sb = StringBuffer()
    val diffInSeconds = this / 1000
    val milliseconds = this % 1000
    val seconds = if (diffInSeconds >= 60) diffInSeconds % 60 else diffInSeconds
    val minutes = if ((diffInSeconds / 60) >= 60) (diffInSeconds / 60) % (60) else diffInSeconds / 60
    val hours = if ((diffInSeconds / 3600) >= 24) (diffInSeconds / 3600) % (24) else diffInSeconds / 3600
    val days = diffInSeconds / 60 / 60 / 24

    if (days > 0 || hours > 0 || minutes > 0) {
        if (days > 0) {
            sb.append(pluralize("day", days))
            sb.append(" ")
        }
        if (hours > 0 || days > 0) {
            sb.append(pluralize("hour", hours))
            sb.append(" and ")
        }
        if (minutes > 0 || hours > 0 || days > 0) {
            sb.append(pluralize("minute", minutes))
        }
    } else {
        sb.append(pluralize("second", seconds))
        if (milliSecondPrecision) {
            sb.append(" and ")
            sb.append(pluralize("millisecond", milliseconds))
        }
    }
    return sb.toString()
}
