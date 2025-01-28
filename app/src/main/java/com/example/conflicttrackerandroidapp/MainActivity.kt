package com.example.conflicttrackerandroidapp

import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.conflicttrackerandroidapp.api.ConflictEvent
import com.example.conflicttrackerandroidapp.api.ConflictRepository
import com.google.android.material.button.MaterialButton
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.gestures.gestures
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// Extension functions
fun Drawable.toBitmap(width: Int, height: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    setBounds(0, 0, width, height)
    draw(canvas)
    return bitmap
}

fun Int.dpToPx(): Int {
    return (this * Resources.getSystem().displayMetrics.density).roundToInt()
}

class MainActivity : AppCompatActivity() {
    private lateinit var mapView: MapView
    private val repository = ConflictRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize map
        mapView = findViewById(R.id.mapView)

        // Updated style loading
        mapView.mapboxMap.loadStyle(Style.MAPBOX_STREETS)

        // Set initial camera position (world view)
        mapView.mapboxMap.setCamera(
            CameraOptions.Builder()
                .center(Point.fromLngLat(0.0, 0.0))
                .zoom(1.0)
                .build()
        )

        // Enable zoom gestures
        mapView.gestures.pinchToZoomEnabled = true
        mapView.gestures.doubleTapToZoomInEnabled = true
        mapView.gestures.doubleTouchToZoomOutEnabled = true

        // Add map controls
        addMapControls()

        // Set up RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.watchlistRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.overScrollMode = RecyclerView.OVER_SCROLL_NEVER

        // Load initial data
        loadConflictData()
    }

    private fun addMapControls() {
        val mapParent = mapView.parent as ViewGroup

        // Add zoom controls
        val zoomButtonsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.END
                bottomMargin = 16.dpToPx()
                marginEnd = 16.dpToPx()
            }
        }

        val buttonSize = 40.dpToPx() // Increased button size

        // Create zoom in button
        val zoomInButton = MaterialButton(this).apply {
            text = "+"
            textSize = 20f // Increased text size
            insetTop = 0
            insetBottom = 0
            minHeight = buttonSize
            minWidth = buttonSize
            layoutParams = LinearLayout.LayoutParams(buttonSize, buttonSize).apply {
                bottomMargin = 8.dpToPx()
            }
            alpha = 0.8f // Increased opacity
            setBackgroundResource(R.drawable.rounded_square_button_bg)
            setTextColor(ContextCompat.getColor(context, android.R.color.white))
            setPadding(0, 0, 0, 0) // Remove padding
            setOnClickListener {
                val currentZoom = mapView.mapboxMap.cameraState.zoom
                mapView.mapboxMap.setCamera(
                    CameraOptions.Builder()
                        .zoom(currentZoom + 1.0)
                        .build()
                )
            }
        }

        // Create zoom out button
        val zoomOutButton = MaterialButton(this).apply {
            text = "−" // Using proper minus sign
            textSize = 20f // Increased text size
            insetTop = 0
            insetBottom = 0
            minHeight = buttonSize
            minWidth = buttonSize
            layoutParams = LinearLayout.LayoutParams(buttonSize, buttonSize)
            alpha = 0.8f // Increased opacity
            setBackgroundResource(R.drawable.rounded_square_button_bg)
            setTextColor(ContextCompat.getColor(context, android.R.color.white))
            setPadding(0, 0, 0, 0) // Remove padding
            setOnClickListener {
                val currentZoom = mapView.mapboxMap.cameraState.zoom
                mapView.mapboxMap.setCamera(
                    CameraOptions.Builder()
                        .zoom(currentZoom - 1.0)
                        .build()
                )
            }
        }

        zoomButtonsContainer.addView(zoomInButton)
        zoomButtonsContainer.addView(zoomOutButton)
        mapParent.addView(zoomButtonsContainer)

        // Add legend (now at bottom-left)
        val legendContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = ResourcesCompat.getDrawable(resources, R.drawable.semi_transparent_background, theme)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.START
                bottomMargin = 16.dpToPx()
                marginStart = 16.dpToPx()
                setPadding(8.dpToPx(), 8.dpToPx(), 8.dpToPx(), 8.dpToPx())
            }
        }

        // Add legend items
        val legendItems = listOf(
            Pair(R.drawable.marker_severe, "Severe Conflict"),
            Pair(R.drawable.marker_regular, "Active Conflict")
        )

        legendItems.forEach { (drawableId, text) ->
            val itemLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 4.dpToPx()
                }
            }

            val icon = ImageView(this).apply {
                setImageResource(drawableId)
                layoutParams = LinearLayout.LayoutParams(16.dpToPx(), 16.dpToPx()).apply {
                    marginEnd = 8.dpToPx()
                    gravity = Gravity.CENTER_VERTICAL
                }
            }

            val label = TextView(this).apply {
                this.text = text
                textSize = 12f
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            }

            itemLayout.addView(icon)
            itemLayout.addView(label)
            legendContainer.addView(itemLayout)
        }

        mapParent.addView(legendContainer)
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

        // Set up Read More button click
        findViewById<MaterialButton>(R.id.escalatingReadMore).setOnClickListener {
            val intent = Intent(this, EscalatingConflictActivity::class.java)
            intent.putExtra("conflict", conflict)
            startActivity(intent)
        }
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