package com.example.conflicttrackerandroidapp

import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.conflicttrackerandroidapp.api.ConflictEvent
import com.example.conflicttrackerandroidapp.api.ConflictRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class EscalatingConflictActivity : AppCompatActivity() {
    private var isDescriptionExpanded = false
    private val repository = ConflictRepository()
    private val TAG = "EscalatingConflict"
    private var currentJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_escalating_conflict)

        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish()
        }

        val conflict = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("conflict", ConflictEvent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("conflict")
        }

        conflict?.let {
            updateMainCard(it)
            loadRegionStats(it)
        }
    }

    override fun onDestroy() {
        currentJob?.cancel()
        super.onDestroy()
    }

    private fun updateMainCard(conflict: ConflictEvent) {
        findViewById<TextView>(R.id.conflictTitle).text = "${conflict.country} Conflict"

        val description = findViewById<TextView>(R.id.conflictDescription)
        description.text = conflict.notes

        val readMoreBtn = findViewById<TextView>(R.id.readMore)
        readMoreBtn.setOnClickListener {
            isDescriptionExpanded = !isDescriptionExpanded
            if (isDescriptionExpanded) {
                description.maxLines = Integer.MAX_VALUE
                readMoreBtn.text = "Read less"
            } else {
                description.maxLines = 3
                readMoreBtn.text = "Read more"
            }
        }

        findViewById<TextView>(R.id.casualties).text = "Total Casualties: ${conflict.fatalities}"
        findViewById<TextView>(R.id.actors).text = "Main Actor: ${conflict.actor1}"
        findViewById<TextView>(R.id.date).text = "Last Updated: ${conflict.event_date}"
    }

    private fun loadRegionStats(conflict: ConflictEvent) {
        // Cancel any existing job
        currentJob?.cancel()

        // Start new job
        currentJob = lifecycleScope.launch {
            try {
                Log.d(TAG, "Loading region stats for ${conflict.country}")

                // Show loading state
                findViewById<TextView>(R.id.regionName).text = "Loading regional data..."
                findViewById<TextView>(R.id.conflictFrequency).text = ""
                findViewById<TextView>(R.id.casualtyComparison).text = ""
                findViewById<TextView>(R.id.severityLevel).text = ""

                val regionalConflicts = repository.getRegionalConflicts(conflict.country)
                Log.d(TAG, "Found ${regionalConflicts.size} regional conflicts")

                if (regionalConflicts.isEmpty()) {
                    Log.d(TAG, "No regional conflicts found")
                    findViewById<TextView>(R.id.regionName).text = "Region: ${conflict.region}"
                    findViewById<TextView>(R.id.conflictFrequency).text = "No regional data available"
                    return@launch
                }

                updateRegionOverview(conflict.region, regionalConflicts)
            } catch (e: CancellationException) {
                Log.d(TAG, "Region stats loading was cancelled")
                throw e // Rethrow cancellation exceptions
            } catch (e: Exception) {
                Log.e(TAG, "Error loading regional data: ${e.message}", e)
                Toast.makeText(this@EscalatingConflictActivity,
                    "Error loading regional data. Please try again.",
                    Toast.LENGTH_SHORT).show()

                // Show error state
                findViewById<TextView>(R.id.regionName).text = "Region: ${conflict.region}"
                findViewById<TextView>(R.id.conflictFrequency).text = "Error loading regional data"
            }
        }
    }

    private fun updateRegionOverview(region: String, conflicts: List<ConflictEvent>) {
        Log.d(TAG, "Updating region overview for $region with ${conflicts.size} conflicts")

        findViewById<TextView>(R.id.regionName).text = "Region: $region"

        val totalConflicts = conflicts.size
        val totalCasualties = conflicts.sumOf { it.fatalities }
        val averageCasualties = if (totalConflicts > 0)
            totalCasualties.toDouble() / totalConflicts else 0.0

        Log.d(TAG, """
            Region Statistics:
            Total Conflicts: $totalConflicts
            Total Casualties: $totalCasualties
            Average Casualties: $averageCasualties
        """.trimIndent())

        findViewById<TextView>(R.id.conflictFrequency).text =
            "$totalConflicts active conflicts in region"

        findViewById<TextView>(R.id.casualtyComparison).text =
            "Average of ${String.format("%.1f", averageCasualties)} casualties per conflict"

        val severityLevel = when {
            averageCasualties > 100 -> "Critical"
            averageCasualties > 50 -> "Severe"
            averageCasualties > 20 -> "Elevated"
            else -> "Moderate"
        }
        findViewById<TextView>(R.id.severityLevel).text = "Regional Severity Level: $severityLevel"
    }
}