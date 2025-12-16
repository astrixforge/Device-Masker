package com.astrixforge.devicemasker.hook.hooker

import com.astrixforge.devicemasker.data.generators.IMEIGenerator
import com.astrixforge.devicemasker.data.generators.SerialGenerator
import com.astrixforge.devicemasker.data.generators.UUIDGenerator
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.type.java.IntType
import com.highcapable.yukihookapi.hook.type.java.StringClass
import com.highcapable.yukihookapi.hook.log.YLog

/**
 * Device Identifier Hooker - Spoofs hardware and device identifiers.
 *
 * Hooks:
 * - TelephonyManager: getDeviceId(), getImei(), getMeid(), getSubscriberId(), getSimSerialNumber()
 * - Build: SERIAL, getSerial(), MODEL, MANUFACTURER, BRAND, DEVICE, PRODUCT, etc.
 * - Settings.Secure: getString() for android_id
 *
 * This hooker requires Phase 4 (DataStore) for persistent values.
 * Currently uses generator defaults.
 */
object DeviceHooker : YukiBaseHooker() {

    // ═══════════════════════════════════════════════════════════
    // CACHED SPOOFED VALUES
    // These will be replaced with DataStore reads in Phase 4
    // ═══════════════════════════════════════════════════════════

    private val spoofedImei: String by lazy { IMEIGenerator.generate() }
    private val spoofedMeid: String by lazy { IMEIGenerator.generate() }
    private val spoofedImsi: String by lazy { generateImsi() }
    private val spoofedSerial: String by lazy { SerialGenerator.generate() }
    private val spoofedAndroidId: String by lazy { UUIDGenerator.generateAndroidId() }
    private val spoofedSimSerial: String by lazy { generateSimSerial() }

    /**
     * Generates a fake IMSI (15 digits: MCC-MNC-MSIN).
     */
    private fun generateImsi(): String {
        // MCC (3 digits) + MNC (2-3 digits) + MSIN (9-10 digits)
        return "310" + "260" + (100000000L..999999999L).random().toString()
    }

    /**
     * Generates a fake SIM serial number (ICCID - 19-20 digits).
     */
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

    /**
     * Hooks TelephonyManager methods for device identifiers.
     */
    private fun hookTelephonyManager() {
        "android.telephony.TelephonyManager".toClass().apply {

            // getDeviceId() - Legacy device ID (IMEI/MEID)
            method {
                name = "getDeviceId"
                emptyParam()
            }.hook {
                after {
                    YLog.debug("DeviceHooker: Spoofing getDeviceId() -> $spoofedImei")
                    result = spoofedImei
                }
            }

            // getDeviceId(int slotIndex) - Per-slot device ID
            method {
                name = "getDeviceId"
                param(IntType)
            }.hook {
                after {
                    YLog.debug("DeviceHooker: Spoofing getDeviceId(slot) -> $spoofedImei")
                    result = spoofedImei
                }
            }

            // getImei() - IMEI with no slot parameter
            method {
                name = "getImei"
                emptyParam()
            }.hook {
                after {
                    YLog.debug("DeviceHooker: Spoofing getImei() -> $spoofedImei")
                    result = spoofedImei
                }
            }

            // getImei(int slotIndex) - Per-slot IMEI
            method {
                name = "getImei"
                param(IntType)
            }.hook {
                after {
                    YLog.debug("DeviceHooker: Spoofing getImei(slot) -> $spoofedImei")
                    result = spoofedImei
                }
            }

            // getMeid() - MEID for CDMA devices (may not exist on all devices)
            runCatching {
                method {
                    name = "getMeid"
                    emptyParam()
                }.hook {
                    after {
                        YLog.debug("DeviceHooker: Spoofing getMeid() -> $spoofedMeid")
                        result = spoofedMeid
                    }
                }
            }

            // getMeid(int slotIndex) - Per-slot MEID
            runCatching {
                method {
                    name = "getMeid"
                    param(IntType)
                }.hook {
                    after {
                        YLog.debug("DeviceHooker: Spoofing getMeid(slot) -> $spoofedMeid")
                        result = spoofedMeid
                    }
                }
            }

            // getSubscriberId() - IMSI
            method {
                name = "getSubscriberId"
                emptyParam()
            }.hook {
                after {
                    YLog.debug("DeviceHooker: Spoofing getSubscriberId() -> $spoofedImsi")
                    result = spoofedImsi
                }
            }

            // getSubscriberId(int subId) - Per-subscription IMSI
            runCatching {
                method {
                    name = "getSubscriberId"
                    param(IntType)
                }.hook {
                    after {
                        YLog.debug("DeviceHooker: Spoofing getSubscriberId(subId) -> $spoofedImsi")
                        result = spoofedImsi
                    }
                }
            }

            // getSimSerialNumber() - ICCID
            method {
                name = "getSimSerialNumber"
                emptyParam()
            }.hook {
                after {
                    YLog.debug("DeviceHooker: Spoofing getSimSerialNumber() -> $spoofedSimSerial")
                    result = spoofedSimSerial
                }
            }

            // getSimSerialNumber(int subId) - Per-subscription ICCID
            runCatching {
                method {
                    name = "getSimSerialNumber"
                    param(IntType)
                }.hook {
                    after {
                        YLog.debug("DeviceHooker: Spoofing getSimSerialNumber(subId) -> $spoofedSimSerial")
                        result = spoofedSimSerial
                    }
                }
            }
        }
    }

    /**
     * Hooks Build class fields for device information.
     */
    private fun hookBuildClass() {
        // Hook Build.SERIAL static field
        "android.os.Build".toClass().apply {

            // Build.getSerial() method (API 26+)
            runCatching {
                method {
                    name = "getSerial"
                    modifiers { isStatic }
                }.hook {
                    after {
                        YLog.debug("DeviceHooker: Spoofing Build.getSerial() -> $spoofedSerial")
                        result = spoofedSerial
                    }
                }
            }
        }

        // For comprehensive coverage, hook SystemProperties.get() for ro.serialno
        hookSystemProperties()
    }

    /**
     * Hooks SystemProperties for low-level property access.
     */
    private fun hookSystemProperties() {
        "android.os.SystemProperties".toClass().apply {

            method {
                name = "get"
                param(StringClass)
            }.hook {
                after {
                    val key = args(0).string()
                    when (key) {
                        "ro.serialno", "ro.boot.serialno", "ril.serialnumber" -> {
                            YLog.debug("DeviceHooker: Spoofing SystemProperties.get($key) -> $spoofedSerial")
                            result = spoofedSerial
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
                    when (key) {
                        "ro.serialno", "ro.boot.serialno", "ril.serialnumber" -> {
                            YLog.debug("DeviceHooker: Spoofing SystemProperties.get($key, def) -> $spoofedSerial")
                            result = spoofedSerial
                        }
                    }
                }
            }
        }
    }

    /**
     * Hooks Settings.Secure for Android ID.
     */
    private fun hookSettingsSecure() {
        "android.provider.Settings\$Secure".toClass().apply {

            // Settings.Secure.getString(ContentResolver, String)
            method {
                name = "getString"
                param("android.content.ContentResolver".toClass(), StringClass)
            }.hook {
                after {
                    val key = args(1).string()
                    if (key == "android_id") {
                        YLog.debug("DeviceHooker: Spoofing Settings.Secure.android_id -> $spoofedAndroidId")
                        result = spoofedAndroidId
                    }
                }
            }

            // Settings.Secure.getStringForUser (hidden API)
            runCatching {
                method {
                    name = "getStringForUser"
                    param("android.content.ContentResolver".toClass(), StringClass, IntType)
                }.hook {
                    after {
                        val key = args(1).string()
                        if (key == "android_id") {
                            YLog.debug("DeviceHooker: Spoofing Settings.Secure.android_id (user) -> $spoofedAndroidId")
                            result = spoofedAndroidId
                        }
                    }
                }
            }
        }
    }
}
