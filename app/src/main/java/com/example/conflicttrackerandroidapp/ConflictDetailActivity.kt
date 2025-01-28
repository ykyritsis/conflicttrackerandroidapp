package com.example.conflicttrackerandroidapp

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.os.Parcelable
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.conflicttrackerandroidapp.api.ConflictEvent
import com.example.conflicttrackerandroidapp.api.ConflictRepository
import com.example.conflicttrackerandroidapp.api.WorldBankRepository
import com.example.conflicttrackerandroidapp.api.CountryStats
import kotlinx.coroutines.launch

class ConflictDetailActivity : AppCompatActivity() {
    private val conflictRepository = ConflictRepository()
    private val worldBankRepository = WorldBankRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_conflict_detail)

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

        // Set placeholder text for statistics card
        findViewById<TextView>(R.id.countryPopulation).text = "Population: Loading..."
        findViewById<TextView>(R.id.populationComparison).text = ""
        findViewById<TextView>(R.id.countryGDP).text = "GDP: Loading..."
        findViewById<TextView>(R.id.gdpComparison).text = ""
        findViewById<TextView>(R.id.militaryExpenditure).text = "Military Expenditure: Loading..."
        findViewById<TextView>(R.id.militaryComparison).text = ""
    }

    private fun loadData(conflict: ConflictEvent) {
        findViewById<View>(R.id.loadingStats).visibility = View.VISIBLE
        findViewById<View>(R.id.loadingRegional).visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                // Load country statistics
                val countryStats = worldBankRepository.getCountryStats(conflict.country)
                updateStatistics(countryStats)

                // Load regional conflicts
                val regionalConflicts = conflictRepository.getRegionalConflicts(conflict.country)
                    .filter { it.event_id_cnty != conflict.event_id_cnty }
                updateRegionalConflicts(regionalConflicts)

                findViewById<View>(R.id.loadingStats).visibility = View.GONE
                findViewById<View>(R.id.loadingRegional).visibility = View.GONE
            } catch (e: Exception) {
                findViewById<View>(R.id.loadingStats).visibility = View.GONE
                findViewById<View>(R.id.loadingRegional).visibility = View.GONE
                Toast.makeText(
                    this@ConflictDetailActivity,
                    "Error loading data: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
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

    private fun updateStatistics(stats: CountryStats?) {
        if (stats == null) {
            Log.d("ConflictDetail", "No stats available")
            findViewById<TextView>(R.id.countryPopulation).text = "Population: Not available"
            findViewById<TextView>(R.id.populationComparison).text = ""
            findViewById<TextView>(R.id.countryGDP).text = "GDP: Not available"
            findViewById<TextView>(R.id.gdpComparison).text = ""
            findViewById<TextView>(R.id.militaryExpenditure).text = "Military Expenditure: Not available"
            findViewById<TextView>(R.id.militaryComparison).text = ""
            return
        }

        // Population
        findViewById<TextView>(R.id.countryPopulation).text = when (stats.population) {
            null -> "Population: Not available"
            else -> "Population: ${formatNumber(stats.population)}"
        }
        findViewById<TextView>(R.id.populationComparison).text = stats.getPopulationComparison()

        // GDP
        findViewById<TextView>(R.id.countryGDP).text = when (stats.gdp) {
            null -> "GDP: Not available"
            else -> "GDP: $${formatNumber(stats.gdp)}"
        }
        findViewById<TextView>(R.id.gdpComparison).text = stats.getGDPComparison()

        // Military Expenditure
        findViewById<TextView>(R.id.militaryExpenditure).text = when (stats.militaryExpenditure) {
            null -> "Military Expenditure: Not available"
            else -> "Military Expenditure: $${formatNumber(stats.militaryExpenditure)}"
        }
        findViewById<TextView>(R.id.militaryComparison).text = stats.getMilitaryComparison()
    }

    private fun formatNumber(value: Double): String {
        return when {
            value >= 1_000_000_000 -> String.format("%.1fB", value / 1_000_000_000)
            value >= 1_000_000 -> String.format("%.1fM", value / 1_000_000)
            value >= 1_000 -> String.format("%.1fK", value / 1_000)
            else -> String.format("%.1f", value)
        }
    }
}