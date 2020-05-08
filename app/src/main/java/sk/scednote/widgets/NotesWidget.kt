package sk.scednote.widgets

import android.app.Activity
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import sk.scednote.R
import sk.scednote.activities.NoteList
import sk.scednote.activities.NotesWidgetConf
import sk.scednote.adapters.NoteWidgetService
import sk.scednote.model.Database
import sk.scednote.model.Note

/**
 * Widget zobrazuje zoznam uloh podla zvolenej kategorie
 * Zdroj: https://www.youtube.com/playlist?list=PLrnPJCHvNZuDCoET8jL2VK4YVRNhVEy0K
 */
class NotesWidget : AppWidgetProvider() {
    companion object {
        const val ACTION_DELETE = "REMOVAL"
        const val ITEM_POSITION = "ITEM_POS"
        const val ITEM_ID = "ITEM_ID"

        /**
         * metoda je znovu pouzitelna v aktivite ktora konfiguruje widget pri prvom vytvoreni
         * vola sa pre kazdy widget osobitne. Jej ulohou je aktualizovat obsah widgetu
         */
        fun confirmUpdate (ctx: Context, widget: Int, manager: AppWidgetManager, category: Long, title: String) {
            val adapterIntent = Intent(ctx, NoteWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widget)
                putExtra(NotesWidgetConf.CAT_ID, category)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            val deleteIntent = Intent(ctx, NotesWidget::class.java).let {
                it.action = ACTION_DELETE
                PendingIntent.getBroadcast(ctx, 0, it, 0)
            }
            val openAppIntent = Intent(ctx, NoteList::class.java).let {
                it.action = NoteList.OPEN_FROM_WIDGET
                it.putExtra(NoteList.CATEGORY, category)
                PendingIntent.getActivity(ctx, widget, it, PendingIntent.FLAG_UPDATE_CURRENT)
            }

            val remote = RemoteViews(ctx.packageName, R.layout.notes_widget).apply {
                setOnClickPendingIntent(R.id.note_heading, openAppIntent)
                setTextViewText(R.id.note_heading, title)

                setRemoteAdapter(R.id.widget_note_list, adapterIntent)
                setEmptyView(R.id.widget_note_list, R.id.empty)
                setPendingIntentTemplate(R.id.widget_note_list, deleteIntent)
            }
            manager.updateAppWidget(widget, remote)
            manager.notifyAppWidgetViewDataChanged(widget, R.id.widget_note_list)
        }
    }

    /**
     * Aktualizacia obsahu widgetu
     */
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            val title: String
            val category: Long

            context.getSharedPreferences(NotesWidgetConf.SHARED_PREFS + appWidgetId, Activity.MODE_PRIVATE).apply {
                title = getString(NotesWidgetConf.CATEGORY, "") ?: ""
                category = getLong(NotesWidgetConf.CAT_ID, Note.NO_DATA)
            }
            confirmUpdate(context, appWidgetId, appWidgetManager, category, title)
        }
    }

    /**
     * pokus o odstranenie zaznamu v zozname vo widgete
     */
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_DELETE) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, NotesWidget::class.java))
            val cat = intent.getLongExtra(NotesWidgetConf.CATEGORY, Note.NO_DATA)

            for (id in ids) {
                val pref = context.getSharedPreferences(NotesWidgetConf.SHARED_PREFS + id, Activity.MODE_PRIVATE)
                val cat2 = pref.getLong(NotesWidgetConf.CAT_ID, Note.NO_DATA)

                //kategorie sa nezhoduju, widget nezdiela ten isty obsah
                if (cat > 0 && cat2 > 0 && cat != cat2) continue
                intent.getLongExtra(ITEM_ID, -1).also { noteID ->
                    if (noteID > -1) {
                        Database().apply {
                            removeNote(noteID)
                            close()
                        }
                        manager.notifyAppWidgetViewDataChanged(id, R.id.widget_note_list)
                    }
                }
            }
        }
        super.onReceive(context, intent)
    }


}

