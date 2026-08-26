package com.example.muslimvn.data.sensor

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

@Singleton
class CompassSensorManager @Inject constructor(
    private val sensorManager: SensorManager
) {

    private val rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magneticFieldSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    data class CompassData(
        val azimuth: Float,
        val accuracy: Int
    )

    private var _compassDataFlow = callbackFlow<CompassData> {
        val listener = object : SensorEventListener {
            private var lastAzimuth = 0f
            private val alpha = 0.15f // Smoothing factor
            private var currentAccuracy = SensorManager.SENSOR_STATUS_ACCURACY_LOW

            // For fallback
            private var gravity = FloatArray(3)
            private var geomagnetic = FloatArray(3)
            private var hasGravity = false
            private var hasGeomagnetic = false

            override fun onSensorChanged(event: SensorEvent) {
                var azimuth = 0f

                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    val rotationMatrix = FloatArray(9)
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(rotationMatrix, orientation)
                    azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
                } else {
                    if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                        gravity = event.values.clone()
                        hasGravity = true
                    } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                        geomagnetic = event.values.clone()
                        hasGeomagnetic = true
                    }

                    if (hasGravity && hasGeomagnetic) {
                        val r = FloatArray(9)
                        val i = FloatArray(9)
                        if (SensorManager.getRotationMatrix(r, i, gravity, geomagnetic)) {
                            val orientation = FloatArray(3)
                            SensorManager.getOrientation(r, orientation)
                            azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
                        }
                    } else {
                        return
                    }
                }

                // Normalize to 0-360
                azimuth = (azimuth + 360) % 360

                // Smooth the transition (handle wrap-around)
                val smoothedAzimuth = smoothAzimuth(lastAzimuth, azimuth, alpha)
                lastAzimuth = smoothedAzimuth
                
                trySend(CompassData(smoothedAzimuth, currentAccuracy))
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                if (sensor?.type == Sensor.TYPE_ROTATION_VECTOR || 
                    sensor?.type == Sensor.TYPE_MAGNETIC_FIELD) {
                    currentAccuracy = accuracy
                }
            }
        }

        if (rotationVectorSensor != null) {
            sensorManager.registerListener(listener, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            sensorManager.registerListener(listener, accelerometerSensor, SensorManager.SENSOR_DELAY_UI)
            sensorManager.registerListener(listener, magneticFieldSensor, SensorManager.SENSOR_DELAY_UI)
        }

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }

    val compassDataFlow: Flow<CompassData> = _compassDataFlow

    /**
     * Exponential smoothing that handles wrap-around from 359 to 0.
     */
    private fun smoothAzimuth(oldAzimuth: Float, newAzimuth: Float, alpha: Float): Float {
        // Use sine and cosine to smooth correctly across the 0/360 boundary
        val oldRad = Math.toRadians(oldAzimuth.toDouble())
        val newRad = Math.toRadians(newAzimuth.toDouble())

        val sinAvg = (1 - alpha) * sin(oldRad) + alpha * sin(newRad)
        val cosAvg = (1 - alpha) * cos(oldRad) + alpha * cos(newRad)

        val result = Math.toDegrees(atan2(sinAvg, cosAvg)).toFloat()
        return (result + 360) % 360
    }
}
