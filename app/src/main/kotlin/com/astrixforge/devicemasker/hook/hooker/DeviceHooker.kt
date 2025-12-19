package com.astrixforge.devicemasker.hook.hooker

import android.content.Context
import com.astrixforge.devicemasker.data.generators.IMEIGenerator
import com.astrixforge.devicemasker.data.generators.SerialGenerator
import com.astrixforge.devicemasker.data.generators.UUIDGenerator
import com.astrixforge.devicemasker.data.models.SpoofType
import com.astrixforge.devicemasker.hook.HookDataProvider
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.*
import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.type.java.IntType
import com.highcapable.yukihookapi.hook.type.java.StringClass

/**
 * Device Identifier Hooker - Spoofs hardware and device identifiers.
 */
object DeviceHooker : YukiBaseHooker() {

    private var dataProvider: HookDataProvider? = null

    private fun getProvider(context: Context?): HookDataProvider? {
        if (dataProvider == null && context != null) {
            dataProvider = runCatching { HookDataProvider.getInstance(context, packageName) }.getOrNull()
        }
        return dataProvider
    }

    private fun getSpoofValueOrGenerate(
        context: Context?,
        type: SpoofType,
        generator: () -> String,
    ): String {
        return getProvider(context)?.getSpoofValue(type) ?: generator()
    }

    private val fallbackImei by lazy { IMEIGenerator.generate() }
    private val fallbackMeid by lazy { IMEIGenerator.generate() }
    private val fallbackSerial by lazy { SerialGenerator.generate() }
    private val fallbackAndroidId by lazy { UUIDGenerator.generateAndroidId() }

    private fun generateImsi() = "310" + "260" + (100000000L..999999999L).random().toString()
    private fun generateSimSerial() = "8901" + List(16) { (0..9).random() }.joinToString("")

    override fun onHook() {
        YLog.debug("DeviceHooker: Starting hooks for package: $packageName")
        hookTelephonyManager()
        hookBuildClass()
        hookSettingsSecure()
    }

    private fun hookTelephonyManager() {
        runCatching {
            "android.telephony.TelephonyManager".toClass().apply {
                method {
                    name = "getDeviceId"
                    emptyParam()
                }.hook {
                    after {
                        val value = getSpoofValueOrGenerate(appContext, SpoofType.IMEI) { fallbackImei }
                        result = value
                    }
                }

                method {
                    name = "getDeviceId"
                    param(IntType)
                }.hook {
                    after {
                        val value = getSpoofValueOrGenerate(appContext, SpoofType.IMEI) { fallbackImei }
                        result = value
                    }
                }

                method {
                    name = "getImei"
                    emptyParam()
                }.hook {
                    after {
                        val value = getSpoofValueOrGenerate(appContext, SpoofType.IMEI) { fallbackImei }
                        result = value
                    }
                }

                method {
                    name = "getImei"
                    param(IntType)
                }.hook {
                    after {
                        val value = getSpoofValueOrGenerate(appContext, SpoofType.IMEI) { fallbackImei }
                        result = value
                    }
                }

                runCatching {
                    method {
                        name = "getMeid"
                        emptyParam()
                    }.hook {
                        after {
                            val value = getSpoofValueOrGenerate(appContext, SpoofType.MEID) { fallbackMeid }
                            result = value
                        }
                    }
                }

                runCatching {
                    method {
                        name = "getMeid"
                        param(IntType)
                    }.hook {
                        after {
                            val value = getSpoofValueOrGenerate(appContext, SpoofType.MEID) { fallbackMeid }
                            result = value
                        }
                    }
                }

                method {
                    name = "getSubscriberId"
                    emptyParam()
                }.hook {
                    after {
                        val value = getSpoofValueOrGenerate(appContext, SpoofType.IMSI) { generateImsi() }
                        result = value
                    }
                }

                runCatching {
                    method {
                        name = "getSubscriberId"
                        param(IntType)
                    }.hook {
                        after {
                            val value = getSpoofValueOrGenerate(appContext, SpoofType.IMSI) { generateImsi() }
                            result = value
                        }
                    }
                }

                method {
                    name = "getSimSerialNumber"
                    emptyParam()
                }.hook {
                    after {
                        val value = getSpoofValueOrGenerate(appContext, SpoofType.ICCID) { generateSimSerial() }
                        result = value
                    }
                }

                runCatching {
                    method {
                        name = "getSimSerialNumber"
                        param(IntType)
                    }.hook {
                        after {
                            val value = getSpoofValueOrGenerate(appContext, SpoofType.ICCID) { generateSimSerial() }
                            result = value
                        }
                    }
                }
            }
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
                            val value = getSpoofValueOrGenerate(appContext, SpoofType.SERIAL) { fallbackSerial }
                            result = value
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
                            result = getSpoofValueOrGenerate(appContext, SpoofType.SERIAL) { fallbackSerial }
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
                            result = getSpoofValueOrGenerate(appContext, SpoofType.SERIAL) { fallbackSerial }
                        }
                    }
                }
            }
        }
    }

    private fun hookSettingsSecure() {
        runCatching {
            $$"android.provider.Settings$Secure".toClass().apply {
                method {
                    name = "getString"
                    param("android.content.ContentResolver".toClass(), StringClass)
                }.hook {
                    after {
                        if (args(1).string() == "android_id") {
                            result = getSpoofValueOrGenerate(appContext, SpoofType.ANDROID_ID) { fallbackAndroidId }
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
                                result = getSpoofValueOrGenerate(appContext, SpoofType.ANDROID_ID) { fallbackAndroidId }
                            }
                        }
                    }
                }
            }
        }
    }
}
