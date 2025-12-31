package com.astrixforge.devicemasker.xposed.hooker

import com.astrixforge.devicemasker.common.DeviceProfilePreset
import com.astrixforge.devicemasker.common.SpoofType
import com.astrixforge.devicemasker.xposed.PrefsHelper
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.type.java.StringClass

/**
 * WebView Hooker - Spoofs WebView User-Agent and related fingerprinting vectors.
 *
 * Apps use WebView's JavaScript to fingerprint devices via:
 * - navigator.userAgent
 * - navigator.platform
 * - screen dimensions
 */
object WebViewHooker : BaseSpoofHooker("WebViewHooker") {

    // Cached class reference
    private val webSettingsClass by lazy { "android.webkit.WebSettings".toClassOrNull() }

    private fun getActivePreset(): DeviceProfilePreset? {
        val presetId =
            PrefsHelper.getSpoofValue(prefs, packageName, SpoofType.DEVICE_PROFILE) { "" }
        if (presetId.isEmpty()) return null
        return DeviceProfilePreset.findById(presetId)
    }

    override fun onHook() {
        logStart()
        hookWebSettings()
        recordSuccess()
    }

    // ═══════════════════════════════════════════════════════════
    // WEB SETTINGS HOOKS
    // ═══════════════════════════════════════════════════════════

    private fun hookWebSettings() {
        webSettingsClass?.apply {
            // Hook getUserAgentString()
            method {
                    name = "getUserAgentString"
                    emptyParam()
                }
                .hook {
                    after {
                        val preset = getActivePreset() ?: return@after
                        val originalUA = result as? String ?: return@after
                        val spoofedUA = modifyUserAgent(originalUA, preset.model)
                        if (spoofedUA != originalUA) {
                            result = spoofedUA
                            logDebug("Spoofed User-Agent with model: ${preset.model}")
                        }
                    }
                }

            // Hook setUserAgentString(String ua)
            method {
                    name = "setUserAgentString"
                    param(StringClass)
                }
                .hook {
                    before {
                        val preset = getActivePreset() ?: return@before
                        val customUA = args(0).string()
                        if (customUA.isNotEmpty() && !customUA.contains(preset.model)) {
                            args[0] = modifyUserAgent(customUA, preset.model)
                            logDebug("Modified custom User-Agent")
                        }
                    }
                }
        }
    }

    private fun modifyUserAgent(originalUA: String, model: String): String {
        // Replace device model in User-Agent
        // Original format: "... (Linux; Android XX; DEVICE_MODEL) ..."
        val regex = Regex("""\(Linux; Android (\d+); ([^)]+)\)""")

        return regex.replace(originalUA) { matchResult ->
            val androidVersion = matchResult.groupValues[1]
            "(Linux; Android $androidVersion; $model)"
        }
    }
}
