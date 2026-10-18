package manoellribeiro.dev.martp.core.services.sensors

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LightSensorTest {

    private lateinit var context: Context
    private lateinit var packageManager: PackageManager
    private lateinit var sensorManager: SensorManager
    private lateinit var lightSensor: LightSensor

    @BeforeEach
    fun setup() {
        context = mockk()
        packageManager = mockk()
        sensorManager = mockk()

        lightSensor = LightSensor(context = context)
    }

    private fun createFakeSensor(type: Int): Sensor {
        val fakeSensor = mockk<Sensor>()
        every { fakeSensor.type } returns type
        return fakeSensor
    }

    // SensorEvent.values is a final field and the SensorEvent constructor is not available in
    // unit tests, so the values are written through reflection
    private fun createFakeSensorEvent(sensor: Sensor?, values: FloatArray): SensorEvent {
        val fakeEvent = mockk<SensorEvent>()
        fakeEvent.sensor = sensor
        SensorEvent::class.java.getField("values").apply { isAccessible = true }.set(fakeEvent, values)
        return fakeEvent
    }

    // region isSensorAvailable

    @Test
    fun `if the device has the light sensor feature should report the sensor as available without asking the sensor manager`() {
        // Given
        every { context.packageManager } returns packageManager
        every { packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_LIGHT) } returns true

        // When
        val result = lightSensor.isSensorAvailable

        // Then
        assertTrue(result)
        verify(exactly = 1) { packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_LIGHT) }
        verify(exactly = 0) { context.getSystemService(SensorManager::class.java) }
    }

    @Test
    fun `if the device does not have the light sensor feature should report the sensor as unavailable`() {
        // Given
        every { context.packageManager } returns packageManager
        every { packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_LIGHT) } returns false

        // When
        val result = lightSensor.isSensorAvailable

        // Then
        assertFalse(result)
    }

    // endregion

    // region startListening

    @Test
    fun `if the light sensor is available should register the listener with the normal delay`() {
        // Given
        val fakeSensor = createFakeSensor(Sensor.TYPE_LIGHT)
        every { context.packageManager } returns packageManager
        every { packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_LIGHT) } returns true
        every { context.getSystemService(SensorManager::class.java) } returns sensorManager
        every { sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) } returns fakeSensor
        every { sensorManager.registerListener(any(), any<Sensor>(), any()) } returns true

        // When
        lightSensor.startListening()

        // Then
        verify(exactly = 1) { context.getSystemService(SensorManager::class.java) }
        verify(exactly = 1) { sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) }
        verify(exactly = 1) {
            sensorManager.registerListener(lightSensor, fakeSensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    @Test
    fun `if the light sensor feature is missing should not ask the sensor manager nor register the listener`() {
        // Given
        every { context.packageManager } returns packageManager
        every { packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_LIGHT) } returns false

        // When
        lightSensor.startListening()

        // Then
        verify(exactly = 0) { context.getSystemService(SensorManager::class.java) }
    }

    @Test
    fun `if the feature exists but the sensor manager has no light sensor should not register the listener`() {
        // Given
        every { context.packageManager } returns packageManager
        every { packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_LIGHT) } returns true
        every { context.getSystemService(SensorManager::class.java) } returns sensorManager
        every { sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) } returns null

        // When
        lightSensor.startListening()

        // Then
        verify(exactly = 0) { sensorManager.registerListener(any(), any<Sensor>(), any()) }
    }

    @Test
    fun `if startListening is called twice should get the sensor manager only once`() {
        // Given
        val fakeSensor = createFakeSensor(Sensor.TYPE_LIGHT)
        every { context.packageManager } returns packageManager
        every { packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_LIGHT) } returns true
        every { context.getSystemService(SensorManager::class.java) } returns sensorManager
        every { sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) } returns fakeSensor
        every { sensorManager.registerListener(any(), any<Sensor>(), any()) } returns true

        // When
        lightSensor.startListening()
        lightSensor.startListening()

        // Then
        verify(exactly = 1) { context.getSystemService(SensorManager::class.java) }
        verify(exactly = 1) { sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) }
    }

    // endregion

    // region stopListening

    @Test
    fun `if the light sensor was listening should unregister the listener`() {
        // Given
        val fakeSensor = createFakeSensor(Sensor.TYPE_LIGHT)
        every { context.packageManager } returns packageManager
        every { packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_LIGHT) } returns true
        every { context.getSystemService(SensorManager::class.java) } returns sensorManager
        every { sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) } returns fakeSensor
        every { sensorManager.registerListener(any(), any<Sensor>(), any()) } returns true
        every { sensorManager.unregisterListener(any<SensorEventListener>()) } just runs
        lightSensor.startListening()

        // When
        lightSensor.stopListening()

        // Then
        verify(exactly = 1) { sensorManager.unregisterListener(lightSensor) }
    }

    @Test
    fun `if the light sensor never started listening should not unregister the listener`() {
        // Given
        every { context.packageManager } returns packageManager
        every { packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_LIGHT) } returns true

        // When
        lightSensor.stopListening()

        // Then
        verify(exactly = 0) { sensorManager.unregisterListener(any<SensorEventListener>()) }
        verify(exactly = 0) { context.getSystemService(SensorManager::class.java) }
    }

    @Test
    fun `if the light sensor feature is missing should not unregister the listener`() {
        // Given
        every { context.packageManager } returns packageManager
        every { packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_LIGHT) } returns false

        // When
        lightSensor.stopListening()

        // Then
        verify(exactly = 0) { sensorManager.unregisterListener(any<SensorEventListener>()) }
    }

    // endregion

    // region onSensorChanged

    @Test
    fun `if a light event arrives should send its values to the listener`() {
        // Given
        val fakeSensor = createFakeSensor(Sensor.TYPE_LIGHT)
        val fakeEvent = createFakeSensorEvent(sensor = fakeSensor, values = floatArrayOf(321.5F))
        val fakeReceivedValues = mutableListOf<List<Float>>()
        every { context.packageManager } returns packageManager
        every { packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_LIGHT) } returns true
        lightSensor.setOnSensorValuesChangedListener { values -> fakeReceivedValues.add(values) }

        // When
        lightSensor.onSensorChanged(fakeEvent)

        // Then
        assertEquals(listOf(listOf(321.5F)), fakeReceivedValues)
    }

    @Test
    fun `if an event of another sensor type arrives should not call the light listener`() {
        // Given
        val fakeGravitySensor = createFakeSensor(Sensor.TYPE_GRAVITY)
        val fakeEvent = createFakeSensorEvent(sensor = fakeGravitySensor, values = floatArrayOf(0F, 9.8F, 0F))
        val fakeReceivedValues = mutableListOf<List<Float>>()
        every { context.packageManager } returns packageManager
        every { packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_LIGHT) } returns true
        lightSensor.setOnSensorValuesChangedListener { values -> fakeReceivedValues.add(values) }

        // When
        lightSensor.onSensorChanged(fakeEvent)

        // Then
        assertTrue(fakeReceivedValues.isEmpty())
    }

    @Test
    fun `if the light sensor feature is missing should not call the listener`() {
        // Given
        val fakeSensor = createFakeSensor(Sensor.TYPE_LIGHT)
        val fakeEvent = createFakeSensorEvent(sensor = fakeSensor, values = floatArrayOf(50F))
        val fakeReceivedValues = mutableListOf<List<Float>>()
        every { context.packageManager } returns packageManager
        every { packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_LIGHT) } returns false
        lightSensor.setOnSensorValuesChangedListener { values -> fakeReceivedValues.add(values) }

        // When
        lightSensor.onSensorChanged(fakeEvent)

        // Then
        assertTrue(fakeReceivedValues.isEmpty())
    }

    // endregion
}
