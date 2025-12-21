package com.astrixforge.devicemasker.xposed.hooker

import com.astrixforge.devicemasker.common.SpoofType
import com.astrixforge.devicemasker.xposed.DeviceMaskerService
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.type.java.DoubleType
import com.highcapable.yukihookapi.hook.type.java.StringClass

/**
 * Location Hooker - Spoofs location-related identifiers.
 *
 * Hooks for:
 * - GPS coordinates (latitude, longitude)
 * - Timezone
 * - Locale
 */
object LocationHooker : YukiBaseHooker() {

    private fun getSpoofValue(type: SpoofType, fallback: () -> String): String {
        val service = DeviceMaskerService.instance ?: return fallback()
        val config = service.config
        val profile = config.getProfileForApp(packageName) ?: return fallback()

        if (!profile.isEnabled) return fallback()
        if (!profile.isTypeEnabled(type)) return fallback()

        return profile.getValue(type) ?: fallback()
    }

    override fun onHook() {
        YLog.debug("LocationHooker: Starting hooks for: $packageName")

        hookLocation()
        hookTimeZone()
        hookLocale()

        DeviceMaskerService.instance?.incrementHookCount()
    }

    private fun hookLocation() {
        runCatching {
            "android.location.Location".toClass().apply {
                method {
                    name = "getLatitude"
                    emptyParam()
                }.hook {
                    after {
                        val current = result as? Double ?: 0.0
                        val spoofed = getSpoofValue(SpoofType.LOCATION_LATITUDE) { current.toString() }
                        result = spoofed.toDoubleOrNull() ?: current
                    }
                }

                method {
                    name = "getLongitude"
                    emptyParam()
                }.hook {
                    after {
                        val current = result as? Double ?: 0.0
                        val spoofed = getSpoofValue(SpoofType.LOCATION_LONGITUDE) { current.toString() }
                        result = spoofed.toDoubleOrNull() ?: current
                    }
                }
            }
        }

        // Hook LocationManager
        runCatching {
            "android.location.LocationManager".toClass().apply {
                method {
                    name = "getLastKnownLocation"
                    param(StringClass)
                }.hook {
                    after {
                        val location = result ?: return@after
                        // Modify the location object
                        val latStr = getSpoofValue(SpoofType.LOCATION_LATITUDE) { "" }
                        val lonStr = getSpoofValue(SpoofType.LOCATION_LONGITUDE) { "" }

                        if (latStr.isNotEmpty() || lonStr.isNotEmpty()) {
                            runCatching {
                                val locationClass = location.javaClass
                                latStr.toDoubleOrNull()?.let { lat ->
                                    locationClass.getMethod("setLatitude", Double::class.java)
                                        .invoke(location, lat)
                                }
                                lonStr.toDoubleOrNull()?.let { lon ->
                                    locationClass.getMethod("setLongitude", Double::class.java)
                                        .invoke(location, lon)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun hookTimeZone() {
        runCatching {
            "java.util.TimeZone".toClass().apply {
                method {
                    name = "getDefault"
                    emptyParam()
                    modifiers { isStatic }
                }.hook {
                    after {
                        val current = result as? java.util.TimeZone ?: return@after
                        val spoofedId = getSpoofValue(SpoofType.TIMEZONE) { "" }

                        if (spoofedId.isNotBlank()) {
                            result = java.util.TimeZone.getTimeZone(spoofedId)
                        }
                    }
                }

                method {
                    name = "getID"
                    emptyParam()
                }.hook {
                    after {
                        val current = result as? String ?: ""
                        val spoofed = getSpoofValue(SpoofType.TIMEZONE) { "" }
                        if (spoofed.isNotBlank()) {
                            result = spoofed
                        }
                    }
                }
            }
        }
    }

    private fun hookLocale() {
        runCatching {
            "java.util.Locale".toClass().apply {
                method {
                    name = "getDefault"
                    emptyParam()
                    modifiers { isStatic }
                }.hook {
                    after {
                        val current = result as? java.util.Locale ?: return@after
                        val spoofedLocale = getSpoofValue(SpoofType.LOCALE) { "" }

                        if (spoofedLocale.isNotBlank()) {
                            result = try {
                                val parts = spoofedLocale.split("_", "-")
                                when (parts.size) {
                                    1 -> java.util.Locale(parts[0])
                                    2 -> java.util.Locale(parts[0], parts[1])
                                    else -> java.util.Locale(parts[0], parts[1], parts[2])
                                }
                            } catch (e: Exception) {
                                current
                            }
                        }
                    }
                }

                method {
                    name = "toString"
                    emptyParam()
                }.hook {
                    after {
                        val current = result as? String ?: ""
                        val spoofed = getSpoofValue(SpoofType.LOCALE) { "" }
                        if (spoofed.isNotBlank()) {
                            result = spoofed
                        }
                    }
                }
            }
        }
    }
}
