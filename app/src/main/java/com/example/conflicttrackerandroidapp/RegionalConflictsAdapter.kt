package com.example.conflicttrackerandroidapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.conflicttrackerandroidapp.api.ConflictEvent

class RegionalConflictsAdapter(
    private val conflicts: List<ConflictEvent>,
    private val onConflictSelected: (ConflictEvent) -> Unit
) : RecyclerView.Adapter<RegionalConflictsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val location: TextView = view.findViewById(R.id.conflictLocation)
        val fatalities: TextView = view.findViewById(R.id.conflictFatalities)
        val date: TextView = view.findViewById(R.id.conflictDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_conflict, parent, false)  // Using existing item_conflict layout
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val conflict = conflicts[position]
        holder.location.text = "${conflict.country} - ${conflict.location}"
        holder.fatalities.text = "${conflict.fatalities} casualties"
        holder.date.text = conflict.event_date

        holder.itemView.setOnClickListener {
            onConflictSelected(conflict)
        }
    }

    override fun getItemCount() = conflicts.size
}