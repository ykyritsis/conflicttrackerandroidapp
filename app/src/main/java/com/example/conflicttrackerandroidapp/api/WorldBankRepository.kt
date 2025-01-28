// api/WorldBankRepository.kt
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

    private val worldBankApi = Retrofit.Builder()
        .baseUrl("https://api.worldbank.org/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(WorldBankApiService::class.java)

    private val countryCodeMap = mapOf(
        "Israel" to "ISR",
        "Palestine" to "PSE",
        "Syria" to "SYR",
        "Yemen" to "YEM",
        "Iraq" to "IRQ",
        "Lebanon" to "LBN",
        "Ukraine" to "UKR",
        "Russia" to "RUS",
        "Belarus" to "BLR"
    )

    private fun extractValue(response: List<Any>): Double? {
        return try {
            val jsonArray = gson.toJsonTree(response[1]).asJsonArray
            if (jsonArray.size() > 0) {
                val firstElement = jsonArray.get(0).asJsonObject
                if (firstElement.has("value")) {
                    firstElement.get("value").asDouble
                } else null
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting value: ${e.message}")
            null
        }
    }

    suspend fun getCountryStats(country: String): CountryStats? {
        val countryCode = countryCodeMap[country]
        if (countryCode == null) {
            Log.d(TAG, "No country code found for: $country")
            return null
        }

        Log.d(TAG, "Fetching data for country: $country ($countryCode)")

        return try {
            coroutineScope {
                val populationDeferred = async {
                    try {
                        val response = worldBankApi.getPopulation(countryCode)
                        Log.d(TAG, "Population raw response: $response")
                        extractValue(response)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error fetching population: ${e.message}")
                        null
                    }
                }

                val gdpDeferred = async {
                    try {
                        val response = worldBankApi.getGDP(countryCode)
                        Log.d(TAG, "GDP raw response: $response")
                        extractValue(response)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error fetching GDP: ${e.message}")
                        null
                    }
                }

                val militaryExpenditureDeferred = async {
                    try {
                        val response = worldBankApi.getMilitaryExpenditure(countryCode)
                        Log.d(TAG, "Military expenditure raw response: $response")
                        extractValue(response)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error fetching military expenditure: ${e.message}")
                        null
                    }
                }

                CountryStats(
                    population = populationDeferred.await(),
                    gdp = gdpDeferred.await(),
                    militaryExpenditure = militaryExpenditureDeferred.await()
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting country stats: ${e.message}")
            null
        }
    }
}