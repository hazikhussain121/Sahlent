package com.sahlent.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.os.Build
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class LocationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // location stuff - uses google play services
    // dealing with location permissions is always a pain
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    
    @SuppressLint("MissingPermission")
    suspend fun getLastLocation(): Location? {
        return suspendCancellableCoroutine { continuation ->
            val cancellationTokenSource = CancellationTokenSource()
            
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                cancellationTokenSource.token
            ).addOnSuccessListener { location ->
                continuation.resume(location)
            }.addOnFailureListener {
                // current location failed, try last known as fallback
                // this happens sometimes when gps is off
                fusedLocationClient.lastLocation.addOnSuccessListener { lastLocation ->
                    continuation.resume(lastLocation)
                }.addOnFailureListener {
                    // welp, no location at all
                    continuation.resume(null)
                }
            }
            
            continuation.invokeOnCancellation {
                cancellationTokenSource.cancel()
            }
        }
    }
    
    @Suppress("DEPRECATION")
    fun getLocationName(latitude: Double, longitude: Double): String? {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                var result: String? = null
                geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                    result = addresses.firstOrNull()?.let { address ->
                        address.locality ?: address.subAdminArea ?: address.adminArea ?: address.countryName
                    }
                }
                result
            } else {
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                addresses?.firstOrNull()?.let { address ->
                    address.locality ?: address.subAdminArea ?: address.adminArea ?: address.countryName
                }
            }
        } catch (e: Exception) {
            // geocoder can fail for various reasons, just return null
            null
        }
    }
}
