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
        val endDate = LocalDate.now()
        val startDate = endDate.minusYears(10)
        val dateRange = "${startDate.format(DateTimeFormatter.ISO_DATE)}|${endDate.format(DateTimeFormatter.ISO_DATE)}"

        return try {
            val response = api.getConflicts(
                dateRange = dateRange,
                limit = 1000
            )
            // Get most severe conflict by fatalities
            response.data
                .sortedByDescending { it.fatalities }
                .firstOrNull()
        } catch (e: Exception) {
            println("Error fetching severe conflict: ${e.message}")
            null
        }
    }

    suspend fun getTopOngoingConflicts(): List<ConflictEvent> {
        val endDate = LocalDate.now()
        val startDate = endDate.minusYears(10)
        val dateRange = "${startDate.format(DateTimeFormatter.ISO_DATE)}|${endDate.format(DateTimeFormatter.ISO_DATE)}"

        return try {
            val response = api.getConflicts(
                dateRange = dateRange,
                limit = 1000
            )
            // Get top conflicts by fatalities
            response.data
                .sortedByDescending { it.fatalities }
                .distinctBy { it.country }
                .take(4)
        } catch (e: Exception) {
            println("Error fetching watchlist conflicts: ${e.message}")
            emptyList()
        }
    }

    suspend fun getRegionalConflicts(country: String): List<ConflictEvent> {
        val endDate = LocalDate.now()
        val startDate = endDate.minusYears(10)
        val dateRange = "${startDate.format(DateTimeFormatter.ISO_DATE)}|${endDate.format(DateTimeFormatter.ISO_DATE)}"

        return try {
            val response = api.getConflicts(
                dateRange = dateRange,
                limit = 500
            )
            response.data
                .filter { it.region == getRegionForCountry(country) }
                .sortedByDescending { it.fatalities }
                .take(5)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun getRegionForCountry(country: String): String {
        return when (country) {
            "Israel", "Palestine", "Syria", "Yemen" -> "Middle East"
            "Ukraine", "Russia" -> "Europe"
            else -> ""
        }
    }
}