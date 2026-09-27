package com.sahlent.data.api.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PrayerTimesResponse(
    @Json(name = "code") val code: Int,
    @Json(name = "status") val status: String,
    @Json(name = "data") val data: PrayerData
)

@JsonClass(generateAdapter = true)
data class PrayerData(
    @Json(name = "timings") val timings: PrayerTimings,
    @Json(name = "date") val date: DateInfo,
    @Json(name = "meta") val meta: MetaInfo
)

@JsonClass(generateAdapter = true)
data class PrayerTimings(
    @Json(name = "Fajr") val fajr: String,
    @Json(name = "Sunrise") val sunrise: String,
    @Json(name = "Dhuhr") val dhuhr: String,
    @Json(name = "Asr") val asr: String,
    @Json(name = "Maghrib") val maghrib: String,
    @Json(name = "Isha") val isha: String,
    @Json(name = "Imsak") val imsak: String? = null,
    @Json(name = "Midnight") val midnight: String? = null,
    @Json(name = "Firstthird") val firstThird: String? = null,
    @Json(name = "Lastthird") val lastThird: String? = null
)

@JsonClass(generateAdapter = true)
data class DateInfo(
    @Json(name = "readable") val readable: String,
    @Json(name = "timestamp") val timestamp: String,
    @Json(name = "gregorian") val gregorian: GregorianDate,
    @Json(name = "hijri") val hijri: HijriDate
)

@JsonClass(generateAdapter = true)
data class GregorianDate(
    @Json(name = "date") val date: String,
    @Json(name = "day") val day: String,
    @Json(name = "month") val month: MonthInfo,
    @Json(name = "year") val year: String
)

@JsonClass(generateAdapter = true)
data class HijriDate(
    @Json(name = "date") val date: String,
    @Json(name = "day") val day: String,
    @Json(name = "month") val month: MonthInfo,
    @Json(name = "year") val year: String
)

@JsonClass(generateAdapter = true)
data class MonthInfo(
    @Json(name = "number") val number: Int,
    @Json(name = "en") val en: String,
    @Json(name = "ar") val ar: String? = null
)

@JsonClass(generateAdapter = true)
data class MetaInfo(
    @Json(name = "latitude") val latitude: Double,
    @Json(name = "longitude") val longitude: Double,
    @Json(name = "timezone") val timezone: String,
    @Json(name = "method") val method: MethodInfo
)

@JsonClass(generateAdapter = true)
data class MethodInfo(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String
)
