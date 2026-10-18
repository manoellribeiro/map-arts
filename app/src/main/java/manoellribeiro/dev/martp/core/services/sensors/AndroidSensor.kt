package manoellribeiro.dev.martp.core.services.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

abstract class AndroidSensor(
    private val context: Context,
    private val sensorFeature: String?,
    sensorType: Int
): MeasurableSensor(sensorType), SensorEventListener {

    override val isSensorAvailable: Boolean
        get() {
            if(sensorFeature != null) {
                return context.packageManager.hasSystemFeature(sensorFeature)
            } else {
                sensorManager = context.getSystemService(SensorManager::class.java)
                sensor = sensorManager.getDefaultSensor(sensorType)
                return sensor != null
            }
        }

    private lateinit var sensorManager: SensorManager
    private var sensor: Sensor? = null

    override fun startListening() {
        if(!isSensorAvailable) {
            return
        }

        if(!::sensorManager.isInitialized && sensor == null) {
            sensorManager = context.getSystemService(SensorManager::class.java)
            sensor = sensorManager.getDefaultSensor(sensorType)
        }

        sensor?.let {
            sensorManager.registerListener(
                this, it, SensorManager.SENSOR_DELAY_NORMAL
            )
        }

    }

    override fun stopListening() {
        if(!isSensorAvailable || !::sensorManager.isInitialized) {
            return
        }

        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if(!isSensorAvailable) {
            return
        }

        if(event?.sensor?.type == sensorType) {
            onSensorValuesChanged?.invoke(event.values.toList())
        }

    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) = Unit
}