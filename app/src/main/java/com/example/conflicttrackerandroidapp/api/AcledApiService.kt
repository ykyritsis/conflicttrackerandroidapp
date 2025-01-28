package com.example.conflicttrackerandroidapp.api

import retrofit2.http.GET
import retrofit2.http.Query

interface AcledApiService {
    @GET("acled/read")
    suspend fun getConflicts(
        @Query("key") apiKey: String = "2rXDfX!lOs4q9wlw2ICG",
        @Query("email") email: String = "103980370@student.swin.edu.au",
        @Query("limit") limit: Int = 10,  // Start with small limit for testing
        @Query("event_date_where") dateWhere: String = "BETWEEN",
        @Query("event_date") dateRange: String
    ): AcledResponse
}