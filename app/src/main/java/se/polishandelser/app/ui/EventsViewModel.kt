package se.polishandelser.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import retrofit2.HttpException
import se.polishandelser.app.data.PoliceRepository
import se.polishandelser.app.data.model.PoliceEvent
import java.io.IOException
import java.util.Locale

private val SV = Locale("sv", "SE")

data class EventsUiState(
    val events: List<PoliceEvent> = emptyList(),
    val isLoading: Boolean = false,     // första laddningen (helskärmsspinner)
    val isRefreshing: Boolean = false,  // pull-to-refresh
    val error: String? = null
)

class EventsViewModel(
    private val repo: PoliceRepository = PoliceRepository()
) : ViewModel() {

    var state by mutableStateOf(EventsUiState(isLoading = true))
        private set

    init {
        load(pullToRefresh = false)
    }

    fun refresh() = load(pullToRefresh = true)

    fun retry() = load(pullToRefresh = false)

    fun clearError() {
        state = state.copy(error = null)
    }

    fun eventById(id: Int): PoliceEvent? = state.events.firstOrNull { it.id == id }

    /** Händelser för vald ort, nyast klockslag först. */
    fun eventsFor(ort: String): List<PoliceEvent> =
        state.events
            .filter { it.matchesOrt(ort) }
            .sortedWith(compareByDescending<PoliceEvent> { it.sortTime }.thenByDescending { it.id })

    /**
     * Orter som förekommer i den hämtade datan, med antal händelser – för förslag i sökfältet.
     * Inkluderar län så att man kan välja ett helt län.
     */
    fun ortSuggestions(query: String, limit: Int = 8): List<Pair<String, Int>> {
        val q = query.trim().lowercase(SV)
        val counts = HashMap<String, Int>()
        for (e in state.events) {
            val names = setOf(e.ort, e.locationName).filter { it.isNotBlank() }
            for (n in names) counts[n] = (counts[n] ?: 0) + 1
        }
        return counts.entries
            .filter { q.isEmpty() || it.key.lowercase(SV).startsWith(q) }
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .take(limit)
            .map { it.key to it.value }
    }

    private fun load(pullToRefresh: Boolean) {
        if (state.isRefreshing) return
        state = state.copy(
            isLoading = !pullToRefresh && state.events.isEmpty(),
            isRefreshing = pullToRefresh,
            error = null
        )
        viewModelScope.launch {
            state = try {
                state.copy(events = repo.getEvents(), isLoading = false, isRefreshing = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                state.copy(isLoading = false, isRefreshing = false, error = e.toUserMessage())
            }
        }
    }

    private fun Exception.toUserMessage(): String = when (this) {
        is IOException -> "Ingen anslutning. Kontrollera internet och försök igen."
        is HttpException -> "Polisens server svarade med fel (${code()}). Försök igen om en stund."
        else -> "Något gick fel när händelserna hämtades."
    }
}
