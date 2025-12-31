package com.astrixforge.devicemasker.xposed.hooker

import com.astrixforge.devicemasker.common.SpoofType
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.type.java.StringClass
import java.util.UUID

/**
 * Advertising Hooker - Spoofs advertising and tracking identifiers.
 *
 * Hooks for:
 * - Google Advertising ID
 * - GSF ID (Google Services Framework)
 * - Media DRM ID
 */
object AdvertisingHooker : BaseSpoofHooker("AdvertisingHooker") {

    // Fallback values (thread-safe lazy)
    private val fallbackAdvertisingId by lazy { UUID.randomUUID().toString() }
    private val fallbackGsfId by lazy { generateHexId(16) }
    private val fallbackMediaDrmId by lazy { generateHexId(64) }

    override fun onHook() {
        logStart()
        hookAdvertisingIdClient()
        hookGooglePlayServices()
        hookMediaDrm()
        recordSuccess()
    }

    // ═══════════════════════════════════════════════════════════
    // ADVERTISING ID HOOKS
    // ═══════════════════════════════════════════════════════════

    private fun hookAdvertisingIdClient() {
        "com.google.android.gms.ads.identifier.AdvertisingIdClient\$Info".toClassOrNull()?.apply {
            method {
                    name = "getId"
                    emptyParam()
                }
                .hook {
                    after {
                        result = getSpoofValue(SpoofType.ADVERTISING_ID) { fallbackAdvertisingId }
                    }
                }
        }
    }

    // ═══════════════════════════════════════════════════════════
    // GOOGLE PLAY SERVICES HOOKS
    // ═══════════════════════════════════════════════════════════

    private fun hookGooglePlayServices() {
        "com.google.android.gsf.Gservices".toClassOrNull()?.apply {
            runCatching {
                method {
                        name = "getString"
                        paramCount = 2
                    }
                    .hook {
                        after {
                            val key = args(1).string()
                            if (key == "android_id") {
                                result = getSpoofValue(SpoofType.GSF_ID) { fallbackGsfId }
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
                                val spoofed = getSpoofValue(SpoofType.GSF_ID) { fallbackGsfId }
                                result = runCatching { spoofed.toLong(16) }.getOrElse { result }
                            }
                        }
                    }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    // MEDIA DRM HOOKS
    // ═══════════════════════════════════════════════════════════

    private fun hookMediaDrm() {
        "android.media.MediaDrm".toClassOrNull()?.apply {
            runCatching {
                method {
                        name = "getPropertyByteArray"
                        param(StringClass)
                    }
                    .hook {
                        after {
                            val property = args(0).string()
                            if (property == "deviceUniqueId") {
                                val spoofed =
                                    getSpoofValue(SpoofType.MEDIA_DRM_ID) { fallbackMediaDrmId }
                                result = hexToBytes(spoofed)
                            }
                        }
                    }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════

    private fun generateHexId(length: Int): String {
        val chars = "0123456789abcdef"
        return (1..length).map { chars.random() }.joinToString("")
    }

    private fun hexToBytes(hex: String): ByteArray {
        return runCatching { hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray() }
            .getOrElse { ByteArray(32) }
    }
}
