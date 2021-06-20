package com.flx_apps.digitaldetox

import android.app.NotificationManager
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import com.flx_apps.digitaldetox.prefs.Prefs_
import java.util.concurrent.TimeUnit


/**
 * Creation Date: 11/9/20
 * @author felix
 */
object DetoxUtil {
    const val DISPLAY_DALTONIZER_ENABLED = "accessibility_display_daltonizer_enabled"
    const val DISPLAY_DALTONIZER = "accessibility_display_daltonizer"

    const val ZEN_MODE = "zen_mode"
    const val ZEN_MODE_OFF = 0
    const val ZEN_MODE_IMPORTANT_INTERRUPTIONS = 1
    const val ZEN_MODE_NO_INTERRUPTIONS = 2

//    @JvmStatic
//    fun isAccessibilityServiceEnabled(
//        context: Context,
//        service: Class<out AccessibilityService?>
//    ): Boolean {
//        val am =
//            context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
//        val enabledServices =
//            am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
//        for (enabledService in enabledServices) {
//            val enabledServiceInfo: ServiceInfo = enabledService.resolveInfo.serviceInfo
//            if (enabledServiceInfo.packageName == context.packageName && enabledServiceInfo.name == service.name
//            ) return true
//        }
//        return false
//    }

    @JvmStatic
    fun setGrayscale(
        context: Context,
        grayscale: Boolean,
        forceSetting: Boolean = false
    ): Boolean {
        if (!MainActivity.hasWriteSecureSettingsPermission || (!forceSetting && !Prefs_(context).grayscaleEnabled().get())) return false

        val contentResolver = context.contentResolver
        val result1 = Settings.Secure.putInt(
            contentResolver,
            DISPLAY_DALTONIZER_ENABLED,
            if (grayscale) 1 else 0
        )
        val result2 = Settings.Secure.putInt(
            contentResolver,
            DISPLAY_DALTONIZER,
            if (grayscale) 0 else -1
        )
        return result1 && result2
    }

    @JvmStatic
    fun setZenMode(
        context: Context,
        enabled: Boolean
    ): Boolean {
        if (!Prefs_(context).zenModeDefaultEnabled().get()) return false

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            // Fallback for Android 5, probably not working properly though (Settings don't seem to be persisted)
            return Settings.Global.putInt(
                context.contentResolver,
                ZEN_MODE,
                if (enabled) ZEN_MODE_IMPORTANT_INTERRUPTIONS else ZEN_MODE_OFF
            )
        }

        val notificationManager: NotificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (notificationManager.isNotificationPolicyAccessGranted) {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//                var policy = NotificationManager.Policy(
//                    NotificationManager.Policy.PRIORITY_CATEGORY_CALLS,
//                    NotificationManager.Policy.PRIORITY_SENDERS_CONTACTS,
//                    NotificationManager.Policy.PRIORITY_SENDERS_CONTACTS,
//                    NotificationManager.Policy.SUPPRESSED_EFFECT_SCREEN_ON and NotificationManager.Policy.SUPPRESSED_EFFECT_SCREEN_OFF
//                )
//                notificationManager.notificationPolicy = policy
//            }
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                var zenPolicy = ZenPolicy.Builder()
//                    .hideAllVisualEffects()
//                    .allowAlarms(true)
//                    .allowMedia(true)
//                    .build()
//            }
            notificationManager.setInterruptionFilter(if (enabled) NotificationManager.INTERRUPTION_FILTER_PRIORITY else NotificationManager.INTERRUPTION_FILTER_ALL)
            return true
        }
        return false
    }

    @JvmStatic
    fun togglePause(
        context: Context,
        minutes: Int? = null
    ): Boolean {
        val now = System.currentTimeMillis()
        val prefs = Prefs_(context)
        var isPausing = (now < prefs.pauseUntil().get())
        if (!isPausing && now < prefs.nextPauseAllowedAt().get()) {
            // we are currently not in a pause, and a pause is not allowed right now either
            Toast.makeText(
                context,
                context.getString(
                    R.string.app_quickSettingsTile_noPause,
                    TimeUnit.MILLISECONDS.toMinutes(prefs.nextPauseAllowedAt().get() - now) + 1
                ),
                Toast.LENGTH_SHORT
            ).show()
            return false
        }

        isPausing = !isPausing // new pause state: inversion of "are we currently pausing?"

        val pauseMinutes = minutes ?: prefs.pauseDuration().get()
        prefs.edit().pauseUntil().put(if (isPausing) now + TimeUnit.MINUTES.toMillis(pauseMinutes.toLong()) else -1).apply()
        setGrayscale(context, !isPausing)
        setZenMode(context, !isPausing)
        if (isPausing) {
            // a pause was made, let's show a hint to the user
            Toast.makeText(
                context,
                context.getString(
                    R.string.app_quickSettingsTile_paused,
                    pauseMinutes
                ),
                Toast.LENGTH_SHORT
            ).show()
            prefs.edit().nextPauseAllowedAt()
                .put(prefs.pauseUntil().get() + TimeUnit.MINUTES.toMillis(prefs.timeBetweenPauses().get().toLong()))
                .apply()
        } else {
            // a pause was interrupted
            prefs.edit().nextPauseAllowedAt()
                .put(now + TimeUnit.MINUTES.toMillis(prefs.timeBetweenPauses().get().toLong()))
                .apply()
        }
        return isPausing
    }
}