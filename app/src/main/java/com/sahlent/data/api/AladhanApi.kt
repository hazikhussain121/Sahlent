package com.sahlent.data.api

import com.sahlent.data.api.models.PrayerTimesResponse
import com.sahlent.data.api.models.QiblaResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface AladhanApi {
    
    /**
     * Get prayer times for a specific date and location
     * 
     * @param timestamp Unix timestamp or date in DD-MM-YYYY format
     * @param latitude Latitude of the location
     * @param longitude Longitude of the location
     * @param method Calculation method (1-15, default 2 = ISNA)
     *   1 - University of Islamic Sciences, Karachi
     *   2 - Islamic Society of North America (ISNA)
     *   3 - Muslim World League
     *   4 - Umm Al-Qura University, Makkah
     *   5 - Egyptian General Authority of Survey
     *   7 - Institute of Geophysics, University of Tehran
     *   8 - Gulf Region
     *   9 - Kuwait
     *   10 - Qatar
     *   11 - Majlis Ugama Islam Singapura
     *   12 - Union Organization islamic de France
     *   13 - Diyanet İşleri Başkanlığı, Turkey
     *   14 - Spiritual Administration of Muslims of Russia
     *   15 - Moonsighting Committee Worldwide
     * @param school Juristic method for Asr calculation:
     *   0 - Shafi'i, Maliki, Hanbali (standard - Asr when shadow = object length)
     *   1 - Hanafi (Asr when shadow = 2x object length, later Asr time)
     */
    @GET("v1/timings/{timestamp}")
    suspend fun getPrayerTimes(
        @Path("timestamp") timestamp: Long,
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("method") method: Int = 3, // Default to Muslim World League
        @Query("school") school: Int = 0 // Default to Shafi'i/Standard
    ): PrayerTimesResponse
    
    /**
     * Get Qibla direction for a location
     */
    @GET("v1/qibla/{latitude}/{longitude}")
    suspend fun getQiblaDirection(
        @Path("latitude") latitude: Double,
        @Path("longitude") longitude: Double
    ): QiblaResponse
}
