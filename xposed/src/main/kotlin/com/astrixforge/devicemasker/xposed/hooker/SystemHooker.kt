package com.astrixforge.devicemasker.xposed.hooker

import com.astrixforge.devicemasker.common.DeviceProfilePreset
import com.astrixforge.devicemasker.common.SpoofType
import com.astrixforge.devicemasker.xposed.PrefsHelper
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.field
import com.highcapable.yukihookapi.hook.factory.method
import com.astrixforge.devicemasker.xposed.DualLog
import com.highcapable.yukihookapi.hook.type.java.StringClass

/**
 * System Hooker - Spoofs Build.* and SystemProperties using DeviceProfilePreset.
 *
 * This hooker applies a complete, consistent device profile rather than
 * individual Build fields, ensuring all values match and avoiding detection.
 *
 * The DEVICE_PROFILE spoof type value is a preset ID (e.g., "pixel_8_pro")
 * that maps to a DeviceProfilePreset containing all Build.* values.
 *
 * Uses ServiceProxy for cross-process config access via Binder IPC.
 */
object SystemHooker : YukiBaseHooker() {

    private const val TAG = "SystemHooker"

    private fun getActivePreset(): DeviceProfilePreset? {
        // Get the preset ID from config
        val presetId = PrefsHelper.getSpoofValue(
            prefs,
            packageName, 
            SpoofType.DEVICE_PROFILE
        ) { "" }
        
        if (presetId.isEmpty()) return null
        return DeviceProfilePreset.findById(presetId)
    }

    override fun onHook() {
        DualLog.debug(TAG, "Starting hooks for: $packageName")

        hookBuildFields()
        hookSystemProperties()

        // Hook count tracking removed
    }

    private fun hookBuildFields() {
        val preset = getActivePreset() ?: return
        
        runCatching {
            val buildClass = "android.os.Build".toClass()

            // Apply all Build.* fields from the preset
            val fieldMappings = mapOf(
                "FINGERPRINT" to preset.fingerprint,
                "MODEL" to preset.model,
                "MANUFACTURER" to preset.manufacturer,
                "BRAND" to preset.brand,
                "DEVICE" to preset.device,
                "PRODUCT" to preset.product,
                "BOARD" to preset.board,
            )

            fieldMappings.forEach { (fieldName, spoofedValue) ->
                runCatching {
                    val currentValue = buildClass.field { name = fieldName }.get().string()
                    if (spoofedValue.isNotEmpty() && spoofedValue != currentValue) {
                        buildClass.field { name = fieldName }.get().set(spoofedValue)
                        DualLog.debug(TAG, "Set Build.$fieldName to $spoofedValue")
                    }
                }
            }

            DualLog.info(TAG, "Applied device profile '${preset.name}' to Build fields")
        }.onFailure { e ->
            DualLog.error(TAG, "Failed to hook Build fields: ${e.message}")
        }
    }

    private fun hookSystemProperties() {
        val preset = getActivePreset() ?: return

        runCatching {
            "android.os.SystemProperties".toClass().apply {
                // Map of system properties to their spoofed values
                val propertyMappings = mapOf(
                    "ro.build.fingerprint" to preset.fingerprint,
                    "ro.product.model" to preset.model,
                    "ro.product.manufacturer" to preset.manufacturer,
                    "ro.product.brand" to preset.brand,
                    "ro.product.device" to preset.device,
                    "ro.product.name" to preset.product,
                    "ro.product.board" to preset.board,
                    // Additional properties for consistency
                    "ro.build.product" to preset.product,
                    "ro.vendor.product.model" to preset.model,
                    "ro.vendor.product.brand" to preset.brand,
                    "ro.vendor.product.device" to preset.device,
                    "ro.vendor.product.manufacturer" to preset.manufacturer,
                    "ro.vendor.product.name" to preset.product,
                    "ro.bootimage.build.fingerprint" to preset.fingerprint,
                    "ro.system.build.fingerprint" to preset.fingerprint,
                    "ro.vendor.build.fingerprint" to preset.fingerprint,
                )

                method {
                    name = "get"
                    param(StringClass)
                }.hook {
                    after {
                        val key = args(0).string()
                        propertyMappings[key]?.let { spoofedValue ->
                            if (spoofedValue.isNotEmpty()) {
                                result = spoofedValue
                            }
                        }
                    }
                }

                method {
                    name = "get"
                    param(StringClass, StringClass)
                }.hook {
                    after {
                        val key = args(0).string()
                        propertyMappings[key]?.let { spoofedValue ->
                            if (spoofedValue.isNotEmpty()) {
                                result = spoofedValue
                            }
                        }
                    }
                }
            }
            
            DualLog.info(TAG, "Applied device profile '${preset.name}' to SystemProperties")
        }.onFailure { e ->
            DualLog.error(TAG, "Failed to hook SystemProperties: ${e.message}")
        }
    }
}
