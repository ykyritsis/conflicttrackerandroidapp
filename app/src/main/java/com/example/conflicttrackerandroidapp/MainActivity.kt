package com.example.conflicttrackerandroidapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.android.material.card.MaterialCardView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Find our cards by their IDs
        val mapCard = findViewById<MaterialCardView>(R.id.mapCard)
        val escalatingCard = findViewById<MaterialCardView>(R.id.escalatingCard)
        val watchlistCard = findViewById<MaterialCardView>(R.id.watchlistCard)

        // Set click listeners for each card
        mapCard.setOnClickListener {
            Toast.makeText(this, "Map View Clicked", Toast.LENGTH_SHORT).show()
        }

        escalatingCard.setOnClickListener {
            Toast.makeText(this, "Escalating Conflicts Clicked", Toast.LENGTH_SHORT).show()
        }

        watchlistCard.setOnClickListener {
            Toast.makeText(this, "Conflict Watchlist Clicked", Toast.LENGTH_SHORT).show()
        }
    }
}