package com.ats.galery

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import com.ats.galery.pip.LocalIsInPip
import com.ats.galery.pip.Pip
import com.ats.galery.pip.PipActionSpec
import com.ats.galery.pip.PipActions
import com.ats.galery.pip.PipHost
import com.ats.galery.ui.AppRoot
import kotlin.math.roundToInt

/**
 * Satu-satunya Activity. Mendukung Picture-in-Picture untuk video maupun gambar
 * dan menjadi jembatan antara UI Compose dan PIP (remote actions).
 */
class MainActivity : ComponentActivity(), PipHost {

    private val inPip = mutableStateOf(false)

    /** Dipanggil saat pengguna menekan Home; viewer mengisinya bila auto-PIP aktif. */
    var onLeaveHint: (() -> Unit)? = null

    private var cachedActions: List<PipActionSpec> = emptyList()
    private var cachedRatio: Float? = null

    private val actionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val id = intent?.getStringExtra(PipActions.EXTRA) ?: return
            Pip.delegate?.onPipAction(id)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        Pip.host = this
        ContextCompat.registerReceiver(
            this,
            actionReceiver,
            IntentFilter(PipActions.ACTION),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        setContent {
            CompositionLocalProvider(LocalIsInPip provides inPip.value) {
                AppRoot()
            }
        }
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(actionReceiver) }
        if (Pip.host === this) Pip.host = null
        super.onDestroy()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        onLeaveHint?.invoke()
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        inPip.value = isInPictureInPictureMode
    }

    // ---- PipHost -------------------------------------------------------------

    override fun setPipActions(actions: List<PipActionSpec>) {
        cachedActions = actions
        applyPipParams(enter = false)
    }

    override fun enterPip(aspectRatio: Float) {
        if (inPip.value) return
        cachedRatio = aspectRatio.coerceIn(0.42f, 2.39f)
        applyPipParams(enter = true)
    }

    private fun applyPipParams(enter: Boolean) {
        val params = PictureInPictureParams.Builder()
            .apply {
                cachedRatio?.let { r ->
                    setAspectRatio(Rational((r * 100f).roundToInt().coerceAtLeast(1), 100))
                }
                if (cachedActions.isNotEmpty()) {
                    setActions(cachedActions.map { spec -> spec.toRemoteAction(this@MainActivity) })
                }
            }
            .build()

        runCatching {
            if (enter) {
                enterPictureInPictureMode(params)
            } else if (inPip.value) {
                setPictureInPictureParams(params)
            }
        }
    }

    private fun PipActionSpec.toRemoteAction(context: Context): RemoteAction {
        val intent = Intent(PipActions.ACTION)
            .setPackage(packageName)
            .putExtra(PipActions.EXTRA, id)
        val pending = PendingIntent.getBroadcast(
            context,
            id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return RemoteAction(
            Icon.createWithResource(context, iconRes),
            id,
            id,
            pending
        )
    }
}
