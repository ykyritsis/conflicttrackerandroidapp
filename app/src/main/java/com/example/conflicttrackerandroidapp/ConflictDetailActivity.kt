package com.example.conflicttrackerandroidapp

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.conflicttrackerandroidapp.api.ConflictEvent
import com.example.conflicttrackerandroidapp.api.ConflictRepository
import kotlinx.coroutines.launch

class ConflictDetailActivity : AppCompatActivity() {
    private val repository = ConflictRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_conflict_detail)

        val conflict = intent.getParcelableExtra<ConflictEvent>("conflict")
        conflict?.let {
            setupUI(it)
            loadRegionalConflicts(it)
        }
    }

    private fun setupUI(conflict: ConflictEvent) {
        // Main conflict info
        findViewById<TextView>(R.id.conflictTitle).text = "${conflict.country} Conflict"
        findViewById<TextView>(R.id.conflictDescription).text = conflict.notes
        findViewById<TextView>(R.id.casualties).text = "Casualties: ${conflict.fatalities}"
        findViewById<TextView>(R.id.date).text = "Last Updated: ${conflict.event_date}"
        findViewById<TextView>(R.id.actors).text = "Main Actor: ${conflict.actor1}"

        // Region info
        findViewById<TextView>(R.id.regionTitle).text = "Regional Overview"
        findViewById<TextView>(R.id.regionName).text = conflict.region

        // Statistics
        findViewById<TextView>(R.id.statsTitle).text = "Region Statistics"
        updateStatistics(conflict)
    }

    private fun loadRegionalConflicts(mainConflict: ConflictEvent) {
        lifecycleScope.launch {
            try {
                val regionalConflicts = repository.getRegionalConflicts(mainConflict.country)
                    .filter { it.event_id_cnty != mainConflict.event_id_cnty } // Exclude current conflict

                val recyclerView = findViewById<RecyclerView>(R.id.regionalConflictsRecyclerView)
                recyclerView.layoutManager = LinearLayoutManager(this@ConflictDetailActivity)
                recyclerView.adapter = RegionalConflictsAdapter(regionalConflicts) { selectedConflict ->
                    // Update the UI with the selected conflict
                    setupUI(selectedConflict)
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun updateStatistics(conflict: ConflictEvent) {
        val statsText = """
            Region: ${conflict.region}
            Country: ${conflict.country}
            Location: ${conflict.location}
            Total Fatalities: ${conflict.fatalities}
            Last Updated: ${conflict.event_date}
        """.trimIndent()

        findViewById<TextView>(R.id.regionStats).text = statsText
    }
}