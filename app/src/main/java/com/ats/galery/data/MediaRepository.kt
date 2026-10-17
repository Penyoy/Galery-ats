package com.ats.galery.data

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Membaca seluruh foto & video dari MediaStore. Tidak butuh jaringan.
 * Hasil di-cache di memori dan dimuat ulang saat diminta (mis. onResume).
 */
class MediaRepository(private val context: Context) {

    suspend fun loadAll(): List<MediaItem> = withContext(Dispatchers.IO) {
        val images = query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, isVideo = false)
        val videos = query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, isVideo = true)
        (images + videos).sortedByDescending { it.dateMillis }
    }

    private fun query(collection: Uri, isVideo: Boolean): List<MediaItem> {
        val projection = buildList {
            add(MediaStore.MediaColumns._ID)
            add(MediaStore.MediaColumns.DISPLAY_NAME)
            add(MediaStore.MediaColumns.SIZE)
            add(MediaStore.MediaColumns.DATE_ADDED)
            add(MediaStore.MediaColumns.DATE_TAKEN)
            add(MediaStore.MediaColumns.WIDTH)
            add(MediaStore.MediaColumns.HEIGHT)
            add(MediaStore.MediaColumns.BUCKET_ID)
            add(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
            if (isVideo) add(MediaStore.Video.VideoColumns.DURATION)
        }.toTypedArray()

        val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            "${MediaStore.MediaColumns.IS_TRASHED} = 0"
        } else {
            null
        }

        val result = ArrayList<MediaItem>()
        context.contentResolver.query(
            collection,
            projection,
            selection,
            null,
            "${MediaStore.MediaColumns.DATE_ADDED} DESC"
        )?.use { cursor: Cursor ->
            val idIx = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameIx = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
            val sizeIx = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
            val addedIx = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED)
            val takenIx = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_TAKEN)
            val widthIx = cursor.getColumnIndex(MediaStore.MediaColumns.WIDTH)
            val heightIx = cursor.getColumnIndex(MediaStore.MediaColumns.HEIGHT)
            val bucketIdIx = cursor.getColumnIndex(MediaStore.MediaColumns.BUCKET_ID)
            val bucketNameIx = cursor.getColumnIndex(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
            val durationIx = if (isVideo) cursor.getColumnIndex(MediaStore.Video.VideoColumns.DURATION) else -1

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIx)
                val taken = if (takenIx >= 0) cursor.getLong(takenIx) else 0L
                val added = if (addedIx >= 0) cursor.getLong(addedIx) * 1000L else 0L
                result += MediaItem(
                    id = id,
                    uri = ContentUris.withAppendedId(collection, id).toString(),
                    name = if (nameIx >= 0) cursor.getString(nameIx) ?: "media" else "media",
                    albumId = if (bucketIdIx >= 0) cursor.getLong(bucketIdIx) else 0L,
                    album = (if (bucketNameIx >= 0) cursor.getString(bucketNameIx) else null) ?: "Lainnya",
                    isVideo = isVideo,
                    durationMs = if (durationIx >= 0) cursor.getLong(durationIx) else 0L,
                    dateMillis = if (taken > 0L) taken else added,
                    sizeBytes = if (sizeIx >= 0) cursor.getLong(sizeIx) else 0L,
                    width = if (widthIx >= 0) cursor.getInt(widthIx) else 0,
                    height = if (heightIx >= 0) cursor.getInt(heightIx) else 0,
                )
            }
        }
        return result
    }
}
