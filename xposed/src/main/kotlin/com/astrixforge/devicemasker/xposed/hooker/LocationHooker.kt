package com.astrixforge.devicemasker.xposed.hooker

import com.astrixforge.devicemasker.common.SpoofType
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.type.java.StringClass

/**
 * Location Hooker - Spoofs location-related identifiers.
 *
 * Hooks for:
 * - GPS coordinates (latitude, longitude)
 * - Timezone
 * - Locale
 */
object LocationHooker : BaseSpoofHooker("LocationHooker") {

    override fun onHook() {
        logStart()
        hookLocation()
        hookTimeZone()
        hookLocale()
        recordSuccess()
    }

    // ═══════════════════════════════════════════════════════════
    // LOCATION HOOKS
    // ═══════════════════════════════════════════════════════════

    private fun hookLocation() {
        "android.location.Location".toClassOrNull()?.apply {
            method {
                    name = "getLatitude"
                    emptyParam()
                }
                .hook {
                    after {
                        val current = result as? Double ?: 0.0
                        val spoofed =
                            getSpoofValue(SpoofType.LOCATION_LATITUDE) { current.toString() }
                        result = spoofed.toDoubleOrNull() ?: current
                    }
                }

            method {
                    name = "getLongitude"
                    emptyParam()
                }
                .hook {
                    after {
                        val current = result as? Double ?: 0.0
                        val spoofed =
                            getSpoofValue(SpoofType.LOCATION_LONGITUDE) { current.toString() }
                        result = spoofed.toDoubleOrNull() ?: current
                    }
                }
        }

        // Hook LocationManager
        "android.location.LocationManager".toClassOrNull()?.apply {
            runCatching {
                method {
                        name = "getLastKnownLocation"
                        param(StringClass)
                    }
                    .hook {
                        after {
                            val location = result ?: return@after
                            val latStr = getSpoofValue(SpoofType.LOCATION_LATITUDE) { "" }
                            val lonStr = getSpoofValue(SpoofType.LOCATION_LONGITUDE) { "" }

                            if (latStr.isNotEmpty() || lonStr.isNotEmpty()) {
                                runCatching {
                                    val locationClass = location::class.java
                                    latStr.toDoubleOrNull()?.let { lat ->
                                        locationClass
                                            .getMethod("setLatitude", Double::class.java)
                                            .invoke(location, lat)
                                    }
                                    lonStr.toDoubleOrNull()?.let { lon ->
                                        locationClass
                                            .getMethod("setLongitude", Double::class.java)
                                            .invoke(location, lon)
                                    }
                                }
                            }
                        }
                    }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    // TIMEZONE HOOKS
    // ═══════════════════════════════════════════════════════════

    private fun hookTimeZone() {
        "java.util.TimeZone".toClass().apply {
            method {
                    name = "getDefault"
                    emptyParam()
                    modifiers { isStatic }
                }
                .hook {
                    after {
                        val spoofedId = getSpoofValue(SpoofType.TIMEZONE) { "" }
                        if (spoofedId.isNotBlank()) {
                            result = java.util.TimeZone.getTimeZone(spoofedId)
                        }
                    }
                }

            method {
                    name = "getID"
                    emptyParam()
                }
                .hook {
                    after {
                        val spoofed = getSpoofValue(SpoofType.TIMEZONE) { "" }
                        if (spoofed.isNotBlank()) {
                            result = spoofed
                        }
                    }
                }
        }
    }

    // ═══════════════════════════════════════════════════════════
    // LOCALE HOOKS
    // ═══════════════════════════════════════════════════════════

    private fun hookLocale() {
        "java.util.Locale".toClass().apply {
            method {
                    name = "getDefault"
                    emptyParam()
                    modifiers { isStatic }
                }
                .hook {
                    after {
                        val current = result as? java.util.Locale ?: return@after
                        val spoofedLocale = getSpoofValue(SpoofType.LOCALE) { "" }

                        if (spoofedLocale.isNotBlank()) {
                            result =
                                runCatching {
                                        val parts = spoofedLocale.split("_", "-")
                                        when (parts.size) {
                                            1 -> java.util.Locale(parts[0])
                                            2 -> java.util.Locale(parts[0], parts[1])
                                            else -> java.util.Locale(parts[0], parts[1], parts[2])
                                        }
                                    }
                                    .getOrElse { current }
                        }
                    }
                }

            method {
                    name = "toString"
                    emptyParam()
                }
                .hook {
                    after {
                        val spoofed = getSpoofValue(SpoofType.LOCALE) { "" }
                        if (spoofed.isNotBlank()) {
                            result = spoofed
                        }
                    }
                }
        }
    }
}
