package com.astrixforge.devicemasker.xposed.hooker

import com.astrixforge.devicemasker.xposed.DualLog
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method

/**
 * Anti-Detection Hooker - Hides Xposed/LSPosed presence from detection.
 *
 * ⚠️ CRITICAL: This hooker MUST be loaded FIRST before any other hookers!
 *
 * Detection Vectors Covered:
 * 1. Stack Trace Analysis - Filter Thread/Throwable.getStackTrace()
 * 2. /proc/self/maps - Filter native library paths
 * 3. Package Detection - Hide Xposed installer packages
 *
 * NOTE: Does not extend BaseSpoofHooker since it doesn't use spoofing config.
 */
object AntiDetectHooker : YukiBaseHooker() {

    private const val TAG = "AntiDetectHooker"

    // ═══════════════════════════════════════════════════════════
    // HIDDEN PATTERNS
    // ═══════════════════════════════════════════════════════════

    private val HIDDEN_CLASS_PATTERNS =
        listOf(
            "de.robv.android.xposed",
            "XposedBridge",
            "XposedHelpers",
            "XC_MethodHook",
            "XC_MethodReplacement",
            "io.github.lsposed",
            "LSPHooker",
            "LSPosed",
            "com.highcapable.yukihookapi",
            "YukiHookAPI",
            "YukiBaseHooker",
            "EdHooker",
            "EdXposed",
            "com.elderdrivers.riru",
            "rikka.ndk",
            "org.lsposed.lspd",
            "LSPosedService",
        )

    private val HIDDEN_LIBRARY_PATTERNS =
        listOf(
            "libxposed",
            "liblspd",
            "libriru",
            "libsandhook",
            "libpine",
            "libwhale",
            "libdobby",
            "libsubstrate",
        )

    private val HIDDEN_PACKAGES =
        listOf(
            "de.robv.android.xposed.installer",
            "org.lsposed.manager",
            "io.github.lsposed.manager",
            "com.topjohnwu.magisk",
            "me.weishu.exp",
            "org.meowcat.edxposed.manager",
        )

    private val isFiltering = ThreadLocal<Boolean>()

    override fun onHook() {
        val selfPackages = listOf("com.astrixforge.devicemasker", "android", "system_server")
        if (packageName in selfPackages || processName in selfPackages) {
            DualLog.debug(TAG, "Skipping for protected process: $packageName")
            return
        }

        DualLog.debug(TAG, "Loading anti-detection hooks for: $packageName")

        hookStackTraces()
        hookProcMaps()
        hookPackageManager()

        DualLog.debug(TAG, "Anti-detection hooks registered")
    }

    // ═══════════════════════════════════════════════════════════
    // STACK TRACE HOOKS
    // ═══════════════════════════════════════════════════════════

    private fun hookStackTraces() {
        // Hook Thread.getStackTrace()
        runCatching {
            "java.lang.Thread".toClass().apply {
                method {
                        name = "getStackTrace"
                        emptyParam()
                    }
                    .hook {
                        after {
                            @Suppress("UNCHECKED_CAST")
                            val originalStack = result as? Array<StackTraceElement> ?: return@after
                            runCatching { result = filterStackTrace(originalStack) }
                        }
                    }

                runCatching {
                    method {
                            name = "getAllStackTraces"
                            emptyParam()
                        }
                        .hook {
                            after {
                                @Suppress("UNCHECKED_CAST")
                                val allTraces =
                                    result as? Map<Thread, Array<StackTraceElement>> ?: return@after
                                runCatching {
                                    result =
                                        allTraces.mapValues { (_, stack) ->
                                            filterStackTrace(stack)
                                        }
                                }
                            }
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
                    }
                    .hook {
                        after {
                            @Suppress("UNCHECKED_CAST")
                            val originalStack = result as? Array<StackTraceElement> ?: return@after
                            runCatching { result = filterStackTrace(originalStack) }
                        }
                    }
            }
        }
    }

    private fun filterStackTrace(stack: Array<StackTraceElement>): Array<StackTraceElement> {
        if (isFiltering.get() == true) return stack
        isFiltering.set(true)
        return try {
            stack
                .filterNot { element ->
                    val className = element.className
                    if (className.startsWith("com.astrixforge.devicemasker")) return@filterNot false
                    if (className.startsWith("android.")) return@filterNot false
                    if (className.startsWith("java.")) return@filterNot false
                    if (className.startsWith("kotlin.")) return@filterNot false
                    if (className.startsWith("androidx.")) return@filterNot false
                    HIDDEN_CLASS_PATTERNS.any { className.contains(it, ignoreCase = true) }
                }
                .toTypedArray()
        } finally {
            isFiltering.set(false)
        }
    }

    // ═══════════════════════════════════════════════════════════
    // /proc/self/maps HOOKS
    // ═══════════════════════════════════════════════════════════

    private fun hookProcMaps() {
        "java.io.BufferedReader".toClass().apply {
            method {
                    name = "readLine"
                    emptyParam()
                }
                .hook {
                    after {
                        val line = result as? String ?: return@after
                        if (HIDDEN_LIBRARY_PATTERNS.any { line.contains(it, ignoreCase = true) }) {
                            result = ""
                        }
                    }
                }
        }
    }

    // ═══════════════════════════════════════════════════════════
    // PACKAGE MANAGER HOOKS
    // ═══════════════════════════════════════════════════════════

    private fun hookPackageManager() {
        "android.app.ApplicationPackageManager".toClass().apply {
            method {
                    name = "getPackageInfo"
                    paramCount = 2
                }
                .hook {
                    before {
                        val pkgName = args(0).string()
                        if (HIDDEN_PACKAGES.any { pkgName.equals(it, ignoreCase = true) }) {
                            throw android.content.pm.PackageManager.NameNotFoundException(pkgName)
                        }
                    }
                }

            method {
                    name = "getApplicationInfo"
                    paramCount = 2
                }
                .hook {
                    before {
                        val pkgName = args(0).string()
                        if (HIDDEN_PACKAGES.any { pkgName.equals(it, ignoreCase = true) }) {
                            throw android.content.pm.PackageManager.NameNotFoundException(pkgName)
                        }
                    }
                }

            runCatching {
                method {
                        name = "getInstalledPackages"
                        paramCount = 1
                    }
                    .hook {
                        after {
                            val packages = result as? MutableList<*> ?: return@after
                            packages.removeAll { pkg ->
                                val pkgInfo = pkg as? android.content.pm.PackageInfo
                                pkgInfo?.packageName?.let { name ->
                                    HIDDEN_PACKAGES.any { name.equals(it, ignoreCase = true) }
                                } ?: false
                            }
                        }
                    }
            }

            runCatching {
                method {
                        name = "getInstalledApplications"
                        paramCount = 1
                    }
                    .hook {
                        after {
                            val apps = result as? MutableList<*> ?: return@after
                            apps.removeAll { app ->
                                val appInfo = app as? android.content.pm.ApplicationInfo
                                appInfo?.packageName?.let { name ->
                                    HIDDEN_PACKAGES.any { name.equals(it, ignoreCase = true) }
                                } ?: false
                            }
                        }
                    }
            }
        }
    }
}
