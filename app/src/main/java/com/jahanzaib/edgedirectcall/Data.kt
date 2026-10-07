package com.jahanzaib.edgedirectcall

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

data class Contact(
    val id: String,
    val name: String,
    val number: String,
    val colorIndex: Int
)

/** Contacts + settings, stored in SharedPreferences as a small JSON list. */
object Store {
    const val MAX = 30

    private fun sp(c: Context) =
        c.applicationContext.getSharedPreferences("direct_call", Context.MODE_PRIVATE)

    fun load(c: Context): MutableList<Contact> {
        val out = mutableListOf<Contact>()
        try {
            val arr = JSONArray(sp(c).getString("contacts", "[]"))
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                out.add(
                    Contact(
                        o.getString("id"),
                        o.getString("name"),
                        o.getString("number"),
                        o.optInt("color", 0)
                    )
                )
            }
        } catch (_: Exception) {
        }
        return out
    }

    fun save(c: Context, list: List<Contact>) {
        val arr = JSONArray()
        for (x in list.take(MAX)) {
            arr.put(
                JSONObject()
                    .put("id", x.id)
                    .put("name", x.name)
                    .put("number", x.number)
                    .put("color", x.colorIndex)
            )
        }
        sp(c).edit().putString("contacts", arr.toString()).apply()
    }

    fun columns(c: Context): Int = sp(c).getInt("columns", 3).coerceIn(2, 3)
    fun setColumns(c: Context, v: Int) = sp(c).edit().putInt("columns", v.coerceIn(2, 3)).apply()

    fun lockShortcut(c: Context): Boolean = sp(c).getBoolean("lock_shortcut", false)
    fun setLockShortcut(c: Context, v: Boolean) =
        sp(c).edit().putBoolean("lock_shortcut", v).apply()

    /** Keep only characters a dialer understands. */
    fun cleanNumber(raw: String): String = raw.filter { it.isDigit() || it == '+' || it == '*' || it == '#' }
}

/** Small square contact photos copied into app storage (no READ_CONTACTS permission needed). */
object Photos {
    private fun dir(c: Context): File = File(c.filesDir, "photos").apply { mkdirs() }
    fun file(c: Context, id: String): File = File(dir(c), "$id.jpg")

    fun loadSquare(c: Context, uri: Uri, size: Int): Bitmap? = try {
        c.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            ?.let { centerCrop(it, size) }
    } catch (_: Exception) {
        null
    }

    fun save(c: Context, id: String, bmp: Bitmap) {
        try {
            FileOutputStream(file(c, id)).use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        } catch (_: Exception) {
        }
    }

    fun delete(c: Context, id: String) {
        file(c, id).delete()
    }

    private fun centerCrop(src: Bitmap, size: Int): Bitmap {
        val side = minOf(src.width, src.height)
        val x = (src.width - side) / 2
        val y = (src.height - side) / 2
        val square = Bitmap.createBitmap(src, x, y, side, side)
        return Bitmap.createScaledBitmap(square, size, size, true)
    }
}
