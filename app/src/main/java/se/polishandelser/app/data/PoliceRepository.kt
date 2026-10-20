package se.polishandelser.app.data

import se.polishandelser.app.data.model.PoliceEvent

class PoliceRepository(
    private val api: PoliceApi = ApiModule.api
) {
    suspend fun getEvents(): List<PoliceEvent> = api.getEvents()
}
