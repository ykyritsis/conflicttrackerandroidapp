package com.example.conflicttrackerandroidapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.conflicttrackerandroidapp.api.ConflictEvent

class ConflictAdapter(
    private val conflicts: List<ConflictEvent>
) : RecyclerView.Adapter<ConflictAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val location: TextView = view.findViewById(R.id.conflictLocation)
        val fatalities: TextView = view.findViewById(R.id.conflictFatalities)
        val date: TextView = view.findViewById(R.id.conflictDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_conflict, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val conflict = conflicts[position]
        holder.location.text = "${conflict.country} - ${conflict.event_type}"
        holder.fatalities.text = "${conflict.fatalities} casualties"
        holder.date.text = "Date: ${conflict.event_date}"
    }

    override fun getItemCount() = conflicts.size
}