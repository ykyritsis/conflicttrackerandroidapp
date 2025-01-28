package com.example.conflicttrackerandroidapp.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class ConflictRepository {
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val acledApi = Retrofit.Builder()
        .baseUrl("https://api.acleddata.com/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(AcledApiService::class.java)

    companion object {
        private const val TAG = "ConflictRepository"
        private const val MIN_FATALITIES = 1
    }

    suspend fun getMostSevereRecentConflict(): ConflictEvent? = withContext(Dispatchers.IO) {
        val endDate = LocalDate.now()
        val startDate = endDate.minusYears(10)
        val dateRange = "${startDate.format(DateTimeFormatter.ISO_DATE)}|${endDate.format(DateTimeFormatter.ISO_DATE)}"

        try {
            Log.d(TAG, "Fetching most severe conflict from $startDate to $endDate")
            val response = acledApi.getConflicts(
                dateRange = dateRange,
                limit = 1000
            )

            response.data
                .filter { it.fatalities > MIN_FATALITIES }
                .maxByOrNull { it.fatalities }
                ?.also { conflict ->
                    Log.d(TAG, """
                        Most severe conflict found:
                        Country: ${conflict.country}
                        Location: ${conflict.location}
                        Fatalities: ${conflict.fatalities}
                        Date: ${conflict.event_date}
                        Region: ${conflict.region}
                    """.trimIndent())
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching severe conflict: ${e.message}")
            null
        }
    }

    suspend fun getTopOngoingConflicts(): List<ConflictEvent> = withContext(Dispatchers.IO) {
        val endDate = LocalDate.now()
        val startDate = endDate.minusYears(2)
        val dateRange = "${startDate.format(DateTimeFormatter.ISO_DATE)}|${endDate.format(DateTimeFormatter.ISO_DATE)}"

        try {
            Log.d(TAG, "Fetching top conflicts from $startDate to $endDate")
            val response = acledApi.getConflicts(
                dateRange = dateRange,
                limit = 1000
            )

            response.data
                .filter { it.fatalities > MIN_FATALITIES }
                .sortedByDescending { it.fatalities }
                .distinctBy { "${it.country}${it.actor1}" }
                .take(5)
                .also { conflicts ->
                    Log.d(TAG, "Found ${conflicts.size} top conflicts:")
                    conflicts.forEach { conflict ->
                        Log.d(TAG, "Country: ${conflict.country}, Fatalities: ${conflict.fatalities}")
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching watchlist conflicts: ${e.message}")
            emptyList()
        }
    }

    suspend fun getRegionalConflicts(country: String): List<ConflictEvent> = withContext(Dispatchers.IO) {
        val endDate = LocalDate.now()
        val startDate = endDate.minusYears(1)
        val dateRange = "${startDate.format(DateTimeFormatter.ISO_DATE)}|${endDate.format(DateTimeFormatter.ISO_DATE)}"

        try {
            Log.d(TAG, "Fetching regional conflicts for $country from $startDate to $endDate")
            val response = acledApi.getConflicts(
                dateRange = dateRange,
                limit = 1000
            )

            val region = response.data
                .find { it.country == country }
                ?.region
                ?.also { Log.d(TAG, "Found region: $it for country: $country") }

            if (region == null) {
                Log.e(TAG, "Could not determine region for country: $country")
                return@withContext emptyList()
            }

            response.data
                .filter { it.region == region && it.fatalities > MIN_FATALITIES }
                .sortedByDescending { it.fatalities }
                .take(10)
                .also { conflicts ->
                    Log.d(TAG, "Found ${conflicts.size} conflicts in region $region")
                    conflicts.forEach { conflict ->
                        Log.d(TAG, "Regional conflict: ${conflict.country}, Fatalities: ${conflict.fatalities}")
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching regional conflicts: ${e.message}")
            emptyList()
        }
    }
}