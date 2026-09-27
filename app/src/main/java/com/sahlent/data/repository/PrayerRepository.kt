package com.sahlent.data.repository

import com.sahlent.data.api.AladhanApi
import com.sahlent.data.api.models.PrayerTimings
import com.sahlent.data.model.CalculationMethod
import com.sahlent.data.model.DailyPrayers
import com.sahlent.data.model.Madhab
import com.sahlent.data.model.Prayer
import com.sahlent.data.model.PrayerName
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrayerRepository @Inject constructor(
    private val api: AladhanApi
) {
    // simple in-memory cache for prayer times
    // TODO: add proper caching with Room later if needed
    private var cachedPrayers: DailyPrayers? = null
    private var cachedDate: String? = null
    private var cachedLocation: Pair<Double, Double>? = null
    
    suspend fun getPrayerTimes(
        latitude: Double,
        longitude: Double,
        method: CalculationMethod = CalculationMethod.MWL,
        madhab: Madhab = Madhab.SHAFII
    ): Result<DailyPrayers> {
        return try {
            val today = SimpleDateFormat("dd-MM-yyyy", Locale.US).format(System.currentTimeMillis())
            
            // Return cached if same day and location
            if (cachedDate == today && 
                cachedLocation?.first == latitude && 
                cachedLocation?.second == longitude) {
                cachedPrayers?.let { return Result.success(it) }
            }
            
            val timestamp = System.currentTimeMillis() / 1000
            val response = api.getPrayerTimes(timestamp, latitude, longitude, method.id, madhab.id)
            
            if (response.code == 200) {
                // Log.d("PrayerRepo", "API response: ${response.data}")
                val prayers = mapToDailyPrayers(response.data.timings, response.data.date.readable, response.data.date.hijri.date)
                cachedPrayers = prayers
                cachedDate = today
                cachedLocation = latitude to longitude
                Result.success(prayers)
            } else {
                // api error, log it
                Result.failure(Exception("API error: ${response.status}"))
            }
        } catch (e: Exception) {
            // network error probably
            Result.failure(e)
        }
    }
    
    suspend fun getQiblaDirection(latitude: Double, longitude: Double): Result<Double> {
        return try {
            val response = api.getQiblaDirection(latitude, longitude)
            if (response.code == 200) {
                Result.success(response.data.direction)
            } else {
                Result.failure(Exception("API error: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun mapToDailyPrayers(timings: PrayerTimings, date: String, hijriDate: String): DailyPrayers {
        return DailyPrayers(
            fajr = createPrayer(PrayerName.FAJR, timings.fajr),
            sunrise = createPrayer(PrayerName.SUNRISE, timings.sunrise),
            dhuhr = createPrayer(PrayerName.DHUHR, timings.dhuhr),
            asr = createPrayer(PrayerName.ASR, timings.asr),
            maghrib = createPrayer(PrayerName.MAGHRIB, timings.maghrib),
            isha = createPrayer(PrayerName.ISHA, timings.isha),
            date = date,
            hijriDate = hijriDate
        )
    }
    
    private fun createPrayer(name: PrayerName, timeString: String): Prayer {
        // Time comes as "HH:mm (TZ)" - extract just HH:mm
        val time = timeString.split(" ").first()
        val parts = time.split(":")
        // idk why aladhan sometimes sends weird formats but this handles it
        val hour = parts[0].toIntOrNull() ?: 0
        val minute = parts[1].toIntOrNull() ?: 0
        
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        return Prayer(
            name = name,
            time = time,
            timeInMillis = calendar.timeInMillis
        )
    }
}
