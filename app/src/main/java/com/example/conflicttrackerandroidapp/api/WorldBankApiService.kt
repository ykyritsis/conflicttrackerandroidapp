package com.example.conflicttrackerandroidapp.api

import retrofit2.http.GET
import retrofit2.http.Path

// retrofit interface for world bank api endpoints
interface WorldBankApiService {

    // fetches population data for the specified country code
    @GET("v2/country/{countryCode}/indicator/SP.POP.TOTL?format=json&per_page=1")
    suspend fun getPopulation(@Path("countryCode") countryCode: String): List<Any>

    // fetches gdp data for the specified country code
    @GET("v2/country/{countryCode}/indicator/NY.GDP.MKTP.CD?format=json&per_page=1")
    suspend fun getGDP(@Path("countryCode") countryCode: String): List<Any>

    // fetches military expenditure data for the specified country code
    @GET("v2/country/{countryCode}/indicator/MS.MIL.XPND.CD?format=json&per_page=1")
    suspend fun getMilitaryExpenditure(@Path("countryCode") countryCode: String): List<Any>

    // fetches a list of countries
    @GET("v2/countries?format=json&per_page=300")
    suspend fun getCountries(): List<Any>
}