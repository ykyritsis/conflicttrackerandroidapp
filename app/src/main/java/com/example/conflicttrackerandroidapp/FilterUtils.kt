package com.example.conflicttrackerandroidapp

import com.example.conflicttrackerandroidapp.api.ConflictEvent
import java.util.Calendar

// holds user-selected filter options
data class FilterOptions(
    val region: String? = null,
    val severityLevel: String? = null,
    val eventType: String? = null,
    val timeframe: String? = null
)

// filters a list of conflicts according to provided filter options
fun filterConflicts(conflicts: List<ConflictEvent>, filters: FilterOptions): List<ConflictEvent> {
    var filteredList = conflicts

    // filter by region if specified
    filters.region?.let { regionName ->
        filteredList = filteredList.filter { conflict ->
            isConflictInRegion(conflict, regionName)
        }
    }

    // filter by severity level if specified
    filters.severityLevel?.let { severity ->
        filteredList = filteredList.filter { conflict ->
            when (severity) {
                "High (20+ casualties)"     -> conflict.fatalities >= 20
                "Medium (10-20 casualties)" -> conflict.fatalities in 10..19
                "Low (<10 casualties)"      -> conflict.fatalities < 10
                else                        -> true
            }
        }
    }

    // filter by event type (case-insensitive) if specified
    filters.eventType?.let { eventType ->
        filteredList = filteredList.filter { conflict ->
            conflict.event_type.equals(eventType, ignoreCase = true)
        }
    }

    // filter by timeframe if specified
    filters.timeframe?.let { timeframe ->
        filteredList = filterByTimeframe(filteredList, timeframe)
    }

    return filteredList
}

// checks if a conflict's country belongs to the specified region
fun isConflictInRegion(conflict: ConflictEvent, regionName: String): Boolean {
    val africa = setOf("Nigeria", "Kenya", "Sudan", "Somalia", "Ethiopia")
    val middleEast = setOf("Syria", "Iraq", "Yemen", "Saudi Arabia", "Iran")
    val asia = setOf("China", "India", "Pakistan", "Afghanistan", "Japan")
    val europe = setOf("Ukraine", "France", "Germany", "United Kingdom", "Spain")
    val americas = setOf("United States", "Mexico", "Brazil", "Colombia", "Canada")

    return when (regionName) {
        "Africa"      -> conflict.country in africa
        "Middle East" -> conflict.country in middleEast
        "Asia"        -> conflict.country in asia
        "Europe"      -> conflict.country in europe
        "Americas"    -> conflict.country in americas
        else          -> true  // default to include if region is unknown or not specified
    }
}

// filters conflicts based on a timeframe (e.g. "Last Week", "Last Month", "Last 3 Months")
fun filterByTimeframe(conflicts: List<ConflictEvent>, timeframe: String): List<ConflictEvent> {
    // convert timeframe to a number of days
    val daysAgo = when (timeframe) {
        "Last Week"     -> 7
        "Last Month"    -> 30
        "Last 3 Months" -> 90
        else            -> Int.MAX_VALUE
    }

    val currentTime = System.currentTimeMillis()
    val millisThreshold = daysAgo * 24L * 60 * 60 * 1000

    return conflicts.filter { conflict ->
        val conflictTime = parseConflictDateToMillis(conflict.event_date)
        (currentTime - conflictTime) <= millisThreshold
    }
}

// parses a date string in the format "yyyy-mm-dd" into milliseconds since epoch
fun parseConflictDateToMillis(dateString: String): Long {
    return try {
        val parts = dateString.split("-")
        val year = parts[0].toInt()
        val month = parts[1].toInt()
        val day = parts[2].toInt()

        Calendar.getInstance().apply {
            set(year, month - 1, day, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    } catch (e: Exception) {
        0L
    }
}