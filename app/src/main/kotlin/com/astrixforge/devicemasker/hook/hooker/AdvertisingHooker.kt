package com.astrixforge.devicemasker.hook.hooker

import android.content.Context
import com.astrixforge.devicemasker.data.generators.UUIDGenerator
import com.astrixforge.devicemasker.data.models.SpoofType
import com.astrixforge.devicemasker.hook.HookDataProvider
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.type.java.StringClass

/**
 * Advertising Identifier Hooker - Spoofs tracking and advertising IDs.
 *
 * Hooks:
 * - GSF ID (Google Services Framework): Gservices.getString("android_id")
 * - Advertising ID: AdvertisingIdClient.getAdvertisingIdInfo()
 * - Media DRM ID: MediaDrm.getPropertyByteArray()
 *
 * Uses HookDataProvider to read profile-based values and global config.
 */
object AdvertisingHooker : YukiBaseHooker() {

    // ═══════════════════════════════════════════════════════════
    // DATA PROVIDER
    // ═══════════════════════════════════════════════════════════

    private var dataProvider: HookDataProvider? = null

    private fun getProvider(context: Context?): HookDataProvider? {
        if (dataProvider == null && context != null) {
            dataProvider =
                runCatching { HookDataProvider.getInstance(context, packageName) }
                    .onFailure {
                        YLog.error(
                            "AdvertisingHooker: Failed to create HookDataProvider: ${it.message}"
                        )
                    }
                    .getOrNull()
        }
        return dataProvider
    }

    private fun getSpoofValueOrGenerate(
        context: Context?,
        type: SpoofType,
        generator: () -> String,
    ): String? {
        val provider = getProvider(context)
        if (provider == null) {
            YLog.debug("AdvertisingHooker: No provider for $type, using generated value")
            return generator()
        }

        // getSpoofValue now handles all profile-based checks (profile exists, profile enabled, type
        // enabled)
        return provider.getSpoofValue(type) ?: generator()
    }

    // ═══════════════════════════════════════════════════════════
    // FALLBACK GENERATORS
    // ═══════════════════════════════════════════════════════════

    private val fallbackGsfId: String by lazy { UUIDGenerator.generateGSFId() }
    private val fallbackAdvertisingId: String by lazy { UUIDGenerator.generateAdvertisingId() }
    private val fallbackMediaDrmId: String by lazy { UUIDGenerator.generateMediaDrmId() }

    override fun onHook() {
        YLog.debug("AdvertisingHooker: Starting hooks for package: $packageName")

        hookGsfId()
        hookAdvertisingId()
        hookMediaDrmId()

        YLog.debug("AdvertisingHooker: Hooks registered for package: $packageName")
    }

    /** Hooks GSF ID (Google Services Framework ID). */
    private fun hookGsfId() {
        runCatching {
            "com.google.android.gsf.Gservices".toClass().apply {
                runCatching {
                    method {
                            name = "getString"
                            param("android.content.ContentResolver".toClass(), StringClass)
                        }
                        .hook {
                            after {
                                val key = args(1).string()
                                if (key == "android_id") {
                                    val value =
                                        getSpoofValueOrGenerate(appContext, SpoofType.GSF_ID) {
                                            fallbackGsfId
                                        }
                                    if (value != null) {
                                        YLog.debug(
                                            "AdvertisingHooker: Spoofing GSF android_id -> $value"
                                        )
                                        result = value
                                    }
                                }
                            }
                        }
                }

                runCatching {
                    method {
                            name = "getLong"
                            paramCount = 3
                        }
                        .hook {
                            after {
                                val key = args(1).string()
                                if (key == "android_id") {
                                    val value =
                                        getSpoofValueOrGenerate(appContext, SpoofType.GSF_ID) {
                                            fallbackGsfId
                                        }
                                    if (value != null) {
                                        val longValue = value.toLongOrNull(16) ?: 0L
                                        YLog.debug(
                                            "AdvertisingHooker: Spoofing GSF android_id (long) -> $longValue"
                                        )
                                        result = longValue
                                    }
                                }
                            }
                        }
                }
            }
        }
    }

    /** Hooks Advertising ID (Google Advertising ID / GAID). */
    private fun hookAdvertisingId() {
        runCatching {
            "com.google.android.gms.ads.identifier.AdvertisingIdClient".toClass().apply {
                runCatching {
                    method {
                            name = "getAdvertisingIdInfo"
                            paramCount = 1
                        }
                        .hook {
                            after { YLog.debug("AdvertisingHooker: getAdvertisingIdInfo() called") }
                        }
                }
            }
        }

        runCatching {
            "com.google.android.gms.ads.identifier.AdvertisingIdClient\$Info".toClass().apply {
                runCatching {
                    method {
                            name = "getId"
                            emptyParam()
                        }
                        .hook {
                            after {
                                val value =
                                    getSpoofValueOrGenerate(appContext, SpoofType.ADVERTISING_ID) {
                                        fallbackAdvertisingId
                                    }
                                if (value != null) {
                                    YLog.debug(
                                        "AdvertisingHooker: Spoofing AdvertisingId -> $value"
                                    )
                                    result = value
                                }
                            }
                        }
                }

                runCatching {
                    method {
                            name = "isLimitAdTrackingEnabled"
                            emptyParam()
                        }
                        .hook {
                            after {
                                // Optionally return true to indicate user opted out
                                // result = true
                            }
                        }
                }
            }
        }

        // Hook Firebase Instance ID
        runCatching {
            "com.google.firebase.iid.FirebaseInstanceId".toClass().apply {
                runCatching {
                    method {
                            name = "getId"
                            emptyParam()
                        }
                        .hook {
                            after {
                                val spoofedInstanceId = UUIDGenerator.generateInstanceId()
                                YLog.debug(
                                    "AdvertisingHooker: Spoofing FirebaseInstanceId -> $spoofedInstanceId"
                                )
                                result = spoofedInstanceId
                            }
                        }
                }
            }
        }
    }

    /** Hooks Media DRM ID (Widevine device ID). */
    private fun hookMediaDrmId() {
        "android.media.MediaDrm".toClass().apply {
            method {
                    name = "getPropertyByteArray"
                    param(StringClass)
                }
                .hook {
                    after {
                        val propertyName = args(0).string()

                        if (
                            propertyName in
                                listOf("deviceUniqueId", "provisioningUniqueId", "serialNumber")
                        ) {
                            val value =
                                getSpoofValueOrGenerate(appContext, SpoofType.MEDIA_DRM_ID) {
                                    fallbackMediaDrmId
                                }
                            if (value != null) {
                                val bytes =
                                    value.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
                                YLog.debug("AdvertisingHooker: Spoofing MediaDrm.$propertyName")
                                result = bytes
                            }
                        }
                    }
                }

            method {
                    name = "getPropertyString"
                    param(StringClass)
                }
                .hook {
                    after {
                        val propertyName = args(0).string()

                        when (propertyName) {
                            "vendor" -> result = "Google"
                            "version" -> result = "1.0"
                        }
                    }
                }
        }
    }
}
