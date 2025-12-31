package com.astrixforge.devicemasker.xposed.hooker

import com.astrixforge.devicemasker.common.DeviceProfilePreset
import com.astrixforge.devicemasker.common.SpoofType
import com.astrixforge.devicemasker.xposed.PrefsHelper
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.type.java.IntType

/**
 * Sensor Hooker - Spoofs sensor information to match device profile.
 *
 * Hooks SensorManager and Sensor classes to prevent device fingerprinting via sensor enumeration.
 */
object SensorHooker : BaseSpoofHooker("SensorHooker") {

    // Cached class references
    private val sensorManagerClass by lazy { "android.hardware.SensorManager".toClassOrNull() }
    private val sensorClass by lazy { "android.hardware.Sensor".toClassOrNull() }

    // Sensor types that should always be present on modern devices
    private val ESSENTIAL_SENSOR_TYPES =
        setOf(
            1, // TYPE_ACCELEROMETER
            2, // TYPE_MAGNETIC_FIELD
            4, // TYPE_GYROSCOPE
            5, // TYPE_LIGHT
            8, // TYPE_PROXIMITY
            11, // TYPE_ROTATION_VECTOR
        )

    private fun getActivePreset(): DeviceProfilePreset? {
        val presetId =
            PrefsHelper.getSpoofValue(prefs, packageName, SpoofType.DEVICE_PROFILE) { "" }
        if (presetId.isEmpty()) return null
        return DeviceProfilePreset.findById(presetId)
    }

    override fun onHook() {
        logStart()
        hookSensorManager()
        hookSensorMetadata()
        recordSuccess()
    }

    // ═══════════════════════════════════════════════════════════
    // SENSOR MANAGER HOOKS
    // ═══════════════════════════════════════════════════════════

    private fun hookSensorManager() {
        sensorManagerClass?.apply {
            // Hook getSensorList(int type)
            method {
                    name = "getSensorList"
                    param(IntType)
                }
                .hook {
                    after {
                        @Suppress("UNCHECKED_CAST")
                        val originalList = result as? List<Any> ?: return@after
                        val sensorType = args(0).int()

                        // For TYPE_ALL (-1), filter to only essential sensors
                        if (sensorType == -1 && originalList.size > 10) {
                            result = filterSensorList(originalList)
                            logDebug(
                                "Filtered sensor list: ${originalList.size} -> ${(result as List<*>).size}"
                            )
                        }
                    }
                }

            // Hook getDefaultSensor(int type)
            method {
                    name = "getDefaultSensor"
                    param(IntType)
                }
                .hook {
                    after {
                        val sensorType = args(0).int()
                        if (result == null && sensorType in ESSENTIAL_SENSOR_TYPES) {
                            logDebug("Default sensor type $sensorType returned null")
                        }
                    }
                }
        }
    }

    // ═══════════════════════════════════════════════════════════
    // SENSOR METADATA HOOKS
    // ═══════════════════════════════════════════════════════════

    private fun hookSensorMetadata() {
        sensorClass?.apply {
            // Hook getVendor()
            method {
                    name = "getVendor"
                    emptyParam()
                }
                .hook {
                    after {
                        val preset = getActivePreset() ?: return@after
                        if (preset.manufacturer.isNotEmpty()) {
                            result = preset.manufacturer
                        }
                    }
                }

            // Hook getVersion() - Normalize to avoid detection
            method {
                    name = "getVersion"
                    emptyParam()
                }
                .hook {
                    after {
                        val version = result as? Int ?: 1
                        if (version > 3) {
                            result = 1
                        }
                    }
                }

            // Hook getName() - Remove manufacturer-specific prefixes
            method {
                    name = "getName"
                    emptyParam()
                }
                .hook {
                    after {
                        val originalName = result as? String ?: return@after
                        if (
                            originalName.contains("Qualcomm") ||
                                originalName.contains("MediaTek") ||
                                originalName.contains("Samsung")
                        ) {
                            result =
                                originalName
                                    .replace("Qualcomm ", "")
                                    .replace("MediaTek ", "")
                                    .replace("Samsung ", "")
                        }
                    }
                }
        }
    }

    private fun filterSensorList(sensors: List<Any>): List<Any> {
        return sensors.filter { sensor ->
            runCatching {
                    val typeMethod = sensor.javaClass.getMethod("getType")
                    val sensorType = typeMethod.invoke(sensor) as Int
                    sensorType in ESSENTIAL_SENSOR_TYPES
                }
                .getOrDefault(true)
        }
    }
}
