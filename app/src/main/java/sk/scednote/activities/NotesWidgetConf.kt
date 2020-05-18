package sk.scednote.activities

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.notes_widget_configure.*
import sk.scednote.R
import sk.scednote.ScedNoteApp
import sk.scednote.model.Note
import sk.scednote.model.Subject
import sk.scednote.widgets.NotesWidget

/**
 * Nastavenia konfiguracie pred tvorbou widgetu
 */
class NotesWidgetConf : Activity() {
    companion object {
        const val LABEL = "CATEGORY"
        const val CATEGORY = "LIST"
        const val SHARED_PREFS = "SHARED_PREFS"
    }
    private data class Option(val label: String, val category: Long, private val sub: Subject? = null) {
        constructor(resource: Int, category: Long) : this(ScedNoteApp.res.getString(resource), category)
    }

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private val options = arrayListOf(
        Option(R.string.today, Note.DEADLINE_TODAY),
        Option(R.string.tomorrow, Note.DEADLINE_TOMORROW),
        Option(R.string.this_week, Note.DEADLINE_THIS_WEEK),
        Option(R.string.late, Note.DEADLINE_LATE),
        Option(R.string.forever, Note.DEADLINE_FOREVER)
    ).apply {
        val subs = ScedNoteApp.database.loadSubjects()
        for (sub in subs)
            add(Option(sub.abb, sub.id))
    }

    /**
     * Tvorba obsahu a udalosti
     * @param icicle zaloha
     */
    public override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        setContentView(R.layout.notes_widget_configure)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        timeRelated.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, ArrayList<String>().apply {
            for (opt in options)
                add(opt.label)
        })

        //získať widget id
        intent?.extras?.let {
            appWidgetId = it.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        }

        //ak aktivita nebola otvorena pri tvorbe widgetu, tak zavrieť
        setResult(RESULT_CANCELED, Intent().apply { putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId) })
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) finish()

        confirm.setOnClickListener {
            val option = options[timeRelated.selectedItemPosition]
            getSharedPreferences(SHARED_PREFS + appWidgetId, Context.MODE_PRIVATE).edit().also {
                it.putString(LABEL, option.label)
                it.putLong(CATEGORY, option.category)
                it.apply()
            }
            NotesWidget.createOrUpdateWidget(this, appWidgetId, AppWidgetManager.getInstance(this), option.category, option.label)

            setResult(RESULT_OK, Intent().apply { putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId) })
            finish()
        }
    }
}
