package com.astrixforge.devicemasker.xposed.hooker

import com.astrixforge.devicemasker.common.SpoofType
import com.astrixforge.devicemasker.xposed.DeviceMaskerService
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.type.java.StringClass

/**
 * Network Identifier Hooker - Spoofs network-related identifiers.
 *
 * Hooks WifiManager, BluetoothAdapter, TelephonyManager for:
 * - WiFi MAC address
 * - Bluetooth MAC address
 * - WiFi SSID/BSSID
 * - Carrier name and MCC/MNC
 */
object NetworkHooker : YukiBaseHooker() {

    private val fallbackWifiMac by lazy { generateMac() }
    private val fallbackBluetoothMac by lazy { generateMac() }

    private fun getSpoofValue(type: SpoofType, fallback: () -> String): String {
        val service = DeviceMaskerService.instance ?: return fallback()
        val config = service.config
        val group = config.getGroupForApp(packageName) ?: return fallback()

        if (!group.isEnabled) return fallback()
        if (!group.isTypeEnabled(type)) return fallback()

        return group.getValue(type) ?: fallback()
    }

    override fun onHook() {
        YLog.debug("NetworkHooker: Starting hooks for: $packageName")

        hookWifiInfo()
        hookNetworkInterface()
        hookBluetoothAdapter()
        hookTelephonyCarrier()

        DeviceMaskerService.instance?.incrementHookCount()
    }

    private fun hookWifiInfo() {
        runCatching {
            "android.net.wifi.WifiInfo".toClass().apply {
                method {
                    name = "getMacAddress"
                    emptyParam()
                }.hook {
                    after {
                        result = getSpoofValue(SpoofType.WIFI_MAC) { fallbackWifiMac }
                    }
                }

                runCatching {
                    method {
                        name = "getSSID"
                        emptyParam()
                    }.hook {
                        after {
                            val currentSsid = result as? String
                            val spoofed = getSpoofValue(SpoofType.WIFI_SSID) { currentSsid ?: "\"unknown\"" }
                            result = if (spoofed.startsWith("\"")) spoofed else "\"$spoofed\""
                        }
                    }
                }

                runCatching {
                    method {
                        name = "getBSSID"
                        emptyParam()
                    }.hook {
                        after {
                            result = getSpoofValue(SpoofType.WIFI_BSSID) { generateMac() }
                        }
                    }
                }
            }
        }
    }

    private fun hookNetworkInterface() {
        runCatching {
            "java.net.NetworkInterface".toClass().apply {
                method {
                    name = "getHardwareAddress"
                    emptyParam()
                }.hook {
                    after {
                        val spoofedMac = getSpoofValue(SpoofType.WIFI_MAC) { fallbackWifiMac }
                        result = parseMacToBytes(spoofedMac)
                    }
                }
            }
        }
    }

    private fun hookBluetoothAdapter() {
        runCatching {
            "android.bluetooth.BluetoothAdapter".toClass().apply {
                method {
                    name = "getAddress"
                    emptyParam()
                }.hook {
                    after {
                        result = getSpoofValue(SpoofType.BLUETOOTH_MAC) { fallbackBluetoothMac }
                    }
                }
            }
        }
    }

    private fun hookTelephonyCarrier() {
        runCatching {
            "android.telephony.TelephonyManager".toClass().apply {
                method {
                    name = "getNetworkOperatorName"
                    emptyParam()
                }.hook {
                    after {
                        val current = result as? String
                        result = getSpoofValue(SpoofType.CARRIER_NAME) { current ?: "Unknown" }
                    }
                }

                method {
                    name = "getSimOperatorName"
                    emptyParam()
                }.hook {
                    after {
                        val current = result as? String
                        result = getSpoofValue(SpoofType.CARRIER_NAME) { current ?: "Unknown" }
                    }
                }

                method {
                    name = "getNetworkOperator"
                    emptyParam()
                }.hook {
                    after {
                        val current = result as? String
                        result = getSpoofValue(SpoofType.CARRIER_MCC_MNC) { current ?: "310260" }
                    }
                }

                method {
                    name = "getSimOperator"
                    emptyParam()
                }.hook {
                    after {
                        val current = result as? String
                        result = getSpoofValue(SpoofType.CARRIER_MCC_MNC) { current ?: "310260" }
                    }
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    // GENERATORS
    // ═══════════════════════════════════════════════════════════

    private fun generateMac(): String {
        val bytes = ByteArray(6)
        java.util.Random().nextBytes(bytes)
        // Clear multicast bit, set local bit
        bytes[0] = ((bytes[0].toInt() and 0xFC) or 0x02).toByte()
        return bytes.joinToString(":") { String.format("%02X", it) }
    }

    private fun parseMacToBytes(mac: String): ByteArray {
        return try {
            mac.split(":").map { it.toInt(16).toByte() }.toByteArray()
        } catch (e: Exception) {
            ByteArray(6)
        }
    }
}
