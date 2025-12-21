@file:Suppress("unused")

package com.astrixforge.devicemasker.data.models

/**
 * Type aliases for backward compatibility.
 * 
 * HMA-OSS Architecture: Models have been moved to the :common module.
 * These type aliases allow existing UI code to continue working without changes.
 */

// SpoofType - Same enum, now in common
typealias SpoofType = com.astrixforge.devicemasker.common.SpoofType

// SpoofCategory - Same enum, now in common  
typealias SpoofCategory = com.astrixforge.devicemasker.common.SpoofCategory

// DeviceIdentifier - Same data class, now in common
typealias DeviceIdentifier = com.astrixforge.devicemasker.common.DeviceIdentifier

// SpoofProfile - Same data class, now in common
typealias SpoofProfile = com.astrixforge.devicemasker.common.SpoofProfile

// AppConfig - Same data class, now in common
typealias AppConfig = com.astrixforge.devicemasker.common.AppConfig
