package com.sahlent.data.model

/**
 * Represents a single prayer with its name and time
 */
data class Prayer(
    val name: PrayerName,
    val time: String, // HH:mm format
    val timeInMillis: Long
)

enum class PrayerName {
    FAJR,
    SUNRISE,
    DHUHR,
    ASR,
    MAGHRIB,
    ISHA
}

/**
 * All prayer times for a day
 */
data class DailyPrayers(
    val fajr: Prayer,
    val sunrise: Prayer,
    val dhuhr: Prayer,
    val asr: Prayer,
    val maghrib: Prayer,
    val isha: Prayer,
    val date: String,
    val hijriDate: String,
    val locationName: String? = null
) {
    fun asList(): List<Prayer> = listOf(fajr, sunrise, dhuhr, asr, maghrib, isha)
    
    fun getNextPrayer(currentTimeMillis: Long): Prayer? {
        return asList()
            .filter { it.name != PrayerName.SUNRISE } // Sunrise is not a prayer
            .firstOrNull { it.timeInMillis > currentTimeMillis }
    }
    
    fun getCurrentOrNextPrayer(currentTimeMillis: Long): Prayer? {
        val prayers = asList().filter { it.name != PrayerName.SUNRISE }
        return prayers.firstOrNull { it.timeInMillis > currentTimeMillis }
            ?: prayers.lastOrNull() // If all prayers passed, return Isha
    }
}

/**
 * Calculation method for prayer times
 */
enum class CalculationMethod(val id: Int, val displayName: String) {
    KARACHI(1, "University of Karachi"),
    ISNA(2, "ISNA (North America)"),
    MWL(3, "Muslim World League"),
    MAKKAH(4, "Umm al-Qura (Makkah)"),
    EGYPT(5, "Egyptian General Authority"),
    TEHRAN(7, "Institute of Geophysics, Tehran"),
    GULF(8, "Gulf Region"),
    KUWAIT(9, "Kuwait"),
    QATAR(10, "Qatar"),
    SINGAPORE(11, "Singapore"),
    FRANCE(12, "UOIF (France)"),
    TURKEY(13, "Diyanet (Turkey)"),
    RUSSIA(14, "Russia"),
    MOONSIGHTING(15, "Moonsighting Committee");
    
    companion object {
        fun fromId(id: Int): CalculationMethod = entries.find { it.id == id } ?: MWL
    }
}

/**
 * Juristic method for Asr calculation (School of thought)
 * This affects the Asr prayer time specifically
 */
enum class Madhab(val id: Int, val displayName: String, val description: String) {
    /**
     * Standard method used by Shafi'i, Maliki, and Hanbali schools
     * Asr begins when shadow of an object equals its length
     */
    SHAFII(0, "Shafi'i / Ahl Hadith", "Earlier Asr (shadow = object length)"),
    
    /**
     * Hanafi method - later Asr time
     * Asr begins when shadow of an object equals twice its length
     */
    HANAFI(1, "Hanafi", "Later Asr (shadow = 2× object length)");
    
    companion object {
        fun fromId(id: Int): Madhab = entries.find { it.id == id } ?: SHAFII
    }
}

