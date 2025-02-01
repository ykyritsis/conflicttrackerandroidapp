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
    val event_date: String,
    val timestamp: String,
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

// filter options for conflict queries
data class FilterOptions(
    var region: String? = null,
    var severityLevel: String? = null,
    var eventType: String? = null,
    var timeframe: String? = null
)

// country statistics with comparison helpers
data class CountryStats(
    val population: Double?,
    val gdp: Double?,
    val militaryExpenditure: Double?,
    val note: String = "",
    val globalAvgPopulation: Double = 67000000.0,
    val globalAvgGDP: Double = 1300000000000.0,
    val globalAvgMilitary: Double = 25000000000.0
) {
    // compares local population to the global average
    fun getPopulationComparison(): String {
        return population?.let {
            val percentage = ((it - globalAvgPopulation) / globalAvgPopulation * 100)
            formatComparison(percentage)
        } ?: "no population data available"
    }

    // compares local gdp to the global average
    fun getGDPComparison(): String {
        return gdp?.let {
            val percentage = ((it - globalAvgGDP) / globalAvgGDP * 100)
            formatComparison(percentage)
        } ?: "no gdp data available"
    }

    // compares local military expenditure to the global average
    fun getMilitaryComparison(): String {
        return militaryExpenditure?.let {
            val percentage = ((it - globalAvgMilitary) / globalAvgMilitary * 100)
            formatComparison(percentage)
        } ?: "no military expenditure data available"
    }

    // formats the percentage comparison string
    private fun formatComparison(percentage: Double): String {
        return when {
            percentage > 0 -> "${String.format("%.1f", percentage)}% higher than global average"
            percentage < 0 -> "${String.format("%.1f", -percentage)}% lower than global average"
            else -> "equal to global average"
        }
    }
}