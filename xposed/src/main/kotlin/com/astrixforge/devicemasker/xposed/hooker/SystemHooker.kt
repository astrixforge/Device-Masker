package com.astrixforge.devicemasker.xposed.hooker

import com.astrixforge.devicemasker.common.DeviceProfilePreset
import com.astrixforge.devicemasker.common.SpoofType
import com.astrixforge.devicemasker.xposed.DeviceMaskerService
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.field
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.type.java.StringClass

/**
 * System Hooker - Spoofs Build.* and SystemProperties using DeviceProfilePreset.
 *
 * This hooker applies a complete, consistent device profile rather than
 * individual Build fields, ensuring all values match and avoiding detection.
 *
 * The DEVICE_PROFILE spoof type value is a preset ID (e.g., "pixel_8_pro")
 * that maps to a DeviceProfilePreset containing all Build.* values.
 */
object SystemHooker : YukiBaseHooker() {

    /**
     * Gets the active device profile preset for the current package.
     * Returns null if not enabled or no profile is set.
     */
    private fun getActivePreset(): DeviceProfilePreset? {
        val service = DeviceMaskerService.instance ?: return null
        val config = service.config
        val group = config.getGroupForApp(packageName) ?: return null

        if (!group.isEnabled) return null
        if (!group.isTypeEnabled(SpoofType.DEVICE_PROFILE)) return null

        val presetId = group.getValue(SpoofType.DEVICE_PROFILE) ?: return null
        return DeviceProfilePreset.findById(presetId)
    }

    override fun onHook() {
        YLog.debug("SystemHooker: Starting hooks for: $packageName")

        hookBuildFields()
        hookSystemProperties()

        DeviceMaskerService.instance?.incrementHookCount()
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
                        YLog.debug("SystemHooker: Set Build.$fieldName to $spoofedValue")
                    }
                }
            }

            YLog.info("SystemHooker: Applied device profile '${preset.name}' to Build fields")
        }.onFailure { e ->
            YLog.error("SystemHooker: Failed to hook Build fields: ${e.message}")
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
            
            YLog.info("SystemHooker: Applied device profile '${preset.name}' to SystemProperties")
        }.onFailure { e ->
            YLog.error("SystemHooker: Failed to hook SystemProperties: ${e.message}")
        }
    }
}
