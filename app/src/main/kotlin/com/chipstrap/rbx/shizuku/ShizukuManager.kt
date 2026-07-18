package com.chipstrap.rbx.shizuku

import android.content.Context
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.content.ContextCompat
import com.chipstrap.rbx.core.Logger
import moe.shizuku.server.IRemoteProcess
import moe.shizuku.server.IShizukuService
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader

/**
 * Wraps the Shizuku API so the rest of the app doesn't have to deal with
 * its v11/pre-v11 quirks. All calls are defensive — if Shizuku isn't installed,
 * isn't running, or the user hasn't granted permission, every method returns
 * a safe default instead of throwing.
 *
 * Lifecycle:
 *   1. App starts → [ShizukuManager.init] registers binder + permission listeners.
 *   2. User grants permission → [onGranted] callback fires.
 *   3. App calls [ShizukuManager.runShellCommand] to execute privileged commands.
 */
object ShizukuManager {

    private const val TAG = "Chipstrap.Shizuku"
    private const val SHIZUKU_PERMISSION_REQUEST_CODE = 0xC001

    /** Whether the Shizuku manager app is installed on the device. */
    fun isShizukuInstalled(context: Context): Boolean = try {
        context.packageManager.getPackageInfo("moe.shizuku.privileged.api", 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    } catch (_: Throwable) {
        false
    }

    /**
     * True only when Shizuku is running AND the user has granted our app
     * permission to use its binder. This is the precondition for actually
     * running privileged commands.
     */
    fun isReady(): Boolean = try {
        Shizuku.pingBinder() && hasPermission()
    } catch (_: Throwable) {
        false
    }

    /** True if the binder is alive (Shizuku service is running). */
    fun isBinderAlive(): Boolean = try {
        Shizuku.pingBinder()
    } catch (_: Throwable) {
        false
    }

    /** True if the user has granted our app the Shizuku permission. */
    fun hasPermission(): Boolean = try {
        if (Shizuku.isPreV11()) {
            // Pre-v11: check the legacy permission
            ContextCompat.checkSelfPermission(
                appContext,
                "moe.shizuku.manager.permission.API_V23"
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }
    } catch (_: Throwable) {
        false
    }

    /** True if we should show a rationale before requesting permission. */
    fun shouldShowRationale(): Boolean = try {
        !Shizuku.isPreV11() && Shizuku.shouldShowRequestPermissionRationale()
    } catch (_: Throwable) {
        false
    }

    /**
     * Request permission from the user. Safe to call multiple times — if
     * permission is already granted, this is a no-op. If the binder isn't
     * alive, the request will be queued and shown once Shizuku starts.
     *
     * Returns true if permission was already granted (no request needed).
     */
    fun requestPermission(): Boolean {
        return try {
            if (Shizuku.isPreV11()) {
                // Pre-v11 can't programmatically request — user has to grant
                // manually in Shizuku's UI. Return false to indicate "not granted".
                hasPermission()
            } else {
                if (hasPermission()) {
                    true
                } else {
                    Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE)
                    false
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "requestPermission failed", e)
            false
        }
    }

    /**
     * Initialize Shizuku listeners. Call this from MainActivity.onCreate.
     * The [onGranted] callback fires when:
     *   - The binder is received (Shizuku started), AND
     *   - The user has granted permission
     *
     * The [onLost] callback fires when the binder is lost (Shizuku stopped).
     *
     * Returns a [ShizukuListeners] handle that the caller can use to remove
     * the listeners in onDestroy.
     */
    fun init(onGranted: () -> Unit, onLost: () -> Unit): ShizukuListeners {
        val binderListener = Shizuku.OnBinderReceivedListener {
            Log.d(TAG, "Shizuku binder received")
            if (hasPermission()) {
                onGranted()
            }
        }
        val permissionListener = object : Shizuku.OnRequestPermissionResultListener {
            override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
                Log.d(TAG, "Shizuku permission result: requestCode=$requestCode grantResult=$grantResult")
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    onGranted()
                }
            }
        }
        try {
            Shizuku.addBinderReceivedListenerSticky(binderListener)
            Shizuku.addRequestPermissionResultListener(permissionListener)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to register Shizuku listeners", e)
        }
        return ShizukuListeners(binderListener, permissionListener)
    }

    /** Remove listeners registered in [init]. */
    fun cleanup(listeners: ShizukuListeners) {
        try { Shizuku.removeBinderReceivedListener(listeners.binderListener) } catch (_: Throwable) {}
        try { Shizuku.removeRequestPermissionResultListener(listeners.permissionListener) } catch (_: Throwable) {}
    }

    /**
     * Run a shell command as the Shizuku user (uid=2000 by default, or root
     * if Shizuku was started via root). Returns the command's stdout + stderr
     * and exit code. Returns null if Shizuku isn't ready.
     *
     * The command runs via `sh -c <script>` so pipes, redirects, && etc. all work.
     *
     * Implementation: we fetch the raw IBinder from [Shizuku.getBinder], wrap it
     * as an [IShizukuService] (the AIDL interface), and call newProcess() on it.
     * The returned [IRemoteProcess] gives us ParcelFileDescriptors for stdin /
     * stdout / stderr that we wrap in our own [RemoteProcessWrapper] (extends
     * java.lang.Process) so we can use the standard inputStream / errorStream /
     * waitFor / exitValue API.
     *
     * We can't use ShizukuRemoteProcess directly because its constructor is
     * package-private.
     */
    fun runShellCommand(script: String, timeoutMs: Long = 10_000L): ShellResult? {
        if (!isReady()) {
            Logger.writeLine(TAG, "runShellCommand: Shizuku not ready, skipping")
            return null
        }
        return try {
            val binder: IBinder = Shizuku.getBinder()
                ?: return ShellResult(-1, "", "Shizuku binder is null")
            val service = IShizukuService.Stub.asInterface(binder)
                ?: return ShellResult(-1, "", "Failed to get IShizukuService")

            // newProcess(cmd, env, dir) — env=null means inherit, dir=null means cwd
            val remoteProcess: IRemoteProcess = service.newProcess(
                arrayOf("sh", "-c", script),
                null,
                null
            )
            val process = RemoteProcessWrapper(remoteProcess)

            // Read stdout + stderr in parallel (they're separate FDs that can
            // deadlock if we read sequentially and the pipe fills up).
            val stdoutBuilder = StringBuilder()
            val stderrBuilder = StringBuilder()
            val stdoutThread = Thread {
                runCatching {
                    BufferedReader(InputStreamReader(process.inputStream)).useLines { lines ->
                        lines.forEach { stdoutBuilder.append(it).append('\n') }
                    }
                }
            }
            val stderrThread = Thread {
                runCatching {
                    BufferedReader(InputStreamReader(process.errorStream)).useLines { lines ->
                        lines.forEach { stderrBuilder.append(it).append('\n') }
                    }
                }
            }
            stdoutThread.start()
            stderrThread.start()

            // Wait for the process with timeout
            val waitThread = Thread { runCatching { process.waitFor() } }
            waitThread.start()
            waitThread.join(timeoutMs)
            val exitCode = if (waitThread.isAlive) {
                waitThread.interrupt()
                runCatching { process.destroy() }
                -1
            } else {
                runCatching { process.exitValue() }.getOrDefault(-1)
            }

            // Make sure we've drained the streams
            stdoutThread.join(1000)
            stderrThread.join(1000)

            ShellResult(exitCode, stdoutBuilder.toString(), stderrBuilder.toString())
        } catch (e: Throwable) {
            Logger.writeException(TAG, e)
            ShellResult(-1, "", e.message ?: e.javaClass.simpleName)
        }
    }

    /**
     * Minimal java.lang.Process wrapper around an [IRemoteProcess]. We can't
     * use ShizukuRemoteProcess directly because its constructor is
     * package-private, so we read the ParcelFileDescriptors ourselves and
     * expose them as standard InputStream / OutputStream.
     */
    private class RemoteProcessWrapper(private val remote: IRemoteProcess) : Process() {
        override fun getOutputStream() = FileOutputStream(remote.outputStream.fileDescriptor)
        override fun getInputStream() = FileInputStream(remote.inputStream.fileDescriptor)
        override fun getErrorStream() = FileInputStream(remote.errorStream.fileDescriptor)
        override fun waitFor(): Int = remote.waitFor()
        override fun exitValue(): Int = remote.exitValue()
        override fun destroy() { runCatching { remote.destroy() } }
    }

    data class ShellResult(val exitCode: Int, val stdout: String, val stderr: String) {
        val success: Boolean get() = exitCode == 0
    }

    /** Listener handles returned by [init], passed back to [cleanup]. */
    class ShizukuListeners(
        val binderListener: Shizuku.OnBinderReceivedListener,
        val permissionListener: Shizuku.OnRequestPermissionResultListener
    )

    /**
     * App context used for permission checks. Set from ChipstrapApp.onCreate.
     */
    @Volatile
    private lateinit var appContext: Context

    fun initContext(context: Context) {
        appContext = context.applicationContext
    }
}
