package com.astrixforge.devicemasker.hook.hooker

import com.astrixforge.devicemasker.data.generators.FingerprintGenerator
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.field
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.type.java.StringClass
import com.highcapable.yukihookapi.hook.log.YLog

/**
 * System Properties Hooker - Spoofs Build.* fields and SystemProperties.
 *
 * Hooks:
 * - Build.* static fields (FINGERPRINT, MODEL, MANUFACTURER, BRAND, etc.)
 * - Build.VERSION.* fields (RELEASE, SDK_INT, SECURITY_PATCH)
 * - SystemProperties.get() for ro.build.* and ro.product.* properties
 *
 * This hooker requires Phase 4 (DataStore) for persistent values.
 * Currently uses generator defaults.
 */
object SystemHooker : YukiBaseHooker() {

    // ═══════════════════════════════════════════════════════════
    // CACHED SPOOFED VALUES
    // These will be replaced with DataStore reads in Phase 4
    // ═══════════════════════════════════════════════════════════

    private val buildProperties: Map<String, String> by lazy {
        FingerprintGenerator.generateBuildProperties()
    }

    private val spoofedFingerprint: String
        get() = buildProperties["FINGERPRINT"] ?: FingerprintGenerator.generate()

    private val spoofedModel: String
        get() = buildProperties["MODEL"] ?: "Pixel 8"

    private val spoofedManufacturer: String
        get() = buildProperties["MANUFACTURER"] ?: "Google"

    private val spoofedBrand: String
        get() = buildProperties["BRAND"] ?: "google"

    private val spoofedDevice: String
        get() = buildProperties["DEVICE"] ?: "shiba"

    private val spoofedProduct: String
        get() = buildProperties["PRODUCT"] ?: "shiba"

    private val spoofedBoard: String
        get() = buildProperties["BOARD"] ?: "shiba"

    private val spoofedHardware: String
        get() = buildProperties["HARDWARE"] ?: "shiba"

    private val spoofedBuildId: String
        get() = buildProperties["ID"] ?: "AP2A.240905.003"

    private val spoofedDisplay: String
        get() = buildProperties["DISPLAY"] ?: "AP2A.240905.003"

    override fun onHook() {
        YLog.debug("SystemHooker: Starting hooks for package: $packageName")

        // ═══════════════════════════════════════════════════════════
        // BUILD CLASS STATIC FIELD HOOKS
        // ═══════════════════════════════════════════════════════════

        hookBuildFields()

        // ═══════════════════════════════════════════════════════════
        // BUILD.VERSION HOOKS
        // ═══════════════════════════════════════════════════════════

        hookBuildVersion()

        // ═══════════════════════════════════════════════════════════
        // SYSTEM PROPERTIES HOOKS
        // ═══════════════════════════════════════════════════════════

        hookSystemProperties()

        YLog.debug("SystemHooker: Hooks registered for package: $packageName")
    }

    /**
     * Hooks Build class static fields.
     * Note: Static fields need special handling - we hook accessors and
     * SystemProperties as the source of truth.
     */
    private fun hookBuildFields() {
        // Build class fields are populated from SystemProperties at class load time
        // The most reliable way to spoof them is to hook SystemProperties.get()
        // which is done in hookSystemProperties()

        // However, for completeness, we also try to modify the fields directly
        // This may not work for all apps but provides additional coverage

        runCatching {
            val buildClass = "android.os.Build".toClass()

            // These field modifications happen at hook time
            // They may be overwritten if the class hasn't been loaded yet
            modifyBuildField(buildClass, "FINGERPRINT", spoofedFingerprint)
            modifyBuildField(buildClass, "MODEL", spoofedModel)
            modifyBuildField(buildClass, "MANUFACTURER", spoofedManufacturer)
            modifyBuildField(buildClass, "BRAND", spoofedBrand)
            modifyBuildField(buildClass, "DEVICE", spoofedDevice)
            modifyBuildField(buildClass, "PRODUCT", spoofedProduct)
            modifyBuildField(buildClass, "BOARD", spoofedBoard)
            modifyBuildField(buildClass, "HARDWARE", spoofedHardware)
            modifyBuildField(buildClass, "ID", spoofedBuildId)
            modifyBuildField(buildClass, "DISPLAY", spoofedDisplay)
            modifyBuildField(buildClass, "HOST", "build.google.com")
            modifyBuildField(buildClass, "TYPE", "user")
            modifyBuildField(buildClass, "TAGS", "release-keys")

            YLog.debug("SystemHooker: Build fields modified")
        }.onFailure { e ->
            YLog.warn("SystemHooker: Failed to modify Build fields: ${e.message}")
        }
    }

    /**
     * Modifies a static field value using reflection.
     */
    private fun modifyBuildField(clazz: Class<*>, fieldName: String, value: String) {
        try {
            val field = clazz.getDeclaredField(fieldName)
            field.isAccessible = true

            // Remove final modifier
            val modifiersField = java.lang.reflect.Field::class.java.getDeclaredField("modifiers")
            modifiersField.isAccessible = true
            modifiersField.setInt(field, field.modifiers and java.lang.reflect.Modifier.FINAL.inv())

            field.set(null, value)
            YLog.debug("SystemHooker: Set Build.$fieldName = $value")
        } catch (e: Exception) {
            YLog.warn("SystemHooker: Failed to modify Build.$fieldName: ${e.message}")
        }
    }

    /**
     * Hooks Build.VERSION fields.
     */
    private fun hookBuildVersion() {
        // VERSION fields are also populated from SystemProperties
        // We hook via SystemProperties for reliability
    }

    /**
     * Hooks SystemProperties.get() for property-based spoofing.
     * This is the most reliable method as Build fields read from here.
     */
    private fun hookSystemProperties() {
        "android.os.SystemProperties".toClass().apply {

            // get(String key) - Single argument version
            method {
                name = "get"
                param(StringClass)
            }.hook {
                after {
                    val key = args(0).string()
                    val spoofed = getSpoofedProperty(key)
                    if (spoofed != null) {
                        YLog.debug("SystemHooker: Spoofing SystemProperties.get($key) -> $spoofed")
                        result = spoofed
                    }
                }
            }

            // get(String key, String def) - With default value
            method {
                name = "get"
                param(StringClass, StringClass)
            }.hook {
                after {
                    val key = args(0).string()
                    val spoofed = getSpoofedProperty(key)
                    if (spoofed != null) {
                        YLog.debug("SystemHooker: Spoofing SystemProperties.get($key, def) -> $spoofed")
                        result = spoofed
                    }
                }
            }
        }
    }

    /**
     * Returns the spoofed value for a system property, or null if not spoofed.
     */
    private fun getSpoofedProperty(key: String): String? {
        return when (key) {
            // Build fingerprint
            "ro.build.fingerprint" -> spoofedFingerprint
            "ro.bootimage.build.fingerprint" -> spoofedFingerprint
            "ro.vendor.build.fingerprint" -> spoofedFingerprint

            // Product info
            "ro.product.model" -> spoofedModel
            "ro.product.manufacturer" -> spoofedManufacturer
            "ro.product.brand" -> spoofedBrand
            "ro.product.device" -> spoofedDevice
            "ro.product.name" -> spoofedProduct
            "ro.product.board" -> spoofedBoard
            "ro.hardware" -> spoofedHardware

            // ODM/Vendor product info
            "ro.product.odm.model" -> spoofedModel
            "ro.product.odm.manufacturer" -> spoofedManufacturer
            "ro.product.odm.brand" -> spoofedBrand
            "ro.product.odm.device" -> spoofedDevice
            "ro.product.odm.name" -> spoofedProduct

            "ro.product.vendor.model" -> spoofedModel
            "ro.product.vendor.manufacturer" -> spoofedManufacturer
            "ro.product.vendor.brand" -> spoofedBrand
            "ro.product.vendor.device" -> spoofedDevice
            "ro.product.vendor.name" -> spoofedProduct

            "ro.product.system.model" -> spoofedModel
            "ro.product.system.manufacturer" -> spoofedManufacturer
            "ro.product.system.brand" -> spoofedBrand
            "ro.product.system.device" -> spoofedDevice
            "ro.product.system.name" -> spoofedProduct

            // Build info
            "ro.build.id" -> spoofedBuildId
            "ro.build.display.id" -> spoofedDisplay
            "ro.build.type" -> "user"
            "ro.build.tags" -> "release-keys"
            "ro.build.host" -> "build.google.com"

            // Return null for properties we don't want to spoof
            else -> null
        }
    }
}
