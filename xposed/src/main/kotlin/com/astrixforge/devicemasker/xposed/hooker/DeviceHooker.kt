package com.astrixforge.devicemasker.xposed.hooker

import com.astrixforge.devicemasker.common.SpoofType
import com.astrixforge.devicemasker.xposed.PrefsHelper
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method
import com.astrixforge.devicemasker.xposed.DualLog
import com.highcapable.yukihookapi.hook.type.java.IntType
import com.highcapable.yukihookapi.hook.type.java.StringClass

/**
 * Device Identifier Hooker - Spoofs hardware and device identifiers.
 *
 * Hooks TelephonyManager, Build class, and Settings.Secure to spoof:
 * - IMEI, IMSI, ICCID (device & SIM identifiers)
 * - Serial number
 * - Android ID
 *
 * Uses PrefsHelper with YukiHookAPI's XSharedPreferences for cross-process config.
 */
object DeviceHooker : YukiBaseHooker() {

    private const val TAG = "DeviceHooker"

    // Fallback values generated lazily
    private val fallbackImei by lazy { generateImei() }
    private val fallbackSerial by lazy { generateSerial() }
    private val fallbackAndroidId by lazy { generateAndroidId() }

    private fun getSpoofValue(type: SpoofType, fallback: () -> String): String {
        return PrefsHelper.getSpoofValue(prefs, packageName, type, fallback)
    }

    override fun onHook() {
        DualLog.debug(TAG, "Starting hooks for: $packageName")

        hookTelephonyManager()
        hookSubscriptionInfo()  // NEW: Dual-SIM support
        hookBuildClass()
        hookSettingsSecure()

        // Hook count tracking removed - not needed for file-based config
    }

    private fun hookTelephonyManager() {
        runCatching {
            "android.telephony.TelephonyManager".toClass().apply {
                // getDeviceId()
                method {
                    name = "getDeviceId"
                    emptyParam()
                }.hook {
                    after {
                        result = getSpoofValue(SpoofType.IMEI) { fallbackImei }
                    }
                }

                method {
                    name = "getDeviceId"
                    param(IntType)
                }.hook {
                    after {
                        result = getSpoofValue(SpoofType.IMEI) { fallbackImei }
                    }
                }

                // getImei()
                method {
                    name = "getImei"
                    emptyParam()
                }.hook {
                    after {
                        result = getSpoofValue(SpoofType.IMEI) { fallbackImei }
                    }
                }

                method {
                    name = "getImei"
                    param(IntType)
                }.hook {
                    after {
                        result = getSpoofValue(SpoofType.IMEI) { fallbackImei }
                    }
                }

                // Note: getMeid() hooks removed - CDMA/MEID deprecated since 2022

                // getSubscriberId() - IMSI
                method {
                    name = "getSubscriberId"
                    emptyParam()
                }.hook {
                    after {
                        result = getSpoofValue(SpoofType.IMSI) { generateImsi() }
                    }
                }

                runCatching {
                    method {
                        name = "getSubscriberId"
                        param(IntType)
                    }.hook {
                        after {
                            result = getSpoofValue(SpoofType.IMSI) { generateImsi() }
                        }
                    }
                }

                // getSimSerialNumber() - ICCID
                method {
                    name = "getSimSerialNumber"
                    emptyParam()
                }.hook {
                    after {
                        result = getSpoofValue(SpoofType.ICCID) { generateSimSerial() }
                    }
                }

                runCatching {
                    method {
                        name = "getSimSerialNumber"
                        param(IntType)
                    }.hook {
                        after {
                            result = getSpoofValue(SpoofType.ICCID) { generateSimSerial() }
                        }
                    }
                }

                // ═══════════════════════════════════════════════════════════
                // NEW: Additional SIM hooks for comprehensive spoofing
                // ═══════════════════════════════════════════════════════════

                // getSimCountryIso() - SIM country code (e.g., "in" for India)
                method {
                    name = "getSimCountryIso"
                    emptyParam()
                }.hook {
                    after {
                        result = getSpoofValue(SpoofType.SIM_COUNTRY_ISO) { "us" }
                    }
                }

                runCatching {
                    method {
                        name = "getSimCountryIso"
                        param(IntType)
                    }.hook {
                        after {
                            result = getSpoofValue(SpoofType.SIM_COUNTRY_ISO) { "us" }
                        }
                    }
                }

                // getNetworkCountryIso() - Network country code
                method {
                    name = "getNetworkCountryIso"
                    emptyParam()
                }.hook {
                    after {
                        result = getSpoofValue(SpoofType.NETWORK_COUNTRY_ISO) { "us" }
                    }
                }

                runCatching {
                    method {
                        name = "getNetworkCountryIso"
                        param(IntType)
                    }.hook {
                        after {
                            result = getSpoofValue(SpoofType.NETWORK_COUNTRY_ISO) { "us" }
                        }
                    }
                }

                // getSimOperatorName() - SIM carrier name
                method {
                    name = "getSimOperatorName"
                    emptyParam()
                }.hook {
                    after {
                        result = getSpoofValue(SpoofType.SIM_OPERATOR_NAME) { "Carrier" }
                    }
                }

                runCatching {
                    method {
                        name = "getSimOperatorName"
                        param(IntType)
                    }.hook {
                        after {
                            result = getSpoofValue(SpoofType.SIM_OPERATOR_NAME) { "Carrier" }
                        }
                    }
                }

                // getNetworkOperator() - Network operator MCC+MNC (e.g., "310260")
                method {
                    name = "getNetworkOperator"
                    emptyParam()
                }.hook {
                    after {
                        result = getSpoofValue(SpoofType.NETWORK_OPERATOR) { "310260" }
                    }
                }

                runCatching {
                    method {
                        name = "getNetworkOperator"
                        param(IntType)
                    }.hook {
                        after {
                            result = getSpoofValue(SpoofType.NETWORK_OPERATOR) { "310260" }
                        }
                    }
                }

                // getSimOperator() - SIM operator MCC+MNC (same as CARRIER_MCC_MNC)
                method {
                    name = "getSimOperator"
                    emptyParam()
                }.hook {
                    after {
                        result = getSpoofValue(SpoofType.CARRIER_MCC_MNC) { "310260" }
                    }
                }

                runCatching {
                    method {
                        name = "getSimOperator"
                        param(IntType)
                    }.hook {
                        after {
                            result = getSpoofValue(SpoofType.CARRIER_MCC_MNC) { "310260" }
                        }
                    }
                }
            }
        }
    }

    /**
     * Hooks SubscriptionInfo for dual-SIM support.
     * 
     * Some apps query SubscriptionInfo directly instead of TelephonyManager.
     * We need to hook these to ensure consistent spoof values across all APIs.
     */
    private fun hookSubscriptionInfo() {
        runCatching {
            "android.telephony.SubscriptionInfo".toClass().apply {
                
                // getCountryIso() - Returns SIM country code
                method {
                    name = "getCountryIso"
                    emptyParam()
                }.hook {
                    after {
                        result = getSpoofValue(SpoofType.SIM_COUNTRY_ISO) { "us" }
                    }
                }

                // getCarrierName() - Returns carrier display name
                method {
                    name = "getCarrierName"
                    emptyParam()
                }.hook {
                    after {
                        val spoofedName = getSpoofValue(SpoofType.CARRIER_NAME) { "Carrier" }
                        // SubscriptionInfo.getCarrierName() returns CharSequence
                        result = spoofedName as CharSequence
                    }
                }

                // getDisplayName() - User-visible subscription name
                method {
                    name = "getDisplayName"
                    emptyParam()
                }.hook {
                    after {
                        val spoofedName = getSpoofValue(SpoofType.CARRIER_NAME) { "Carrier" }
                        result = spoofedName as CharSequence
                    }
                }

                // getMcc() - Mobile Country Code (returns int)
                runCatching {
                    method {
                        name = "getMcc"
                        emptyParam()
                    }.hook {
                        after {
                            val mccMnc = getSpoofValue(SpoofType.CARRIER_MCC_MNC) { "310260" }
                            // MCC is first 3 digits
                            result = mccMnc.take(3).toIntOrNull() ?: 310
                        }
                    }
                }

                // getMnc() - Mobile Network Code (returns int)
                runCatching {
                    method {
                        name = "getMnc"
                        emptyParam()
                    }.hook {
                        after {
                            val mccMnc = getSpoofValue(SpoofType.CARRIER_MCC_MNC) { "310260" }
                            // MNC is digits after first 3
                            result = mccMnc.drop(3).toIntOrNull() ?: 260
                        }
                    }
                }

                // getMccString() - MCC as string (API 29+)
                runCatching {
                    method {
                        name = "getMccString"
                        emptyParam()
                    }.hook {
                        after {
                            val mccMnc = getSpoofValue(SpoofType.CARRIER_MCC_MNC) { "310260" }
                            result = mccMnc.take(3)
                        }
                    }
                }

                // getMncString() - MNC as string (API 29+)
                runCatching {
                    method {
                        name = "getMncString"
                        emptyParam()
                    }.hook {
                        after {
                            val mccMnc = getSpoofValue(SpoofType.CARRIER_MCC_MNC) { "310260" }
                            result = mccMnc.drop(3)
                        }
                    }
                }

                // getIccId() - ICCID for this subscription
                runCatching {
                    method {
                        name = "getIccId"
                        emptyParam()
                    }.hook {
                        after {
                            result = getSpoofValue(SpoofType.ICCID) { generateSimSerial() }
                        }
                    }
                }

                // getNumber() - Phone number for this subscription  
                runCatching {
                    method {
                        name = "getNumber"
                        emptyParam()
                    }.hook {
                        after {
                            result = getSpoofValue(SpoofType.PHONE_NUMBER) { "+1234567890" }
                        }
                    }
                }
            }
        }.onFailure {
            // SubscriptionInfo may not be available on all devices/API levels
            DualLog.debug(TAG, "SubscriptionInfo hooks skipped: ${it.message}")
        }
    }

    private fun hookBuildClass() {
        runCatching {
            "android.os.Build".toClass().apply {
                runCatching {
                    method {
                        name = "getSerial"
                        modifiers { isStatic }
                    }.hook {
                        after {
                            result = getSpoofValue(SpoofType.SERIAL) { fallbackSerial }
                        }
                    }
                }
            }
        }
        hookSystemProperties()
    }

    private fun hookSystemProperties() {
        runCatching {
            "android.os.SystemProperties".toClass().apply {
                method {
                    name = "get"
                    param(StringClass)
                }.hook {
                    after {
                        val key = args(0).string()
                        if (key in listOf("ro.serialno", "ro.boot.serialno", "ril.serialnumber")) {
                            result = getSpoofValue(SpoofType.SERIAL) { fallbackSerial }
                        }
                    }
                }

                method {
                    name = "get"
                    param(StringClass, StringClass)
                }.hook {
                    after {
                        val key = args(0).string()
                        if (key in listOf("ro.serialno", "ro.boot.serialno", "ril.serialnumber")) {
                            result = getSpoofValue(SpoofType.SERIAL) { fallbackSerial }
                        }
                    }
                }
            }
        }
    }

    private fun hookSettingsSecure() {
        runCatching {
            "android.provider.Settings\$Secure".toClass().apply {
                method {
                    name = "getString"
                    param("android.content.ContentResolver".toClass(), StringClass)
                }.hook {
                    after {
                        if (args(1).string() == "android_id") {
                            result = getSpoofValue(SpoofType.ANDROID_ID) { fallbackAndroidId }
                        }
                    }
                }

                runCatching {
                    method {
                        name = "getStringForUser"
                        param("android.content.ContentResolver".toClass(), StringClass, IntType)
                    }.hook {
                        after {
                            if (args(1).string() == "android_id") {
                                result = getSpoofValue(SpoofType.ANDROID_ID) { fallbackAndroidId }
                            }
                        }
                    }
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    // GENERATORS (fallback values)
    // ═══════════════════════════════════════════════════════════

    private fun generateImei(): String {
        // TAC (Type Allocation Code) + Serial + Luhn checksum
        val tac = listOf("35", "86", "01", "45").random()
        val serial = (1000000..9999999).random().toString()
        val base = tac + serial.padStart(12, '0').take(12)
        return base + calculateLuhn(base)
    }

    private fun calculateLuhn(digits: String): Char {
        var sum = 0
        for ((index, char) in digits.reversed().withIndex()) {
            var digit = char.digitToInt()
            if (index % 2 == 0) {
                digit *= 2
                if (digit > 9) digit -= 9
            }
            sum += digit
        }
        return ((10 - (sum % 10)) % 10).digitToChar()
    }

    private fun generateSerial(): String {
        val chars = "0123456789ABCDEF"
        return (1..16).map { chars.random() }.joinToString("")
    }

    private fun generateAndroidId(): String {
        val chars = "0123456789abcdef"
        return (1..16).map { chars.random() }.joinToString("")
    }

    private fun generateImsi(): String {
        return "310260" + (100000000L..999999999L).random().toString()
    }

    private fun generateSimSerial(): String {
        return "8901" + List(16) { (0..9).random() }.joinToString("")
    }
}
