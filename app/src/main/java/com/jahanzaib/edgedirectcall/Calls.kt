package com.jahanzaib.edgedirectcall

import android.Manifest
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle

object Calls {
    fun hasPermission(ctx: Context) =
        ctx.checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED

    /** Places the call straight away. Returns false when the phone permission is missing. */
    fun place(ctx: Context, number: String): Boolean {
        if (!hasPermission(ctx)) return false
        val i = Intent(Intent.ACTION_CALL, Uri.fromParts("tel", number, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(i)
        return true
    }
}

/**
 * Invisible activity the Edge panel taps open. It is allowed to appear over the lock screen,
 * places the call and closes itself, so the phone never needs to be unlocked.
 */
class CallActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        val number = intent.getStringExtra(EXTRA_NUMBER)
        if (!number.isNullOrBlank()) {
            if (!Calls.place(this, number)) {
                startActivity(
                    Intent(this, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                )
            }
        }
        finish()
        overridePendingTransition(0, 0)
    }

    companion object {
        const val EXTRA_NUMBER = "number"
        const val EXTRA_NAME = "name"
    }
}

/** Optional ongoing notification that opens Quick Call from the lock screen without unlocking. */
object LockShortcut {
    private const val CHANNEL = "quick_call"
    private const val ID = 1

    fun sync(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        val allowed = Build.VERSION.SDK_INT < 33 ||
                ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!Store.lockShortcut(ctx) || !allowed) {
            nm.cancel(ID)
            return
        }
        val channel = NotificationChannel(
            CHANNEL, ctx.getString(R.string.notif_channel), NotificationManager.IMPORTANCE_LOW
        ).apply {
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setShowBadge(false)
        }
        nm.createNotificationChannel(channel)
        val pi = PendingIntent.getActivity(
            ctx, 1,
            Intent(ctx, QuickCallActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = Notification.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_phone)
            .setContentTitle(ctx.getString(R.string.app_name))
            .setContentText(ctx.getString(R.string.notif_text))
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pi)
            .build()
        nm.notify(ID, n)
    }
}

/** Brings the lock-screen notification back after a reboot or app update. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        LockShortcut.sync(context)
    }
}
