package com.astrixforge.devicemasker.hook.hooker

import com.astrixforge.devicemasker.data.generators.MACGenerator
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.type.java.StringClass
import com.highcapable.yukihookapi.hook.log.YLog

/**
 * Network Identifier Hooker - Spoofs WiFi and Bluetooth identifiers.
 *
 * Hooks:
 * - WifiInfo: getMacAddress(), getSSID(), getBSSID()
 * - NetworkInterface: getHardwareAddress()
 * - BluetoothAdapter: getAddress()
 * - TelephonyManager: getNetworkOperatorName(), getNetworkOperator()
 *
 * This hooker requires Phase 4 (DataStore) for persistent values.
 * Currently uses generator defaults.
 */
object NetworkHooker : YukiBaseHooker() {

    // ═══════════════════════════════════════════════════════════
    // CACHED SPOOFED VALUES
    // These will be replaced with DataStore reads in Phase 4
    // ═══════════════════════════════════════════════════════════

    private val spoofedWifiMac: String by lazy { MACGenerator.generateWiFiMAC() }
    private val spoofedBluetoothMac: String by lazy { MACGenerator.generateBluetoothMAC() }
    private val spoofedSsid: String by lazy { "\"SpoofedNetwork\"" } // SSID is quoted
    private val spoofedBssid: String by lazy { MACGenerator.generate() }
    private val spoofedCarrierName: String by lazy { "Carrier" }
    private val spoofedMccMnc: String by lazy { "310260" } // T-Mobile US

    override fun onHook() {
        YLog.debug("NetworkHooker: Starting hooks for package: $packageName")

        // ═══════════════════════════════════════════════════════════
        // WIFI INFO HOOKS
        // ═══════════════════════════════════════════════════════════

        hookWifiInfo()

        // ═══════════════════════════════════════════════════════════
        // NETWORK INTERFACE HOOKS
        // ═══════════════════════════════════════════════════════════

        hookNetworkInterface()

        // ═══════════════════════════════════════════════════════════
        // BLUETOOTH ADAPTER HOOKS
        // ═══════════════════════════════════════════════════════════

        hookBluetoothAdapter()

        // ═══════════════════════════════════════════════════════════
        // CARRIER/NETWORK OPERATOR HOOKS
        // ═══════════════════════════════════════════════════════════

        hookCarrierInfo()

        YLog.debug("NetworkHooker: Hooks registered for package: $packageName")
    }

    /**
     * Hooks WifiInfo methods for WiFi MAC address and network info.
     */
    private fun hookWifiInfo() {
        "android.net.wifi.WifiInfo".toClass().apply {

            // getMacAddress() - WiFi MAC address
            method {
                name = "getMacAddress"
                emptyParam()
            }.hook {
                after {
                    // On Android 6+, this returns 02:00:00:00:00:00 for privacy
                    // We return our spoofed MAC
                    YLog.debug("NetworkHooker: Spoofing WifiInfo.getMacAddress() -> $spoofedWifiMac")
                    result = spoofedWifiMac
                }
            }

            // getSSID() - Connected network name
            method {
                name = "getSSID"
                emptyParam()
            }.hook {
                after {
                    // SSID is wrapped in quotes: "NetworkName"
                    YLog.debug("NetworkHooker: Spoofing WifiInfo.getSSID() -> $spoofedSsid")
                    result = spoofedSsid
                }
            }

            // getBSSID() - Connected access point MAC
            method {
                name = "getBSSID"
                emptyParam()
            }.hook {
                after {
                    YLog.debug("NetworkHooker: Spoofing WifiInfo.getBSSID() -> $spoofedBssid")
                    result = spoofedBssid
                }
            }
        }
    }

    /**
     * Hooks NetworkInterface for hardware address access.
     * This is a lower-level API that some apps use to get MAC addresses.
     */
    private fun hookNetworkInterface() {
        "java.net.NetworkInterface".toClass().apply {

            // getHardwareAddress() - Returns byte array of MAC address
            method {
                name = "getHardwareAddress"
                emptyParam()
            }.hook {
                after {
                    val originalResult = result as? ByteArray
                    if (originalResult != null && originalResult.size == 6) {
                        // Convert spoofed MAC string to byte array
                        val spoofedBytes = macStringToBytes(spoofedWifiMac)
                        YLog.debug("NetworkHooker: Spoofing NetworkInterface.getHardwareAddress()")
                        result = spoofedBytes
                    }
                }
            }

            // getName() - Interface name check for filtering
            // Some apps iterate interfaces looking for "wlan0" or "eth0"
            // We don't need to hook this, just the hardware address
        }
    }

    /**
     * Hooks BluetoothAdapter for Bluetooth MAC address.
     */
    private fun hookBluetoothAdapter() {
        "android.bluetooth.BluetoothAdapter".toClass().apply {

            // getAddress() - Bluetooth MAC address
            method {
                name = "getAddress"
                emptyParam()
            }.hook {
                after {
                    YLog.debug("NetworkHooker: Spoofing BluetoothAdapter.getAddress() -> $spoofedBluetoothMac")
                    result = spoofedBluetoothMac
                }
            }
        }
    }

    /**
     * Hooks TelephonyManager for carrier/network operator information.
     */
    private fun hookCarrierInfo() {
        "android.telephony.TelephonyManager".toClass().apply {

            // getNetworkOperatorName() - Carrier name
            method {
                name = "getNetworkOperatorName"
                emptyParam()
            }.hook {
                after {
                    YLog.debug("NetworkHooker: Spoofing getNetworkOperatorName() -> $spoofedCarrierName")
                    result = spoofedCarrierName
                }
            }

            // getNetworkOperator() - MCC+MNC string (e.g., "310260")
            method {
                name = "getNetworkOperator"
                emptyParam()
            }.hook {
                after {
                    YLog.debug("NetworkHooker: Spoofing getNetworkOperator() -> $spoofedMccMnc")
                    result = spoofedMccMnc
                }
            }

            // getSimOperatorName() - SIM carrier name
            method {
                name = "getSimOperatorName"
                emptyParam()
            }.hook {
                after {
                    YLog.debug("NetworkHooker: Spoofing getSimOperatorName() -> $spoofedCarrierName")
                    result = spoofedCarrierName
                }
            }

            // getSimOperator() - SIM MCC+MNC
            method {
                name = "getSimOperator"
                emptyParam()
            }.hook {
                after {
                    YLog.debug("NetworkHooker: Spoofing getSimOperator() -> $spoofedMccMnc")
                    result = spoofedMccMnc
                }
            }
        }
    }

    /**
     * Converts a MAC address string (XX:XX:XX:XX:XX:XX) to a byte array.
     */
    private fun macStringToBytes(mac: String): ByteArray {
        return mac.split("[:-]".toRegex())
            .map { it.toInt(16).toByte() }
            .toByteArray()
    }
}
