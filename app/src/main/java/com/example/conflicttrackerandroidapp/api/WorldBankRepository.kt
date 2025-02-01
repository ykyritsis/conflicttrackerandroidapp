package com.example.conflicttrackerandroidapp.api

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// repository for interacting with the world bank api
class WorldBankRepository {
    private val TAG = "WorldBankRepository"
    private val gson = Gson()
    private var countryCodeMap: Map<String, String> = emptyMap()

    private val worldBankApi = Retrofit.Builder()
        .baseUrl("https://api.worldbank.org/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(WorldBankApiService::class.java)

    // initialise and cache country codes from the world bank api
    suspend fun initializeCountryCodes() {
        try {
            val response = worldBankApi.getCountries()
            countryCodeMap = extractCountryCodes(response)
            Log.d(TAG, "initialized ${countryCodeMap.size} country codes")
        } catch (e: Exception) {
            Log.e(TAG, "error initializing country codes: ${e.message}")
        }
    }

    // extract a map of country name to country code from the api response
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
            Log.e(TAG, "error extracting country codes: ${e.message}")
            emptyMap()
        }
    }

    // get stats for a given country by its name
    suspend fun getCountryStats(country: String): CountryStats? {
        // ensure country codes are initialized
        if (countryCodeMap.isEmpty()) {
            initializeCountryCodes()
        }

        val countryCode = countryCodeMap[country]
        if (countryCode == null) {
            Log.d(TAG, "no country code found for: $country")
            return CountryStats(
                population = null,
                gdp = null,
                militaryExpenditure = null,
                note = "data not available for this country"
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
                            "no current data available for this country"
                        population == null || gdp == null || military == null ->
                            "some data may be from previous years or unavailable"
                        else ->
                            "data available from world bank"
                    }
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "error getting country stats for $country: ${e.message}")
            CountryStats(
                population = null,
                gdp = null,
                militaryExpenditure = null,
                note = "error fetching country data"
            )
        }
    }

    // safely executes an api call and extracts a double value from the response
    private suspend fun safeApiCall(apiCall: suspend () -> List<Any>): Double? {
        return try {
            val response = apiCall()
            extractValue(response)
        } catch (e: Exception) {
            Log.e(TAG, "api call failed: ${e.message}")
            null
        }
    }

    // extract the value from the api response json structure
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
            Log.e(TAG, "error extracting value: ${e.message}")
            null
        }
    }
}