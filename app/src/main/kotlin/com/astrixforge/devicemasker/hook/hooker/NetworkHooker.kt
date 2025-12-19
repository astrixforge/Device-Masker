package com.astrixforge.devicemasker.hook.hooker

import android.content.Context
import com.astrixforge.devicemasker.data.generators.MACGenerator
import com.astrixforge.devicemasker.data.models.SpoofType
import com.astrixforge.devicemasker.hook.HookDataProvider
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.*
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
 * Uses HookDataProvider to read profile-based values and global config.
 */
object NetworkHooker : YukiBaseHooker() {

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
                            "NetworkHooker: Failed to create HookDataProvider: ${it.message}"
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
            YLog.debug("NetworkHooker: No provider for $type, using generated value")
            return generator()
        }

        // getSpoofValue now handles all profile-based checks
        return provider.getSpoofValue(type) ?: generator()
    }

    // ═══════════════════════════════════════════════════════════
    // FALLBACK GENERATORS
    // ═══════════════════════════════════════════════════════════

    private val fallbackWifiMac: String by lazy { MACGenerator.generateWiFiMAC() }
    private val fallbackBluetoothMac: String by lazy { MACGenerator.generateBluetoothMAC() }
    private val fallbackSsid: String by lazy { "\"SpoofedNetwork\"" }
    private val fallbackBssid: String by lazy { MACGenerator.generate() }
    private val fallbackCarrierName: String by lazy { "Carrier" }
    private val fallbackMccMnc: String by lazy { "310260" } // T-Mobile US

    override fun onHook() {
        YLog.debug("NetworkHooker: Starting hooks for package: $packageName")

        hookWifiInfo()
        hookNetworkInterface()
        hookBluetoothAdapter()
        hookCarrierInfo()

        YLog.debug("NetworkHooker: Hooks registered for package: $packageName")
    }

    /** Hooks WifiInfo methods for WiFi MAC address and network info. */
    private fun hookWifiInfo() {
        "android.net.wifi.WifiInfo".toClass().apply {

            // getMacAddress() - WiFi MAC address
            method {
                    name = "getMacAddress"
                    emptyParam()
                }
                .hook {
                    after {
                        val value =
                            getSpoofValueOrGenerate(appContext, SpoofType.WIFI_MAC) {
                                fallbackWifiMac
                            }
                        YLog.debug("NetworkHooker: Spoofing WifiInfo.getMacAddress() -> $value")
                        result = value
                    }
                }

            // getSSID() - Connected network name
            method {
                    name = "getSSID"
                    emptyParam()
                }
                .hook {
                    after {
                        val value =
                            getSpoofValueOrGenerate(appContext, SpoofType.WIFI_SSID) {
                                fallbackSsid
                            }
                        YLog.debug("NetworkHooker: Spoofing WifiInfo.getSSID() -> $value")
                        result = value
                    }
                }

            // getBSSID() - Connected access point MAC
            method {
                    name = "getBSSID"
                    emptyParam()
                }
                .hook {
                    after {
                        val value =
                            getSpoofValueOrGenerate(appContext, SpoofType.WIFI_BSSID) {
                                fallbackBssid
                            }
                        YLog.debug("NetworkHooker: Spoofing WifiInfo.getBSSID() -> $value")
                        result = value
                    }
                }
        }
    }

    /** Hooks NetworkInterface for hardware address access. */
    private fun hookNetworkInterface() {
        "java.net.NetworkInterface".toClass().apply {

            // getHardwareAddress() - Returns byte array of MAC address
            method {
                    name = "getHardwareAddress"
                    emptyParam()
                }
                .hook {
                    after {
                        val originalResult = result as? ByteArray
                        if (originalResult != null && originalResult.size == 6) {
                            val value =
                                getSpoofValueOrGenerate(appContext, SpoofType.WIFI_MAC) {
                                    fallbackWifiMac
                                }
                            val spoofedBytes = macStringToBytes(value)
                            YLog.debug(
                                "NetworkHooker: Spoofing NetworkInterface.getHardwareAddress()"
                            )
                            result = spoofedBytes
                        }
                    }
                }
        }
    }

    /** Hooks BluetoothAdapter for Bluetooth MAC address. */
    private fun hookBluetoothAdapter() {
        "android.bluetooth.BluetoothAdapter".toClass().apply {

            // getAddress() - Bluetooth MAC address
            method {
                    name = "getAddress"
                    emptyParam()
                }
                .hook {
                    after {
                        val value =
                            getSpoofValueOrGenerate(appContext, SpoofType.BLUETOOTH_MAC) {
                                fallbackBluetoothMac
                            }
                        YLog.debug(
                            "NetworkHooker: Spoofing BluetoothAdapter.getAddress() -> $value"
                        )
                        result = value
                    }
                }
        }
    }

    /** Hooks TelephonyManager for carrier/network operator information. */
    private fun hookCarrierInfo() {
        "android.telephony.TelephonyManager".toClass().apply {

            // getNetworkOperatorName() - Carrier name
            method {
                    name = "getNetworkOperatorName"
                    emptyParam()
                }
                .hook {
                    after {
                        val value =
                            getSpoofValueOrGenerate(appContext, SpoofType.CARRIER_NAME) {
                                fallbackCarrierName
                            }
                        YLog.debug("NetworkHooker: Spoofing getNetworkOperatorName() -> $value")
                        result = value
                    }
                }

            // getNetworkOperator() - MCC+MNC string
            method {
                    name = "getNetworkOperator"
                    emptyParam()
                }
                .hook {
                    after {
                        val value =
                            getSpoofValueOrGenerate(appContext, SpoofType.CARRIER_MCC_MNC) {
                                fallbackMccMnc
                            }
                        YLog.debug("NetworkHooker: Spoofing getNetworkOperator() -> $value")
                        result = value
                    }
                }

            // getSimOperatorName() - SIM carrier name
            method {
                    name = "getSimOperatorName"
                    emptyParam()
                }
                .hook {
                    after {
                        val value =
                            getSpoofValueOrGenerate(appContext, SpoofType.CARRIER_NAME) {
                                fallbackCarrierName
                            }
                        YLog.debug("NetworkHooker: Spoofing getSimOperatorName() -> $value")
                        result = value
                    }
                }

            // getSimOperator() - SIM MCC+MNC
            method {
                    name = "getSimOperator"
                    emptyParam()
                }
                .hook {
                    after {
                        val value =
                            getSpoofValueOrGenerate(appContext, SpoofType.CARRIER_MCC_MNC) {
                                fallbackMccMnc
                            }
                        YLog.debug("NetworkHooker: Spoofing getSimOperator() -> $value")
                        result = value
                    }
                }
        }
    }

    /** Converts a MAC address string (XX:XX:XX:XX:XX:XX) to a byte array. */
    private fun macStringToBytes(mac: String): ByteArray {
        return mac.split("[:-]".toRegex()).map { it.toInt(16).toByte() }.toByteArray()
    }
}
