package com.astrixforge.devicemasker.common.generators

import com.astrixforge.devicemasker.common.DeviceProfilePreset
import com.astrixforge.devicemasker.common.models.DeviceHardwareProfile

/**
 * Generates complete, correlated device hardware profiles.
 * 
 * This ensures device hardware values are consistent:
 * - IMEI is always generated (all modern devices have IMEI)
 * - Serial number matches manufacturer pattern
 * - WiFi MAC may use manufacturer OUI
 * - All values align with the device profile
 * 
 * Note: MEID has been removed as CDMA networks were deprecated in 2022.
 */
object DeviceHardwareProfileGenerator {
    
    /**
     * Generates a complete device hardware profile.
     * 
     * @param deviceProfile The device profile to base hardware on
     * @return DeviceHardwareProfile with all correlated values
     */
    fun generate(deviceProfile: DeviceProfilePreset): DeviceHardwareProfile {
        return DeviceHardwareProfile(
            deviceProfile = deviceProfile,
            imei = IMEIGenerator.generate(),
            serial = SerialGenerator.generate(deviceProfile.manufacturer),
            wifiMAC = MACGenerator.generateWiFiMAC(deviceProfile.manufacturer),
            bluetoothMAC = MACGenerator.generateBluetoothMAC()  // Independent
        )
    }
    
    /**
     * Generates a device hardware profile from a random preset.
     * 
     * @return DeviceHardwareProfile with random device
     */
    fun generate(): DeviceHardwareProfile {
        val randomPreset = DeviceProfilePreset.PRESETS.random()
        return generate(randomPreset)
    }
    
    /**
     * Generates a device hardware profile from a preset ID.
     * 
     * @param presetId Device profile preset ID (e.g., "pixel_8_pro")
     * @return DeviceHardwareProfile or null if preset not found
     */
    fun generateFromPresetId(presetId: String): DeviceHardwareProfile? {
        val preset = DeviceProfilePreset.findById(presetId) ?: return null
        return generate(preset)
    }
}

