package com.jahanzaib.edgedirectcall

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.BitmapShader
import android.graphics.Typeface

/** Draws the round contact picture: photo if we have one, otherwise a gradient with initials. */
object Avatar {
    private val gradients = arrayOf(
        intArrayOf(0xFF7C6CFF.toInt(), 0xFFFF6AA2.toInt()),
        intArrayOf(0xFF00C6FF.toInt(), 0xFF3B6BFF.toInt()),
        intArrayOf(0xFF11C9A0.toInt(), 0xFF38EF7D.toInt()),
        intArrayOf(0xFFFF8008.toInt(), 0xFFFFC837.toInt()),
        intArrayOf(0xFFFC466B.toInt(), 0xFF7B4DFF.toInt()),
        intArrayOf(0xFFF857A6.toInt(), 0xFFFF7A59.toInt()),
        intArrayOf(0xFF2DD4BF.toInt(), 0xFF3B82F6.toInt()),
        intArrayOf(0xFFA770EF.toInt(), 0xFFE58BF3.toInt())
    )

    val paletteSize get() = gradients.size

    fun initials(name: String): String {
        val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val s = when (parts.size) {
            0 -> "#"
            1 -> parts[0].take(1)
            else -> parts[0].take(1) + parts[1].take(1)
        }
        return s.uppercase()
    }

    fun render(ctx: Context, c: Contact, size: Int, photoOverride: Bitmap? = null): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val cv = Canvas(bmp)
        val r = size / 2f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val photo = photoOverride ?: Photos.file(ctx, c.id).takeIf { it.exists() }
            ?.let { BitmapFactory.decodeFile(it.path) }

        if (photo != null) {
            val scaled = Bitmap.createScaledBitmap(photo, size, size, true)
            paint.shader = BitmapShader(scaled, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            cv.drawCircle(r, r, r, paint)
        } else {
            val g = gradients[Math.floorMod(c.colorIndex, gradients.size)]
            paint.shader = LinearGradient(0f, 0f, size.toFloat(), size.toFloat(), g[0], g[1], Shader.TileMode.CLAMP)
            cv.drawCircle(r, r, r, paint)
            val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = size * 0.38f
                textAlign = Paint.Align.CENTER
            }
            val y = r - (text.descent() + text.ascent()) / 2f
            cv.drawText(initials(c.name), r, y, text)
        }
        return bmp
    }
}
