package com.jahanzaib.edgedirectcall

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.samsung.android.sdk.look.cocktailbar.SlookCocktailManager
import com.samsung.android.sdk.look.cocktailbar.SlookCocktailProvider

/**
 * The Samsung Edge-panel entry. Samsung's Edge screen looks for a provider class with this
 * exact name, so don't rename it.
 */
class SinglePlusCocktailProvider : SlookCocktailProvider() {
    override fun onUpdate(context: Context, manager: SlookCocktailManager, cocktailIds: IntArray) {
        EdgePanel.update(context, manager, cocktailIds)
    }
}

object EdgePanel {
    fun update(ctx: Context, manager: SlookCocktailManager, ids: IntArray) {
        val cols = Store.columns(ctx)
        val layout = if (cols == 2) R.layout.panel_2 else R.layout.panel_3
        for (id in ids) {
            val rv = RemoteViews(ctx.packageName, layout)
            val svc = Intent(ctx, PanelGridService::class.java).apply {
                putExtra("cocktailId", id)
                data = Uri.parse("dcpanel://grid/$id/$cols")
            }
            rv.setRemoteAdapter(R.id.grid, svc)
            rv.setEmptyView(R.id.grid, R.id.empty)
            val template = PendingIntent.getActivity(
                ctx, 0, Intent(ctx, CallActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            rv.setPendingIntentTemplate(R.id.grid, template)
            val help = RemoteViews(ctx.packageName, R.layout.panel_help)
            manager.updateCocktail(id, rv, help)
        }
    }

    /** Called after contacts or settings change. Silently does nothing on non-Samsung phones. */
    fun refresh(ctx: Context) {
        try {
            val manager = SlookCocktailManager.getInstance(ctx)
            val ids = manager.getCocktailIds(ComponentName(ctx, SinglePlusCocktailProvider::class.java))
            if (ids != null && ids.isNotEmpty()) {
                update(ctx, manager, ids)
                for (id in ids) manager.notifyCocktailViewDataChanged(id, R.id.grid)
            }
        } catch (_: Throwable) {
        }
    }
}

/** Feeds the scrolling grid inside the Edge panel. */
class PanelGridService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory = Factory(applicationContext)

    private class Factory(private val ctx: Context) : RemoteViewsFactory {
        private var items: List<Contact> = emptyList()

        override fun onCreate() {
            items = Store.load(ctx)
        }

        override fun onDataSetChanged() {
            items = Store.load(ctx)
        }

        override fun onDestroy() {}

        override fun getCount(): Int = items.size

        override fun getViewAt(position: Int): RemoteViews {
            val rv = RemoteViews(ctx.packageName, R.layout.item_panel)
            val c = items.getOrNull(position) ?: return rv
            rv.setImageViewBitmap(R.id.avatar, Avatar.render(ctx, c, 160))
            rv.setTextViewText(R.id.name, c.name)
            val fill = Intent()
                .putExtra(CallActivity.EXTRA_NUMBER, c.number)
                .putExtra(CallActivity.EXTRA_NAME, c.name)
            rv.setOnClickFillInIntent(R.id.item_root, fill)
            return rv
        }

        override fun getLoadingView(): RemoteViews? = null
        override fun getViewTypeCount(): Int = 1
        override fun getItemId(position: Int): Long =
            items.getOrNull(position)?.id?.hashCode()?.toLong() ?: position.toLong()

        override fun hasStableIds(): Boolean = true
    }
}
