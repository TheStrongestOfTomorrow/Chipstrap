package com.chipstrap.rbx.fflags.strategies

import android.content.Context
import com.chipstrap.rbx.core.Logger
import com.chipstrap.rbx.data.AppPaths
import com.chipstrap.rbx.shizuku.ShizukuManager

/**
 * Shizuku-based injection.
 *
 * Uses the official Shizuku API (dev.rikka.shizuku:api) to run a privileged
 * shell command that copies ClientAppSettings.json from our local
 * Modifications dir into Roblox's private /data/data/.../files/ClientSettings/
 * directory — which Android's scoped storage would otherwise prevent us from
 * touching on Android 10+.
 *
 * Requirements:
 *   - Shizuku manager app installed (com.roblox.client… wait, moe.shizuku.privileged.api)
 *   - Shizuku service started (via wireless debugging or ADB or root)
 *   - User has granted our app the Shizuku permission (managed by ShizukuManager)
 *
 * On a clean install, this strategy reports isAvailable = false until the user
 * has set up Shizuku + granted permission. The UI walks them through it.
 */
object ShizukuInjectionStrategy : InjectionStrategy {

    override val id: String = "shizuku"

    /**
     * True only when we can actually use Shizuku right now — binder is alive
     * AND the user has granted our app permission.
     */
    override suspend fun isAvailable(context: Context): Boolean {
        return try {
            ShizukuManager.isReady()
        } catch (e: Throwable) {
            Logger.writeException("ShizukuInjection::isAvailable", e)
            false
        }
    }

    /**
     * Copy ClientAppSettings.json into Roblox's data dir using a Shizuku
     * privileged shell command. We:
     *   1. Resolve the live Roblox data dir via `pm path` (survives Roblox updates)
     *   2. mkdir -p the ClientSettings dir (under Roblox's files/)
     *   3. cat our local JSON into the target path
     *   4. chown to Roblox's uid:gid so Roblox can read it
     *   5. chmod 660 so Roblox's engine can read but not world-readable
     */
    override suspend fun apply(context: Context, robloxPackage: String): Result<Unit> {
        val src = AppPaths.clientAppSettingsFile
        if (!src.exists()) {
            return Result.failure(IllegalStateException("No ClientAppSettings.json to inject"))
        }
        if (!ShizukuManager.isReady()) {
            return Result.failure(IllegalStateException("Shizuku not ready — start Shizuku and grant permission"))
        }
        return try {
            val srcPath = src.absolutePath
            val dataDir = "/data/data/$robloxPackage"
            val targetDir = "$dataDir/files/ClientSettings"
            val targetFile = "$targetDir/ClientAppSettings.json"

            // Single shell script that does everything atomically.
            // Uses `stat` to resolve Roblox's uid/gid so we can chown the file
            // to match — Roblox's engine won't read a file owned by another uid.
            //
            // Note: ${'$'}OWNER is the Kotlin way to emit a literal $OWNER in a
            // multiline string — without it, Kotlin would try to interpolate
            // OWNER as a Kotlin variable.
            val script = """
                set -e
                mkdir -p "$targetDir"
                cat "$srcPath" > "$targetFile"
                OWNER=${'$'}(stat -c '%u:%g' "$dataDir")
                chown "${'$'}OWNER" "$targetFile" "$targetDir"
                chmod 660 "$targetFile"
                chmod 770 "$targetDir"
                echo "INJECTED_OK"
            """.trimIndent()

            val result = ShizukuManager.runShellCommand(script, timeoutMs = 15_000L)
                ?: return Result.failure(IllegalStateException("Shizuku not ready"))

            if (result.success && result.stdout.contains("INJECTED_OK")) {
                Logger.writeLine("ShizukuInjection::apply", "Injected $targetFile via Shizuku binder")
                Result.success(Unit)
            } else {
                Logger.writeLine(
                    "ShizukuInjection::apply",
                    "Shizuku command failed: exit=${result.exitCode} stdout=${result.stdout} stderr=${result.stderr}"
                )
                Result.failure(RuntimeException("Shizuku exit ${result.exitCode}: ${result.stderr.ifBlank { result.stdout }}"))
            }
        } catch (e: Exception) {
            Logger.writeException("ShizukuInjection::apply", e)
            Result.failure(e)
        }
    }
}
