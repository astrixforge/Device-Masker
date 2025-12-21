package com.astrixforge.devicemasker.xposed.hooker

import com.astrixforge.devicemasker.common.SpoofType
import com.astrixforge.devicemasker.xposed.DeviceMaskerService
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.type.java.StringClass

/**
 * Advertising Hooker - Spoofs advertising and tracking identifiers.
 *
 * Hooks for:
 * - Google Advertising ID
 * - GSF ID (Google Services Framework)
 * - Media DRM ID
 */
object AdvertisingHooker : YukiBaseHooker() {

    private val fallbackAdvertisingId by lazy { java.util.UUID.randomUUID().toString() }
    private val fallbackGsfId by lazy { generateHexId(16) }
    private val fallbackMediaDrmId by lazy { generateHexId(64) }

    private fun getSpoofValue(type: SpoofType, fallback: () -> String): String {
        val service = DeviceMaskerService.instance ?: return fallback()
        val config = service.config
        val profile = config.getProfileForApp(packageName) ?: return fallback()

        if (!profile.isEnabled) return fallback()
        if (!profile.isTypeEnabled(type)) return fallback()

        return profile.getValue(type) ?: fallback()
    }

    override fun onHook() {
        YLog.debug("AdvertisingHooker: Starting hooks for: $packageName")

        hookAdvertisingIdClient()
        hookGooglePlayServices()
        hookMediaDrm()

        DeviceMaskerService.instance?.incrementHookCount()
    }

    private fun hookAdvertisingIdClient() {
        // Hook AdvertisingIdClient.Info
        runCatching {
            "com.google.android.gms.ads.identifier.AdvertisingIdClient\$Info".toClass().apply {
                method {
                    name = "getId"
                    emptyParam()
                }.hook {
                    after {
                        result = getSpoofValue(SpoofType.ADVERTISING_ID) { fallbackAdvertisingId }
                    }
                }
            }
        }

        // Alternative package
        runCatching {
            "com.google.android.gms.common.api.GoogleApiClient".toClass()
        }
    }

    private fun hookGooglePlayServices() {
        // Hook GSF ID from Settings.Secure
        runCatching {
            "android.provider.Settings\$Secure".toClass().apply {
                method {
                    name = "getString"
                    param("android.content.ContentResolver".toClass(), StringClass)
                }.hook {
                    after {
                        val key = args(1).string()
                        if (key == "android_id") {
                            // Already handled by DeviceHooker
                        }
                    }
                }
            }
        }

        // Hook GServices directly
        runCatching {
            "com.google.android.gsf.Gservices".toClass().apply {
                method {
                    name = "getString"
                    paramCount = 2
                }.hook {
                    after {
                        val key = args(1).string()
                        if (key == "android_id") {
                            result = getSpoofValue(SpoofType.GSF_ID) { fallbackGsfId }
                        }
                    }
                }

                method {
                    name = "getLong"
                    paramCount = 3
                }.hook {
                    after {
                        val key = args(1).string()
                        if (key == "android_id") {
                            val spoofed = getSpoofValue(SpoofType.GSF_ID) { fallbackGsfId }
                            result = try {
                                spoofed.toLong(16)
                            } catch (e: Exception) {
                                result
                            }
                        }
                    }
                }
            }
        }
    }

    private fun hookMediaDrm() {
        runCatching {
            "android.media.MediaDrm".toClass().apply {
                method {
                    name = "getPropertyByteArray"
                    param(StringClass)
                }.hook {
                    after {
                        val property = args(0).string()
                        if (property == "deviceUniqueId") {
                            val spoofed = getSpoofValue(SpoofType.MEDIA_DRM_ID) { fallbackMediaDrmId }
                            result = hexToBytes(spoofed)
                        }
                    }
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    // GENERATORS
    // ═══════════════════════════════════════════════════════════

    private fun generateHexId(length: Int): String {
        val chars = "0123456789abcdef"
        return (1..length).map { chars.random() }.joinToString("")
    }

    private fun hexToBytes(hex: String): ByteArray {
        return try {
            hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        } catch (e: Exception) {
            ByteArray(32)
        }
    }
}
