package com.example.conflicttrackerandroidapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.conflicttrackerandroidapp.api.ConflictEvent

// recycler view adapter for displaying conflict events in a list
class ConflictAdapter(
    private val conflicts: List<ConflictEvent>,
    private val onConflictClick: (ConflictEvent) -> Unit
) : RecyclerView.Adapter<ConflictAdapter.ViewHolder>() {

    // view holder for a single conflict item view
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

    // binds conflict data to the view holder
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val conflict = conflicts[position]
        holder.location.text = "${conflict.country} - ${conflict.location}"
        holder.fatalities.text = "${conflict.fatalities} Casualties"
        holder.date.text = "Updated: ${conflict.event_date}"

        holder.itemView.setOnClickListener { onConflictClick(conflict) }
    }

    override fun getItemCount() = conflicts.size
}