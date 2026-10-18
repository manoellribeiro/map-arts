package manoellribeiro.dev.martp.core.services.sensors

import android.content.Context
import android.hardware.Sensor

class GravitySensor(
    context: Context
): AndroidSensor(
    context = context,
    sensorFeature = null,
    sensorType = Sensor.TYPE_GRAVITY
)