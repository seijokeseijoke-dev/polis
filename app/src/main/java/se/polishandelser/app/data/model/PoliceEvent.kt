package se.polishandelser.app.data.model

import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Motsvarar ett objekt från https://polisen.se/api/events
 *
 * Exempel:
 * {"id":656771,"datetime":"2026-10-20 8:28:36 +02:00","name":"20 oktober 08.06, Trafikolycka, Skövde",
 *  "summary":"...","url":"/aktuellt/handelser/2026/oktober/20/...","type":"Trafikolycka",
 *  "location":{"name":"Västra Götalands län","gps":"58.252793,13.059643"}}
 *
 * Gson respekterar inte Kotlins null-säkerhet, därför är alla fält nullable
 * och UI:t använder de säkra hjälpegenskaperna nedan.
 */
data class PoliceEvent(
    val id: Int,
    val datetime: String?,
    val name: String?,
    val summary: String?,
    val url: String?,
    val type: String?,
    val location: Location?
) {
    val title: String get() = name.orEmpty()
    val summaryText: String get() = summary.orEmpty()
    val typeText: String get() = type.orEmpty()
    val locationName: String get() = location?.name.orEmpty()

    /** Full länk till händelsesidan på polisen.se (API:t ger en relativ sökväg). */
    val webUrl: String? get() = url?.let { if (it.startsWith("http")) it else "https://polisen.se$it" }

    val parsedTime: OffsetDateTime?
        get() = datetime?.let {
            runCatching { OffsetDateTime.parse(it.trim(), API_FORMAT) }.getOrNull()
        }

    /**
     * Orten (kommunen) – sista ledet i namnet: "20 oktober 08.06, Trafikolycka, Skövde" -> "Skövde".
     * location.name är numera oftast bara länet, så kommunen finns bara här.
     * Länsövergripande poster ger länet ("..., Sammanfattning natt, Västra Götalands län").
     */
    val ort: String
        get() = name?.substringAfterLast(',', missingDelimiterValue = "")?.trim()
            ?.ifBlank { null } ?: locationName

    /**
     * När händelsen inträffade, enligt namnet ("20 oktober 08.06"). datetime är publiceringstiden,
     * som kan ligga flera timmar senare. Året saknas i namnet och tas från publiceringstiden
     * (händelse i december som publiceras i januari -> föregående år).
     */
    val eventTime: OffsetDateTime?
        get() {
            val m = NAME_TIME.find(name.orEmpty()) ?: return null
            val (day, monthName, hour, minute) = m.destructured
            val month = SV_MONTHS[monthName.lowercase(SV)] ?: return null
            val published = parsedTime ?: return null
            return runCatching {
                var local = LocalDateTime.of(published.year, month, day.toInt(), hour.toInt(), minute.toInt())
                if (local.isAfter(published.toLocalDateTime().plusDays(1))) local = local.minusYears(1)
                local.atZone(STOCKHOLM).toOffsetDateTime()
            }.getOrNull()
        }

    /** Sorteringsnyckel: händelsetid, annars publiceringstid. */
    val sortTime: OffsetDateTime? get() = eventTime ?: parsedTime

    /** T.ex. "20 okt. 08:06" – händelsetid i första hand, faller tillbaka på råsträngen. */
    val displayTime: String
        get() = sortTime?.format(DISPLAY_FORMAT) ?: datetime.orEmpty()

    /** Matchar orten (kommun) eller länet, skiftlägesokänsligt. */
    fun matchesOrt(query: String): Boolean {
        val q = query.trim().lowercase(SV)
        if (q.isEmpty()) return false
        return ort.lowercase(SV) == q || locationName.lowercase(SV) == q
    }

    companion object {
        private val SV = Locale("sv", "SE")
        private val STOCKHOLM: ZoneId = ZoneId.of("Europe/Stockholm")

        // Timmen saknar inledande nolla i API:t ("8:28:36"), därav H.
        private val API_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm:ss XXX")
        private val DISPLAY_FORMAT = DateTimeFormatter.ofPattern("d MMM HH:mm", SV)

        // "20 oktober 08.06, ..."
        private val NAME_TIME = Regex("""^\s*(\d{1,2})\s+(\p{L}+)\s+(\d{1,2})[.:](\d{2})""")

        private val SV_MONTHS = mapOf(
            "januari" to 1, "februari" to 2, "mars" to 3, "april" to 4, "maj" to 5, "juni" to 6,
            "juli" to 7, "augusti" to 8, "september" to 9, "oktober" to 10, "november" to 11, "december" to 12
        )
    }
}

data class Location(
    val name: String?,
    /** "lat,lng" för kommunen/länet – används senare för karta. */
    val gps: String?
) {
    val latLng: Pair<Double, Double>?
        get() = gps?.split(",")?.takeIf { it.size == 2 }?.let { (lat, lng) ->
            val a = lat.trim().toDoubleOrNull()
            val b = lng.trim().toDoubleOrNull()
            if (a != null && b != null) a to b else null
        }
}
