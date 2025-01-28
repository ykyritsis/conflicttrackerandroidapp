package com.example.conflicttrackerandroidapp

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.conflicttrackerandroidapp.api.ConflictEvent
import com.example.conflicttrackerandroidapp.api.ConflictRepository
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.scalebar.scalebar
import com.mapbox.maps.plugin.compass.compass
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class EscalatingConflictActivity : AppCompatActivity() {
    private lateinit var mapView: MapView
    private var isDescriptionExpanded = false
    private val repository = ConflictRepository()
    private val TAG = "EscalatingConflict"
    private var currentJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_escalating_conflict)

        // Initialize map
        mapView = findViewById(R.id.regionMapView)
        mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS)

        // Add zoom controls
        mapView.gestures.pinchToZoomEnabled = true
        mapView.gestures.doubleTapToZoomInEnabled = true
        mapView.gestures.doubleTouchToZoomOutEnabled = true

        // Enable map controls
        mapView.scalebar.enabled = true
        mapView.compass.enabled = true

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

            // Center map on conflict location
            mapView.getMapboxMap().setCamera(
                CameraOptions.Builder()
                    .center(Point.fromLngLat(it.longitude, it.latitude))
                    .zoom(5.0)
                    .build()
            )

            // Add marker for main conflict
            addConflictMarker(it, true)
        }

        // Set up description expansion
        val description = findViewById<TextView>(R.id.conflictDescription)
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
    }

    private fun updateMainCard(conflict: ConflictEvent) {
        findViewById<TextView>(R.id.conflictTitle).text = "${conflict.country} Conflict"
        findViewById<TextView>(R.id.conflictDescription).text = conflict.notes
        findViewById<TextView>(R.id.casualties).text = "Total Casualties: ${conflict.fatalities}"
        findViewById<TextView>(R.id.actors).text = "Main Actor: ${conflict.actor1}"
        findViewById<TextView>(R.id.date).text = "Last Updated: ${conflict.event_date}"
    }

    private fun loadRegionStats(conflict: ConflictEvent) {
        currentJob?.cancel()

        currentJob = lifecycleScope.launch {
            try {
                Log.d(TAG, "Loading region stats for ${conflict.country}")

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

                // Add markers for regional conflicts
                regionalConflicts.forEach { regionalConflict ->
                    if (regionalConflict.event_id_cnty != conflict.event_id_cnty) {
                        addConflictMarker(regionalConflict, false)
                    }
                }

                updateRegionOverview(conflict.region, regionalConflicts)
            } catch (e: CancellationException) {
                Log.d(TAG, "Region stats loading was cancelled")
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error loading regional data: ${e.message}", e)
                Toast.makeText(this@EscalatingConflictActivity,
                    "Error loading regional data. Please try again.",
                    Toast.LENGTH_SHORT).show()

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

    private fun addConflictMarker(conflict: ConflictEvent, isMain: Boolean) {
        val pointAnnotationManager = mapView.annotations.createPointAnnotationManager()

        // Get the appropriate marker drawable
        val markerDrawable = if (isMain) {
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
        pointAnnotationManager.create(pointAnnotationOptions)
    }

    // MapView lifecycle methods
    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onDestroy() {
        currentJob?.cancel()
        super.onDestroy()
        mapView.onDestroy()
    }
}