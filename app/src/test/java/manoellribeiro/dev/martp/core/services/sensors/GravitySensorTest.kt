package manoellribeiro.dev.martp.core.services.sensors

import android.content.Context
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
import org.junit.jupiter.api.assertDoesNotThrow

class GravitySensorTest {

    private lateinit var context: Context
    private lateinit var sensorManager: SensorManager
    private lateinit var gravitySensor: GravitySensor

    @BeforeEach
    fun setup() {
        context = mockk()
        sensorManager = mockk()

        gravitySensor = GravitySensor(context = context)
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
    fun `if sensor manager has a gravity sensor should report the sensor as available`() {
        // Given
        val fakeSensor = createFakeSensor(Sensor.TYPE_GRAVITY)
        every { context.getSystemService(SensorManager::class.java) } returns sensorManager
        every { sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY) } returns fakeSensor

        // When
        val result = gravitySensor.isSensorAvailable

        // Then
        assertTrue(result)
        verify(exactly = 1) { sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY) }
    }

    @Test
    fun `if sensor manager has no gravity sensor should report the sensor as unavailable`() {
        // Given
        every { context.getSystemService(SensorManager::class.java) } returns sensorManager
        every { sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY) } returns null

        // When
        val result = gravitySensor.isSensorAvailable

        // Then
        assertFalse(result)
    }

    // endregion

    // region startListening

    @Test
    fun `if the gravity sensor is available should register the listener with the normal delay`() {
        // Given
        val fakeSensor = createFakeSensor(Sensor.TYPE_GRAVITY)
        every { context.getSystemService(SensorManager::class.java) } returns sensorManager
        every { sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY) } returns fakeSensor
        every { sensorManager.registerListener(any(), any<Sensor>(), any()) } returns true

        // When
        gravitySensor.startListening()

        // Then
        verify(exactly = 1) {
            sensorManager.registerListener(gravitySensor, fakeSensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    @Test
    fun `if the gravity sensor is unavailable should not register the listener`() {
        // Given
        every { context.getSystemService(SensorManager::class.java) } returns sensorManager
        every { sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY) } returns null

        // When
        gravitySensor.startListening()

        // Then
        verify(exactly = 0) { sensorManager.registerListener(any(), any<Sensor>(), any()) }
    }

    // endregion

    // region stopListening

    @Test
    fun `if the gravity sensor was listening should unregister the listener`() {
        // Given
        val fakeSensor = createFakeSensor(Sensor.TYPE_GRAVITY)
        every { context.getSystemService(SensorManager::class.java) } returns sensorManager
        every { sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY) } returns fakeSensor
        every { sensorManager.registerListener(any(), any<Sensor>(), any()) } returns true
        every { sensorManager.unregisterListener(any<SensorEventListener>()) } just runs
        gravitySensor.startListening()

        // When
        gravitySensor.stopListening()

        // Then
        verify(exactly = 1) { sensorManager.unregisterListener(gravitySensor) }
    }

    @Test
    fun `if the gravity sensor is unavailable should not unregister the listener`() {
        // Given
        every { context.getSystemService(SensorManager::class.java) } returns sensorManager
        every { sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY) } returns null

        // When
        gravitySensor.stopListening()

        // Then
        verify(exactly = 0) { sensorManager.unregisterListener(any<SensorEventListener>()) }
    }

    // endregion

    // region onSensorChanged

    @Test
    fun `if a gravity event arrives should send its values to the listener`() {
        // Given
        val fakeValues = floatArrayOf(0.5F, 9.7F, -0.25F)
        val fakeSensor = createFakeSensor(Sensor.TYPE_GRAVITY)
        val fakeEvent = createFakeSensorEvent(sensor = fakeSensor, values = fakeValues)
        val fakeReceivedValues = mutableListOf<List<Float>>()
        every { context.getSystemService(SensorManager::class.java) } returns sensorManager
        every { sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY) } returns fakeSensor
        gravitySensor.setOnSensorValuesChangedListener { values -> fakeReceivedValues.add(values) }

        // When
        gravitySensor.onSensorChanged(fakeEvent)

        // Then
        assertEquals(listOf(listOf(0.5F, 9.7F, -0.25F)), fakeReceivedValues)
    }

    @Test
    fun `if an event of another sensor type arrives should not call the listener`() {
        // Given
        val fakeGravitySensor = createFakeSensor(Sensor.TYPE_GRAVITY)
        val fakeLightSensor = createFakeSensor(Sensor.TYPE_LIGHT)
        val fakeEvent = createFakeSensorEvent(sensor = fakeLightSensor, values = floatArrayOf(120F))
        val fakeReceivedValues = mutableListOf<List<Float>>()
        every { context.getSystemService(SensorManager::class.java) } returns sensorManager
        every { sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY) } returns fakeGravitySensor
        gravitySensor.setOnSensorValuesChangedListener { values -> fakeReceivedValues.add(values) }

        // When
        gravitySensor.onSensorChanged(fakeEvent)

        // Then
        assertTrue(fakeReceivedValues.isEmpty())
    }

    @Test
    fun `if a null event arrives should not call the listener`() {
        // Given
        val fakeSensor = createFakeSensor(Sensor.TYPE_GRAVITY)
        val fakeReceivedValues = mutableListOf<List<Float>>()
        every { context.getSystemService(SensorManager::class.java) } returns sensorManager
        every { sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY) } returns fakeSensor
        gravitySensor.setOnSensorValuesChangedListener { values -> fakeReceivedValues.add(values) }

        // When
        gravitySensor.onSensorChanged(null)

        // Then
        assertTrue(fakeReceivedValues.isEmpty())
    }

    @Test
    fun `if the gravity sensor is unavailable should not call the listener`() {
        // Given
        val fakeSensor = createFakeSensor(Sensor.TYPE_GRAVITY)
        val fakeEvent = createFakeSensorEvent(sensor = fakeSensor, values = floatArrayOf(1F, 2F, 3F))
        val fakeReceivedValues = mutableListOf<List<Float>>()
        every { context.getSystemService(SensorManager::class.java) } returns sensorManager
        every { sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY) } returns null
        gravitySensor.setOnSensorValuesChangedListener { values -> fakeReceivedValues.add(values) }

        // When
        gravitySensor.onSensorChanged(fakeEvent)

        // Then
        assertTrue(fakeReceivedValues.isEmpty())
    }

    @Test
    fun `if a gravity event arrives without a listener should not throw`() {
        // Given
        val fakeSensor = createFakeSensor(Sensor.TYPE_GRAVITY)
        val fakeEvent = createFakeSensorEvent(sensor = fakeSensor, values = floatArrayOf(0F, 9.8F, 0F))
        every { context.getSystemService(SensorManager::class.java) } returns sensorManager
        every { sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY) } returns fakeSensor

        // When
        // Then
        assertDoesNotThrow {
            gravitySensor.onSensorChanged(fakeEvent)
        }
    }

    @Test
    fun `if the listener is replaced should send the values only to the last listener`() {
        // Given
        val fakeSensor = createFakeSensor(Sensor.TYPE_GRAVITY)
        val fakeEvent = createFakeSensorEvent(sensor = fakeSensor, values = floatArrayOf(4F, 5F, 6F))
        val fakeFirstReceivedValues = mutableListOf<List<Float>>()
        val fakeLastReceivedValues = mutableListOf<List<Float>>()
        every { context.getSystemService(SensorManager::class.java) } returns sensorManager
        every { sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY) } returns fakeSensor
        gravitySensor.setOnSensorValuesChangedListener { values -> fakeFirstReceivedValues.add(values) }
        gravitySensor.setOnSensorValuesChangedListener { values -> fakeLastReceivedValues.add(values) }

        // When
        gravitySensor.onSensorChanged(fakeEvent)

        // Then
        assertTrue(fakeFirstReceivedValues.isEmpty())
        assertEquals(listOf(listOf(4F, 5F, 6F)), fakeLastReceivedValues)
    }

    // endregion
}
