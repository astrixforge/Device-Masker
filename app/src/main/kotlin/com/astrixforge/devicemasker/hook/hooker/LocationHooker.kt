package com.astrixforge.devicemasker.hook.hooker

import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.type.java.StringClass
import com.highcapable.yukihookapi.hook.log.YLog
import java.util.Locale
import java.util.TimeZone

/**
 * Location Identifier Hooker - Spoofs GPS coordinates, timezone, and locale.
 *
 * Hooks:
 * - Location: getLatitude(), getLongitude(), getAltitude()
 * - TimeZone: getDefault()
 * - Locale: getDefault()
 *
 * This hooker requires Phase 4 (DataStore) for persistent values.
 * Currently uses preset defaults.
 */
object LocationHooker : YukiBaseHooker() {

    // ═══════════════════════════════════════════════════════════
    // CACHED SPOOFED VALUES
    // These will be replaced with DataStore reads in Phase 4
    // ═══════════════════════════════════════════════════════════

    // Default to San Francisco coordinates
    private const val SPOOFED_LATITUDE: Double = 37.7749
    private const val SPOOFED_LONGITUDE: Double = -122.4194
    private const val SPOOFED_ALTITUDE: Double = 10.0

    // Default timezone and locale
    private const val SPOOFED_TIMEZONE_ID: String = "America/Los_Angeles"
    private const val SPOOFED_LOCALE_LANGUAGE: String = "en"
    private const val SPOOFED_LOCALE_COUNTRY: String = "US"

    override fun onHook() {
        YLog.debug("LocationHooker: Starting hooks for package: $packageName")

        // ═══════════════════════════════════════════════════════════
        // LOCATION HOOKS
        // ═══════════════════════════════════════════════════════════

        hookLocation()

        // ═══════════════════════════════════════════════════════════
        // TIMEZONE HOOKS
        // ═══════════════════════════════════════════════════════════

        hookTimeZone()

        // ═══════════════════════════════════════════════════════════
        // LOCALE HOOKS
        // ═══════════════════════════════════════════════════════════

        hookLocale()

        YLog.debug("LocationHooker: Hooks registered for package: $packageName")
    }

    /**
     * Hooks Location class methods for GPS coordinates.
     */
    private fun hookLocation() {
        "android.location.Location".toClass().apply {

            // getLatitude() - GPS latitude
            method {
                name = "getLatitude"
                emptyParam()
            }.hook {
                after {
                    YLog.debug("LocationHooker: Spoofing getLatitude() -> $SPOOFED_LATITUDE")
                    result = SPOOFED_LATITUDE
                }
            }

            // getLongitude() - GPS longitude
            method {
                name = "getLongitude"
                emptyParam()
            }.hook {
                after {
                    YLog.debug("LocationHooker: Spoofing getLongitude() -> $SPOOFED_LONGITUDE")
                    result = SPOOFED_LONGITUDE
                }
            }

            // getAltitude() - GPS altitude
            method {
                name = "getAltitude"
                emptyParam()
            }.hook {
                after {
                    YLog.debug("LocationHooker: Spoofing getAltitude() -> $SPOOFED_ALTITUDE")
                    result = SPOOFED_ALTITUDE
                }
            }

            // getAccuracy() - Location accuracy in meters
            method {
                name = "getAccuracy"
                emptyParam()
            }.hook {
                after {
                    // Return a reasonable accuracy (5 meters)
                    result = 5.0f
                }
            }

            // getProvider() - Location provider name
            method {
                name = "getProvider"
                emptyParam()
            }.hook {
                after {
                    result = "gps"
                }
            }
        }

        // Hook LocationManager for location requests
        hookLocationManager()
    }

    /**
     * Hooks LocationManager for location provider information.
     */
    private fun hookLocationManager() {
        "android.location.LocationManager".toClass().apply {

            // getLastKnownLocation(String provider) - Last known location
            method {
                name = "getLastKnownLocation"
                param(StringClass)
            }.hook {
                after {
                    // The Location class hooks above will handle individual getters
                    YLog.debug("LocationHooker: getLastKnownLocation() called")
                }
            }
        }
    }

    /**
     * Hooks TimeZone.getDefault() for timezone spoofing.
     */
    private fun hookTimeZone() {
        "java.util.TimeZone".toClass().apply {

            // getDefault() - Get device default timezone
            method {
                name = "getDefault"
                modifiers { isStatic }
            }.hook {
                after {
                    val spoofedTimezone = TimeZone.getTimeZone(SPOOFED_TIMEZONE_ID)
                    YLog.debug("LocationHooker: Spoofing TimeZone.getDefault() -> $SPOOFED_TIMEZONE_ID")
                    result = spoofedTimezone
                }
            }
        }
    }

    /**
     * Hooks Locale.getDefault() for locale/language spoofing.
     */
    private fun hookLocale() {
        "java.util.Locale".toClass().apply {

            // getDefault() - Get device default locale
            method {
                name = "getDefault"
                emptyParam()
            }.hook {
                after {
                    val spoofedLocale = Locale(SPOOFED_LOCALE_LANGUAGE, SPOOFED_LOCALE_COUNTRY)
                    YLog.debug("LocationHooker: Spoofing Locale.getDefault() -> $spoofedLocale")
                    result = spoofedLocale
                }
            }

            // getDefault(Locale.Category) - Category-specific locale
            method {
                name = "getDefault"
                paramCount = 1
            }.hook {
                after {
                    val spoofedLocale = Locale(SPOOFED_LOCALE_LANGUAGE, SPOOFED_LOCALE_COUNTRY)
                    YLog.debug("LocationHooker: Spoofing Locale.getDefault(category) -> $spoofedLocale")
                    result = spoofedLocale
                }
            }
        }

        // Hook Resources for configuration locale
        hookResourcesLocale()
    }

    /**
     * Hooks Resources for configuration locale.
     */
    private fun hookResourcesLocale() {
        // Configuration class - getLocales() for API 24+
        runCatching {
            "android.content.res.Configuration".toClass().apply {
                runCatching {
                    method {
                        name = "getLocales"
                        emptyParam()
                    }.hook {
                        after {
                            // Return a LocaleList with our spoofed locale
                            runCatching {
                                val spoofedLocale = Locale(SPOOFED_LOCALE_LANGUAGE, SPOOFED_LOCALE_COUNTRY)
                                val localeListClass = "android.os.LocaleList".toClass()
                                val constructor = localeListClass.getConstructor(Array<Locale>::class.java)
                                result = constructor.newInstance(arrayOf(spoofedLocale))
                            }
                        }
                    }
                }
            }
        }
    }
}
