package com.ats.galery.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/** Helper izin akses media untuk semua versi Android yang didukung. */
object MediaPermissions {

    /** Izin yang diminta saat runtime. */
    fun requested(): Array<String> = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
        )
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
        )
        else -> arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
        )
    }

    /**
     * Akses penuh, atau akses sebagian ("pilih foto tertentu") yang tetap
     * valid untuk menampilkan item yang diizinkan.
     */
    fun hasAccess(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val images = granted(context, Manifest.permission.READ_MEDIA_IMAGES)
            val video = granted(context, Manifest.permission.READ_MEDIA_VIDEO)
            if (images || video) return true
        } else {
            if (granted(context, Manifest.permission.READ_EXTERNAL_STORAGE)) return true
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (granted(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)) return true
        }
        return false
    }

    private fun granted(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
