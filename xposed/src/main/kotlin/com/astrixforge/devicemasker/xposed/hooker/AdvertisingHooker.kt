package com.astrixforge.devicemasker.xposed.hooker

import com.astrixforge.devicemasker.common.SpoofType
import com.astrixforge.devicemasker.xposed.PrefsHelper
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method
import com.astrixforge.devicemasker.xposed.DualLog
import com.highcapable.yukihookapi.hook.type.java.StringClass

/**
 * Advertising Hooker - Spoofs advertising and tracking identifiers.
 *
 * Hooks for:
 * - Google Advertising ID
 * - GSF ID (Google Services Framework)
 * - Media DRM ID
 *
 * Uses ServiceProxy for cross-process config access via Binder IPC.
 */
object AdvertisingHooker : YukiBaseHooker() {

    private const val TAG = "AdvertisingHooker"

    private val fallbackAdvertisingId by lazy { java.util.UUID.randomUUID().toString() }
    private val fallbackGsfId by lazy { generateHexId(16) }
    private val fallbackMediaDrmId by lazy { generateHexId(64) }

    private fun getSpoofValue(type: SpoofType, fallback: () -> String): String {
        return PrefsHelper.getSpoofValue(prefs, packageName, type, fallback)
    }

    override fun onHook() {
        DualLog.debug(TAG, "Starting hooks for: $packageName")

        hookAdvertisingIdClient()
        hookGooglePlayServices()
        hookMediaDrm()

        // Hook count tracking removed
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
                        // Note: android_id is handled by DeviceHooker
                        // Other Settings.Secure keys are not spoofed
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
