package com.example.conflicttrackerandroidapp.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ConflictRepository {
    private val api = Retrofit.Builder()
        .baseUrl("https://api.acleddata.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(AcledApiService::class.java)

    suspend fun getMostSevereRecentConflict(): ConflictEvent? {
        // Get conflicts from last 5 years
        val endDate = LocalDate.now()
        val startDate = endDate.minusYears(5)
        val dateRange = "${startDate.format(DateTimeFormatter.ISO_DATE)}|${endDate.format(DateTimeFormatter.ISO_DATE)}"

        return try {
            val response = api.getConflicts(
                dateRange = dateRange,
                limit = 1000  // Increased to get more historical data
            )
            // Return the conflict with highest fatalities
            response.data.maxByOrNull { it.fatalities }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getTopOngoingConflicts(): List<ConflictEvent> {
        // Get conflicts from last 5 years
        val endDate = LocalDate.now()
        val startDate = endDate.minusYears(5)
        val dateRange = "${startDate.format(DateTimeFormatter.ISO_DATE)}|${endDate.format(DateTimeFormatter.ISO_DATE)}"

        return try {
            val response = api.getConflicts(
                dateRange = dateRange,
                limit = 1000  // Increased to get more historical data
            )
            // Return top 5 by fatalities
            response.data
                .sortedByDescending { it.fatalities }
                .take(5)
        } catch (e: Exception) {
            emptyList()
        }
    }
}