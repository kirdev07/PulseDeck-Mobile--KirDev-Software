package com.kirdev.pulsedeckmobile

import android.content.Context

/** Version from the installed package (no BuildConfig generation needed). */
object BuildConfigInfo {
    fun version(context: Context): String =
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: ""
}
