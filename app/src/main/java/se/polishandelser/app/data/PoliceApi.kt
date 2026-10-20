package se.polishandelser.app.data

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import se.polishandelser.app.BuildConfig
import se.polishandelser.app.data.model.PoliceEvent
import java.util.concurrent.TimeUnit

interface PoliceApi {
    @GET("api/events")
    suspend fun getEvents(): List<PoliceEvent>
}

object ApiModule {

    private const val BASE_URL = "https://polisen.se/"

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                // Identifiera appen mot Polisens API (god sed, underlättar om de behöver kontakta/strypa).
                val request = chain.request().newBuilder()
                    .header("User-Agent", "PolisHandelser-Android/${BuildConfig.VERSION_NAME}")
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    val api: PoliceApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PoliceApi::class.java)
    }
}
