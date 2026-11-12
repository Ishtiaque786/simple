package com.simpleplayer.android

object SubtitleParser {
    private val fullTime = Regex("""(\d{1,2}):(\d{2}):(\d{2})[,.](\d{1,3})""")
    private val shortTime = Regex("""(\d{1,2}):(\d{2})[,.](\d{1,3})""")

    fun parse(raw: String): List<SubtitleCue> {
        val normalized = raw.removePrefix("\uFEFF")
            .replace("\r\n", "\n").replace('\r', '\n')
        val cues = mutableListOf<SubtitleCue>()
        for (block in normalized.split(Regex("""\n\s*\n"""))) {
            val lines = block.lines()
            val index = lines.indexOfFirst { "-->" in it }
            if (index < 0) continue
            val parts = lines[index].split("-->")
            if (parts.size < 2) continue
            val start = parseTime(parts[0].trim()) ?: continue
            val endToken = parts[1].trim().split(Regex("""\s+""")).firstOrNull() ?: continue
            val end = parseTime(endToken) ?: continue
            if (end <= start) continue
            val text = lines.drop(index + 1).joinToString("\n")
                .replace(Regex("""<[^>]*>"""), "").trim()
            if (text.isNotEmpty()) cues += SubtitleCue(start, end, text)
        }
        return cues.sortedBy { it.startMs }
    }

    private fun parseTime(value: String): Long? {
        val full = fullTime.find(value)
        if (full != null) {
            val h = full.groupValues[1].toLongOrNull() ?: return null
            val m = full.groupValues[2].toLongOrNull() ?: return null
            val s = full.groupValues[3].toLongOrNull() ?: return null
            val ms = full.groupValues[4].padEnd(3, '0').take(3).toLongOrNull() ?: 0L
            return ((h * 3600 + m * 60 + s) * 1000) + ms
        }

        val short = shortTime.find(value) ?: return null
        val m = short.groupValues[1].toLongOrNull() ?: return null
        val s = short.groupValues[2].toLongOrNull() ?: return null
        val ms = short.groupValues[3].padEnd(3, '0').take(3).toLongOrNull() ?: 0L
        return ((m * 60 + s) * 1000) + ms
    }
}
