package com.appropriate.riskauditor.core.platform

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.collection.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * High-performance, memory-capped icon loader with an LRU cache of 150 entries.
 * Never performs binder calls or bitmap scaling on the main thread.
 * Never stores icons in SQLite database (AGENTS.md rule).
 */
class AndroidIconLoader(context: Context) {

    private val packageManager: PackageManager = context.packageManager

    // Cache up to 150 bitmaps in memory (96x96 x 4 bytes ~= 36KB each, total cache < 6MB)
    private val iconCache = LruCache<String, Bitmap>(150)

    suspend fun load(packageName: String): Bitmap? = withContext(Dispatchers.IO) {
        iconCache.get(packageName)?.let { return@withContext it }

        try {
            val drawable = packageManager.getApplicationIcon(packageName)
            val bitmap = drawableToBitmap(drawable, 96, 96)
            iconCache.put(packageName, bitmap)
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    private fun drawableToBitmap(drawable: Drawable, width: Int, height: Int): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return Bitmap.createScaledBitmap(drawable.bitmap, width, height, true)
        }

        val bitmap = Bitmap.createBitmap(
            if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else width,
            if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }
}
