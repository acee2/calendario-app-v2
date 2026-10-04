package com.calendario.app.util

import android.content.Context
import androidx.core.content.ContextCompat
import com.calendario.app.R

object ColorUtils {
    val names = listOf("Rosso","Arancione","Giallo","Verde","Blu","Viola","Rosa","Azzurro")

    fun resolve(c: Context, index: Int): Int {
        val arr = c.resources.obtainTypedArray(R.array.event_colors)
        val color = arr.getColor(index.coerceIn(0, 7), 0xFF3B82F6.toInt())
        arr.recycle()
        return color
    }

    fun dotDrawable(c: Context, index: Int): android.graphics.drawable.GradientDrawable {
        val d = android.graphics.drawable.GradientDrawable()
        d.shape = android.graphics.drawable.GradientDrawable.OVAL
        d.setColor(resolve(c, index))
        d.setSize(28, 28)
        return d
    }
}
