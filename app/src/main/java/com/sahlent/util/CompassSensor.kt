package com.sahlent.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class CompassSensor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val sensorManager: SensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    
    val isAvailable: Boolean
        get() = accelerometer != null && magnetometer != null
    
    /**
     * Get compass bearing as a Flow of degrees (0-360)
     * 0 = North, 90 = East, 180 = South, 270 = West
     */
    fun getBearing(): Flow<Float> = callbackFlow {
        var gravity: FloatArray? = null
        var geomagnetic: FloatArray? = null
        
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> {
                        gravity = lowPassFilter(event.values.clone(), gravity)
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        geomagnetic = lowPassFilter(event.values.clone(), geomagnetic)
                    }
                }
                
                val g = gravity
                val m = geomagnetic
                
                if (g != null && m != null) {
                    val rotationMatrix = FloatArray(9)
                    val inclinationMatrix = FloatArray(9)
                    
                    if (SensorManager.getRotationMatrix(rotationMatrix, inclinationMatrix, g, m)) {
                        val orientation = FloatArray(3)
                        SensorManager.getOrientation(rotationMatrix, orientation)
                        
                        // Convert radians to degrees
                        var azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
                        
                        // Normalize to 0-360
                        azimuth = (azimuth + 360) % 360
                        
                        trySend(azimuth)
                    }
                }
            }
            
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        
        accelerometer?.let {
            sensorManager.registerListener(
                listener,
                it,
                SensorManager.SENSOR_DELAY_UI
            )
        }
        
        magnetometer?.let {
            sensorManager.registerListener(
                listener,
                it,
                SensorManager.SENSOR_DELAY_UI
            )
        }
        
        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
    
    /**
     * Low-pass filter to smooth sensor data
     */
    private fun lowPassFilter(input: FloatArray, output: FloatArray?): FloatArray {
        if (output == null) return input
        
        val alpha = 0.15f
        for (i in input.indices) {
            output[i] = output[i] + alpha * (input[i] - output[i])
        }
        return output
    }
    
    companion object {
        // Kaaba coordinates
        const val KAABA_LATITUDE = 21.4225
        const val KAABA_LONGITUDE = 39.8262
        
        /**
         * Calculate Qibla direction from a given location
         * Returns bearing in degrees (0-360)
         */
        fun calculateQiblaDirection(latitude: Double, longitude: Double): Double {
            val lat1 = Math.toRadians(latitude)
            val lon1 = Math.toRadians(longitude)
            val lat2 = Math.toRadians(KAABA_LATITUDE)
            val lon2 = Math.toRadians(KAABA_LONGITUDE)
            
            val deltaLon = lon2 - lon1
            
            val y = kotlin.math.sin(deltaLon) * kotlin.math.cos(lat2)
            val x = kotlin.math.cos(lat1) * kotlin.math.sin(lat2) -
                    kotlin.math.sin(lat1) * kotlin.math.cos(lat2) * kotlin.math.cos(deltaLon)
            
            var bearing = Math.toDegrees(kotlin.math.atan2(y, x))
            bearing = (bearing + 360) % 360
            
            return bearing
        }
    }
}
