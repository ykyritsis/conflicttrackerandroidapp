package com.example.conflicttrackerandroidapp

import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Typeface
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
import com.example.conflicttrackerandroidapp.api.WorldBankRepository
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

fun Drawable.toBitmap(width: Int, height: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    setBounds(0, 0, width, height)
    draw(canvas)
    return bitmap
}

fun Int.dpToPx(): Int {
    return (this * Resources.getSystem().displayMetrics.density).toInt()
}

class MainActivity : AppCompatActivity() {
    private lateinit var mapView: MapView
    private val repository = ConflictRepository()
    private val worldBankRepository = WorldBankRepository()
    private val plottedConflicts = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize country codes
        lifecycleScope.launch {
            worldBankRepository.initializeCountryCodes()
        }

        // Initialize map
        mapView = findViewById(R.id.mapView)
        mapView.mapboxMap.loadStyle(Style.MAPBOX_STREETS)

        // Set initial camera position
        mapView.mapboxMap.setCamera(
            CameraOptions.Builder()
                .center(Point.fromLngLat(0.0, 20.0))
                .zoom(2.0)
                .build()
        )

        // Enable zoom gestures
        mapView.gestures.pinchToZoomEnabled = true
        mapView.gestures.doubleTapToZoomInEnabled = true
        mapView.gestures.doubleTouchToZoomOutEnabled = true

        // Add map controls
        addMapControls()
        val legendItems = listOf(
            Pair(R.drawable.marker_severe, "Severe (20+ casualties)"),
            Pair(R.drawable.marker_regular, "Escalating (10-20 casualties)"),
            Pair(R.drawable.marker_concerning, "Concerning (<10 casualties)")
        )

        // Set up RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.watchlistRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.overScrollMode = RecyclerView.OVER_SCROLL_NEVER

        // Load initial data
        loadConflictData()
    }
    private fun addMapControls() {
        val mapParent = mapView.parent as ViewGroup
        val buttonSize = 32.dpToPx()

        // Add zoom controls container
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

        // Create zoom in button
        val zoomInButton = MaterialButton(this).apply {
            text = "+"
            textSize = 20f
            insetTop = 0
            insetBottom = 0
            minHeight = buttonSize
            minWidth = buttonSize
            layoutParams = LinearLayout.LayoutParams(buttonSize, buttonSize).apply {
                bottomMargin = 8.dpToPx()
            }
            alpha = 0.9f
            setBackgroundResource(R.drawable.rounded_square_button_bg)
            setTextColor(ContextCompat.getColor(context, android.R.color.white))
            setPadding(0, 0, 0, 0)
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
            text = "−"
            textSize = 20f
            insetTop = 0
            insetBottom = 0
            minHeight = buttonSize
            minWidth = buttonSize
            layoutParams = LinearLayout.LayoutParams(buttonSize, buttonSize)
            alpha = 0.9f
            setBackgroundResource(R.drawable.rounded_square_button_bg)
            setTextColor(ContextCompat.getColor(context, android.R.color.white))
            setPadding(0, 0, 0, 0)
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

        // Add legend
        // In MainActivity.kt, update the legendItems section in addMapControls():

        // Add legend
        val legendContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = ResourcesCompat.getDrawable(resources, R.drawable.legend_background, theme)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.START
                bottomMargin = 16.dpToPx()
                marginStart = 16.dpToPx()
            }
        }

        // Create a single row container
        val legendContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12.dpToPx(), 8.dpToPx(), 12.dpToPx(), 8.dpToPx())
        }

        // Define legend items more concisely
        listOf(
            Triple(R.drawable.marker_severe, "20+", "#FF4444"),
            Triple(R.drawable.marker_regular, "10-20", "#FFA500"),
            Triple(R.drawable.marker_concerning, "<10", "#4BA3E3")
        ).forEach { (icon, range, color) ->
            legendContent.addView(
                LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = 4.dpToPx()
                    }

                    addView(ImageView(context).apply {
                        setImageResource(icon)
                        layoutParams = LinearLayout.LayoutParams(12.dpToPx(), 12.dpToPx()).apply {
                            marginEnd = 8.dpToPx()
                        }
                    })

                    addView(TextView(context).apply {
                        text = range
                        setTextColor(Color.parseColor(color))
                        textSize = 11f
                        typeface = Typeface.DEFAULT_BOLD
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            marginEnd = 4.dpToPx()
                        }
                    })

                    addView(TextView(context).apply {
                        text = "casualties"
                        setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                        textSize = 11f
                        alpha = 0.8f
                    })
                }
            )
        }

        legendContainer.addView(legendContent)
        mapParent.addView(legendContainer)
    }

    private fun loadConflictData() {
        findViewById<ProgressBar>(R.id.escalatingProgress).visibility = View.VISIBLE
        findViewById<ProgressBar>(R.id.watchlistProgress).visibility = View.VISIBLE
        findViewById<LinearLayout>(R.id.escalatingContent).visibility = View.GONE
        findViewById<LinearLayout>(R.id.watchlistContent).visibility = View.GONE

        lifecycleScope.launch {
            try {
                // First Priority: Load and display escalating conflict
                val severeConflict = repository.getMostSevereRecentConflict()
                severeConflict?.let {
                    updateEscalatingCard(it)
                    addConflictMarker(it, true)
                    findViewById<ProgressBar>(R.id.escalatingProgress).visibility = View.GONE
                    findViewById<LinearLayout>(R.id.escalatingContent).visibility = View.VISIBLE
                }

                // Second Priority: Load and display watchlist
                val conflicts = repository.getTopOngoingConflicts()
                updateWatchlistCard(conflicts)
                findViewById<ProgressBar>(R.id.watchlistProgress).visibility = View.GONE
                findViewById<LinearLayout>(R.id.watchlistContent).visibility = View.VISIBLE

                // Third Priority: Plot initial conflicts on map (limited to 50)
                // In loadConflictData() function, update these sections:

                // Third Priority: Plot initial conflicts on map (increased limit)
                conflicts.take(150).forEach { conflict ->  // Increased from 50 to 150
                    addConflictMarker(conflict, false)
                }

                // Fourth Priority (Background): Load and plot additional conflicts
                lifecycleScope.launch {
                    try {
                        // Load regional conflicts for severe conflict
                        severeConflict?.let { severe ->
                            val regionalConflicts = repository.getRegionalConflicts(severe.country)
                            regionalConflicts
                                .take(60)  // Increased from 20 to 60
                                .forEach { regionalConflict ->
                                    if (regionalConflict.event_id_cnty != severe.event_id_cnty) {
                                        addConflictMarker(regionalConflict, false)
                                    }
                                }
                        }

                        // Load regional conflicts for top 5 watchlist conflicts
                        conflicts.take(5).forEach { conflict ->
                            val regionalConflicts = repository.getRegionalConflicts(conflict.country)
                            regionalConflicts
                                .take(60)  // Increased from 20 to 60
                                .forEach { regionalConflict ->
                                    if (regionalConflict.event_id_cnty != conflict.event_id_cnty) {
                                        addConflictMarker(regionalConflict, false)
                                    }
                                }
                        }
                    } catch (e: Exception) {
                        // Silent fail for background loading - main content already displayed
                    }
                }
            } catch (e: Exception) {
                findViewById<ProgressBar>(R.id.escalatingProgress).visibility = View.GONE
                findViewById<ProgressBar>(R.id.watchlistProgress).visibility = View.GONE
                Toast.makeText(this@MainActivity, "Error loading data", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateEscalatingCard(conflict: ConflictEvent) {
        findViewById<TextView>(R.id.escalatingLocation).text = "${conflict.country} - ${conflict.event_type}"
        findViewById<TextView>(R.id.escalatingFatalities).text = "${conflict.fatalities} casualties"
        findViewById<TextView>(R.id.escalatingDate).text = "Event Date: ${conflict.event_date}"

        findViewById<MaterialButton>(R.id.escalatingReadMore).setOnClickListener {
            val intent = Intent(this, EscalatingConflictActivity::class.java)
            intent.putExtra("conflict", conflict)
            startActivity(intent)
        }
    }

    private fun updateWatchlistCard(conflicts: List<ConflictEvent>) {
        val topFiveConflicts = conflicts.take(5)
        val recyclerView = findViewById<RecyclerView>(R.id.watchlistRecyclerView)
        recyclerView.adapter = ConflictAdapter(topFiveConflicts) { selectedConflict ->
            val intent = Intent(this, ConflictDetailActivity::class.java)
            intent.putExtra("conflict", selectedConflict)
            startActivity(intent)
        }
    }

    private fun addConflictMarker(conflict: ConflictEvent, isMainConflict: Boolean) {
        if (plottedConflicts.contains(conflict.event_id_cnty)) {
            return
        }

        val pointAnnotationManager = mapView.annotations.createPointAnnotationManager()

        val markerDrawable = when {
            isMainConflict -> resources.getDrawable(R.drawable.marker_severe, theme)
            conflict.fatalities >= 20 -> resources.getDrawable(R.drawable.marker_severe, theme)
            conflict.fatalities >= 10 -> resources.getDrawable(R.drawable.marker_regular, theme)
            else -> resources.getDrawable(R.drawable.marker_concerning, theme)
        }.apply {
            alpha = 180
        }

        val bitmap = markerDrawable.toBitmap(40, 40)

        val pointAnnotationOptions = PointAnnotationOptions()
            .withPoint(Point.fromLngLat(conflict.longitude, conflict.latitude))
            .withIconImage(bitmap)
            .withIconSize(1.0)

        val annotation = pointAnnotationManager.create(pointAnnotationOptions)

        pointAnnotationManager.addClickListener { clickedAnnotation ->
            if (clickedAnnotation == annotation) {
                val intent = Intent(this, ConflictDetailActivity::class.java)
                intent.putExtra("conflict", conflict)
                startActivity(intent)
                true
            } else false
        }

        plottedConflicts.add(conflict.event_id_cnty)
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onDestroy() {
        plottedConflicts.clear()
        super.onDestroy()
        mapView.onDestroy()
    }
}