package dev.tvsopen.protocol

import dev.tvsopen.Nucleus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [ApacheFrames] and [ApacheDriver].
 *
 * Byte offsets are transcribed from `com.tvs.bike.core.protocol.apache.*`
 * (`ApacheSpeedOMeter1..5` and `ApacheIncomingFrameIdentifier`). Every
 * multi-byte assertion uses big-endian fixtures and explicitly checks the
 * offsets that differ from [NtorqFrames] (RPM at 16..17, Trip F 2 bytes at
 * 14..15, 0-60 at 12, and gear position in `0x11` byte 5).
 */
class ApacheFrameTest {

    private fun frame(vararg bytes: Int): ByteArray =
        ByteArray(20) { i -> if (i < bytes.size) bytes[i].toByte() else 0 }

    @Test
    fun `0x10 decodes speed, odometer, fuel, 0-60, 2-byte trip F and RPM at 16-17`() {
        // Example odometer: 52,966.2 km -> 529,662 tenths = 0x08 0x15 0x0E (BE)
        // Example RPM: 4200 = 0x10 0x68 at bytes 16..17 (Ntorq puts RPM at 17..18)
        // Example Trip F: 250 = 0x00 0xFA at bytes 14..15 (2 bytes, not 3)
        val raw = frame(
            0x5A, 0x10,
            64,                 // idx 2: speed 64 km/h
            0x08, 0x15, 0x0E,   // idx 3..5: odometer raw 529662 -> 52966.2 km
            0x03,               // idx 6: fuel level
            39,                 // idx 7: average speed
            42,                 // idx 8: mileage
            98,                 // idx 9: top speed
            50,                 // idx 10: throttle
            0x00,               // idx 11: switch / location tag
            47,                 // idx 12: 0-60 time (4.7 s raw)
            41,                 // idx 13: average mileage
            0x00, 0xFA,         // idx 14..15: trip F (2 bytes)
            0x10, 0x68,         // idx 16..17: engine RPM (4200)
            0x00,               // idx 18: checksum
            0xFF,               // idx 19: end byte
        )
        val d = ApacheDriver(VehicleModel.APACHE_160).decode(raw)!!
        assertEquals(64L, d.field(Field.SPEED))
        assertEquals(529_662L, d.field(Field.ODOMETER))
        assertEquals(3L, d.field(Field.FUEL_LEVEL))
        assertEquals(39L, d.field(Field.AVERAGE_SPEED))
        assertEquals(98L, d.field(Field.TOP_SPEED))
        assertEquals(50L, d.field(Field.THROTTLE_POSITION))
        assertEquals(47L, d.field(Field.TIME_60KMPH))
        assertEquals(250L, d.field(Field.TRIP_F_METER))
        assertEquals(4200L, d.field(Field.ENGINE_RPM))
    }

    @Test
    fun `0x11 decodes gear position at byte 5, service reminder and software version`() {
        val raw = frame(
            0x5A, 0x11,
            0x00,   // idx 2: diagnostics 2
            0x01,   // idx 3: state 1
            0x01,   // idx 4: service reminder due
            0x02,   // idx 5: gear 2
            0x00,   // idx 6: state 2
            0x14,   // idx 7: software version
            0x00, 0x00,
            0x04,   // idx 10: diagnostics 1 (MIL bit 2)
            0x00,
            0x01,   // idx 12: turn indicator
            0x00, 0x00, 0x00, 0x00,
            0x05,   // idx 17: backlight
            0x00,   // idx 18: checksum
            0xFF,
        )
        val d = ApacheDriver(VehicleModel.APACHE_160).decode(raw)!!
        assertEquals(2L, d.field(Field.GEAR_POSITION))
        assertEquals(1L, d.field(Field.SERVICE_REMINDER))
        assertEquals(0x14L, d.field(Field.SOFTWARE_VERSION))
        assertEquals(0x04L, d.field(Field.VEHICLE_DIAGNOSTICS_1))
    }

    @Test
    fun `0x12 decodes lean angle, cruising range and trip distance big-endian`() {
        val raw = frame(
            0x5A, 0x12,
            24,         // idx 2: lean angle
            0x01, 0x18, // idx 3..4: cruising range 280 km
            0x00,       // idx 5: wheel angle offset
            12,         // idx 6: acceleration
            14,         // idx 7: torque
            0x00, 0x7D, // idx 8..9: trip distance 125
            0x01,       // idx 10: trip hr
            0x1E,       // idx 11: trip min
            45,         // idx 12: trip mileage
            0x00, 0x20, // idx 13..14: trip fuel
            0x00, 0x00, 0x00, 0x00,
            0xFF,
        )
        val d = ApacheDriver(VehicleModel.APACHE_160).decode(raw)!!
        assertEquals(24L, d.field(Field.LEAN_ANGLE))
        assertEquals(280L, d.field(Field.CRUISING_RANGE))
        assertEquals(125L, d.field(Field.TRIP_DISTANCE))
    }

    @Test
    fun `0x18 decodes engine temperature and battery voltage`() {
        val raw = frame(
            0x5A, 0x18,
            35,         // idx 2: engine load
            0x00, 0x10, // idx 3..4: accumulated fuel injection time
            0x64,       // idx 5: manifold air pressure
            0x64,       // idx 6: barometric pressure
            29,         // idx 7: intake air temp
            88,         // idx 8: engine temp
            0x00, 0x20, // idx 9..10: fuel injection time
            126,        // idx 11: battery voltage (12.6 V raw)
            0x00, 0x3C, // idx 12..13: run time
            0x00, 0x0A, // idx 14..15: distance traveled
            0x00, 0x05, // idx 16..17: fuel injection volume
            0x00,       // idx 18: checksum
            0xFF,
        )
        val d = ApacheDriver(VehicleModel.APACHE_160).decode(raw)!!
        assertEquals(88L, d.field(Field.ENGINE_TEMPRATURE))
        assertEquals(126L, d.field(Field.BATTERY_VOLTAGE))
    }

    @Test
    fun `apache driver registers for APACHE_160 and APACHE_180`() {
        Nucleus.init()
        val d160 = DriverRegistry.forModel(VehicleModel.APACHE_160)
        val d180 = DriverRegistry.forModel(VehicleModel.APACHE_180)
        assertTrue(d160 is ApacheDriver)
        assertTrue(d180 is ApacheDriver)
        assertTrue(d160.capabilities.navigationHud)
        assertTrue(d160.capabilities.keepalive)
    }

    @Test
    fun `apache navigation uses 0x5A 0x4E big-endian and single primary text row`() {
        val driver = ApacheDriver(VehicleModel.APACHE_160)
        val step = NavStep(96, 4, 1500, 3, "MUY A LA DERECHA")

        val ctrl = driver.buildNavControl(step)!!
        assertEquals(20, ctrl.size)
        assertEquals(0x5A, ctrl[0].toInt() and 0xFF)
        assertEquals(0x4E, ctrl[1].toInt() and 0xFF)
        assertEquals(0x00, ctrl[2].toInt() and 0xFF)
        assertEquals(0x60, ctrl[3].toInt() and 0xFF) // 96 m
        assertEquals(0xFF.toByte(), ctrl[19])

        val text = driver.buildNavText(step)!!
        assertEquals(20, text.size)
        assertEquals(0x5B, text[0].toInt() and 0xFF)
        assertEquals(0x4F, text[1].toInt() and 0xFF)
        assertEquals(0xFF.toByte(), text[19])

        assertNull(driver.buildNavTextSecondary(step))
    }

    @Test
    fun `apache iot service UUID is distinct from CSR OTAU service UUID`() {
        assertNotEquals(Gatt.APACHE_IOT_SERVICE, Gatt.CSR_OTAU_SERVICE)
        assertTrue(Gatt.ALL_KNOWN_SERVICES.contains(Gatt.SERVICE))
        assertTrue(Gatt.ALL_KNOWN_SERVICES.contains(Gatt.APACHE_IOT_SERVICE))
        assertTrue(!Gatt.ALL_KNOWN_SERVICES.contains(Gatt.CSR_OTAU_SERVICE))
    }
}
