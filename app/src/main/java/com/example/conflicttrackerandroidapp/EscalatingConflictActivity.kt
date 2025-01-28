package com.example.conflicttrackerandroidapp

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.conflicttrackerandroidapp.api.ConflictEvent
import com.google.android.material.card.MaterialCardView

class EscalatingConflictActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_escalating_conflict)

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
        findViewById<TextView>(R.id.casualties).text = "Casualties: ${conflict.fatalities}"
        findViewById<TextView>(R.id.date).text = "Date: ${conflict.event_date}"
        findViewById<TextView>(R.id.actors).text = "Main Actor: ${conflict.actor1}"
    }

    private fun updateRegionOverviewCard(conflict: ConflictEvent) {
        // Will implement map view later
        findViewById<TextView>(R.id.regionName).text = conflict.region
    }

    private fun updateStatisticsCard(conflict: ConflictEvent) {
        // Will add more statistics from API
        findViewById<TextView>(R.id.regionStats).text = "Region: ${conflict.region}\nLocation: ${conflict.location}"
    }
}