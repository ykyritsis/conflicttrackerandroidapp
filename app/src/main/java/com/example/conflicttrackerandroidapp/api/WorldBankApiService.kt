package com.example.conflicttrackerandroidapp.api

import retrofit2.http.GET
import retrofit2.http.Path

interface WorldBankApiService {
    @GET("v2/country/{countryCode}/indicator/SP.POP.TOTL?format=json")
    suspend fun getPopulation(@Path("countryCode") countryCode: String): List<WorldBankResponse>

    @GET("v2/country/{countryCode}/indicator/NY.GDP.MKTP.CD?format=json")
    suspend fun getGDP(@Path("countryCode") countryCode: String): List<WorldBankResponse>

    @GET("v2/country/{countryCode}/indicator/NY.GDP.PCAP.CD?format=json")
    suspend fun getGDPPerCapita(@Path("countryCode") countryCode: String): List<WorldBankResponse>

    @GET("v2/country/{countryCode}/indicator/SP.DYN.LE00.IN?format=json")
    suspend fun getLifeExpectancy(@Path("countryCode") countryCode: String): List<WorldBankResponse>
}

data class WorldBankResponse(
    val value: Double?,
    val date: String
)