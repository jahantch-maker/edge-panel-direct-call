package com.jahanzaib.edgedirectcall

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.provider.ContactsContract
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import java.util.UUID

class MainActivity : AppCompatActivity(), ContactAdapter.Listener {

    private lateinit var recycler: RecyclerView
    private lateinit var layoutManager: GridLayoutManager
    private val adapter = ContactAdapter(this)
    private var contacts = mutableListOf<Contact>()
    private var editMode = false

    private lateinit var count: TextView
    private lateinit var subtitle: TextView
    private lateinit var empty: View
    private lateinit var banner: View
    private lateinit var btnEdit: ImageView

    // State of the add/edit dialog while the contact picker is open on top of it
    private var dlgName: EditText? = null
    private var dlgNumber: EditText? = null
    private var dlgPhoto: Bitmap? = null
    private var dlgRefreshPreview: (() -> Unit)? = null

    private val pickContact =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
            if (res.resultCode == Activity.RESULT_OK) res.data?.data?.let { handlePicked(it) }
        }

    private val askCall =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { updateBanner() }

    private val askNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) Store.setLockShortcut(this, false)
            LockShortcut.sync(this)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        count = findViewById(R.id.count)
        subtitle = findViewById(R.id.subtitle)
        empty = findViewById(R.id.empty)
        banner = findViewById(R.id.banner)
        btnEdit = findViewById(R.id.btn_edit)
        recycler = findViewById(R.id.recycler)

        layoutManager = GridLayoutManager(this, Store.columns(this))
        recycler.layoutManager = layoutManager
        recycler.adapter = adapter
        recycler.itemAnimator?.changeDuration = 0

        val touch = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT, 0
        ) {
            override fun isLongPressDragEnabled() = editMode

            override fun getMovementFlags(rv: RecyclerView, vh: RecyclerView.ViewHolder): Int =
                if (vh.itemViewType == ContactAdapter.TYPE_ADD) 0 else super.getMovementFlags(rv, vh)

            override fun onMove(
                rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder
            ): Boolean {
                if (target.itemViewType == ContactAdapter.TYPE_ADD) return false
                adapter.move(vh.bindingAdapterPosition, target.bindingAdapterPosition)
                return true
            }

            override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {}

            override fun onSelectedChanged(vh: RecyclerView.ViewHolder?, state: Int) {
                super.onSelectedChanged(vh, state)
                if (state == ItemTouchHelper.ACTION_STATE_DRAG) {
                    vh?.itemView?.animate()?.scaleX(1.07f)?.scaleY(1.07f)?.setDuration(120)?.start()
                }
            }

            override fun clearView(rv: RecyclerView, vh: RecyclerView.ViewHolder) {
                super.clearView(rv, vh)
                vh.itemView.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                persist()
            }
        })
        touch.attachToRecyclerView(recycler)

        btnEdit.setOnClickListener { setEditMode(!editMode) }
        findViewById<View>(R.id.btn_settings).setOnClickListener { showSettings() }
        findViewById<View>(R.id.btn_allow).setOnClickListener { askCall.launch(Manifest.permission.CALL_PHONE) }

        if (!Calls.hasPermission(this)) askCall.launch(Manifest.permission.CALL_PHONE)
    }

    override fun onResume() {
        super.onResume()
        layoutManager.spanCount = Store.columns(this)
        refresh()
        updateBanner()
        LockShortcut.sync(this)
    }

    private fun refresh() {
        contacts = Store.load(this)
        adapter.submit(contacts, editMode, true)
        count.text = getString(R.string.count_fmt, contacts.size, Store.MAX)
        empty.visibility = if (contacts.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun persist() {
        Store.save(this, contacts)
        EdgePanel.refresh(this)
        count.text = getString(R.string.count_fmt, contacts.size, Store.MAX)
        empty.visibility = if (contacts.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun updateBanner() {
        banner.visibility = if (Calls.hasPermission(this)) View.GONE else View.VISIBLE
    }

    private fun setEditMode(on: Boolean) {
        editMode = on
        btnEdit.setImageResource(if (on) R.drawable.ic_check else R.drawable.ic_edit)
        subtitle.setText(if (on) R.string.subtitle_edit else R.string.subtitle_normal)
        adapter.submit(contacts, editMode, true)
    }

    // ---- ContactAdapter.Listener -------------------------------------------------------------

    override fun onContactClick(c: Contact) {
        if (editMode) {
            showContactDialog(c)
        } else if (!Calls.place(this, c.number)) {
            Toast.makeText(this, R.string.need_call_permission, Toast.LENGTH_LONG).show()
            askCall.launch(Manifest.permission.CALL_PHONE)
        }
    }

    override fun onAddClick() {
        if (contacts.size >= Store.MAX) {
            Toast.makeText(this, R.string.limit_reached, Toast.LENGTH_SHORT).show()
        } else {
            showContactDialog(null)
        }
    }

    // ---- Add / edit dialog -------------------------------------------------------------------

    private fun showContactDialog(existing: Contact?) {
        val v = layoutInflater.inflate(R.layout.dialog_contact, null)
        val preview = v.findViewById<ImageView>(R.id.preview)
        val name = v.findViewById<EditText>(R.id.input_name)
        val number = v.findViewById<EditText>(R.id.input_number)
        val colorIndex = existing?.colorIndex ?: ((contacts.size * 3 + 1) % Avatar.paletteSize)
        val id = existing?.id ?: UUID.randomUUID().toString()
        val previewPx = (80 * resources.displayMetrics.density).toInt()

        name.setText(existing?.name ?: "")
        number.setText(existing?.number ?: "")
        dlgPhoto = null
        dlgName = name
        dlgNumber = number

        val redraw = {
            val tmp = Contact(id, name.text.toString().ifBlank { "?" }, "", colorIndex)
            preview.setImageBitmap(Avatar.render(this, tmp, previewPx, dlgPhoto))
        }
        dlgRefreshPreview = redraw
        redraw()
        name.doAfterTextChanged { redraw() }

        v.findViewById<View>(R.id.btn_pick).setOnClickListener {
            pickContact.launch(Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI))
        }

        val builder = MaterialAlertDialogBuilder(this)
            .setTitle(if (existing == null) R.string.add_contact else R.string.edit_contact)
            .setView(v)
            .setPositiveButton(R.string.save, null)
            .setNegativeButton(R.string.cancel, null)
        if (existing != null) builder.setNeutralButton(R.string.delete, null)
        val dialog = builder.create()
        dialog.setOnDismissListener {
            dlgName = null
            dlgNumber = null
            dlgPhoto = null
            dlgRefreshPreview = null
        }
        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val n = name.text.toString().trim()
            val num = Store.cleanNumber(number.text.toString())
            if (n.isEmpty()) {
                name.error = getString(R.string.err_name); return@setOnClickListener
            }
            if (num.isEmpty()) {
                number.error = getString(R.string.err_number); return@setOnClickListener
            }
            val updated = Contact(id, n, num, colorIndex)
            if (existing == null) {
                if (contacts.size >= Store.MAX) {
                    Toast.makeText(this, R.string.limit_reached, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                contacts.add(updated)
            } else {
                val idx = contacts.indexOfFirst { it.id == id }
                if (idx >= 0) contacts[idx] = updated
            }
            dlgPhoto?.let { Photos.save(this, id, it) }
            persist()
            adapter.submit(contacts, editMode, true)
            dialog.dismiss()
        }
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_title)
                .setMessage(getString(R.string.delete_msg, existing?.name ?: ""))
                .setPositiveButton(R.string.delete) { _, _ ->
                    contacts.removeAll { it.id == id }
                    Photos.delete(this, id)
                    persist()
                    adapter.submit(contacts, editMode, true)
                    dialog.dismiss()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    private fun handlePicked(uri: android.net.Uri) {
        contentResolver.query(uri, null, null, null, null)?.use { cur ->
            if (!cur.moveToFirst()) return
            val nameIdx = cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numIdx = cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val photoIdx = cur.getColumnIndex("photo_uri").takeIf { it >= 0 } ?: cur.getColumnIndex("photo_thumb_uri")
            val n = if (nameIdx >= 0) cur.getString(nameIdx) else null
            val num = if (numIdx >= 0) cur.getString(numIdx) else null
            val photo = if (photoIdx >= 0) cur.getString(photoIdx) else null
            if (!n.isNullOrBlank()) dlgName?.setText(n)
            if (!num.isNullOrBlank()) dlgNumber?.setText(num)
            dlgPhoto = photo?.let { Photos.loadSquare(this, android.net.Uri.parse(it), 256) }
            dlgRefreshPreview?.invoke()
        }
    }

    // ---- Settings ----------------------------------------------------------------------------

    private fun showSettings() {
        val v = layoutInflater.inflate(R.layout.dialog_settings, null)
        val group = v.findViewById<MaterialButtonToggleGroup>(R.id.cols_group)
        group.check(if (Store.columns(this) == 2) R.id.btn_col2 else R.id.btn_col3)
        group.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            Store.setColumns(this, if (checkedId == R.id.btn_col2) 2 else 3)
            layoutManager.spanCount = Store.columns(this)
            EdgePanel.refresh(this)
        }

        val sw = v.findViewById<MaterialSwitch>(R.id.switch_lock)
        sw.isChecked = Store.lockShortcut(this)
        sw.setOnCheckedChangeListener { _, on ->
            Store.setLockShortcut(this, on)
            if (on && Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                LockShortcut.sync(this)
            }
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.settings)
            .setView(v)
            .setPositiveButton(R.string.done, null)
            .show()
    }
}
