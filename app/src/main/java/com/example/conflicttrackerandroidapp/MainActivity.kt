package com.example.conflicttrackerandroidapp

import android.app.Dialog
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
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

/**
 * extension function to convert a drawable to bitmap
 */
fun Drawable.toBitmap(width: Int, height: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    setBounds(0, 0, width, height)
    draw(canvas)
    return bitmap
}

/**
 * extension function to convert dp to px
 */
fun Int.dpToPx(): Int {
    return (this * Resources.getSystem().displayMetrics.density).toInt()
}

class MainActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private val repository = ConflictRepository()
    private val worldBankRepository = WorldBankRepository()
    private val plottedConflicts = mutableSetOf<String>()

    // current filter selections from filterutils.kt
    private var currentFilters: FilterOptions = FilterOptions()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // initialize country codes in background
        lifecycleScope.launch {
            worldBankRepository.initializeCountryCodes()
        }

        mapView = findViewById(R.id.mapView)
        mapView.mapboxMap.loadStyle(Style.MAPBOX_STREETS)

        // create toolbar container for filter and menu buttons
        val toolbarContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP or Gravity.END
                topMargin = 16.dpToPx()
                marginEnd = 16.dpToPx()
            }
        }

        val buttonSize = 32.dpToPx()

        // filter button
        val filterButton = MaterialButton(this).apply {
            setIconResource(R.drawable.ic_filter)
            iconTint = ColorStateList.valueOf(Color.WHITE)
            insetTop = 0
            insetBottom = 0
            minWidth = buttonSize
            minHeight = buttonSize
            setPadding(6.dpToPx(), 6.dpToPx(), 6.dpToPx(), 6.dpToPx())
            layoutParams = LinearLayout.LayoutParams(buttonSize, buttonSize).apply {
                marginEnd = 8.dpToPx()
            }
            setBackgroundResource(R.drawable.transparent_square_button_bg)
            alpha = 0.8f
            setOnClickListener {
                // open filter dialog
                showFilterDialog()
            }
        }

        // menu button with search functionality
        val menuButton = MaterialButton(this).apply {
            setIconResource(R.drawable.ic_menu)
            iconTint = ColorStateList.valueOf(Color.WHITE)
            insetTop = 0
            insetBottom = 0
            minWidth = buttonSize
            minHeight = buttonSize
            setPadding(6.dpToPx(), 6.dpToPx(), 6.dpToPx(), 6.dpToPx())
            layoutParams = LinearLayout.LayoutParams(buttonSize, buttonSize)
            setBackgroundResource(R.drawable.transparent_square_button_bg)
            alpha = 0.8f
            setOnClickListener {
                // open search dialog
                showSearchDialog()
            }
        }

        toolbarContainer.addView(filterButton)
        toolbarContainer.addView(menuButton)

        // add toolbar container to the map parent
        val mapParent = mapView.parent as ViewGroup
        mapParent.addView(toolbarContainer)

        // center and zoom the map
        mapView.mapboxMap.setCamera(
            CameraOptions.Builder()
                .center(Point.fromLngLat(0.0, 20.0))
                .zoom(2.0)
                .build()
        )

        // enable map gestures
        mapView.gestures.pinchToZoomEnabled = true
        mapView.gestures.doubleTapToZoomInEnabled = true
        mapView.gestures.doubleTouchToZoomOutEnabled = true

        // add zoom controls and legend to map
        addMapControls()

        // setup watchlist recycler view
        val recyclerView = findViewById<RecyclerView>(R.id.watchlistRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.overScrollMode = RecyclerView.OVER_SCROLL_NEVER

        // load initial conflict data
        loadConflictData()
    }

    /**
     * adds zoom buttons and legend overlay to the map
     */
    private fun addMapControls() {
        val mapParent = mapView.parent as ViewGroup
        val buttonSize = 32.dpToPx()

        // container for zoom buttons
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

        // zoom in button
        val zoomInButton = MaterialButton(this).apply {
            text = "+"
            textSize = 18f
            insetTop = 0
            insetBottom = 0
            minHeight = buttonSize
            minWidth = buttonSize
            layoutParams = LinearLayout.LayoutParams(buttonSize, buttonSize).apply {
                bottomMargin = 8.dpToPx()
            }
            alpha = 0.8f
            setBackgroundResource(R.drawable.transparent_square_button_bg)
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, 0)
            setOnClickListener {
                // increase zoom level by 1
                val currentZoom = mapView.mapboxMap.cameraState.zoom
                mapView.mapboxMap.setCamera(
                    CameraOptions.Builder()
                        .zoom(currentZoom + 1.0)
                        .build()
                )
            }
        }

        // zoom out button
        val zoomOutButton = MaterialButton(this).apply {
            text = "−"
            textSize = 18f
            insetTop = 0
            insetBottom = 0
            minHeight = buttonSize
            minWidth = buttonSize
            layoutParams = LinearLayout.LayoutParams(buttonSize, buttonSize)
            alpha = 0.8f
            setBackgroundResource(R.drawable.transparent_square_button_bg)
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, 0)
            setOnClickListener {
                // decrease zoom level by 1
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

        // legend container
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

        // legend content with marker info
        val legendContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12.dpToPx(), 8.dpToPx(), 12.dpToPx(), 8.dpToPx())
        }

        // add legend items
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

    /**
     * loads conflict data, applies filters, and updates the ui
     */
    private fun loadConflictData() {
        // show progress indicators
        findViewById<ProgressBar>(R.id.escalatingProgress).visibility = View.VISIBLE
        findViewById<ProgressBar>(R.id.watchlistProgress).visibility = View.VISIBLE
        findViewById<LinearLayout>(R.id.escalatingContent).visibility = View.GONE
        findViewById<LinearLayout>(R.id.watchlistContent).visibility = View.GONE

        lifecycleScope.launch {
            try {
                // load the most severe conflict
                val severeConflict = repository.getMostSevereRecentConflict()

                // load top ongoing conflicts
                val allConflicts = repository.getTopOngoingConflicts()

                // log size for debugging
                Log.d("MainActivity", "allconflicts size: ${allConflicts.size}")

                // apply current filters
                val filteredConflicts = filterConflicts(allConflicts, currentFilters)
                Log.d("MainActivity", "filteredconflicts size: ${filteredConflicts.size}")

                // update escalating card if a severe conflict is found
                severeConflict?.let {
                    Log.d("MainActivity", "severe conflict: ${it.country}, ${it.fatalities} fatalities")
                    updateEscalatingCard(it)
                    // add marker for severe conflict on map
                    addConflictMarker(it, true)
                    findViewById<ProgressBar>(R.id.escalatingProgress).visibility = View.GONE
                    findViewById<LinearLayout>(R.id.escalatingContent).visibility = View.VISIBLE
                } ?: run {
                    Log.d("MainActivity", "severe conflict is null; no escalations to show.")
                    findViewById<ProgressBar>(R.id.escalatingProgress).visibility = View.GONE
                }

                // update watchlist with top 5 filtered conflicts
                val topFiveConflicts = filteredConflicts.take(5)
                Log.d("MainActivity", "topfiveconflicts size: ${topFiveConflicts.size}")
                updateWatchlistCard(topFiveConflicts)
                findViewById<ProgressBar>(R.id.watchlistProgress).visibility = View.GONE
                findViewById<LinearLayout>(R.id.watchlistContent).visibility = View.VISIBLE

                // plot up to 150 map markers from filtered conflicts
                filteredConflicts.take(150).forEach { conflict ->
                    Log.d("MainActivity", "plotting marker: ${conflict.country}, id: ${conflict.event_id_cnty}")
                    addConflictMarker(conflict, false)
                }

            } catch (e: Exception) {
                Log.e("MainActivity", "error loading data", e)
                findViewById<ProgressBar>(R.id.escalatingProgress).visibility = View.GONE
                findViewById<ProgressBar>(R.id.watchlistProgress).visibility = View.GONE
                Toast.makeText(this@MainActivity, "error loading data", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * updates the escalating card with severe conflict details
     */
    private fun updateEscalatingCard(conflict: ConflictEvent) {
        findViewById<TextView>(R.id.escalatingLocation).text = "${conflict.country} - ${conflict.event_type}"
        findViewById<TextView>(R.id.escalatingFatalities).text = "${conflict.fatalities} casualties"
        findViewById<TextView>(R.id.escalatingDate).text = "event date: ${conflict.event_date}"
        findViewById<MaterialButton>(R.id.escalatingReadMore).setOnClickListener {
            // open escalating conflict activity with conflict details
            val intent = Intent(this, EscalatingConflictActivity::class.java)
            intent.putExtra("conflict", conflict)
            startActivity(intent)
        }
    }

    /**
     * shows a dialog to modify filter selections and applies them
     */
    private fun showFilterDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_filter)

        // initialize filter spinners
        val regionSpinner = dialog.findViewById<Spinner>(R.id.regionSpinner)
        val severitySpinner = dialog.findViewById<Spinner>(R.id.severitySpinner)
        val eventTypeSpinner = dialog.findViewById<Spinner>(R.id.eventTypeSpinner)
        val timeframeSpinner = dialog.findViewById<Spinner>(R.id.timeframeSpinner)

        // setup spinner adapters
        val regions = arrayOf("All Regions", "Africa", "Middle East", "Asia", "Europe", "Americas")
        regionSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, regions)
        val severityLevels = arrayOf("All Severities", "High (20+ casualties)", "Medium (10-20 casualties)", "Low (<10 casualties)")
        severitySpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, severityLevels)
        val eventTypes = arrayOf("All Events", "Battles", "Violence against civilians", "Protests", "Riots")
        eventTypeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, eventTypes)
        val timeframes = arrayOf("All Time", "Last Week", "Last Month", "Last 3 Months")
        timeframeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, timeframes)

        // set spinner selections based on current filters
        currentFilters.region?.let { region ->
            val idx = regions.indexOf(region)
            if (idx != -1) regionSpinner.setSelection(idx)
        }
        currentFilters.severityLevel?.let { level ->
            val idx = severityLevels.indexOf(level)
            if (idx != -1) severitySpinner.setSelection(idx)
        }
        currentFilters.eventType?.let { et ->
            val idx = eventTypes.indexOf(et)
            if (idx != -1) eventTypeSpinner.setSelection(idx)
        }
        currentFilters.timeframe?.let { tf ->
            val idx = timeframes.indexOf(tf)
            if (idx != -1) timeframeSpinner.setSelection(idx)
        }

        // reset button to clear selections
        dialog.findViewById<Button>(R.id.resetButton).setOnClickListener {
            regionSpinner.setSelection(0)
            severitySpinner.setSelection(0)
            eventTypeSpinner.setSelection(0)
            timeframeSpinner.setSelection(0)
        }

        // apply button to update filters and reload data
        dialog.findViewById<Button>(R.id.applyButton).setOnClickListener {
            currentFilters = FilterOptions(
                region = if (regionSpinner.selectedItemPosition == 0) null else regionSpinner.selectedItem.toString(),
                severityLevel = if (severitySpinner.selectedItemPosition == 0) null else severitySpinner.selectedItem.toString(),
                eventType = if (eventTypeSpinner.selectedItemPosition == 0) null else eventTypeSpinner.selectedItem.toString(),
                timeframe = if (timeframeSpinner.selectedItemPosition == 0) null else timeframeSpinner.selectedItem.toString()
            )
            applyFilters()
            dialog.dismiss()
        }

        dialog.show()
    }

    /**
     * clears existing markers and reloads data with updated filters
     */
    private fun applyFilters() {
        mapView.annotations.cleanup()
        plottedConflicts.clear()
        loadConflictData()
    }

    /**
     * updates the watchlist recycler view with conflict items
     */
    private fun updateWatchlistCard(conflicts: List<ConflictEvent>) {
        val recyclerView = findViewById<RecyclerView>(R.id.watchlistRecyclerView)
        recyclerView.adapter = ConflictAdapter(conflicts) { selectedConflict ->
            val intent = Intent(this, ConflictDetailActivity::class.java)
            intent.putExtra("conflict", selectedConflict)
            startActivity(intent)
        }
    }

    /**
     * places a marker on the map for a conflict if not already plotted
     */
    private fun addConflictMarker(conflict: ConflictEvent, isMainConflict: Boolean) {
        // skip if marker already exists for this conflict
        if (plottedConflicts.contains(conflict.event_id_cnty)) {
            return
        }

        val pointAnnotationManager = mapView.annotations.createPointAnnotationManager()

        // select marker drawable based on conflict severity or if main conflict
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

        // set click listener for marker to open conflict detail activity
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

    /**
     * shows a search dialog for conflict lookup by country or event id
     */
    private fun showSearchDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_search)

        val searchEditText = dialog.findViewById<EditText>(R.id.searchEditText)
        val searchButton = dialog.findViewById<Button>(R.id.searchButton)

        searchButton.setOnClickListener {
            val query = searchEditText.text.toString().trim()
            if (query.isNotEmpty()) {
                searchConflict(query)
                dialog.dismiss()
            } else {
                Toast.makeText(this, "please enter a search term", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    /**
     * searches for a conflict and navigates the map to its location if found
     */
    private fun searchConflict(query: String) {
        lifecycleScope.launch {
            try {
                val allConflicts = repository.getTopOngoingConflicts()
                val conflict = allConflicts.find {
                    it.country.contains(query, ignoreCase = true) || it.event_id_cnty.contains(query, ignoreCase = true)
                }

                if (conflict != null) {
                    // move map camera to conflict location
                    mapView.mapboxMap.setCamera(
                        CameraOptions.Builder()
                            .center(Point.fromLngLat(conflict.longitude, conflict.latitude))
                            .zoom(10.0) // adjust zoom level as needed
                            .build()
                    )
                    // optionally add marker for conflict
                    addConflictMarker(conflict, true)
                } else {
                    Toast.makeText(this@MainActivity, "conflict not found", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "error searching for conflict", e)
                Toast.makeText(this@MainActivity, "error searching for conflict", Toast.LENGTH_SHORT).show()
            }
        }
    }
}