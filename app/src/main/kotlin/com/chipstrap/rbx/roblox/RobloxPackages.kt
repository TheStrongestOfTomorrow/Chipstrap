package com.chipstrap.rbx.roblox

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.flow.first
import com.chipstrap.rbx.data.SettingsStore

/**
 * Helpers to locate the Roblox package on the device.
 *
 * IMPORTANT: On Android 11+ (API 30+), package visibility rules mean that
 * getPackageInfo() on another app will throw NameNotFoundException unless:
 *   (a) QUERY_ALL_PACKAGES permission is granted (Play Store restricts this), OR
 *   (b) The target package is declared in a <queries> block in AndroidManifest.xml
 *
 * We declare <queries> for the two known Roblox packages AND Shizuku in the
 * manifest, so isInstalled() should work on any device without needing the
 * restricted permission.
 *
 * As a defensive fallback, we also try launching an intent for the package —
 * if the system can resolve a launch intent, the package is installed regardless
 * of what getPackageInfo says.
 */
object RobloxPackages {

    const val GLOBAL = "com.roblox.client"
    const val VNG = "com.roblox.client.vnggames"

    private const val TAG = "Chipstrap.RobloxPkgs"

    suspend fun resolvePreferred(context: Context): String {
        val pref = runCatching { SettingsStore.preferredRobloxApp.first() }.getOrDefault("global")
        return when (pref) {
            "global" -> GLOBAL
            "vng" -> VNG
            "custom" -> {
                val custom = runCatching { SettingsStore.customRobloxPackage.first() }.getOrDefault("")
                custom.ifBlank { GLOBAL }
            }
            else -> GLOBAL
        }
    }

    /**
     * Returns true if [pkg] is installed on the device. Uses three layers of
     * detection to defeat Android 11+ package-visibility restrictions:
     *
     * 1. Direct getPackageInfo() — works if we have <queries> for the package
     *    or QUERY_ALL_PACKAGES.
     * 2. getLaunchIntentForPackage() — the system will resolve a launch intent
     *    for installed apps even if we can't query their full PackageInfo.
     * 3. ApplicationInfo lookup — another path that sometimes works when #1 doesn't.
     */
    fun isInstalled(context: Context, pkg: String): Boolean {
        // Quick reject for blank/invalid input
        if (pkg.isBlank()) return false

        // Method 1: direct getPackageInfo
        runCatching {
            context.packageManager.getPackageInfo(pkg, 0)
            return true
        }.onFailure { e ->
            if (e !is PackageManager.NameNotFoundException) {
                Log.w(TAG, "getPackageInfo($pkg) threw: ${e.message}")
            }
        }

        // Method 2: launch intent — most reliable on Android 11+ for installed apps
        runCatching {
            val intent = context.packageManager.getLaunchIntentForPackage(pkg)
            if (intent != null) {
                Log.d(TAG, "Detected $pkg via getLaunchIntentForPackage (visibility workaround)")
                return true
            }
        }.onFailure {
            Log.w(TAG, "getLaunchIntentForPackage($pkg) threw: ${it.message}")
        }

        // Method 3: ApplicationInfo
        runCatching {
            context.packageManager.getApplicationInfo(pkg, 0)
            return true
        }.onFailure {
            // Ignore — fall through to false
        }

        return false
    }

    /**
     * Returns the list of installed Roblox packages we know about, in priority
     * order (Global first, then VNG). Useful for auto-detecting which Roblox
     * the user has installed even if their preferred setting points at the wrong one.
     */
    fun installedPackages(context: Context): List<String> {
        return listOf(GLOBAL, VNG).filter { isInstalled(context, it) }
    }

    /**
     * Returns true if ANY known Roblox package is installed.
     */
    fun anyInstalled(context: Context): Boolean = installedPackages(context).isNotEmpty()

    fun versionName(context: Context, pkg: String): String? = runCatching {
        context.packageManager.getPackageInfo(pkg, 0).versionName
    }.getOrNull()

    fun isDebugBuild(context: Context, pkg: String): Boolean = runCatching {
        val ai: ApplicationInfo = context.packageManager.getApplicationInfo(pkg, 0)
        (ai.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }.getOrDefault(false)

    /** Path of the Roblox data directory (best-effort). */
    fun dataDir(context: Context, pkg: String): String? = runCatching {
        context.packageManager.getApplicationInfo(pkg, 0).dataDir
    }.getOrNull()
}
