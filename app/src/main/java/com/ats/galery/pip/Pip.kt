package com.ats.galery.pip

import androidx.compose.runtime.staticCompositionLocalOf

/** Aksi yang bisa dikendalikan dari jendela PIP. */
object PipActions {
    const val ACTION = "com.ats.galery.PIP_ACTION"
    const val EXTRA = "pip_action_id"
    const val TOGGLE = "toggle_play"
    const val PREV = "prev"
    const val NEXT = "next"
}

/** Satu tombol pada jendela PIP. */
data class PipActionSpec(
    val id: String,
    val iconRes: Int,
)

/** Dipanggil oleh MainActivity ketika tombol PIP ditekan. */
interface PipDelegate {
    fun onPipAction(id: String)
}

/** Implementasi oleh MainActivity: mengubah parameter & masuk mode PIP. */
interface PipHost {
    fun setPipActions(actions: List<PipActionSpec>)
    fun enterPip(aspectRatio: Float)
}

/** Jembatan sederhana antara Compose dan Activity. */
object Pip {
    var host: PipHost? = null
    var delegate: PipDelegate? = null
}

/** Apakah aplikasi sedang berada dalam mode PIP. */
val LocalIsInPip = staticCompositionLocalOf { false }
