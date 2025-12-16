package com.astrixforge.devicemasker.hook.hooker

import com.astrixforge.devicemasker.data.generators.UUIDGenerator
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.type.java.StringClass
import com.highcapable.yukihookapi.hook.log.YLog

/**
 * Advertising Identifier Hooker - Spoofs tracking and advertising IDs.
 *
 * Hooks:
 * - GSF ID (Google Services Framework): Gservices.getString("android_id")
 * - Advertising ID: AdvertisingIdClient.getAdvertisingIdInfo()
 * - Media DRM ID: MediaDrm.getPropertyByteArray()
 *
 * This hooker requires Phase 4 (DataStore) for persistent values.
 * Currently uses generator defaults.
 */
object AdvertisingHooker : YukiBaseHooker() {

    // ═══════════════════════════════════════════════════════════
    // CACHED SPOOFED VALUES
    // These will be replaced with DataStore reads in Phase 4
    // ═══════════════════════════════════════════════════════════

    private val spoofedGsfId: String by lazy { UUIDGenerator.generateGSFId() }
    private val spoofedAdvertisingId: String by lazy { UUIDGenerator.generateAdvertisingId() }
    private val spoofedMediaDrmId: ByteArray by lazy {
        UUIDGenerator.generateMediaDrmId().chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
    }

    override fun onHook() {
        YLog.debug("AdvertisingHooker: Starting hooks for package: $packageName")

        // ═══════════════════════════════════════════════════════════
        // GSF ID HOOKS
        // ═══════════════════════════════════════════════════════════

        hookGsfId()

        // ═══════════════════════════════════════════════════════════
        // ADVERTISING ID HOOKS
        // ═══════════════════════════════════════════════════════════

        hookAdvertisingId()

        // ═══════════════════════════════════════════════════════════
        // MEDIA DRM ID HOOKS
        // ═══════════════════════════════════════════════════════════

        hookMediaDrmId()

        YLog.debug("AdvertisingHooker: Hooks registered for package: $packageName")
    }

    /**
     * Hooks GSF ID (Google Services Framework ID).
     * This ID is retrieved via Gservices content provider.
     */
    private fun hookGsfId() {
        // Hook the Gservices class in Google Play Services
        runCatching {
            "com.google.android.gsf.Gservices".toClass().apply {

                // getString(ContentResolver, String) for GSF ID
                runCatching {
                    method {
                        name = "getString"
                        param("android.content.ContentResolver".toClass(), StringClass)
                    }.hook {
                        after {
                            val key = args(1).string()
                            if (key == "android_id") {
                                YLog.debug("AdvertisingHooker: Spoofing GSF android_id -> $spoofedGsfId")
                                result = spoofedGsfId
                            }
                        }
                    }
                }

                // getLong(ContentResolver, String, long) variant
                runCatching {
                    method {
                        name = "getLong"
                        paramCount = 3
                    }.hook {
                        after {
                            val key = args(1).string()
                            if (key == "android_id") {
                                // Convert hex GSF ID to long
                                val longValue = spoofedGsfId.toLongOrNull(16) ?: 0L
                                YLog.debug("AdvertisingHooker: Spoofing GSF android_id (long) -> $longValue")
                                result = longValue
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Hooks Advertising ID (Google Advertising ID / GAID).
     */
    private fun hookAdvertisingId() {
        // Hook AdvertisingIdClient in Google Play Services
        runCatching {
            "com.google.android.gms.ads.identifier.AdvertisingIdClient".toClass().apply {

                // getAdvertisingIdInfo(Context) returns Info object
                runCatching {
                    method {
                        name = "getAdvertisingIdInfo"
                        paramCount = 1
                    }.hook {
                        after {
                            YLog.debug("AdvertisingHooker: getAdvertisingIdInfo() called")
                        }
                    }
                }
            }
        }

        // Hook the Info class to return spoofed ID
        runCatching {
            "com.google.android.gms.ads.identifier.AdvertisingIdClient\$Info".toClass().apply {

                // getId() - Returns the advertising ID string
                runCatching {
                    method {
                        name = "getId"
                        emptyParam()
                    }.hook {
                        after {
                            YLog.debug("AdvertisingHooker: Spoofing AdvertisingId -> $spoofedAdvertisingId")
                            result = spoofedAdvertisingId
                        }
                    }
                }

                // isLimitAdTrackingEnabled() - Could also spoof this
                runCatching {
                    method {
                        name = "isLimitAdTrackingEnabled"
                        emptyParam()
                    }.hook {
                        after {
                            // Optionally return true to indicate user opted out
                            // result = true
                        }
                    }
                }
            }
        }

        // Hook Firebase Instance ID for apps using Firebase
        runCatching {
            "com.google.firebase.iid.FirebaseInstanceId".toClass().apply {

                runCatching {
                    method {
                        name = "getId"
                        emptyParam()
                    }.hook {
                        after {
                            val spoofedInstanceId = UUIDGenerator.generateInstanceId()
                            YLog.debug("AdvertisingHooker: Spoofing FirebaseInstanceId -> $spoofedInstanceId")
                            result = spoofedInstanceId
                        }
                    }
                }
            }
        }
    }

    /**
     * Hooks Media DRM ID (Widevine device ID).
     * Used for DRM licensing and can be used for tracking.
     */
    private fun hookMediaDrmId() {
        "android.media.MediaDrm".toClass().apply {

            // getPropertyByteArray(String) - Used to get device unique ID
            method {
                name = "getPropertyByteArray"
                param(StringClass)
            }.hook {
                after {
                    val propertyName = args(0).string()

                    // Common property names for device ID
                    if (propertyName in listOf("deviceUniqueId", "provisioningUniqueId", "serialNumber")) {
                        YLog.debug("AdvertisingHooker: Spoofing MediaDrm.$propertyName")
                        result = spoofedMediaDrmId
                    }
                }
            }

            // getPropertyString(String) - Some properties are strings
            method {
                name = "getPropertyString"
                param(StringClass)
            }.hook {
                after {
                    val propertyName = args(0).string()

                    // Spoof security level and other identifying properties
                    when (propertyName) {
                        "vendor" -> {
                            result = "Google"
                        }
                        "version" -> {
                            result = "1.0"
                        }
                    }
                }
            }
        }
    }
}
