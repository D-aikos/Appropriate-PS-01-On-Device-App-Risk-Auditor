package com.appropriate.riskauditor.core.report

import android.graphics.Paint

/**
 * Text processing utilities for PDF rendering adhering strictly to Section 7.8:
 * - sanitize(): strips control characters (below 0x20 except space) and bidi override chars (U+202A..U+202E, U+2066..U+2069)
 * - ellipsize(): measures text against maxWidth using Paint and appends ellipsis
 * - wrap(): wraps multi-line text into lines fitting within maxWidth
 * - capLabel(): truncates labels at maxChars (default 60)
 */
object PdfTextUtil {

    private val BIDI_CONTROL_REGEX = Regex("[\u202A-\u202E\u2066-\u2069]")

    fun sanitize(text: String?): String {
        if (text == null) return ""
        val sb = java.lang.StringBuilder(text.length)
        for (ch in text) {
            // Strip control characters (below 0x20 except space)
            if (ch.code < 0x20 && ch != ' ') {
                continue
            }
            // Strip bidi override characters (U+202A..U+202E, U+2066..U+2069)
            if (ch in '\u202A'..'\u202E' || ch in '\u2066'..'\u2069') {
                continue
            }
            sb.append(ch)
        }
        return sb.toString().trim()
    }

    fun capLabel(label: String?, maxChars: Int = 60): String {
        val s = sanitize(label)
        return if (s.length > maxChars) {
            s.take(maxChars - 3) + "..."
        } else {
            s
        }
    }

    fun ellipsize(text: String, paint: Paint, maxWidth: Float): String {
        val clean = sanitize(text)
        if (paint.measureText(clean) <= maxWidth) return clean

        val ellipsis = "..."
        val ellipsisWidth = paint.measureText(ellipsis)
        val availableWidth = maxWidth - ellipsisWidth
        if (availableWidth <= 0) return ellipsis

        var low = 0
        var high = clean.length
        var best = 0

        while (low <= high) {
            val mid = (low + high) ushr 1
            val sub = clean.substring(0, mid)
            if (paint.measureText(sub) <= availableWidth) {
                best = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }

        return clean.substring(0, best) + ellipsis
    }

    fun wrap(text: String, paint: Paint, maxWidth: Float): List<String> {
        val clean = sanitize(text)
        if (clean.isEmpty()) return emptyList()
        if (paint.measureText(clean) <= maxWidth) return listOf(clean)

        val words = clean.split(Regex("\\s+"))
        val lines = mutableListOf<String>()
        val currentLine = StringBuilder()

        for (word in words) {
            if (currentLine.isEmpty()) {
                currentLine.append(word)
            } else {
                val candidate = "$currentLine $word"
                if (paint.measureText(candidate) <= maxWidth) {
                    currentLine.append(" ").append(word)
                } else {
                    lines.add(currentLine.toString())
                    currentLine.setLength(0)
                    currentLine.append(word)
                }
            }
        }

        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }

        return lines
    }
}
