package com.astrixforge.devicemasker.xposed.hooker

import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method
import com.astrixforge.devicemasker.xposed.DualLog

/**
 * Anti-Detection Hooker - Hides Xposed/LSPosed presence from detection.
 *
 * ⚠️ CRITICAL: This hooker MUST be loaded FIRST before any other hookers!
 * Early detection checks can discover the hook framework before spoofing
 * hooks are active.
 *
 * Detection Vectors Covered:
 * 1. Stack Trace Analysis - Filter Thread/Throwable.getStackTrace()
 * 2. /proc/self/maps - Filter native library paths
 * 3. Package Detection - Hide Xposed installer packages
 *
 * NOTE: Class.forName() hooks were removed as they caused crashes with AndroidX Startup.
 */
object AntiDetectHooker : YukiBaseHooker() {

    private const val TAG = "AntiDetectHooker"

    // ═══════════════════════════════════════════════════════════
    // HIDDEN PATTERNS
    // ═══════════════════════════════════════════════════════════

    /** Class name patterns that indicate Xposed/LSPosed presence. */
    private val HIDDEN_CLASS_PATTERNS = listOf(
        // Xposed Framework
        "de.robv.android.xposed",
        "XposedBridge",
        "XposedHelpers",
        "XC_MethodHook",
        "XC_MethodReplacement",
        // LSPosed
        "io.github.lsposed",
        "LSPHooker",
        "LSPosed",
        // YukiHookAPI
        "com.highcapable.yukihookapi",
        "YukiHookAPI",
        "YukiBaseHooker",
        "YukiMemberHookCreator",
        // EdXposed
        "EdHooker",
        "EdXposed",
        "com.elderdrivers.riru",
        // Riru
        "rikka.ndk",
        "org.lsposed.lspd",
        // General hook patterns
        "LSPosedService",
    )

    /** Native library paths to hide from /proc/self/maps. */
    private val HIDDEN_LIBRARY_PATTERNS = listOf(
        "libxposed",
        "liblspd",
        "libriru",
        "libsandhook",
        "libpine",
        "libwhale",
        "libdobby",
        "libsubstrate",
        "libandroid_runtime_ext",
    )

    /** Package names of Xposed installers to hide. */
    private val HIDDEN_PACKAGES = listOf(
        "de.robv.android.xposed.installer",
        "org.lsposed.manager",
        "io.github.lsposed.manager",
        "com.topjohnwu.magisk",
        "me.weishu.exp",
        "org.meowcat.edxposed.manager",
    )

    /** ThreadLocal guard to prevent infinite recursion. */
    private val isFiltering = ThreadLocal<Boolean>()

    override fun onHook() {
        // Skip protected processes
        val selfPackages = listOf("com.astrixforge.devicemasker", "android", "system_server")
        if (packageName in selfPackages || processName in selfPackages) {
            DualLog.debug(TAG, "Skipping for protected process: $packageName")
            return
        }

        DualLog.debug(TAG, "Loading anti-detection hooks for: $packageName")

        hookStackTraces()
        // NOTE: hookClassLoading() REMOVED - was causing AndroidX Startup crashes
        // Stack trace filtering is sufficient for hiding Xposed presence
        hookProcMaps()
        hookPackageManager()

        DualLog.debug(TAG, "Anti-detection hooks registered")
    }

    private fun hookStackTraces() {
        // Hook Thread.getStackTrace()
        runCatching {
            "java.lang.Thread".toClass().apply {
                method {
                    name = "getStackTrace"
                    emptyParam()
                }.hook {
                    after {
                        @Suppress("UNCHECKED_CAST")
                        val originalStack = result as? Array<StackTraceElement>
                        if (originalStack.isNullOrEmpty()) return@after

                        runCatching {
                            result = filterStackTrace(originalStack)
                        }.onFailure { result = originalStack }
                    }
                }
            }
        }

        // Hook Throwable.getStackTrace()
        runCatching {
            "java.lang.Throwable".toClass().apply {
                method {
                    name = "getStackTrace"
                    emptyParam()
                }.hook {
                    after {
                        @Suppress("UNCHECKED_CAST")
                        val originalStack = result as? Array<StackTraceElement>
                        if (originalStack.isNullOrEmpty()) return@after

                        runCatching {
                            result = filterStackTrace(originalStack)
                        }.onFailure { result = originalStack }
                    }
                }
            }
        }
    }

    private fun filterStackTrace(stack: Array<StackTraceElement>): Array<StackTraceElement> {
        if (isFiltering.get() == true) return stack
        isFiltering.set(true)

        return try {
            stack.filterNot { element ->
                val className = element.className
                // Never filter standard classes
                if (className.startsWith("com.astrixforge.devicemasker")) return@filterNot false
                if (className.startsWith("android.")) return@filterNot false
                if (className.startsWith("java.")) return@filterNot false
                if (className.startsWith("kotlin.")) return@filterNot false
                if (className.startsWith("androidx.")) return@filterNot false

                HIDDEN_CLASS_PATTERNS.any { pattern ->
                    className.contains(pattern, ignoreCase = true)
                }
            }.toTypedArray()
        } finally {
            isFiltering.set(false)
        }
    }


    // NOTE: hookClassLoading() was removed because it was causing crashes with
    // AndroidX Startup (ClassNotFoundException for WorkManagerInitializer, EmojiCompatInitializer).
    // Stack trace filtering is sufficient for anti-detection purposes.


    private fun hookProcMaps() {
        "java.io.BufferedReader".toClass().apply {
            method {
                name = "readLine"
                emptyParam()
            }.hook {
                after {
                    val line = result as? String ?: return@after
                    if (shouldFilterMapsLine(line)) {
                        result = ""
                    }
                }
            }
        }
    }

    private fun shouldFilterMapsLine(line: String): Boolean {
        return HIDDEN_LIBRARY_PATTERNS.any { pattern ->
            line.contains(pattern, ignoreCase = true)
        }
    }

    private fun hookPackageManager() {
        "android.app.ApplicationPackageManager".toClass().apply {
            method {
                name = "getPackageInfo"
                paramCount = 2
            }.hook {
                before {
                    val packageName = args(0).string()
                    if (shouldHidePackage(packageName)) {
                        throw android.content.pm.PackageManager.NameNotFoundException(packageName)
                    }
                }
            }

            method {
                name = "getApplicationInfo"
                paramCount = 2
            }.hook {
                before {
                    val packageName = args(0).string()
                    if (shouldHidePackage(packageName)) {
                        throw android.content.pm.PackageManager.NameNotFoundException(packageName)
                    }
                }
            }
        }

        runCatching {
            "android.app.ApplicationPackageManager".toClass().apply {
                method {
                    name = "getInstalledPackages"
                    paramCount = 1
                }.hook {
                    after {
                        val packages = result as? MutableList<*> ?: return@after
                        packages.removeAll { pkg ->
                            val pkgInfo = pkg as? android.content.pm.PackageInfo
                            pkgInfo?.packageName?.let { shouldHidePackage(it) } ?: false
                        }
                    }
                }

                method {
                    name = "getInstalledApplications"
                    paramCount = 1
                }.hook {
                    after {
                        val apps = result as? MutableList<*> ?: return@after
                        apps.removeAll { app ->
                            val appInfo = app as? android.content.pm.ApplicationInfo
                            appInfo?.packageName?.let { shouldHidePackage(it) } ?: false
                        }
                    }
                }
            }
        }
    }

    private fun shouldHidePackage(packageName: String): Boolean {
        return HIDDEN_PACKAGES.any { hidden ->
            packageName.equals(hidden, ignoreCase = true)
        }
    }
}
