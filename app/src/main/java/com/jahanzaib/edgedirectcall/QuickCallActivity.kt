package com.jahanzaib.edgedirectcall

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * Read-only call grid that is allowed to show over the lock screen.
 * Tap a person and the call starts immediately, no unlocking needed.
 * It also has its own launcher icon, so it can be put on the lock-screen shortcut or home screen.
 */
class QuickCallActivity : AppCompatActivity(), ContactAdapter.Listener {
    private val askCall = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setContentView(R.layout.activity_quick)

        val contacts = Store.load(this)
        findViewById<android.view.View>(R.id.btn_close).setOnClickListener { finish() }
        findViewById<TextView>(R.id.empty).visibility =
            if (contacts.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE

        val grid = ContactAdapter(this)
        grid.submit(contacts, edit = false, add = false)
        findViewById<RecyclerView>(R.id.recycler).apply {
            layoutManager = GridLayoutManager(this@QuickCallActivity, Store.columns(this@QuickCallActivity))
            this.adapter = grid
        }
        if (!Calls.hasPermission(this)) askCall.launch(Manifest.permission.CALL_PHONE)
    }

    override fun onContactClick(c: Contact) {
        if (!Calls.place(this, c.number)) {
            Toast.makeText(this, R.string.need_call_permission, Toast.LENGTH_LONG).show()
            askCall.launch(Manifest.permission.CALL_PHONE)
        }
    }

    override fun onAddClick() {
        startActivity(Intent(this, MainActivity::class.java))
    }
}
