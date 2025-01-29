package com.example.conflicttrackerandroidapp

import com.example.conflicttrackerandroidapp.api.ConflictEvent
import java.util.Calendar

/**
 * Holds user-selected filter options.
 */
data class FilterOptions(
    val region: String? = null,
    val severityLevel: String? = null,
    val eventType: String? = null,
    val timeframe: String? = null
)

/**
 * Filters a list of conflicts according to the user-selected FilterOptions.
 */
fun filterConflicts(conflicts: List<ConflictEvent>, filters: FilterOptions): List<ConflictEvent> {
    var filteredList = conflicts

    // 1) Filter by region
    filters.region?.let { regionName ->
        filteredList = filteredList.filter { conflict ->
            isConflictInRegion(conflict, regionName)
        }
    }

    // 2) Filter by severity level
    filters.severityLevel?.let { severity ->
        filteredList = filteredList.filter { conflict ->
            when (severity) {
                "High (20+ casualties)"     -> conflict.fatalities >= 20
                "Medium (10-20 casualties)" -> conflict.fatalities in 10..19
                "Low (<10 casualties)"      -> conflict.fatalities < 10
                else -> true
            }
        }
    }

    // 3) Filter by event type
    filters.eventType?.let { eventType ->
        filteredList = filteredList.filter { conflict ->
            // Compare ignoring case
            conflict.event_type.equals(eventType, ignoreCase = true)
        }
    }

    // 4) Filter by timeframe
    filters.timeframe?.let { timeframe ->
        filteredList = filterByTimeframe(filteredList, timeframe)
    }

    return filteredList
}

/**
 * Example function to check if a conflict's country is in a given region.
 * Replace these sets with your actual mappings.
 */
fun isConflictInRegion(conflict: ConflictEvent, regionName: String): Boolean {
    val africaCountries = setOf("Nigeria", "Kenya", "Sudan", "Somalia", "Ethiopia")
    val middleEastCountries = setOf("Syria", "Iraq", "Yemen", "Saudi Arabia", "Iran")
    val asiaCountries = setOf("China", "India", "Pakistan", "Afghanistan", "Japan")
    val europeCountries = setOf("Ukraine", "France", "Germany", "United Kingdom", "Spain")
    val americasCountries = setOf("United States", "Mexico", "Brazil", "Colombia", "Canada")

    return when (regionName) {
        "Africa"      -> africaCountries.contains(conflict.country)
        "Middle East" -> middleEastCountries.contains(conflict.country)
        "Asia"        -> asiaCountries.contains(conflict.country)
        "Europe"      -> europeCountries.contains(conflict.country)
        "Americas"    -> americasCountries.contains(conflict.country)
        else          -> true // "All Regions"
    }
}

/**
 * Filters conflicts by timeframe. (e.g. "Last Week", "Last Month", "Last 3 Months")
 */
fun filterByTimeframe(conflicts: List<ConflictEvent>, timeframe: String): List<ConflictEvent> {
    // Convert timeframe to number of days
    val daysAgo = when (timeframe) {
        "Last Week"     -> 7
        "Last Month"    -> 30
        "Last 3 Months" -> 90
        else            -> Int.MAX_VALUE
    }

    val currentTime = System.currentTimeMillis()

    return conflicts.filter { conflict ->
        val conflictTime = parseConflictDateToMillis(conflict.event_date)
        // Keep if (currentTime - conflictTime) <= X days in milliseconds
        (currentTime - conflictTime) <= (daysAgo * 24L * 60 * 60 * 1000)
    }
}

/**
 * Parses an event_date string "YYYY-MM-DD" into milliseconds (long).
 * Adjust if your date format is different.
 */
fun parseConflictDateToMillis(dateString: String): Long {
    return try {
        val parts = dateString.split("-")
        val year = parts[0].toInt()
        val month = parts[1].toInt()
        val day = parts[2].toInt()

        val cal = Calendar.getInstance()
        cal.set(year, month - 1, day, 0, 0, 0)
        cal.timeInMillis
    } catch (e: Exception) {
        0L
    }
}