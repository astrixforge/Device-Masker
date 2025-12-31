package com.astrixforge.devicemasker.data.models

import android.graphics.Bitmap
import androidx.compose.runtime.Immutable

@Immutable
data class InstalledApp(
    val packageName: String,
    val label: String,
    val isSystemApp: Boolean = false,
    val versionName: String = "",
    val iconBitmap: Bitmap? = null,
)
