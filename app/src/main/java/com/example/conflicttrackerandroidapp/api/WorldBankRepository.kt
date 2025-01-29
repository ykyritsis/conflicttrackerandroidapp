package com.example.conflicttrackerandroidapp.api

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class WorldBankRepository {
    private val TAG = "WorldBankRepository"
    private val gson = Gson()
    private var countryCodeMap: Map<String, String> = emptyMap()

    private val worldBankApi = Retrofit.Builder()
        .baseUrl("https://api.worldbank.org/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(WorldBankApiService::class.java)

    suspend fun initializeCountryCodes() {
        try {
            val response = worldBankApi.getCountries()
            countryCodeMap = extractCountryCodes(response)
            Log.d(TAG, "Initialized ${countryCodeMap.size} country codes")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing country codes: ${e.message}")
        }
    }

    private fun extractCountryCodes(response: List<Any>): Map<String, String> {
        return try {
            val jsonArray = gson.toJsonTree(response).asJsonArray
            if (jsonArray.size() > 1) {
                val countriesArray = jsonArray[1].asJsonArray
                countriesArray.associate { country ->
                    val countryObj = country.asJsonObject
                    val name = countryObj.get("name").asString
                    val code = countryObj.get("id").asString
                    name to code
                }
            } else {
                emptyMap()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting country codes: ${e.message}")
            emptyMap()
        }
    }

    suspend fun getCountryStats(country: String): CountryStats? {
        // Initialize country codes if not already done
        if (countryCodeMap.isEmpty()) {
            initializeCountryCodes()
        }

        val countryCode = countryCodeMap[country]
        if (countryCode == null) {
            Log.d(TAG, "No country code found for: $country")
            return CountryStats(
                population = null,
                gdp = null,
                militaryExpenditure = null,
                note = "Data not available for this country"
            )
        }

        return try {
            coroutineScope {
                val populationDeferred = async { safeApiCall { worldBankApi.getPopulation(countryCode) } }
                val gdpDeferred = async { safeApiCall { worldBankApi.getGDP(countryCode) } }
                val militaryDeferred = async { safeApiCall { worldBankApi.getMilitaryExpenditure(countryCode) } }

                val population = populationDeferred.await()
                val gdp = gdpDeferred.await()
                val military = militaryDeferred.await()

                CountryStats(
                    population = population,
                    gdp = gdp,
                    militaryExpenditure = military,
                    note = when {
                        population == null && gdp == null && military == null ->
                            "No current data available for this country"
                        population == null || gdp == null || military == null ->
                            "Some data may be from previous years or unavailable"
                        else ->
                            "Data available from World Bank"
                    }
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting country stats for $country: ${e.message}")
            CountryStats(
                population = null,
                gdp = null,
                militaryExpenditure = null,
                note = "Error fetching country data"
            )
        }
    }

    private suspend fun safeApiCall(apiCall: suspend () -> List<Any>): Double? {
        return try {
            val response = apiCall()
            extractValue(response)
        } catch (e: Exception) {
            Log.e(TAG, "API call failed: ${e.message}")
            null
        }
    }

    private fun extractValue(response: List<Any>): Double? {
        return try {
            val jsonArray = gson.toJsonTree(response).asJsonArray
            if (jsonArray.size() > 1) {
                val dataArray = jsonArray[1].asJsonArray
                if (dataArray.size() > 0) {
                    val firstElement = dataArray[0].asJsonObject
                    if (firstElement.has("value") && !firstElement.get("value").isJsonNull) {
                        firstElement.get("value").asDouble
                    } else null
                } else null
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting value: ${e.message}")
            null
        }
    }
}