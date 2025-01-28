package com.example.conflicttrackerandroidapp

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.conflicttrackerandroidapp.api.ConflictEvent

class EscalatingConflictActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_escalating_conflict)

        // Set up back button
        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish()
        }

        // Get conflict data passed from MainActivity
        val conflict = intent.getParcelableExtra<ConflictEvent>("conflict")
        conflict?.let {
            updateMainCard(it)
            updateRegionOverviewCard(it)
            updateStatisticsCard(it)
        }
    }

    private fun updateMainCard(conflict: ConflictEvent) {
        findViewById<TextView>(R.id.conflictTitle).text = "${conflict.country} Conflict"
        findViewById<TextView>(R.id.conflictDescription).text = conflict.notes
        findViewById<TextView>(R.id.casualties).text = "Total Casualties: ${conflict.fatalities}"
        findViewById<TextView>(R.id.actors).text = "Main Actor: ${conflict.actor1}"
        findViewById<TextView>(R.id.date).text = "Last Updated: ${conflict.event_date}"
    }

    private fun updateRegionOverviewCard(conflict: ConflictEvent) {
        findViewById<TextView>(R.id.regionName).text = "Region: ${conflict.region}"
    }

    private fun updateStatisticsCard(conflict: ConflictEvent) {
        val statsText = """
            Region: ${conflict.region}
            Country: ${conflict.country}
            Location: ${conflict.location}
            Event Type: ${conflict.event_type}
            Source: ${conflict.source}
        """.trimIndent()

        findViewById<TextView>(R.id.regionStats).text = statsText
    }
}