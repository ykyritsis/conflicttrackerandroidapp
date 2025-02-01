package com.example.conflicttrackerandroidapp

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
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
import com.mapbox.maps.plugin.compass.compass
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.scalebar.scalebar
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

// activity to display escalating conflict details and regional overview
class EscalatingConflictActivity : AppCompatActivity() {
    private lateinit var mapView: MapView
    private var isDescriptionExpanded = false
    private val repository = ConflictRepository()
    private val TAG = "escalatingconflict"
    private var currentJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_escalating_conflict)

        // initialize map and controls
        mapView = findViewById(R.id.regionMapView)
        // Use the new loadStyle method with a callback instead of loadStyleUri()
        mapView.mapboxMap.loadStyle(Style.MAPBOX_STREETS) {
            // style loaded successfully; additional setup if needed
        }
        mapView.gestures.apply {
            pinchToZoomEnabled = true
            doubleTapToZoomInEnabled = true
            doubleTouchToZoomOutEnabled = true
        }
        mapView.scalebar.enabled = true
        mapView.compass.enabled = true

        findViewById<ImageButton>(R.id.backButton).setOnClickListener { finish() }

        // retrieve conflict object from intent extras
        val conflict = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("conflict", ConflictEvent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("conflict")
        }

        conflict?.let {
            updateMainCard(it)
            loadRegionStats(it)

            // center map on conflict location using the new mapboxMap property
            mapView.mapboxMap.setCamera(
                CameraOptions.Builder()
                    .center(Point.fromLngLat(it.longitude, it.latitude))
                    .zoom(5.0)
                    .build()
            )

            // add marker for main conflict
            addConflictMarker(it, true)
        }

        // toggle description expansion
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
        findViewById<TextView>(R.id.conflictTitle).text = "${conflict.country} conflict"
        findViewById<TextView>(R.id.conflictDescription).text = conflict.notes
        findViewById<TextView>(R.id.casualties).text = "Total casualties: ${conflict.fatalities}"
        findViewById<TextView>(R.id.actors).text = "Main actor: ${conflict.actor1}"
        findViewById<TextView>(R.id.date).text = "Last updated: ${conflict.event_date}"
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
                Log.d(TAG, "found ${regionalConflicts.size} regional conflicts")

                if (regionalConflicts.isEmpty()) {
                    Log.d(TAG, "No regional conflicts found")
                    findViewById<TextView>(R.id.regionName).text = "Region: ${conflict.region}"
                    findViewById<TextView>(R.id.conflictFrequency).text = "No regional data available"
                    return@launch
                }

                // add markers for regional conflicts (exclude main conflict)
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
                Log.e(TAG, "error loading regional data: ${e.message}", e)
                Toast.makeText(this@EscalatingConflictActivity,
                    "error loading regional data. please try again.",
                    Toast.LENGTH_SHORT).show()
                findViewById<TextView>(R.id.regionName).text = "Region: ${conflict.region}"
                findViewById<TextView>(R.id.conflictFrequency).text = "error loading regional data"
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
            "$totalConflicts Active conflicts in region"
        findViewById<TextView>(R.id.casualtyComparison).text =
            "Average of ${String.format("%.1f", averageCasualties)} casualties per conflict"

        val severityLevel = when {
            averageCasualties > 100 -> "critical"
            averageCasualties > 50 -> "severe"
            averageCasualties > 20 -> "elevated"
            else -> "moderate"
        }
        findViewById<TextView>(R.id.severityLevel).text = "Regional severity level: $severityLevel"
    }

    private fun addConflictMarker(conflict: ConflictEvent, isMain: Boolean) {
        val pointAnnotationManager = mapView.annotations.createPointAnnotationManager()
        val markerDrawable = if (isMain) {
            resources.getDrawable(R.drawable.marker_severe, theme)
        } else {
            resources.getDrawable(R.drawable.marker_regular, theme)
        }
        val bitmap = markerDrawable.toBitmap(40, 40)
        val pointAnnotationOptions = PointAnnotationOptions()
            .withPoint(Point.fromLngLat(conflict.longitude, conflict.latitude))
            .withIconImage(bitmap)
            .withIconSize(1.0)
        pointAnnotationManager.create(pointAnnotationOptions)
    }

    // extension to convert drawable to bitmap
    private fun Drawable.toBitmap(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).also {
            setBounds(0, 0, width, height)
            draw(it)
        }
        return bitmap
    }
}