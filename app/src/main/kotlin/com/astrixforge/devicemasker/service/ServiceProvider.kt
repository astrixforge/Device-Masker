package com.astrixforge.devicemasker.service

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import com.astrixforge.devicemasker.common.Constants
import timber.log.Timber

/**
 * Service Provider - ContentProvider for receiving binder from system_server.
 *
 * In HMA-OSS architecture:
 * - DeviceMaskerService hooks ContentProvider.call() in system_server
 * - When our provider is accessed, the hook injects the service binder
 * - This provider receives the binder and links it via ServiceClient
 *
 * This is an alternative to Shizuku-style binder injection when
 * the simpler approach of receiving in ContentProvider extras works.
 */
class ServiceProvider : ContentProvider() {

    companion object {
        private const val TAG = "ServiceProvider"

        /**
         * Method used to request the service binder.
         */
        const val METHOD_GET_BINDER = "getBinder"
    }

    override fun onCreate(): Boolean {
        Timber.tag(TAG).d("ServiceProvider created")
        return true
    }

    /**
     * Handles calls to the provider. The system_server hook will inject
     * the binder into the result Bundle.
     */
    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        Timber.tag(TAG).d("call: method=$method")

        when (method) {
            METHOD_GET_BINDER -> {
                // The hook in system_server adds the binder to extras
                // We check if it's already there (from hook) or in the returned Bundle
                extras?.let { bundle ->
                    bundle.classLoader = javaClass.classLoader
                    val binder = bundle.getBinder("binder")
                    if (binder != null) {
                        Timber.tag(TAG).i("Received binder from extras, linking service")
                        ServiceClient.linkService(binder)
                        return Bundle().apply {
                            putBoolean("success", true)
                            putInt("version", ServiceClient.getServiceVersion())
                        }
                    }
                }

                // Return current status if already connected
                return Bundle().apply {
                    putBoolean("success", ServiceClient.isServiceAvailable())
                    putInt("version", ServiceClient.getServiceVersion())
                }
            }
        }

        return null
    }

    // ═══════════════════════════════════════════════════════════
    // Required ContentProvider methods (not used)
    // ═══════════════════════════════════════════════════════════

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?
    ): Int = 0
}
