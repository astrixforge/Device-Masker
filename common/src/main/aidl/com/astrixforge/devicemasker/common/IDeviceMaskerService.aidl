// IDeviceMaskerService.aidl
// AIDL interface for communication between the app UI and the Xposed module
// running in system_server.

package com.astrixforge.devicemasker.common;

/**
 * Device Masker Service Interface
 *
 * This AIDL interface defines the IPC contract between:
 * - App UI (client) - Uses ServiceClient to call these methods
 * - Xposed module (server) - DeviceMaskerService implements this interface
 *
 * The service runs in system_server and holds the config in memory for
 * instant access by hooks.
 */
interface IDeviceMaskerService {

    // ═══════════════════════════════════════════════════════════
    // SERVICE MANAGEMENT
    // ═══════════════════════════════════════════════════════════

    /**
     * Returns the service API version for compatibility checks.
     * @return Version number (e.g., 1)
     */
    int getServiceVersion();

    /**
     * Stops the service and optionally cleans up state.
     * @param cleanEnv If true, clears cached config
     */
    void stopService(boolean cleanEnv);

    // ═══════════════════════════════════════════════════════════
    // CONFIGURATION
    // ═══════════════════════════════════════════════════════════

    /**
     * Reads the current configuration as JSON.
     * @return JSON string of the full configuration
     */
    String readConfig();

    /**
     * Writes configuration from JSON and updates in-memory state.
     * @param json JSON string of the full configuration
     */
    void writeConfig(String json);

    // ═══════════════════════════════════════════════════════════
    // LOGGING
    // ═══════════════════════════════════════════════════════════

    /**
     * Gets recent log entries from the service.
     * @return Array of log message strings
     */
    String[] getLogs();

    /**
     * Clears all stored log entries.
     */
    void clearLogs();

    /**
     * Adds a log entry to the service log buffer.
     * @param level Log level (0=verbose, 1=debug, 2=info, 3=warn, 4=error)
     * @param tag Log tag
     * @param message Log message
     */
    void log(int level, String tag, String message);

    // ═══════════════════════════════════════════════════════════
    // STATUS
    // ═══════════════════════════════════════════════════════════

    /**
     * Checks if the module is enabled globally.
     * @return True if module is active
     */
    boolean isModuleEnabled();

    /**
     * Returns the count of currently active hooks.
     * @return Number of hooked methods
     */
    int getHookCount();

    // ═══════════════════════════════════════════════════════════
    // DIRECT VALUE ACCESS (for diagnostics)
    // ═══════════════════════════════════════════════════════════

    /**
     * Gets the spoofed value for a specific package and type.
     * This is an optimization for diagnostics - direct access without
     * parsing the full config.
     *
     * @param packageName App package name
     * @param spoofType The SpoofType name (e.g., "IMEI")
     * @return Spoofed value or null if not configured
     */
    String getSpoofValue(String packageName, String spoofType);
}
