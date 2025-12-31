package com.astrixforge.devicemasker.xposed.hooker

import com.astrixforge.devicemasker.common.DeviceProfilePreset
import com.astrixforge.devicemasker.common.SpoofType
import com.astrixforge.devicemasker.xposed.DualLog
import com.astrixforge.devicemasker.xposed.PrefsHelper
import com.highcapable.yukihookapi.hook.factory.field
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.type.java.StringClass

/**
 * System Hooker - Spoofs Build.* and SystemProperties using DeviceProfilePreset.
 *
 * This hooker applies a complete, consistent device profile rather than individual Build fields,
 * ensuring all values match and avoiding detection.
 */
object SystemHooker : BaseSpoofHooker("SystemHooker") {

    private fun getActivePreset(): DeviceProfilePreset? {
        val presetId =
            PrefsHelper.getSpoofValue(prefs, packageName, SpoofType.DEVICE_PROFILE) { "" }
        if (presetId.isEmpty()) return null
        return DeviceProfilePreset.findById(presetId)
    }

    override fun onHook() {
        logStart()
        hookBuildFields()
        hookSystemProperties()
        recordSuccess()
    }

    // ═══════════════════════════════════════════════════════════
    // BUILD FIELDS HOOKS
    // ═══════════════════════════════════════════════════════════

    private fun hookBuildFields() {
        val preset = getActivePreset() ?: return

        if (!isValidFingerprint(preset.fingerprint)) {
            logWarn("Invalid fingerprint format in preset '${preset.name}', skipping")
            return
        }

        "android.os.Build".toClass().apply {
            val fieldMappings =
                mapOf(
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
                    val currentValue = field { name = fieldName }.get().string()
                    if (spoofedValue.isNotEmpty() && spoofedValue != currentValue) {
                        field { name = fieldName }.get().set(spoofedValue)
                        logDebug("Set Build.$fieldName to $spoofedValue")
                    }
                }
            }

            DualLog.info(tag, "Applied device profile '${preset.name}' to Build fields")
        }
    }

    private fun isValidFingerprint(fingerprint: String): Boolean {
        if (fingerprint.isEmpty()) return false
        val slashCount = fingerprint.count { it == '/' }
        val colonCount = fingerprint.count { it == ':' }
        if (slashCount < 4 || colonCount < 2) return false
        val validEndings = listOf("user/release-keys", "userdebug/release-keys", "eng/test-keys")
        return validEndings.any { fingerprint.endsWith(it) }
    }

    // ═══════════════════════════════════════════════════════════
    // SYSTEM PROPERTIES HOOKS
    // ═══════════════════════════════════════════════════════════

    private fun hookSystemProperties() {
        val preset = getActivePreset() ?: return

        "android.os.SystemProperties".toClassOrNull()?.apply {
            val propertyMappings =
                mapOf(
                    "ro.build.fingerprint" to preset.fingerprint,
                    "ro.product.model" to preset.model,
                    "ro.product.manufacturer" to preset.manufacturer,
                    "ro.product.brand" to preset.brand,
                    "ro.product.device" to preset.device,
                    "ro.product.name" to preset.product,
                    "ro.product.board" to preset.board,
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
                }
                .hook {
                    after {
                        val key = args(0).string()
                        propertyMappings[key]?.takeIf { it.isNotEmpty() }?.let { result = it }
                    }
                }

            method {
                    name = "get"
                    param(StringClass, StringClass)
                }
                .hook {
                    after {
                        val key = args(0).string()
                        propertyMappings[key]?.takeIf { it.isNotEmpty() }?.let { result = it }
                    }
                }

            DualLog.info(tag, "Applied device profile '${preset.name}' to SystemProperties")
        }
    }
}
