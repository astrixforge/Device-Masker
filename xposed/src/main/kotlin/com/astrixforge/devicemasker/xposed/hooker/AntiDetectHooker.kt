package com.astrixforge.devicemasker.xposed.hooker

import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.type.java.BooleanType
import com.highcapable.yukihookapi.hook.type.java.StringClass

/**
 * Anti-Detection Hooker - Hides Xposed/LSPosed presence from detection.
 *
 * ⚠️ CRITICAL: This hooker MUST be loaded FIRST before any other hookers!
 * Early detection checks can discover the hook framework before spoofing
 * hooks are active.
 *
 * Detection Vectors Covered:
 * 1. Stack Trace Analysis - Filter Thread/Throwable.getStackTrace()
 * 2. Class Loading - Block Class.forName() for Xposed classes
 * 3. /proc/self/maps - Filter native library paths
 * 4. Package Detection - Hide Xposed installer packages
 */
object AntiDetectHooker : YukiBaseHooker() {

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
            YLog.debug("AntiDetectHooker: Skipping for protected process: $packageName")
            return
        }

        YLog.debug("AntiDetectHooker: Loading anti-detection hooks for: $packageName")

        hookStackTraces()
        hookClassLoading()
        hookProcMaps()
        hookPackageManager()

        YLog.debug("AntiDetectHooker: Anti-detection hooks registered")
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

    private fun hookClassLoading() {
        // Hook Class.forName with safer approach
        runCatching {
            "java.lang.Class".toClass().apply {
                method {
                    name = "forName"
                    param(StringClass)
                }.hook {
                    before {
                        val className = args(0).string()
                        if (isExactXposedClass(className)) {
                            // Block Xposed class loading
                            resultNull()
                            throw ClassNotFoundException("Class not found: $className")
                        }
                        // For non-Xposed classes, do nothing - let original execute
                    }
                }

                method {
                    name = "forName"
                    param(StringClass, BooleanType, ClassLoader::class.java)
                }.hook {
                    before {
                        val className = args(0).string()
                        if (isExactXposedClass(className)) {
                            resultNull()
                            throw ClassNotFoundException("Class not found: $className")
                        }
                    }
                }
            }
        }.onFailure { e ->
            YLog.warn("AntiDetectHooker: Failed to hook Class.forName: ${e.message}")
        }

        // Hook ClassLoader.loadClass with safer approach
        runCatching {
            "java.lang.ClassLoader".toClass().apply {
                method {
                    name = "loadClass"
                    param(StringClass)
                }.hook {
                    before {
                        val className = args(0).string()
                        if (isExactXposedClass(className)) {
                            // Throw exception to block class loading
                            resultNull()
                            throw ClassNotFoundException("Class not found: $className")
                        }
                    }
                }

                method {
                    name = "loadClass"
                    param(StringClass, BooleanType)
                }.hook {
                    before {
                        val className = args(0).string()
                        if (isExactXposedClass(className)) {
                            resultNull()
                            throw ClassNotFoundException("Class not found: $className")
                        }
                    }
                }
            }
        }.onFailure { e ->
            YLog.warn("AntiDetectHooker: Failed to hook ClassLoader.loadClass: ${e.message}")
        }
    }

    /**
     * Checks if a class name is an EXACT Xposed-related class that should be blocked.
     * More restrictive than shouldBlockClass to avoid false positives.
     */
    private fun isExactXposedClass(className: String): Boolean {
        // Never block standard Android/Java/Kotlin classes
        val safePatterns = listOf(
            "android.",
            "java.",
            "javax.",
            "kotlin.",
            "kotlinx.",
            "androidx.",
            "com.google.",
            "dalvik.",
            "sun.",
            "org.apache.",
            "org.json.",
            "org.xml.",
            "com.astrixforge.devicemasker",
        )
        
        if (safePatterns.any { className.startsWith(it) }) {
            return false
        }

        // Only block classes that START with Xposed patterns (not contains)
        val xposedStartPatterns = listOf(
            "de.robv.android.xposed",
            "io.github.lsposed",
            "org.lsposed",
            "com.elderdrivers.riru",
            "rikka.ndk",
        )

        // Block if class starts with any Xposed pattern
        return xposedStartPatterns.any { pattern ->
            className.startsWith(pattern)
        }
    }

    private fun shouldBlockClass(className: String): Boolean {
        // Use the more restrictive check
        return isExactXposedClass(className)
    }

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
