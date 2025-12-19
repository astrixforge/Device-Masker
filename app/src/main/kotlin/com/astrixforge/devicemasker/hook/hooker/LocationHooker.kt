package com.astrixforge.devicemasker.hook.hooker

import android.content.Context
import com.astrixforge.devicemasker.data.models.SpoofType
import com.astrixforge.devicemasker.hook.HookDataProvider
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.*
import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.type.java.StringClass
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
 * Uses HookDataProvider to read profile-based values and global config.
 */
object LocationHooker : YukiBaseHooker() {

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
                            "LocationHooker: Failed to create HookDataProvider: ${it.message}"
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
    ): String {
        val provider = getProvider(context)
        if (provider == null) {
            YLog.debug("LocationHooker: No provider for $type, using generated value")
            return generator()
        }

        // getSpoofValue now handles all profile-based checks
        return provider.getSpoofValue(type) ?: generator()
    }

    // ═══════════════════════════════════════════════════════════
    // FALLBACK VALUES
    // ═══════════════════════════════════════════════════════════

    // Default to San Francisco coordinates
    private const val FALLBACK_LATITUDE = "37.7749"
    private const val FALLBACK_LONGITUDE = "-122.4194"
    private const val FALLBACK_ALTITUDE = 10.0

    // Default timezone and locale
    private const val FALLBACK_TIMEZONE = "America/Los_Angeles"
    private const val FALLBACK_LOCALE = "en_US"

    override fun onHook() {
        YLog.debug("LocationHooker: Starting hooks for package: $packageName")

        hookLocation()
        hookTimeZone()
        hookLocale()

        YLog.debug("LocationHooker: Hooks registered for package: $packageName")
    }

    /** Hooks Location class methods for GPS coordinates. */
    private fun hookLocation() {
        "android.location.Location".toClass().apply {

            // getLatitude() - GPS latitude
            method {
                    name = "getLatitude"
                    emptyParam()
                }
                .hook {
                    after {
                        val value =
                            getSpoofValueOrGenerate(appContext, SpoofType.LOCATION_LATITUDE) {
                                FALLBACK_LATITUDE
                            }
                        val latitude = value.toDoubleOrNull() ?: FALLBACK_LATITUDE.toDouble()
                        YLog.debug("LocationHooker: Spoofing getLatitude() -> $latitude")
                        result = latitude
                    }
                }

            // getLongitude() - GPS longitude
            method {
                    name = "getLongitude"
                    emptyParam()
                }
                .hook {
                    after {
                        val value =
                            getSpoofValueOrGenerate(appContext, SpoofType.LOCATION_LONGITUDE) {
                                FALLBACK_LONGITUDE
                            }
                        val longitude = value.toDoubleOrNull() ?: FALLBACK_LONGITUDE.toDouble()
                        YLog.debug("LocationHooker: Spoofing getLongitude() -> $longitude")
                        result = longitude
                    }
                }

            // getAltitude() - GPS altitude
            method {
                    name = "getAltitude"
                    emptyParam()
                }
                .hook {
                    after {
                        YLog.debug("LocationHooker: Spoofing getAltitude() -> $FALLBACK_ALTITUDE")
                        result = FALLBACK_ALTITUDE
                    }
                }

            // getAccuracy() - Location accuracy in meters
            method {
                    name = "getAccuracy"
                    emptyParam()
                }
                .hook { after { result = 5.0f } }

            // getProvider() - Location provider name
            method {
                    name = "getProvider"
                    emptyParam()
                }
                .hook { after { result = "gps" } }
        }

        hookLocationManager()
    }

    /** Hooks LocationManager for location provider information. */
    private fun hookLocationManager() {
        "android.location.LocationManager".toClass().apply {
            method {
                    name = "getLastKnownLocation"
                    param(StringClass)
                }
                .hook { after { YLog.debug("LocationHooker: getLastKnownLocation() called") } }
        }
    }

    /** Hooks TimeZone.getDefault() for timezone spoofing. */
    private fun hookTimeZone() {
        "java.util.TimeZone".toClass().apply {
            method {
                    name = "getDefault"
                    modifiers { isStatic }
                }
                .hook {
                    after {
                        val value =
                            getSpoofValueOrGenerate(appContext, SpoofType.TIMEZONE) {
                                FALLBACK_TIMEZONE
                            }
                        val spoofedTimezone = TimeZone.getTimeZone(value)
                        YLog.debug("LocationHooker: Spoofing TimeZone.getDefault() -> $value")
                        result = spoofedTimezone
                    }
                }
        }
    }

    /** Hooks Locale.getDefault() for locale/language spoofing. */
    private fun hookLocale() {
        "java.util.Locale".toClass().apply {
            method {
                    name = "getDefault"
                    emptyParam()
                }
                .hook {
                    after {
                        val value =
                            getSpoofValueOrGenerate(appContext, SpoofType.LOCALE) {
                                FALLBACK_LOCALE
                            }
                        val parts = value.split("_")
                        val spoofedLocale =
                            if (parts.size >= 2) {
                                Locale(parts[0], parts[1])
                            } else {
                                Locale(parts[0])
                            }
                        YLog.debug(
                            "LocationHooker: Spoofing Locale.getDefault() -> $spoofedLocale"
                        )
                        result = spoofedLocale
                    }
                }

            method {
                    name = "getDefault"
                    paramCount = 1
                }
                .hook {
                    after {
                        val value =
                            getSpoofValueOrGenerate(appContext, SpoofType.LOCALE) {
                                FALLBACK_LOCALE
                            }
                        val parts = value.split("_")
                        val spoofedLocale =
                            if (parts.size >= 2) {
                                Locale(parts[0], parts[1])
                            } else {
                                Locale(parts[0])
                            }
                        YLog.debug(
                            "LocationHooker: Spoofing Locale.getDefault(category) -> $spoofedLocale"
                        )
                        result = spoofedLocale
                    }
                }
        }

        hookResourcesLocale()
    }

    /** Hooks Resources for configuration locale. */
    private fun hookResourcesLocale() {
        runCatching {
            "android.content.res.Configuration".toClass().apply {
                runCatching {
                    method {
                            name = "getLocales"
                            emptyParam()
                        }
                        .hook {
                            after {
                                val value =
                                    getSpoofValueOrGenerate(appContext, SpoofType.LOCALE) {
                                        FALLBACK_LOCALE
                                    }
                                runCatching {
                                    val parts = value.split("_")
                                    val spoofedLocale =
                                        if (parts.size >= 2) {
                                            Locale(parts[0], parts[1])
                                        } else {
                                            Locale(parts[0])
                                        }
                                    val localeListClass = "android.os.LocaleList".toClass()
                                    val constructor =
                                        localeListClass.getConstructor(
                                            Array<Locale>::class.java
                                        )
                                    result = constructor.newInstance(arrayOf(spoofedLocale))
                                }
                            }
                        }
                }
            }
        }
    }
}
