package com.example.conflicttrackerandroidapp

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.conflicttrackerandroidapp.api.ConflictEvent
import com.example.conflicttrackerandroidapp.api.ConflictRepository
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import kotlinx.coroutines.launch

// Extension function to convert drawable to bitmap
fun Drawable.toBitmap(width: Int, height: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    setBounds(0, 0, width, height)
    draw(canvas)
    return bitmap
}

class MainActivity : AppCompatActivity() {
    private lateinit var mapView: MapView
    private val repository = ConflictRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize map
        mapView = findViewById(R.id.mapView)
        mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS)

        // Set initial camera position (world view)
        mapView.getMapboxMap().setCamera(
            CameraOptions.Builder()
                .center(Point.fromLngLat(0.0, 0.0))
                .zoom(1.0)
                .build()
        )

        // Set up RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.watchlistRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.overScrollMode = RecyclerView.OVER_SCROLL_NEVER

        // Set up click listener for escalating conflict card
        findViewById<LinearLayout>(R.id.escalatingContent).setOnClickListener {
            lifecycleScope.launch {
                repository.getMostSevereRecentConflict()?.let { conflict ->
                    val intent = Intent(this@MainActivity, EscalatingConflictActivity::class.java)
                    intent.putExtra("conflict", conflict)
                    startActivity(intent)
                }
            }
        }

        // Load initial data
        loadConflictData()
    }

    private fun loadConflictData() {
        // Show loading indicators
        findViewById<ProgressBar>(R.id.escalatingProgress).visibility = View.VISIBLE
        findViewById<ProgressBar>(R.id.watchlistProgress).visibility = View.VISIBLE
        findViewById<LinearLayout>(R.id.escalatingContent).visibility = View.GONE
        findViewById<LinearLayout>(R.id.watchlistContent).visibility = View.GONE

        lifecycleScope.launch {
            try {
                // Load most severe recent conflict
                val severeConflict = repository.getMostSevereRecentConflict()
                severeConflict?.let {
                    updateEscalatingCard(it)
                    // Add marker for severe conflict
                    addConflictMarker(it, true)
                }

                // Load top ongoing conflicts
                val topConflicts = repository.getTopOngoingConflicts()
                updateWatchlistCard(topConflicts)
                // Add markers for top conflicts
                topConflicts.forEach {
                    addConflictMarker(it, false)
                }

                // Hide loading, show content
                findViewById<ProgressBar>(R.id.escalatingProgress).visibility = View.GONE
                findViewById<ProgressBar>(R.id.watchlistProgress).visibility = View.GONE
                findViewById<LinearLayout>(R.id.escalatingContent).visibility = View.VISIBLE
                findViewById<LinearLayout>(R.id.watchlistContent).visibility = View.VISIBLE
            } catch (e: Exception) {
                // Hide loading indicators on error
                findViewById<ProgressBar>(R.id.escalatingProgress).visibility = View.GONE
                findViewById<ProgressBar>(R.id.watchlistProgress).visibility = View.GONE
                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
    }

    private fun updateEscalatingCard(conflict: ConflictEvent) {
        findViewById<TextView>(R.id.escalatingLocation).text = "${conflict.country} - ${conflict.event_type}"
        findViewById<TextView>(R.id.escalatingFatalities).text = "${conflict.fatalities} casualties"
        findViewById<TextView>(R.id.escalatingDate).text = "Updated: ${conflict.event_date}"
    }

    private fun updateWatchlistCard(conflicts: List<ConflictEvent>) {
        val recyclerView = findViewById<RecyclerView>(R.id.watchlistRecyclerView)
        recyclerView.adapter = ConflictAdapter(conflicts) { selectedConflict ->
            val intent = Intent(this, ConflictDetailActivity::class.java)
            intent.putExtra("conflict", selectedConflict)
            startActivity(intent)
        }
    }

    private fun addConflictMarker(conflict: ConflictEvent, isSevere: Boolean) {
        val pointAnnotationManager = mapView.annotations.createPointAnnotationManager()

        // Get the appropriate marker drawable
        val markerDrawable = if (isSevere) {
            resources.getDrawable(R.drawable.marker_severe, theme)
        } else {
            resources.getDrawable(R.drawable.marker_regular, theme)
        }

        // Convert drawable to bitmap
        val bitmap = markerDrawable.toBitmap(
            width = 40,
            height = 40
        )

        // Create a marker
        val pointAnnotationOptions = PointAnnotationOptions()
            .withPoint(Point.fromLngLat(conflict.longitude, conflict.latitude))
            .withIconImage(bitmap)
            .withIconSize(1.0)

        // Add the marker
        val annotation = pointAnnotationManager.create(pointAnnotationOptions)

        // Add click listener to marker
        pointAnnotationManager.addClickListener { clickedAnnotation ->
            if (clickedAnnotation == annotation) {
                val intent = Intent(this, ConflictDetailActivity::class.java)
                intent.putExtra("conflict", conflict)
                startActivity(intent)
                true
            } else false
        }
    }

    // Lifecycle methods for MapView
    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }
}