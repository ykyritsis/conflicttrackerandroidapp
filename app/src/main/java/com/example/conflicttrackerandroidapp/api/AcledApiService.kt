package com.example.conflicttrackerandroidapp.api

import retrofit2.http.GET
import retrofit2.http.Query

// retrofit interface for acled api
interface AcledApiService {

    // fetches conflict data; requires a result limit and a date range while other parameters use defaults.
    @GET("acled/read")
    suspend fun getConflicts(
        @Query("key") apiKey: String = "2rXDfX!lOs4q9wlw2ICG",   // api key for authentication
        @Query("email") email: String = "103980370@student.swin.edu.au",  // contact email for tracking
        @Query("limit") limit: Int,  // number of results per request
        @Query("page") page: Int = DEFAULT_PAGE,  // current page for pagination
        @Query("event_date_where") dateWhere: String = "BETWEEN",  // filter condition for date
        @Query("event_date") dateRange: String  // specific date range for events
    ): AcledResponse

    companion object {
        // max results per page as per api limitations
        const val MAX_PAGE_SIZE = 500
        // default page number for pagination
        const val DEFAULT_PAGE = 1
    }
}