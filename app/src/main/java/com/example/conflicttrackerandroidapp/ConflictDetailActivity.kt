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

// activity to display detailed conflict information and related regional data
class ConflictDetailActivity : AppCompatActivity() {
    private val conflictRepository = ConflictRepository()
    private val worldBankRepository = WorldBankRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_conflict_detail)

        // setup back button to finish activity
        findViewById<ImageButton>(R.id.backButton).setOnClickListener { finish() }

        // retrieve the conflict object from intent extras
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

    // initialize ui components with conflict data and set placeholders for stats
    private fun setupUI(conflict: ConflictEvent) {
        findViewById<TextView>(R.id.conflictTitle).text = "${conflict.country} conflict"
        findViewById<TextView>(R.id.conflictDescription).text = conflict.notes
        findViewById<TextView>(R.id.casualties).text = "Total casualties: ${conflict.fatalities}"
        findViewById<TextView>(R.id.actors).text = "Main actor: ${conflict.actor1}"
        findViewById<TextView>(R.id.date).text = "Last updated: ${conflict.event_date}"

        // set loading placeholders for country statistics
        findViewById<TextView>(R.id.countryPopulation).text = "Population: loading..."
        findViewById<TextView>(R.id.populationComparison).text = ""
        findViewById<TextView>(R.id.countryGDP).text = "GDP: loading..."
        findViewById<TextView>(R.id.gdpComparison).text = ""
        findViewById<TextView>(R.id.militaryExpenditure).text = "Military expenditure: loading..."
        findViewById<TextView>(R.id.militaryComparison).text = ""
    }

    // load country stats and regional conflicts asynchronously
    private fun loadData(conflict: ConflictEvent) {
        findViewById<View>(R.id.loadingStats).visibility = View.VISIBLE
        findViewById<View>(R.id.loadingRegional).visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                // load and update country statistics
                val countryStats = worldBankRepository.getCountryStats(conflict.country)
                updateStatistics(countryStats)

                // load and update regional conflicts, excluding the current conflict
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
                    "error loading data: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // update the regional conflicts recycler view with new data
    private fun updateRegionalConflicts(conflicts: List<ConflictEvent>) {
        val recyclerView = findViewById<RecyclerView>(R.id.regionalConflictsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = ConflictAdapter(conflicts) { selectedConflict ->
            setupUI(selectedConflict)
            loadData(selectedConflict)
        }
    }

    // update ui elements with country statistics or default messages if data is missing
    private fun updateStatistics(stats: CountryStats?) {
        if (stats == null) {
            Log.d("ConflictDetail", "No stats available")
            findViewById<TextView>(R.id.countryPopulation).text = "Population: not available"
            findViewById<TextView>(R.id.populationComparison).text = ""
            findViewById<TextView>(R.id.countryGDP).text = "GDP: not available"
            findViewById<TextView>(R.id.gdpComparison).text = ""
            findViewById<TextView>(R.id.militaryExpenditure).text = "Military expenditure: not available"
            findViewById<TextView>(R.id.militaryComparison).text = ""
            return
        }

        // update population info
        findViewById<TextView>(R.id.countryPopulation).text = when (stats.population) {
            null -> "Population: not available"
            else -> "Population: ${formatNumber(stats.population)}"
        }
        findViewById<TextView>(R.id.populationComparison).text = stats.getPopulationComparison()

        // update gdp info
        findViewById<TextView>(R.id.countryGDP).text = when (stats.gdp) {
            null -> "GDP: not available"
            else -> "GDP: $${formatNumber(stats.gdp)}"
        }
        findViewById<TextView>(R.id.gdpComparison).text = stats.getGDPComparison()

        // update military expenditure info
        findViewById<TextView>(R.id.militaryExpenditure).text = when (stats.militaryExpenditure) {
            null -> "Military Expenditure: not available"
            else -> "Military Expenditure: $${formatNumber(stats.militaryExpenditure)}"
        }
        findViewById<TextView>(R.id.militaryComparison).text = stats.getMilitaryComparison()
    }

    // format numeric values into a readable string with units
    private fun formatNumber(value: Double): String {
        return when {
            value >= 1_000_000_000 -> String.format("%.1fB", value / 1_000_000_000)
            value >= 1_000_000 -> String.format("%.1fM", value / 1_000_000)
            value >= 1_000 -> String.format("%.1fK", value / 1_000)
            else -> String.format("%.1f", value)
        }
    }
}