package com.jahanzaib.edgedirectcall

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/** Grid of contact tiles, plus a trailing "Add" tile while there is room (max 30). */
class ContactAdapter(private val listener: Listener) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface Listener {
        fun onContactClick(c: Contact)
        fun onAddClick()
    }

    var items: MutableList<Contact> = mutableListOf()
    var editMode = false
    var showAdd = true

    fun submit(list: MutableList<Contact>, edit: Boolean, add: Boolean) {
        items = list
        editMode = edit
        showAdd = add
        notifyDataSetChanged()
    }

    fun move(from: Int, to: Int) {
        if (from !in items.indices || to !in items.indices) return
        val m = items.removeAt(from)
        items.add(to, m)
        notifyItemMoved(from, to)
    }

    override fun getItemCount(): Int = items.size + if (showAdd && items.size < Store.MAX) 1 else 0

    override fun getItemViewType(position: Int): Int = if (position < items.size) TYPE_CONTACT else TYPE_ADD

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inf = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_CONTACT) {
            ContactHolder(inf.inflate(R.layout.item_contact, parent, false))
        } else {
            AddHolder(inf.inflate(R.layout.item_add, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ContactHolder) {
            val c = items[position]
            val ctx = holder.itemView.context
            val px = (72 * ctx.resources.displayMetrics.density).toInt()
            holder.avatar.setImageBitmap(Avatar.render(ctx, c, px))
            holder.name.text = c.name
            holder.number.text = c.number
            holder.badge.visibility = if (editMode) View.VISIBLE else View.GONE
            holder.itemView.setOnClickListener {
                val p = holder.bindingAdapterPosition
                if (p in items.indices) listener.onContactClick(items[p])
            }
        } else {
            holder.itemView.setOnClickListener { listener.onAddClick() }
        }
    }

    private class ContactHolder(v: View) : RecyclerView.ViewHolder(v) {
        val avatar: ImageView = v.findViewById(R.id.avatar)
        val name: TextView = v.findViewById(R.id.name)
        val number: TextView = v.findViewById(R.id.number)
        val badge: ImageView = v.findViewById(R.id.edit_badge)
    }

    private class AddHolder(v: View) : RecyclerView.ViewHolder(v)

    companion object {
        const val TYPE_CONTACT = 0
        const val TYPE_ADD = 1
    }
}
