package com.example.conflicttrackerandroidapp.api

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

data class AcledResponse(
    val status: Int,
    val data: List<ConflictEvent>
)

@Parcelize
data class ConflictEvent(
    val event_id_cnty: String,
    val event_date: String,      // Original event date
    val timestamp: String,       // Last updated timestamp
    val year: Int,
    val event_type: String,
    val actor1: String,
    val country: String,
    val region: String,
    val location: String,
    val latitude: Double,
    val longitude: Double,
    val source: String,
    val notes: String,
    val fatalities: Int
) : Parcelable

data class FilterOptions(
    var region: String? = null,
    var severityLevel: String? = null,
    var eventType: String? = null,
    var timeframe: String? = null
)

data class CountryStats(
    val population: Double?,
    val gdp: Double?,
    val militaryExpenditure: Double?,
    val note: String = "",
    val globalAvgPopulation: Double = 67000000.0,
    val globalAvgGDP: Double = 1300000000000.0,
    val globalAvgMilitary: Double = 25000000000.0
)

{
    fun getPopulationComparison(): String {
        return population?.let {
            val percentage = ((it - globalAvgPopulation) / globalAvgPopulation * 100)
            formatComparison(percentage)
        } ?: "No population data available"
    }

    fun getGDPComparison(): String {
        return gdp?.let {
            val percentage = ((it - globalAvgGDP) / globalAvgGDP * 100)
            formatComparison(percentage)
        } ?: "No GDP data available"
    }

    fun getMilitaryComparison(): String {
        return militaryExpenditure?.let {
            val percentage = ((it - globalAvgMilitary) / globalAvgMilitary * 100)
            formatComparison(percentage)
        } ?: "No military expenditure data available"
    }

    private fun formatComparison(percentage: Double): String {
        return when {
            percentage > 0 -> "${String.format("%.1f", percentage)}% higher than global average"
            percentage < 0 -> "${String.format("%.1f", -percentage)}% lower than global average"
            else -> "Equal to global average"
        }
    }
}