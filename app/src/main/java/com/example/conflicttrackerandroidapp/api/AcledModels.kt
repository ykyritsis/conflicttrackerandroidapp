package com.example.conflicttrackerandroidapp.api

data class AcledResponse(
    val status: Int,
    val data: List<ConflictEvent>
)

data class ConflictEvent(
    val event_id_cnty: String,
    val event_date: String,
    val event_type: String,
    val actor1: String,
    val country: String,
    val fatalities: Int,
    val notes: String
)