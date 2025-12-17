package com.astrixforge.devicemasker.hook.hooker

import android.content.Context
import com.astrixforge.devicemasker.data.generators.IMEIGenerator
import com.astrixforge.devicemasker.data.generators.SerialGenerator
import com.astrixforge.devicemasker.data.generators.UUIDGenerator
import com.astrixforge.devicemasker.data.models.SpoofType
import com.astrixforge.devicemasker.hook.HookDataProvider
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.type.java.IntType
import com.highcapable.yukihookapi.hook.type.java.StringClass

/**
 * Device Identifier Hooker - Spoofs hardware and device identifiers.
 *
 * Hooks:
 * - TelephonyManager: getDeviceId(), getImei(), getMeid(), getSubscriberId(), getSimSerialNumber()
 * - Build: SERIAL, getSerial(), MODEL, MANUFACTURER, BRAND, DEVICE, PRODUCT, etc.
 * - Settings.Secure: getString() for android_id
 *
 * Uses HookDataProvider to read profile-based values and global config.
 */
object DeviceHooker : YukiBaseHooker() {

    // ═══════════════════════════════════════════════════════════
    // DATA PROVIDER - Lazily initialized on first access
    // ═══════════════════════════════════════════════════════════

    private var dataProvider: HookDataProvider? = null

    /**
     * Gets the data provider, creating it if needed. Falls back to generated values if provider
     * creation fails.
     */
    private fun getProvider(context: Context?): HookDataProvider? {
        if (dataProvider == null && context != null) {
            dataProvider =
                runCatching { HookDataProvider.getInstance(context, packageName) }
                    .onFailure {
                        YLog.error("DeviceHooker: Failed to create HookDataProvider: ${it.message}")
                    }
                    .getOrNull()
        }
        return dataProvider
    }

    /** Gets a spoof value from the provider, with fallback to generator. */
    private fun getSpoofValueOrGenerate(
        context: Context?,
        type: SpoofType,
        generator: () -> String,
    ): String? {
        val provider = getProvider(context)
        if (provider == null) {
            // No provider available, fallback to generated value
            YLog.debug("DeviceHooker: No provider for $type, using generated value")
            return generator()
        }

        // getSpoofValue now handles all profile-based checks (profile exists, profile enabled, type
        // enabled)
        return provider.getSpoofValue(type) ?: generator()
    }

    // ═══════════════════════════════════════════════════════════
    // FALLBACK GENERATORS (If DataStore not accessible)
    // ═══════════════════════════════════════════════════════════

    private val fallbackImei: String by lazy { IMEIGenerator.generate() }
    private val fallbackMeid: String by lazy { IMEIGenerator.generate() }
    private val fallbackSerial: String by lazy { SerialGenerator.generate() }
    private val fallbackAndroidId: String by lazy { UUIDGenerator.generateAndroidId() }

    private fun generateImsi(): String {
        // MCC (3 digits) + MNC (2-3 digits) + MSIN (9-10 digits)
        return "310" + "260" + (100000000L..999999999L).random().toString()
    }

    private fun generateSimSerial(): String {
        return "8901" + List(16) { (0..9).random() }.joinToString("")
    }

    override fun onHook() {
        YLog.debug("DeviceHooker: Starting hooks for package: $packageName")

        // ═══════════════════════════════════════════════════════════
        // TELEPHONY MANAGER HOOKS
        // ═══════════════════════════════════════════════════════════

        hookTelephonyManager()

        // ═══════════════════════════════════════════════════════════
        // BUILD CLASS HOOKS
        // ═══════════════════════════════════════════════════════════

        hookBuildClass()

        // ═══════════════════════════════════════════════════════════
        // SETTINGS.SECURE HOOKS (Android ID)
        // ═══════════════════════════════════════════════════════════

        hookSettingsSecure()

        YLog.debug("DeviceHooker: Hooks registered for package: $packageName")
    }

    /** Hooks TelephonyManager methods for device identifiers. */
    private fun hookTelephonyManager() {
        "android.telephony.TelephonyManager".toClass().apply {

            // getDeviceId() - Legacy device ID (IMEI/MEID)
            method {
                    name = "getDeviceId"
                    emptyParam()
                }
                .hook {
                    after {
                        val value =
                            getSpoofValueOrGenerate(appContext, SpoofType.IMEI) { fallbackImei }
                        if (value != null) {
                            YLog.debug("DeviceHooker: Spoofing getDeviceId() -> $value")
                            result = value
                        }
                    }
                }

            // getDeviceId(int slotIndex) - Per-slot device ID
            method {
                    name = "getDeviceId"
                    param(IntType)
                }
                .hook {
                    after {
                        val value =
                            getSpoofValueOrGenerate(appContext, SpoofType.IMEI) { fallbackImei }
                        if (value != null) {
                            YLog.debug("DeviceHooker: Spoofing getDeviceId(slot) -> $value")
                            result = value
                        }
                    }
                }

            // getImei() - IMEI with no slot parameter
            method {
                    name = "getImei"
                    emptyParam()
                }
                .hook {
                    after {
                        val value =
                            getSpoofValueOrGenerate(appContext, SpoofType.IMEI) { fallbackImei }
                        if (value != null) {
                            YLog.debug("DeviceHooker: Spoofing getImei() -> $value")
                            result = value
                        }
                    }
                }

            // getImei(int slotIndex) - Per-slot IMEI
            method {
                    name = "getImei"
                    param(IntType)
                }
                .hook {
                    after {
                        val value =
                            getSpoofValueOrGenerate(appContext, SpoofType.IMEI) { fallbackImei }
                        if (value != null) {
                            YLog.debug("DeviceHooker: Spoofing getImei(slot) -> $value")
                            result = value
                        }
                    }
                }

            // getMeid() - MEID for CDMA devices (may not exist on all devices)
            runCatching {
                method {
                        name = "getMeid"
                        emptyParam()
                    }
                    .hook {
                        after {
                            val value =
                                getSpoofValueOrGenerate(appContext, SpoofType.MEID) { fallbackMeid }
                            if (value != null) {
                                YLog.debug("DeviceHooker: Spoofing getMeid() -> $value")
                                result = value
                            }
                        }
                    }
            }

            // getMeid(int slotIndex) - Per-slot MEID
            runCatching {
                method {
                        name = "getMeid"
                        param(IntType)
                    }
                    .hook {
                        after {
                            val value =
                                getSpoofValueOrGenerate(appContext, SpoofType.MEID) { fallbackMeid }
                            if (value != null) {
                                YLog.debug("DeviceHooker: Spoofing getMeid(slot) -> $value")
                                result = value
                            }
                        }
                    }
            }

            // getSubscriberId() - IMSI
            method {
                    name = "getSubscriberId"
                    emptyParam()
                }
                .hook {
                    after {
                        val value =
                            getSpoofValueOrGenerate(appContext, SpoofType.IMSI) { generateImsi() }
                        if (value != null) {
                            YLog.debug("DeviceHooker: Spoofing getSubscriberId() -> $value")
                            result = value
                        }
                    }
                }

            // getSubscriberId(int subId) - Per-subscription IMSI
            runCatching {
                method {
                        name = "getSubscriberId"
                        param(IntType)
                    }
                    .hook {
                        after {
                            val value =
                                getSpoofValueOrGenerate(appContext, SpoofType.IMSI) {
                                    generateImsi()
                                }
                            if (value != null) {
                                YLog.debug(
                                    "DeviceHooker: Spoofing getSubscriberId(subId) -> $value"
                                )
                                result = value
                            }
                        }
                    }
            }

            // getSimSerialNumber() - ICCID
            method {
                    name = "getSimSerialNumber"
                    emptyParam()
                }
                .hook {
                    after {
                        val value =
                            getSpoofValueOrGenerate(appContext, SpoofType.ICCID) {
                                generateSimSerial()
                            }
                        if (value != null) {
                            YLog.debug("DeviceHooker: Spoofing getSimSerialNumber() -> $value")
                            result = value
                        }
                    }
                }

            // getSimSerialNumber(int subId) - Per-subscription ICCID
            runCatching {
                method {
                        name = "getSimSerialNumber"
                        param(IntType)
                    }
                    .hook {
                        after {
                            val value =
                                getSpoofValueOrGenerate(appContext, SpoofType.ICCID) {
                                    generateSimSerial()
                                }
                            if (value != null) {
                                YLog.debug(
                                    "DeviceHooker: Spoofing getSimSerialNumber(subId) -> $value"
                                )
                                result = value
                            }
                        }
                    }
            }
        }
    }

    /** Hooks Build class fields for device information. */
    private fun hookBuildClass() {
        // Hook Build.SERIAL static field
        "android.os.Build".toClass().apply {

            // Build.getSerial() method (API 26+)
            runCatching {
                method {
                        name = "getSerial"
                        modifiers { isStatic }
                    }
                    .hook {
                        after {
                            val value =
                                getSpoofValueOrGenerate(appContext, SpoofType.SERIAL) {
                                    fallbackSerial
                                }
                            if (value != null) {
                                YLog.debug("DeviceHooker: Spoofing Build.getSerial() -> $value")
                                result = value
                            }
                        }
                    }
            }
        }

        // For comprehensive coverage, hook SystemProperties.get() for ro.serialno
        hookSystemProperties()
    }

    /** Hooks SystemProperties for low-level property access. */
    private fun hookSystemProperties() {
        "android.os.SystemProperties".toClass().apply {
            method {
                    name = "get"
                    param(StringClass)
                }
                .hook {
                    after {
                        val key = args(0).string()
                        when (key) {
                            "ro.serialno",
                            "ro.boot.serialno",
                            "ril.serialnumber" -> {
                                val value =
                                    getSpoofValueOrGenerate(appContext, SpoofType.SERIAL) {
                                        fallbackSerial
                                    }
                                if (value != null) {
                                    YLog.debug(
                                        "DeviceHooker: Spoofing SystemProperties.get($key) -> $value"
                                    )
                                    result = value
                                }
                            }
                        }
                    }
                }

            method {
                    name = "get"
                    param(StringClass, StringClass)
                }
                .hook {
                    after {
                        val key = args(0).string()
                        when (key) {
                            "ro.serialno",
                            "ro.boot.serialno",
                            "ril.serialnumber" -> {
                                val value =
                                    getSpoofValueOrGenerate(appContext, SpoofType.SERIAL) {
                                        fallbackSerial
                                    }
                                if (value != null) {
                                    YLog.debug(
                                        "DeviceHooker: Spoofing SystemProperties.get($key, def) -> $value"
                                    )
                                    result = value
                                }
                            }
                        }
                    }
                }
        }
    }

    /** Hooks Settings.Secure for Android ID. */
    private fun hookSettingsSecure() {
        "android.provider.Settings\$Secure".toClass().apply {

            // Settings.Secure.getString(ContentResolver, String)
            method {
                    name = "getString"
                    param("android.content.ContentResolver".toClass(), StringClass)
                }
                .hook {
                    after {
                        val key = args(1).string()
                        if (key == "android_id") {
                            val value =
                                getSpoofValueOrGenerate(appContext, SpoofType.ANDROID_ID) {
                                    fallbackAndroidId
                                }
                            if (value != null) {
                                YLog.debug(
                                    "DeviceHooker: Spoofing Settings.Secure.android_id -> $value"
                                )
                                result = value
                            }
                        }
                    }
                }

            // Settings.Secure.getStringForUser (hidden API)
            runCatching {
                method {
                        name = "getStringForUser"
                        param("android.content.ContentResolver".toClass(), StringClass, IntType)
                    }
                    .hook {
                        after {
                            val key = args(1).string()
                            if (key == "android_id") {
                                val value =
                                    getSpoofValueOrGenerate(appContext, SpoofType.ANDROID_ID) {
                                        fallbackAndroidId
                                    }
                                if (value != null) {
                                    YLog.debug(
                                        "DeviceHooker: Spoofing Settings.Secure.android_id (user) -> $value"
                                    )
                                    result = value
                                }
                            }
                        }
                    }
            }
        }
    }
}
