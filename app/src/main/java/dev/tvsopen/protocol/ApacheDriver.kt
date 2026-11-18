// Apache RTR protocol driver.
//
// Inbound frame layouts transcribed from `com.tvs.bike.core.protocol.apache.*`
// (`ApacheIncomingFrameIdentifier`, `ApacheSpeedOMeter1..5`, `ApacheBasicData`).
// All `@ParsingMeta(start = N)` offsets upstream are 1-based and converted here
// to 0-based byte indices. Multi-byte values are big-endian, matching
// [FieldSpec].
//
// Outbound navigation framing (`0x5A 0x4E`, 20 bytes, `0xFF` terminator) matches
// `ApacheClusterNavigationData`. Hardware LCD observation on an Apache RTR 160
// 4V (Continental / LGIT CSR101x cluster) confirmed 12-character navigation
// lines with automatic wrapping on the cluster display, so the driver uses the
// single primary text row (`0x5B 0x4F`) rather than sending an unverified `0x50`
// secondary channel.

package dev.tvsopen.protocol

/**
 * Apache RTR inbound frame table, transcribed from
 * `ApacheIncomingFrameIdentifier.getFrameConfig()`:
 *
 *   16 (0x10) -> SPEEDOMETER_DATA_5A_10_FRAME -> ApacheSpeedOMeter1
 *   17 (0x11) -> SPEEDOMETER_DATA_5A_11_FRAME -> ApacheSpeedOMeter2
 *   18 (0x12) -> SPEEDOMETER_DATA_5A_12_FRAME -> ApacheSpeedOMeter3
 *   22 (0x16) -> SPEEDOMETER_DATA_5A_16_FRAME -> ApacheSpeedOMeter4
 *   24 (0x18) -> SPEEDOMETER_DATA_5A_18_FRAME -> ApacheSpeedOMeter5
 *   41 (0x29) -> WIFI_PASSWORD_5A_29_FRAME    -> ApacheBasicData
 *
 * Key differences from [NtorqFrames]:
 *  - `0x10` packs `TIME_60KMPH` at byte 12 (not 13), `TRIP_F_METER` as 2 bytes
 *    at 14..15 (not 3 bytes), `ENGINE_RPM` at 16..17 (not 17..18), and
 *    `CHECKSUM` at byte 18.
 *  - `0x11` adds `GEAR_POSITION` at byte 5.
 *  - `0x12` carries lean angle, cruising range, torque and trip metrics.
 *  - `0x16` carries lap-timer data.
 */
object ApacheFrames {

    /** ApacheSpeedOMeter1 — speed, odometer, fuel, averages, trip F, RPM. */
    val SM1 = FrameLayout(
        dataId = 0x10,
        type = DataFrameType.SPEEDOMETER_DATA_5A_10_FRAME,
        fields = listOf(
            FieldSpec(Field.START_BYTE, 0),
            FieldSpec(Field.DATA_ID, 1),
            FieldSpec(Field.SPEED, 2),
            FieldSpec(Field.ODOMETER, 3, length = 3),
            FieldSpec(Field.FUEL_LEVEL, 6),
            FieldSpec(Field.AVERAGE_SPEED, 7),
            FieldSpec(Field.MILEAGE, 8),
            FieldSpec(Field.TOP_SPEED, 9),
            FieldSpec(Field.THROTTLE_POSITION, 10),
            FieldSpec(Field.SWITCH_STATUS, 11),
            FieldSpec(Field.TIME_60KMPH, 12),
            FieldSpec(Field.AVERAGE_MILEAGE_ALT, 13),
            FieldSpec(Field.TRIP_F_METER, 14, length = 2),
            FieldSpec(Field.ENGINE_RPM, 16, length = 2),
            FieldSpec(Field.CHECKSUM, 18),
            FieldSpec(Field.END_BYTE, 19),
        ),
        checksumAt = 18,
    )

    /** ApacheSpeedOMeter2 — gear position, service reminder, tell-tales, SW version. */
    val SM2 = FrameLayout(
        dataId = 0x11,
        type = DataFrameType.SPEEDOMETER_DATA_5A_11_FRAME,
        fields = listOf(
            FieldSpec(Field.START_BYTE, 0),
            FieldSpec(Field.DATA_ID, 1),
            FieldSpec(Field.VEHICLE_DIAGNOSTICS_2, 2),
            FieldSpec(Field.VEHICLE_STATE_1, 3),
            FieldSpec(Field.SERVICE_REMINDER, 4),
            FieldSpec(Field.GEAR_POSITION, 5),
            FieldSpec(Field.VEHICLE_STATE_2, 6),
            FieldSpec(Field.SOFTWARE_VERSION, 7),
            FieldSpec(Field.VEHICLE_DIAGNOSTICS_1, 10),
            FieldSpec(Field.TURN_INDICATOR_STATUS, 12),
            FieldSpec(Field.BACKLIGHT, 17),
            FieldSpec(Field.CHECKSUM, 18),
            FieldSpec(Field.END_BYTE, 19),
        ),
        checksumAt = 18,
    )

    /** ApacheSpeedOMeter3 — lean angle, cruising range, acceleration, torque, trip stats. */
    val SM3 = FrameLayout(
        dataId = 0x12,
        type = DataFrameType.SPEEDOMETER_DATA_5A_12_FRAME,
        fields = listOf(
            FieldSpec(Field.START_BYTE, 0),
            FieldSpec(Field.DATA_ID, 1),
            FieldSpec(Field.LEAN_ANGLE, 2),
            FieldSpec(Field.CRUISING_RANGE, 3, length = 2),
            FieldSpec(Field.WHEEL_ANGLE_OFFSET, 5),
            FieldSpec(Field.ACCELERATION, 6),
            FieldSpec(Field.TORQUE, 7),
            FieldSpec(Field.TRIP_DISTANCE, 8, length = 2),
            FieldSpec(Field.TRIP_TIME_HR, 10),
            FieldSpec(Field.TRIP_TIME_MIN, 11),
            FieldSpec(Field.TRIP_MILEAGE, 12),
            FieldSpec(Field.TRIP_FUEL, 13, length = 2),
            FieldSpec(Field.OVERSPEED_THRESHOLD, 15),
            FieldSpec(Field.OVERSPEED_SETTING, 16),
            FieldSpec(Field.CHECKSUM, 18),
            FieldSpec(Field.END_BYTE, 19),
        ),
        checksumAt = 18,
    )

    /** ApacheSpeedOMeter4 — lap timer metrics. */
    val SM4 = FrameLayout(
        dataId = 0x16,
        type = DataFrameType.SPEEDOMETER_DATA_5A_16_FRAME,
        fields = listOf(
            FieldSpec(Field.START_BYTE, 0),
            FieldSpec(Field.DATA_ID, 1),
            FieldSpec(Field.LAP_TIME, 2),
            FieldSpec(Field.LAP_NUMBER, 5),
            FieldSpec(Field.BEST_LAP, 6),
            FieldSpec(Field.LAP_TRIGGER, 10),
            FieldSpec(Field.CHECKSUM, 18),
            FieldSpec(Field.END_BYTE, 19),
        ),
        checksumAt = 18,
    )

    /** ApacheSpeedOMeter5 — engine diagnostics, coolant temp, battery voltage. */
    val SM5 = FrameLayout(
        dataId = 0x18,
        type = DataFrameType.SPEEDOMETER_DATA_5A_18_FRAME,
        fields = listOf(
            FieldSpec(Field.START_BYTE, 0),
            FieldSpec(Field.DATA_ID, 1),
            FieldSpec(Field.ENGINE_LOAD, 2),
            FieldSpec(Field.ACCUMULATED_FUEL_INJECTION_TIME, 3, length = 2),
            FieldSpec(Field.MANIFOLD_AIR_PRESSURE, 5),
            FieldSpec(Field.BAROMETRIC_PRESSURE, 6),
            FieldSpec(Field.INTAKE_AIR_TEMPRATURE, 7),
            FieldSpec(Field.ENGINE_TEMPRATURE, 8),
            FieldSpec(Field.FUEL_INJECTION_TIME, 9, length = 2),
            FieldSpec(Field.BATTERY_VOLTAGE, 11),
            FieldSpec(Field.RUN_TIME_SINCE_ENGINE_START, 12, length = 2),
            FieldSpec(Field.DISTANCE_TRAVELED, 14, length = 2),
            FieldSpec(Field.FUEL_INJECTION_VOLUME, 16, length = 2),
            FieldSpec(Field.CHECKSUM, 18),
            FieldSpec(Field.END_BYTE, 19),
        ),
        checksumAt = 18,
    )

    /** ApacheBasicData — 16 raw payload bytes spanning indices 2..17. */
    val BASIC_DATA = FrameLayout(
        dataId = 0x29,
        type = DataFrameType.WIFI_PASSWORD_5A_29_FRAME,
        fields = listOf(
            FieldSpec(Field.START_BYTE, 0),
            FieldSpec(Field.DATA_ID, 1),
            FieldSpec(Field.BYTE_1, 2),
            FieldSpec(Field.BYTE_2, 3),
            FieldSpec(Field.BYTE_3, 4),
            FieldSpec(Field.BYTE_4, 5),
            FieldSpec(Field.BYTE_5, 6),
            FieldSpec(Field.BYTE_6, 7),
            FieldSpec(Field.BYTE_7, 8),
            FieldSpec(Field.BYTE_8, 9),
            FieldSpec(Field.BYTE_9, 10),
            FieldSpec(Field.BYTE_10, 11),
            FieldSpec(Field.BYTE_11, 12),
            FieldSpec(Field.BYTE_12, 13),
            FieldSpec(Field.BYTE_13, 14),
            FieldSpec(Field.BYTE_14, 15),
            FieldSpec(Field.BYTE_15, 16),
            FieldSpec(Field.BYTE_16, 17),
            FieldSpec(Field.CHECKSUM, 18),
            FieldSpec(Field.END_BYTE, 19),
        ),
        checksumAt = 18,
    )

    val all: List<FrameLayout> = listOf(SM1, SM2, SM3, SM4, SM5, BASIC_DATA)

    fun forDataId(id: Int): FrameLayout? = all.firstOrNull { it.dataId == id }
}

/**
 * Driver for the Apache RTR family (160 / 160 4V / 180).
 *
 * Works over both the standard SmartXonnect GATT service (`5456534D-…`) and the
 * Continental / LGIT CSR101x serial-over-GATT service (`00005500-…`) resolved
 * by [dev.tvsopen.transport.ScooterBleService].
 */
class ApacheDriver(
    override val model: VehicleModel = VehicleModel.APACHE_160,
) : ProtocolDriver {

    init {
        require(model == VehicleModel.APACHE_160 || model == VehicleModel.APACHE_180) {
            "${model.displayName} is not an Apache RTR variant"
        }
    }

    override val capabilities = Capabilities(
        findMyScooter = true,
        navigationHud = true,
        textMessages = true,
        callerId = true,
        mediaTitle = false,
        buttonGestures = false,
        timeSync = true,
        wifiPassword = true,
        calibration = false,
        keepalive = true,
        fuelLowNibble = true,
    )

    override fun confidence() = mapOf(
        "GATT services (5456534D / 00005500)" to Confidence.VERIFIED_FROM_APK,
        "inbound frame layouts 0x10..0x18" to Confidence.VERIFIED_FROM_APK,
        "navigation control 0x5A 0x4E" to Confidence.VERIFIED_FROM_APK,
        "navigation text 0x5B 0x4F" to Confidence.INHERITED_UNVERIFIED,
        "outbound ping 0x4A" to Confidence.INHERITED_UNVERIFIED,
        "gear and fuel bar scaling on cluster" to Confidence.NEEDS_CAPTURE,
        "caller ID 0x5B 0x43" to Confidence.NEEDS_CAPTURE,
    )

    override fun acceptsStartId(startId: Int): Boolean =
        startId == StartId.A5 || startId == StartId.B9 || startId == StartId.B5

    override fun decode(raw: ByteArray): DecodedFrame? {
        if (raw.size < 2) return null
        val dataId = raw[1].toInt() and 0xFF
        return ApacheFrames.forDataId(dataId)?.decode(raw)
            ?: GenericDriver(model).decode(raw)
    }

    override fun buildPing(state: DashState): ByteArray? = Keepalive.build(state)

    override fun buildNavControl(nav: NavStep): ByteArray? =
        PacketBuilder.navigationControl(nav)

    override fun buildNavText(nav: NavStep): ByteArray? =
        PacketBuilder.navigationText(nav.streetName)

    /**
     * Null on Apache RTR: physical LCD inspection shows a 12-character primary
     * navigation zone that automatically wraps longer text across lines, so the
     * bridge sticks to `0x5B 0x4F` rather than sending an unverified `0x50`.
     */
    override fun buildNavTextSecondary(nav: NavStep): ByteArray? = null

    override fun buildTextRow(row: Int, text: String): ByteArray? =
        PacketBuilder.textRow(
            if (row <= 0) OutboundId.TEXT_ROW1 else OutboundId.TEXT_ROW2,
            text,
        )

    override fun buildCallerInfo(number: String, name: String?, state: Int): ByteArray? =
        PacketBuilder.callerInfo(number, name, state)

    override fun buildRiderName(name: String): ByteArray? =
        PacketBuilder.riderName(name)

    override fun buildChallengeResponse(challenge: ByteArray): ByteArray? =
        Challenge.buildResponse(challenge)
}
