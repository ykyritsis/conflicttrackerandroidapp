package com.example.conflicttrackerandroidapp

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
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

        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish()
        }

        val conflict = intent.getParcelableExtra<ConflictEvent>("conflict")
        conflict?.let {
            setupUI(it)
            loadData(it)
        }
    }

    private fun setupUI(conflict: ConflictEvent) {
        findViewById<TextView>(R.id.conflictTitle).text = "${conflict.country} Conflict"
        findViewById<TextView>(R.id.conflictDescription).text = conflict.notes
        findViewById<TextView>(R.id.casualties).text = "Total Casualties: ${conflict.fatalities}"
        findViewById<TextView>(R.id.actors).text = "Main Actor: ${conflict.actor1}"
        findViewById<TextView>(R.id.date).text = "Last Updated: ${conflict.event_date}"

        // set placeholder text for statistics card
        findViewById<TextView>(R.id.countryPopulation).text = "Population data unavailable"
        findViewById<TextView>(R.id.countryGDP).text = "GDP data unavailable"
        findViewById<TextView>(R.id.countryGDPPerCapita).text = "GDP per capita unavailable"
        findViewById<TextView>(R.id.countryLifeExpectancy).text = "Life expectancy data unavailable"
    }

    private fun loadData(conflict: ConflictEvent) {
        findViewById<View>(R.id.loadingStats).visibility = View.GONE
        findViewById<View>(R.id.loadingRegional).visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                // Load only regional conflicts
                val regionalConflicts = repository.getRegionalConflicts(conflict.country)
                    .filter { it.event_id_cnty != conflict.event_id_cnty }
                updateRegionalConflicts(regionalConflicts)

                findViewById<View>(R.id.loadingRegional).visibility = View.GONE
            } catch (e: Exception) {
                findViewById<View>(R.id.loadingRegional).visibility = View.GONE
                Toast.makeText(this@ConflictDetailActivity,
                    "Error loading data: ${e.message}",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateRegionalConflicts(conflicts: List<ConflictEvent>) {
        val recyclerView = findViewById<RecyclerView>(R.id.regionalConflictsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = ConflictAdapter(conflicts) { selectedConflict ->
            setupUI(selectedConflict)
            loadData(selectedConflict)
        }
    }
}