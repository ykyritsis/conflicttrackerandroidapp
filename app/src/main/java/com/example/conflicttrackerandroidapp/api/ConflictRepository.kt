package com.example.conflicttrackerandroidapp.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ConflictRepository {
    private val acledApi = Retrofit.Builder()
        .baseUrl("https://api.acleddata.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(AcledApiService::class.java)

    suspend fun getMostSevereRecentConflict(): ConflictEvent? {
        val endDate = LocalDate.now()
        val startDate = endDate.minusMonths(6) // Reduced from 10 years to 6 months
        val dateRange = "${startDate.format(DateTimeFormatter.ISO_DATE)}|${endDate.format(DateTimeFormatter.ISO_DATE)}"

        return try {
            val response = acledApi.getConflicts(
                dateRange = dateRange,
                limit = 1000
            )
            // Reduced fatality threshold and removed country restrictions
            response.data
                .filter { conflict -> conflict.fatalities > 50 }
                .maxByOrNull { it.fatalities }
        } catch (e: Exception) {
            println("Error fetching severe conflict: ${e.message}")
            null
        }
    }

    suspend fun getTopOngoingConflicts(): List<ConflictEvent> {
        val endDate = LocalDate.now()
        val startDate = endDate.minusMonths(6)
        val dateRange = "${startDate.format(DateTimeFormatter.ISO_DATE)}|${endDate.format(DateTimeFormatter.ISO_DATE)}"

        return try {
            val response = acledApi.getConflicts(
                dateRange = dateRange,
                limit = 1000
            )
            response.data
                .filter { it.fatalities > 25 } // Reduced threshold
                .sortedByDescending { it.fatalities }
                .distinctBy { it.country }
                .take(10) // Increased number of results
        } catch (e: Exception) {
            println("Error fetching watchlist conflicts: ${e.message}")
            emptyList()
        }
    }

    suspend fun getRegionalConflicts(country: String): List<ConflictEvent> {
        val endDate = LocalDate.now()
        val startDate = endDate.minusMonths(3) // Reduced timeframe
        val dateRange = "${startDate.format(DateTimeFormatter.ISO_DATE)}|${endDate.format(DateTimeFormatter.ISO_DATE)}"

        return try {
            val response = acledApi.getConflicts(
                dateRange = dateRange,
                limit = 500
            )
            response.data
                .filter { it.region == getRegionForCountry(country) }
                .sortedByDescending { it.fatalities }
                .take(10) // Increased number of results
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun getRegionForCountry(country: String): String {
        return when (country) {
            "Israel", "Palestine", "Syria", "Yemen", "Iraq", "Lebanon" -> "Middle East"
            "Ukraine", "Russia", "Belarus" -> "Europe"
            else -> "Unknown"
        }
    }
}