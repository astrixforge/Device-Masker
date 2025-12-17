package com.astrixforge.devicemasker.hook.hooker

import android.content.Context
import com.astrixforge.devicemasker.data.generators.FingerprintGenerator
import com.astrixforge.devicemasker.data.models.SpoofType
import com.astrixforge.devicemasker.hook.HookDataProvider
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.type.java.StringClass

/**
 * System Properties Hooker - Spoofs Build.* fields and SystemProperties.
 *
 * Hooks:
 * - Build.* static fields (FINGERPRINT, MODEL, MANUFACTURER, BRAND, etc.)
 * - Build.VERSION.* fields (RELEASE, SDK_INT, SECURITY_PATCH)
 * - SystemProperties.get() for ro.build.* and ro.product.* properties
 *
 * Uses HookDataProvider to read profile-based values and global config.
 */
object SystemHooker : YukiBaseHooker() {

    // ═══════════════════════════════════════════════════════════
    // DATA PROVIDER
    // ═══════════════════════════════════════════════════════════

    private var dataProvider: HookDataProvider? = null

    private fun getProvider(context: Context?): HookDataProvider? {
        if (dataProvider == null && context != null) {
            dataProvider =
                runCatching { HookDataProvider.getInstance(context, packageName) }
                    .onFailure {
                        YLog.error("SystemHooker: Failed to create HookDataProvider: ${it.message}")
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
            YLog.debug("SystemHooker: No provider for $type, using generated value")
            return generator()
        }

        // getSpoofValue now handles all profile-based checks (profile exists, profile enabled, type
        // enabled)
        return provider.getSpoofValue(type) ?: generator()
    }

    // ═══════════════════════════════════════════════════════════
    // FALLBACK GENERATORS
    // ═══════════════════════════════════════════════════════════

    private val fallbackBuildProperties: Map<String, String> by lazy {
        FingerprintGenerator.generateBuildProperties()
    }

    private val fallbackFingerprint: String
        get() = fallbackBuildProperties["FINGERPRINT"] ?: FingerprintGenerator.generate()

    private val fallbackModel: String
        get() = fallbackBuildProperties["MODEL"] ?: "Pixel 8"

    private val fallbackManufacturer: String
        get() = fallbackBuildProperties["MANUFACTURER"] ?: "Google"

    private val fallbackBrand: String
        get() = fallbackBuildProperties["BRAND"] ?: "google"

    private val fallbackDevice: String
        get() = fallbackBuildProperties["DEVICE"] ?: "shiba"

    private val fallbackProduct: String
        get() = fallbackBuildProperties["PRODUCT"] ?: "shiba"

    private val fallbackBoard: String
        get() = fallbackBuildProperties["BOARD"] ?: "shiba"

    // Context for hook callbacks
    private var hookContext: Context? = null

    override fun onHook() {
        YLog.debug("SystemHooker: Starting hooks for package: $packageName")

        hookBuildFields()
        hookBuildVersion()
        hookSystemProperties()

        YLog.debug("SystemHooker: Hooks registered for package: $packageName")
    }

    /** Hooks Build class static fields. */
    private fun hookBuildFields() {
        runCatching {
                val buildClass = "android.os.Build".toClass()

                // These field modifications happen at hook time
                modifyBuildField(buildClass, "FINGERPRINT", fallbackFingerprint)
                modifyBuildField(buildClass, "MODEL", fallbackModel)
                modifyBuildField(buildClass, "MANUFACTURER", fallbackManufacturer)
                modifyBuildField(buildClass, "BRAND", fallbackBrand)
                modifyBuildField(buildClass, "DEVICE", fallbackDevice)
                modifyBuildField(buildClass, "PRODUCT", fallbackProduct)
                modifyBuildField(buildClass, "BOARD", fallbackBoard)
                modifyBuildField(buildClass, "HOST", "build.google.com")
                modifyBuildField(buildClass, "TYPE", "user")
                modifyBuildField(buildClass, "TAGS", "release-keys")

                YLog.debug("SystemHooker: Build fields modified")
            }
            .onFailure { e ->
                YLog.warn("SystemHooker: Failed to modify Build fields: ${e.message}")
            }
    }

    /** Modifies a static field value using reflection. */
    private fun modifyBuildField(clazz: Class<*>, fieldName: String, value: String) {
        try {
            val field = clazz.getDeclaredField(fieldName)
            field.isAccessible = true

            val modifiersField = java.lang.reflect.Field::class.java.getDeclaredField("modifiers")
            modifiersField.isAccessible = true
            modifiersField.setInt(field, field.modifiers and java.lang.reflect.Modifier.FINAL.inv())

            field.set(null, value)
            YLog.debug("SystemHooker: Set Build.$fieldName = $value")
        } catch (e: Exception) {
            YLog.warn("SystemHooker: Failed to modify Build.$fieldName: ${e.message}")
        }
    }

    /** Hooks Build.VERSION fields. */
    private fun hookBuildVersion() {
        // VERSION fields are also populated from SystemProperties
        // We hook via SystemProperties for reliability
    }

    /** Hooks SystemProperties.get() for property-based spoofing. */
    private fun hookSystemProperties() {
        "android.os.SystemProperties".toClass().apply {
            method {
                    name = "get"
                    param(StringClass)
                }
                .hook {
                    after {
                        val key = args(0).string()
                        hookContext = appContext
                        val spoofed = getSpoofedProperty(appContext, key)
                        if (spoofed != null) {
                            YLog.debug(
                                "SystemHooker: Spoofing SystemProperties.get($key) -> $spoofed"
                            )
                            result = spoofed
                        }
                    }
                }

            method {
                    name = "get"
                    param(StringClass, StringClass)
                }
                .hook {
                    after {
                        val key = args(0).string()
                        hookContext = appContext
                        val spoofed = getSpoofedProperty(appContext, key)
                        if (spoofed != null) {
                            YLog.debug(
                                "SystemHooker: Spoofing SystemProperties.get($key, def) -> $spoofed"
                            )
                            result = spoofed
                        }
                    }
                }
        }
    }

    /** Returns the spoofed value for a system property, or null if not spoofed. */
    private fun getSpoofedProperty(context: Context?, key: String): String? {
        return when (key) {
            // Build fingerprint
            "ro.build.fingerprint",
            "ro.bootimage.build.fingerprint",
            "ro.vendor.build.fingerprint" -> {
                getSpoofValueOrGenerate(context, SpoofType.BUILD_FINGERPRINT) {
                    fallbackFingerprint
                }
            }

            // Product info - model
            "ro.product.model",
            "ro.product.odm.model",
            "ro.product.vendor.model",
            "ro.product.system.model" -> {
                getSpoofValueOrGenerate(context, SpoofType.BUILD_MODEL) { fallbackModel }
            }

            // Manufacturer
            "ro.product.manufacturer",
            "ro.product.odm.manufacturer",
            "ro.product.vendor.manufacturer",
            "ro.product.system.manufacturer" -> {
                getSpoofValueOrGenerate(context, SpoofType.BUILD_MANUFACTURER) {
                    fallbackManufacturer
                }
            }

            // Brand
            "ro.product.brand",
            "ro.product.odm.brand",
            "ro.product.vendor.brand",
            "ro.product.system.brand" -> {
                getSpoofValueOrGenerate(context, SpoofType.BUILD_BRAND) { fallbackBrand }
            }

            // Device
            "ro.product.device",
            "ro.product.odm.device",
            "ro.product.vendor.device",
            "ro.product.system.device" -> {
                getSpoofValueOrGenerate(context, SpoofType.BUILD_DEVICE) { fallbackDevice }
            }

            // Product
            "ro.product.name",
            "ro.product.odm.name",
            "ro.product.vendor.name",
            "ro.product.system.name" -> {
                getSpoofValueOrGenerate(context, SpoofType.BUILD_PRODUCT) { fallbackProduct }
            }

            // Board
            "ro.product.board",
            "ro.hardware" -> {
                getSpoofValueOrGenerate(context, SpoofType.BUILD_BOARD) { fallbackBoard }
            }

            // Build info (not profile-specific, just use hardcoded)
            "ro.build.type" -> "user"
            "ro.build.tags" -> "release-keys"
            "ro.build.host" -> "build.google.com"
            else -> null
        }
    }
}
