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

// repository to fetch conflict events from the acled api
class ConflictRepository {

    // configure http client with custom timeouts
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // retrofit setup with gson converter and custom http client
    private val acledApi = Retrofit.Builder()
        .baseUrl("https://api.acleddata.com/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(AcledApiService::class.java)

    companion object {
        private const val TAG = "ConflictRepository"
    }

    // helper to build a date range string (format: yyyy-mm-dd|yyyy-mm-dd) from [monthsAgo] to now
    private fun buildDateRange(monthsAgo: Long): String {
        val endDate = LocalDate.now()
        val startDate = endDate.minusMonths(monthsAgo)
        return "${startDate.format(DateTimeFormatter.ISO_DATE)}|${endDate.format(DateTimeFormatter.ISO_DATE)}"
    }

    // fetches the most severe recent conflict (fatalities > 1) in the past 3 months
    suspend fun getMostSevereRecentConflict(): ConflictEvent? = withContext(Dispatchers.IO) {
        val dateRange = buildDateRange(3)

        try {
            val response = acledApi.getConflicts(
                dateRange = dateRange,
                limit = 300
            )

            response.data
                .filter { it.fatalities > 1 }
                .maxByOrNull { it.fatalities }
        } catch (e: Exception) {
            Log.e(TAG, "error fetching severe conflict: ${e.message}")
            null
        }
    }

    // fetches top ongoing conflicts (fatalities > 1) in the past 3 months, sorted descending by fatalities
    suspend fun getTopOngoingConflicts(): List<ConflictEvent> = withContext(Dispatchers.IO) {
        val dateRange = buildDateRange(3)

        try {
            val response = acledApi.getConflicts(
                dateRange = dateRange,
                limit = 500
            )

            response.data
                .filter { it.fatalities > 1 }
                .sortedByDescending { it.fatalities }
                .take(150)
        } catch (e: Exception) {
            Log.e(TAG, "error fetching conflicts: ${e.message}")
            emptyList()
        }
    }

    // fetches regional conflicts (fatalities > 1) for events matching the given country's region over the past 12 months
    suspend fun getRegionalConflicts(country: String): List<ConflictEvent> = withContext(Dispatchers.IO) {
        val dateRange = buildDateRange(12)

        try {
            val response = acledApi.getConflicts(
                dateRange = dateRange,
                limit = 400
            )

            // determine the region from an event that matches the provided country
            val region = response.data.find { it.country == country }?.region
            if (region == null) return@withContext emptyList()

            response.data
                .filter { it.region == region && it.fatalities > 1 }
                .sortedByDescending { it.fatalities }
                .take(60)
        } catch (e: Exception) {
            Log.e(TAG, "error fetching regional conflicts: ${e.message}")
            emptyList()
        }
    }
}