package com.example.conflicttrackerandroidapp.api

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

data class AcledResponse(
    val status: Int,
    val data: List<ConflictEvent>
)

@Parcelize
data class ConflictEvent(
    val event_id_cnty: String,
    val event_date: String,
    val year: Int,
    val event_type: String,
    val actor1: String,
    val country: String,
    val region: String,
    val location: String,
    val latitude: Double,
    val longitude: Double,
    val source: String,
    val notes: String,
    val fatalities: Int
) : Parcelable