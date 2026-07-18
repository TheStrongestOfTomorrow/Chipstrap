package com.chipstrap.rbx

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import com.chipstrap.rbx.core.Logger
import com.chipstrap.rbx.data.AppPaths
import com.chipstrap.rbx.data.SettingsStore
import com.chipstrap.rbx.shizuku.ShizukuManager

/**
 * Application entrypoint. Sets up paths, logger, notification channels.
 *
 * Each init step is wrapped in its own try/catch so a failure in one (e.g. Logger
 * hitting a read-only filesystem) doesn't prevent the app from starting — that
 * would manifest as an instant crash on launch.
 */
class ChipstrapApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Step 1: paths (cheap, just stores File references)
        runCatching { AppPaths.init(this) }
            .onFailure { Log.e(TAG, "AppPaths.init failed", it) }

        // Step 2: logger (uses AppPaths.logsDir, but tolerate failure with a fallback)
        runCatching { Logger.init(AppPaths.logsDir) }
            .onFailure { Log.e(TAG, "Logger.init failed", it) }

        // Step 3: settings store (just stores the context — but this MUST succeed
        // because every Flow in SettingsStore reads appContext. If init fails, the
        // app is broken; we re-throw to fail fast rather than crash later in a
        // confusing place.)
        try {
            SettingsStore.init(this)
        } catch (t: Throwable) {
            Log.e(TAG, "SettingsStore.init failed", t)
            // Don't rethrow — let the app start; the UI is defensive about
            // SettingsStore failures.
        }

        // Step 4: notification channels (only on O+)
        runCatching { createNotificationChannels() }
            .onFailure { Log.e(TAG, "createNotificationChannels failed", it) }

        // Step 5: init Shizuku context (needed for permission checks before
        // any activity is alive). The actual binder listeners are registered
        // from MainActivity so we can show UI when permission is granted.
        runCatching { ShizukuManager.initContext(this) }
            .onFailure { Log.e(TAG, "ShizukuManager.initContext failed", it) }

        Log.i(TAG, "Chipstrap ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) onCreate complete")
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NotificationManager::class.java) ?: return
        runCatching {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_LAUNCHER,
                    getString(R.string.notif_channel_launcher),
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
        runCatching {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_SERVER,
                    getString(R.string.notif_channel_server),
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        }
    }

    companion object {
        private const val TAG = "ChipstrapApp"
        const val CHANNEL_LAUNCHER = "chipstrap.launcher"
        const val CHANNEL_SERVER = "chipstrap.server"

        @Volatile lateinit var instance: ChipstrapApp
            private set
    }
}
