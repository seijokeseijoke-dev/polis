package se.polishandelser.app.data.model

import java.time.OffsetDateTime
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

    /** T.ex. "20 okt 08:28" – faller tillbaka på råsträngen om formatet ändras. */
    val displayTime: String
        get() = parsedTime?.format(DISPLAY_FORMAT) ?: datetime.orEmpty()

    companion object {
        // Timmen saknar inledande nolla i API:t ("8:28:36"), därav H.
        private val API_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm:ss XXX")
        private val DISPLAY_FORMAT = DateTimeFormatter.ofPattern("d MMM HH:mm", Locale("sv", "SE"))
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
