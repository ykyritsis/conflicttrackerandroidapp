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
    }

    suspend fun getMostSevereRecentConflict(): ConflictEvent? = withContext(Dispatchers.IO) {
        val endDate = LocalDate.now()
        val startDate = endDate.minusMonths(3) // Increased from 1 to 3 months
        val dateRange = "${startDate.format(DateTimeFormatter.ISO_DATE)}|${endDate.format(DateTimeFormatter.ISO_DATE)}"

        try {
            val response = acledApi.getConflicts(
                dateRange = dateRange,
                limit = 300 // Increased from 100
            )

            response.data
                .filter { it.fatalities > 1 }
                .maxByOrNull { it.fatalities }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching severe conflict: ${e.message}")
            null
        }
    }

    suspend fun getTopOngoingConflicts(): List<ConflictEvent> = withContext(Dispatchers.IO) {
        val endDate = LocalDate.now()
        val startDate = endDate.minusMonths(3) // Increased from 1 to 3 months
        val dateRange = "${startDate.format(DateTimeFormatter.ISO_DATE)}|${endDate.format(DateTimeFormatter.ISO_DATE)}"

        try {
            val response = acledApi.getConflicts(
                dateRange = dateRange,
                limit = 500 // Increased from 200
            )

            response.data
                .filter { it.fatalities > 1 }
                .sortedByDescending { it.fatalities }
                .take(150) // Increased from 50 to 150
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching conflicts: ${e.message}")
            emptyList()
        }
    }

    suspend fun getRegionalConflicts(country: String): List<ConflictEvent> = withContext(Dispatchers.IO) {
        val endDate = LocalDate.now()
        val startDate = endDate.minusMonths(3) // Increased from 1 to 3 months
        val dateRange = "${startDate.format(DateTimeFormatter.ISO_DATE)}|${endDate.format(DateTimeFormatter.ISO_DATE)}"

        try {
            val response = acledApi.getConflicts(
                dateRange = dateRange,
                limit = 100 // Increased from 200
            )

            val region = response.data.find { it.country == country }?.region

            if (region == null) {
                return@withContext emptyList()
            }

            response.data
                .filter { it.region == region && it.fatalities > 1 }
                .sortedByDescending { it.fatalities }
                .take(60) // Increased from 20 to 60
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching regional conflicts: ${e.message}")
            emptyList()
        }
    }
}