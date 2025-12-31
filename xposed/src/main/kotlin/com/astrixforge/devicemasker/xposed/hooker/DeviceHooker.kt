package com.astrixforge.devicemasker.xposed.hooker

import com.astrixforge.devicemasker.common.SpoofType
import com.astrixforge.devicemasker.xposed.utils.ValueGenerators
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.type.java.IntType
import com.highcapable.yukihookapi.hook.type.java.StringClass

/**
 * Device Identifier Hooker - Spoofs hardware and device identifiers.
 *
 * Hooks TelephonyManager, Build class, and Settings.Secure to spoof:
 * - IMEI, IMSI, ICCID (device & SIM identifiers)
 * - Serial number
 * - Android ID
 */
object DeviceHooker : BaseSpoofHooker("DeviceHooker") {

    // Cached class references
    private val telephonyClass by lazy { "android.telephony.TelephonyManager".toClass() }
    private val subscriptionInfoClass by lazy {
        "android.telephony.SubscriptionInfo".toClassOrNull()
    }

    // Fallback values (thread-safe lazy)
    private val fallbackImei by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { ValueGenerators.imei() }
    private val fallbackSerial by
        lazy(LazyThreadSafetyMode.SYNCHRONIZED) { ValueGenerators.serial() }
    private val fallbackAndroidId by
        lazy(LazyThreadSafetyMode.SYNCHRONIZED) { ValueGenerators.androidId() }

    override fun onHook() {
        logStart()
        hookTelephonyManager()
        hookSubscriptionInfo()
        hookBuildClass()
        hookSettingsSecure()
        recordSuccess()
    }

    // ═══════════════════════════════════════════════════════════
    // TELEPHONY MANAGER HOOKS
    // ═══════════════════════════════════════════════════════════

    private fun hookTelephonyManager() {
        // Cache spoof values at registration time
        val cachedImei = getSpoofValue(SpoofType.IMEI) { fallbackImei }
        val cachedImsi = getSpoofValue(SpoofType.IMSI) { ValueGenerators.imsi() }
        val cachedIccid = getSpoofValue(SpoofType.ICCID) { ValueGenerators.iccid() }

        telephonyClass.apply {
            // getDeviceId() overloads
            method {
                    name = "getDeviceId"
                    emptyParam()
                }
                .hook { replaceAny { cachedImei } }
            method {
                    name = "getDeviceId"
                    param(IntType)
                }
                .hook { replaceAny { cachedImei } }

            // getImei() overloads
            method {
                    name = "getImei"
                    emptyParam()
                }
                .hook { replaceAny { cachedImei } }
            method {
                    name = "getImei"
                    param(IntType)
                }
                .hook { replaceAny { cachedImei } }

            // getSubscriberId (IMSI) overloads
            method {
                    name = "getSubscriberId"
                    emptyParam()
                }
                .hook { replaceAny { cachedImsi } }
            runCatching {
                method {
                        name = "getSubscriberId"
                        param(IntType)
                    }
                    .hook { replaceAny { cachedImsi } }
            }

            // getSimSerialNumber (ICCID) overloads
            method {
                    name = "getSimSerialNumber"
                    emptyParam()
                }
                .hook { after { result = cachedIccid } }
            runCatching {
                method {
                        name = "getSimSerialNumber"
                        param(IntType)
                    }
                    .hook { after { result = cachedIccid } }
            }

            // SIM Hooks
            method {
                    name = "getSimCountryIso"
                    emptyParam()
                }
                .hook { after { result = getSpoofValue(SpoofType.SIM_COUNTRY_ISO) { "us" } } }
            runCatching {
                method {
                        name = "getSimCountryIso"
                        param(IntType)
                    }
                    .hook { after { result = getSpoofValue(SpoofType.SIM_COUNTRY_ISO) { "us" } } }
            }

            method {
                    name = "getNetworkCountryIso"
                    emptyParam()
                }
                .hook { after { result = getSpoofValue(SpoofType.NETWORK_COUNTRY_ISO) { "us" } } }
            runCatching {
                method {
                        name = "getNetworkCountryIso"
                        param(IntType)
                    }
                    .hook {
                        after { result = getSpoofValue(SpoofType.NETWORK_COUNTRY_ISO) { "us" } }
                    }
            }

            method {
                    name = "getSimOperatorName"
                    emptyParam()
                }
                .hook {
                    after { result = getSpoofValue(SpoofType.SIM_OPERATOR_NAME) { "Carrier" } }
                }
            runCatching {
                method {
                        name = "getSimOperatorName"
                        param(IntType)
                    }
                    .hook {
                        after { result = getSpoofValue(SpoofType.SIM_OPERATOR_NAME) { "Carrier" } }
                    }
            }

            method {
                    name = "getNetworkOperator"
                    emptyParam()
                }
                .hook { after { result = getSpoofValue(SpoofType.NETWORK_OPERATOR) { "310260" } } }
            runCatching {
                method {
                        name = "getNetworkOperator"
                        param(IntType)
                    }
                    .hook {
                        after { result = getSpoofValue(SpoofType.NETWORK_OPERATOR) { "310260" } }
                    }
            }

            method {
                    name = "getSimOperator"
                    emptyParam()
                }
                .hook { after { result = getSpoofValue(SpoofType.CARRIER_MCC_MNC) { "310260" } } }
            runCatching {
                method {
                        name = "getSimOperator"
                        param(IntType)
                    }
                    .hook {
                        after { result = getSpoofValue(SpoofType.CARRIER_MCC_MNC) { "310260" } }
                    }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    // SUBSCRIPTION INFO HOOKS (Dual-SIM support)
    // ═══════════════════════════════════════════════════════════

    private fun hookSubscriptionInfo() {
        subscriptionInfoClass?.apply {
            runCatching {
                method {
                        name = "getCountryIso"
                        emptyParam()
                    }
                    .hook { after { result = getSpoofValue(SpoofType.SIM_COUNTRY_ISO) { "us" } } }
            }
            runCatching {
                method {
                        name = "getCarrierName"
                        emptyParam()
                    }
                    .hook {
                        after {
                            result =
                                getSpoofValue(SpoofType.CARRIER_NAME) { "Carrier" } as CharSequence
                        }
                    }
            }
            runCatching {
                method {
                        name = "getDisplayName"
                        emptyParam()
                    }
                    .hook {
                        after {
                            result =
                                getSpoofValue(SpoofType.CARRIER_NAME) { "Carrier" } as CharSequence
                        }
                    }
            }
            runCatching {
                method {
                        name = "getMcc"
                        emptyParam()
                    }
                    .hook {
                        after {
                            val mccMnc = getSpoofValue(SpoofType.CARRIER_MCC_MNC) { "310260" }
                            result = mccMnc.take(3).toIntOrNull() ?: 310
                        }
                    }
            }
            runCatching {
                method {
                        name = "getMnc"
                        emptyParam()
                    }
                    .hook {
                        after {
                            val mccMnc = getSpoofValue(SpoofType.CARRIER_MCC_MNC) { "310260" }
                            result = mccMnc.drop(3).toIntOrNull() ?: 260
                        }
                    }
            }
            runCatching {
                method {
                        name = "getMccString"
                        emptyParam()
                    }
                    .hook {
                        after {
                            val mccMnc = getSpoofValue(SpoofType.CARRIER_MCC_MNC) { "310260" }
                            result = mccMnc.take(3)
                        }
                    }
            }
            runCatching {
                method {
                        name = "getMncString"
                        emptyParam()
                    }
                    .hook {
                        after {
                            val mccMnc = getSpoofValue(SpoofType.CARRIER_MCC_MNC) { "310260" }
                            result = mccMnc.drop(3)
                        }
                    }
            }
            runCatching {
                method {
                        name = "getIccId"
                        emptyParam()
                    }
                    .hook {
                        after {
                            result = getSpoofValue(SpoofType.ICCID) { ValueGenerators.iccid() }
                        }
                    }
            }
            runCatching {
                method {
                        name = "getNumber"
                        emptyParam()
                    }
                    .hook {
                        after { result = getSpoofValue(SpoofType.PHONE_NUMBER) { "+1234567890" } }
                    }
            }
        } ?: logDebug("SubscriptionInfo class not available")
    }

    // ═══════════════════════════════════════════════════════════
    // BUILD CLASS HOOKS
    // ═══════════════════════════════════════════════════════════

    private fun hookBuildClass() {
        runCatching {
            "android.os.Build".toClass().apply {
                method {
                        name = "getSerial"
                        modifiers { isStatic }
                    }
                    .hook { after { result = getSpoofValue(SpoofType.SERIAL) { fallbackSerial } } }
            }
        }
        hookSystemProperties()
    }

    private fun hookSystemProperties() {
        "android.os.SystemProperties".toClassOrNull()?.apply {
            val serialKeys = listOf("ro.serialno", "ro.boot.serialno", "ril.serialnumber")

            method {
                    name = "get"
                    param(StringClass)
                }
                .hook {
                    after {
                        val key = args(0).string()
                        if (key in serialKeys) {
                            result = getSpoofValue(SpoofType.SERIAL) { fallbackSerial }
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
                        if (key in serialKeys) {
                            result = getSpoofValue(SpoofType.SERIAL) { fallbackSerial }
                        }
                    }
                }
        }
    }

    // ═══════════════════════════════════════════════════════════
    // SETTINGS SECURE HOOKS
    // ═══════════════════════════════════════════════════════════

    private fun hookSettingsSecure() {
        "android.provider.Settings\$Secure".toClass().apply {
            method {
                    name = "getString"
                    param("android.content.ContentResolver".toClass(), StringClass)
                }
                .hook {
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
                    }
                    .hook {
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
