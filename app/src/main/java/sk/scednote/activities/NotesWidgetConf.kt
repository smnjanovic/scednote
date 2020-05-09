package sk.scednote.activities

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.notes_widget_configure.*
import sk.scednote.R
import sk.scednote.model.Database
import sk.scednote.model.Note
import sk.scednote.widgets.NotesWidget

/**
 * Nastavenia konfiguracie pred tvorbou widgetu
 */
class NotesWidgetConf : Activity() {
    companion object {
        const val CATEGORY = "CATEGORY"
        const val CAT_ID = "LIST"
        const val SHARED_PREFS = "SHARED_PREFS"
    }

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var database: Database


    /**
     * Tvorba obsahu a udalosti
     * @param icicle zaloha
     */
    public override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        setContentView(R.layout.notes_widget_configure)
        database = Database()

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        val subjects = database.loadSubjects()
        timeRelated.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, ArrayList<String>().apply {
            add(resources.getString(R.string.today))
            add(resources.getString(R.string.tomorow))
            add(resources.getString(R.string.this_week))
            add(resources.getString(R.string.subject_related))
        })
        subjectRelated.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, ArrayList<String>().apply {
            for (sub in subjects)
                add(sub.abb)
        })

        //obnova vysledku
        intent?.extras?.let {
            appWidgetId = it.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        }

        setResult(RESULT_CANCELED, Intent().apply { putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId) })
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) finish()

        timeRelated.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                subjectRelated.visibility = if (position == 3) View.VISIBLE else View.INVISIBLE
            }
        }
        confirm.setOnClickListener {
            val time = arrayOf(Note.DEADLINE_TODAY, Note.DEADLINE_TOMORROW, Note.DEADLINE_RECENT, null)
            val catSubId = time[timeRelated.selectedItemPosition] ?: subjects[subjectRelated.selectedItemPosition].id
            val category = when(catSubId) {
                Note.DEADLINE_TODAY -> resources.getString(R.string.today)
                Note.DEADLINE_TOMORROW -> resources.getString(R.string.tomorow)
                Note.DEADLINE_RECENT -> resources.getString(R.string.this_week)
                else -> database.getSubject(catSubId)?.abb ?: throw NullPointerException("No such subject!")
            }

            getSharedPreferences(SHARED_PREFS + appWidgetId, Context.MODE_PRIVATE).edit().also {
                it.putString(CATEGORY, category)
                it.putLong(CAT_ID, catSubId)
                it.apply()
            }

            NotesWidget.confirmUpdate(this, appWidgetId, AppWidgetManager.getInstance(this), catSubId, category)

            setResult(RESULT_OK, Intent().apply { putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId) })
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        database.close()
    }
}
