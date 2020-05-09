package sk.scednote.adapters

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import sk.scednote.R
import sk.scednote.activities.NotesWidgetConf
import sk.scednote.model.Database
import sk.scednote.model.Note
import sk.scednote.widgets.NotesWidget


/**
 * Trieda pristupuje k vzdialenemu Adapteru pod krycim nazvom RemoteViewsFactory
 */
class NoteWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return NoteWidgetFactory(applicationContext, intent)
    }

    /**
     * Trieda ktora nacitava a spravuje data jednotlive dáta (dáta o poznámkach)
     * @property context Kontext - nesmie byť null
     * @param intent Intent - nesmie byť null
     */
    class NoteWidgetFactory(private val context: Context, intent: Intent): RemoteViewsFactory {
        private val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        private val category = intent.getLongExtra(NotesWidgetConf.CAT_ID, Note.NO_DATA)
        private lateinit var data: Database
        private lateinit var items: ArrayList<Note>

        /**
         * Vrati remoteView pre danu polozku
         * Posle informaciu o vykonanej udalosti Správcovi widgetu (trieda: widgets/NotesWidget),
         * ktory rozhodne co sa ma stat
         *
         * @param position Pozícia
         * @return [RemoteViews]
         */
        override fun getViewAt(position: Int):RemoteViews {
            return RemoteViews(context.packageName, R.layout.note_item_delete_only).also { remote ->
                //vyplnenie obsahu
                remote.setTextViewText(R.id.abb, items[position].sub.abb)
                if (items[position].deadline == null) remote.setViewVisibility(R.id.datetime, View.GONE)
                else remote.setTextViewText(R.id.datetime, items[position].ddlItem)
                remote.setTextViewText(R.id.detail, items[position].info)

                //odstranovaci intent
                val deleteIntent = Intent()
                deleteIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                deleteIntent.putExtra(NotesWidget.ITEM_POSITION, position)
                deleteIntent.putExtra(NotesWidget.ITEM_ID, items[position].id)
                deleteIntent.putExtra(NotesWidgetConf.CATEGORY, category)
                remote.setOnClickFillInIntent(R.id.delete, deleteIntent)
            }
        }

        /**
         * Vytvorenie databázy
         */
        override fun onCreate() {
            data = Database()
        }

        override fun getLoadingView(): RemoteViews? = null

        /**
         * Vrátenie ID poznámy na pozíii [position]
         */
        override fun getItemId(position: Int) = items[position].id

        /**
         * Znovu načítanie dát
         */
        override fun onDataSetChanged() {
            items = data.getNotes(category)
        }
        override fun hasStableIds() = true

        /**
         * @return Počet záznamov v zozname
         */
        override fun getCount() = items.size
        override fun getViewTypeCount() = 1

        /**
         * Zavretie databázy
         */
        override fun onDestroy() {
            data.close()
        }
    }
}
