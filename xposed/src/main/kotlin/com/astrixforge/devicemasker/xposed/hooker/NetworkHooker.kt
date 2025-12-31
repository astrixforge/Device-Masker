package com.astrixforge.devicemasker.xposed.hooker

import com.astrixforge.devicemasker.common.SpoofType
import com.astrixforge.devicemasker.xposed.utils.ValueGenerators
import com.highcapable.yukihookapi.hook.factory.method

/**
 * Network Identifier Hooker - Spoofs network-related identifiers.
 *
 * Hooks WifiManager, BluetoothAdapter, TelephonyManager for:
 * - WiFi MAC address
 * - Bluetooth MAC address
 * - WiFi SSID/BSSID
 * - Carrier name and MCC/MNC
 */
object NetworkHooker : BaseSpoofHooker("NetworkHooker") {

    // Cached class references
    private val wifiInfoClass by lazy { "android.net.wifi.WifiInfo".toClassOrNull() }
    private val bluetoothAdapterClass by lazy {
        "android.bluetooth.BluetoothAdapter".toClassOrNull()
    }
    private val telephonyClass by lazy { "android.telephony.TelephonyManager".toClassOrNull() }
    private val networkInterfaceClass by lazy { "java.net.NetworkInterface".toClassOrNull() }

    // Fallback values (thread-safe lazy)
    private val fallbackWifiMac by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { ValueGenerators.mac() }
    private val fallbackBluetoothMac by
        lazy(LazyThreadSafetyMode.SYNCHRONIZED) { ValueGenerators.mac() }

    override fun onHook() {
        logStart()
        hookWifiInfo()
        hookNetworkInterface()
        hookBluetoothAdapter()
        hookTelephonyCarrier()
        recordSuccess()
    }

    // ═══════════════════════════════════════════════════════════
    // WIFI INFO HOOKS
    // ═══════════════════════════════════════════════════════════

    private fun hookWifiInfo() {
        val cachedWifiMac = getSpoofValue(SpoofType.WIFI_MAC) { fallbackWifiMac }
        val cachedSsid = getSpoofValue(SpoofType.WIFI_SSID) { "\"HomeNetwork\"" }
        val cachedBssid = getSpoofValue(SpoofType.WIFI_BSSID) { ValueGenerators.mac() }

        wifiInfoClass?.apply {
            method {
                    name = "getMacAddress"
                    emptyParam()
                }
                .hook { replaceAny { cachedWifiMac } }

            runCatching {
                method {
                        name = "getSSID"
                        emptyParam()
                    }
                    .hook {
                        replaceAny {
                            if (cachedSsid.startsWith("\"")) cachedSsid else "\"$cachedSsid\""
                        }
                    }
            }

            runCatching {
                method {
                        name = "getBSSID"
                        emptyParam()
                    }
                    .hook { replaceAny { cachedBssid } }
            }
        } ?: logDebug("WifiInfo class not found")
    }

    // ═══════════════════════════════════════════════════════════
    // NETWORK INTERFACE HOOKS
    // ═══════════════════════════════════════════════════════════

    private fun hookNetworkInterface() {
        networkInterfaceClass?.apply {
            method {
                    name = "getHardwareAddress"
                    emptyParam()
                }
                .hook {
                    after {
                        val spoofedMac = getSpoofValue(SpoofType.WIFI_MAC) { fallbackWifiMac }
                        result = ValueGenerators.parseMacToBytes(spoofedMac)
                    }
                }
        } ?: logDebug("NetworkInterface class not found")
    }

    // ═══════════════════════════════════════════════════════════
    // BLUETOOTH ADAPTER HOOKS
    // ═══════════════════════════════════════════════════════════

    private fun hookBluetoothAdapter() {
        bluetoothAdapterClass?.apply {
            method {
                    name = "getAddress"
                    emptyParam()
                }
                .hook {
                    after {
                        result = getSpoofValue(SpoofType.BLUETOOTH_MAC) { fallbackBluetoothMac }
                    }
                }
        } ?: logDebug("BluetoothAdapter class not found")
    }

    // ═══════════════════════════════════════════════════════════
    // TELEPHONY CARRIER HOOKS
    // ═══════════════════════════════════════════════════════════

    private fun hookTelephonyCarrier() {
        telephonyClass?.apply {
            method {
                    name = "getNetworkOperatorName"
                    emptyParam()
                }
                .hook {
                    after {
                        val current = result as? String
                        result = getSpoofValue(SpoofType.CARRIER_NAME) { current ?: "Unknown" }
                    }
                }

            runCatching {
                method {
                        name = "getSimOperatorName"
                        emptyParam()
                    }
                    .hook {
                        after {
                            val current = result as? String
                            result = getSpoofValue(SpoofType.CARRIER_NAME) { current ?: "Unknown" }
                        }
                    }
            }

            method {
                    name = "getNetworkOperator"
                    emptyParam()
                }
                .hook {
                    after {
                        val current = result as? String
                        result = getSpoofValue(SpoofType.CARRIER_MCC_MNC) { current ?: "310260" }
                    }
                }

            runCatching {
                method {
                        name = "getSimOperator"
                        emptyParam()
                    }
                    .hook {
                        after {
                            val current = result as? String
                            result =
                                getSpoofValue(SpoofType.CARRIER_MCC_MNC) { current ?: "310260" }
                        }
                    }
            }
        } ?: logDebug("TelephonyManager class not found")
    }
}
