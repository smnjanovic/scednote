package sk.scednote.activities

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.notelist.*
import kotlinx.android.synthetic.main.tab_button.view.*
import sk.scednote.R
import sk.scednote.adapters.NoteAdapter
import sk.scednote.adapters.SubjectAdapter
import sk.scednote.fragments.DateFragment
import sk.scednote.fragments.TimeFragment
import sk.scednote.model.data.Note
import java.util.*
import kotlin.properties.Delegates

class NoteList : AppCompatActivity() {
    companion object {
        private const val OLD_DATA = "OLD_DATA"
        private const val SUB_POS = "SUB_POS"
        const val TARGET_ID = "TARGET_ID"
        const val CATEGORY = "CATEGORY"

        private const val DIALOG_TARGET = "DIALOG_TARGET"
        private const val DATE_DIALOG = "DATE_DIALOG"
        private const val TIME_DIALOG = "TIME_DIALOG"
        private const val YEAR = "YEAR"
        private const val MONTH = "MONTH"
        private const val DAY = "DAY"
        private const val HOUR = "HOUR"
        private const val MINUTE = "MINUTE"

        const val DEADLINE_TODAY: Long = 0
        const val DEADLINE_TOMORROW: Long = -1
        const val DEADLINE_TIME_OUT: Long = -2
        const val DEADLINE_LONG_TERM: Long = -3
        const val NO_DATA: Long = -4
        //hodnoty vacsie ako nula su id predmetov
    }

    //pri prepinani tlacidiel, aktualne zvyraznene tlacitka znevyraznim priamo - bez zbyt. podmienok a cyklov
    private var chosenSubject: Button? = null
    private var chosenCategory: Button? = null

    private var category: Long = 0 //-2..0 casova zavislost,
    private var s_position: Int = 0

    private var highlightColor by Delegates.notNull<Int>()
    private lateinit var subAdapt: SubjectAdapter
    private lateinit var noteAdapt: NoteAdapter

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTheme(R.style.NoteTheme)
        //window.navigationBarColor = resources.getColor(R.color.notesPrimary, null)
        //window.statusBarColor = resources.getColor(R.color.notesPrimaryDark, null)

        highlightColor = Color.parseColor("#aaffffff")
        //aka mnozina poznamok sa zobrazi

        category = intent.getLongExtra(
            CATEGORY, savedInstanceState?.getLong(
                CATEGORY
            ) ?: 0)
        s_position = savedInstanceState?.getInt(SUB_POS) ?: 0
        intent.removeExtra(CATEGORY)
        setContentView(R.layout.notelist)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        (supportFragmentManager.findFragmentByTag(DATE_DIALOG) as DateFragment?)?.let { onDateChosen(it) }
        (supportFragmentManager.findFragmentByTag(TIME_DIALOG) as TimeFragment?)?.let { onTimeChosen(it) }

        setAdapters(savedInstanceState)
        setEvents()
    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        (subjectTabs.adapter as SubjectAdapter).backupData(outState,
            OLD_DATA
        )
        (noteList.adapter as NoteAdapter).backupData(outState)
        outState.putLong(CATEGORY, category)
        outState.putInt(SUB_POS, s_position)
    }

    override fun onResume() {
        super.onResume()
        //prve nacitanie zoznamu predmetov
        changeHighlight(when(category) {
            DEADLINE_TIME_OUT -> late
            DEADLINE_TODAY -> today
            DEADLINE_TOMORROW -> tomorrow
            DEADLINE_LONG_TERM -> forever
            else -> subject_related
        })
        Handler().postDelayed({
            //scrollnut sa na poziciu vybranej polozky
            chosenCategory?.let { scrollCat(chosenCategory!!) }
            if (category > 0) scrollSub()
            scrollToNote()
        }, 100)
    }

    override fun onDestroy() {
        super.onDestroy()
        subAdapt.close()
        noteAdapt.close()
    }

    private fun setAdapters(saved: Bundle?) {
        subAdapt = SubjectAdapter(
            SubjectAdapter.VIEW_TYPE_TAB,
            saved?.getParcelableArrayList(OLD_DATA)
        )
        noteAdapt = NoteAdapter(
            category,
            saved?.getParcelableArrayList(NoteAdapter.RESTORED_DATA),
            saved?.getParcelable(NoteAdapter.LAST_EDIT) as Note?,
            saved?.getParcelable(NoteAdapter.NEW_ITEM) as Note?
        )

        //vlozenie layoutu a obsahu do RecyclerView-ov
        subjectTabs.apply {
            layoutManager = LinearLayoutManager(this@NoteList, LinearLayoutManager.HORIZONTAL, false)
            adapter = subAdapt
        }
        noteList.apply {
            layoutManager = LinearLayoutManager(this@NoteList)
            adapter = noteAdapt
            saved?.let { layoutManager?.onRestoreInstanceState(saved.getParcelable(NoteAdapter.RESTORED_DATA)) }
        }
    }

    private fun setEvents() {
        //kliknutie na polozku predmet (pri kategorii "k predmetu")
        subAdapt.setChoiceEvent(View.OnClickListener{
            val holder = it.tag as SubjectAdapter.TabHolder
            category = holder.getSubject().id!!
            s_position = holder.layoutPosition
            noteAdapt.loadData(category)
            changeHighlight(it)
        })

        noteAdapt.onDateTimeChange {holder, calendar ->
            val date = DateFragment()
            date.data.putInt(DIALOG_TARGET, holder.adapterPosition)
            date.data.putInt(YEAR, calendar.get(Calendar.YEAR))
            date.data.putInt(MONTH, calendar.get(Calendar.MONTH))
            date.data.putInt(DAY, calendar.get(Calendar.DAY_OF_MONTH))
            date.data.putInt(HOUR, calendar.get(Calendar.HOUR_OF_DAY))
            date.data.putInt(MINUTE, calendar.get(Calendar.MINUTE))
            date.show(supportFragmentManager,
                DATE_DIALOG
            )
            onDateChosen(date)
        }

        //vyber kategorie
        val catChoice = View.OnClickListener {btn ->
            when(btn) {
                late -> category =
                    DEADLINE_TIME_OUT
                today -> category =
                    DEADLINE_TODAY
                tomorrow -> category =
                    DEADLINE_TOMORROW
                forever -> category =
                    DEADLINE_LONG_TERM
                else -> {
                    s_position = s_position.coerceAtMost(subAdapt.itemCount-1)
                    category = if (s_position > -1) subAdapt.getItemId(s_position) else NO_DATA
                }
            }
            noteAdapt.loadData(category)
            changeHighlight(btn)
        }
        subject_related.setOnClickListener(catChoice)
        today.setOnClickListener(catChoice)
        tomorrow.setOnClickListener(catChoice)
        forever.setOnClickListener(catChoice)
        late.setOnClickListener(catChoice)
    }
    private fun onDateChosen(date: DateFragment) {
        date.setOnChoice { year, month, day ->
            val time = TimeFragment()
            time.data.putInt(
                DIALOG_TARGET, date.data.getInt(
                    DIALOG_TARGET
                ))
            time.data.putInt(YEAR, year)
            time.data.putInt(MONTH, month)
            time.data.putInt(DAY, day)
            time.data.putInt(
                HOUR, date.data.getInt(
                    HOUR
                ))
            time.data.putInt(
                MINUTE, date.data.getInt(
                    MINUTE
                ))
            time.show(supportFragmentManager,
                TIME_DIALOG
            )
            onTimeChosen(time)
        }
    }

    private fun onTimeChosen(time: TimeFragment) {
        time.setOnChoice { hour, minute ->
            val pos = time.data.getInt(DIALOG_TARGET)
            if (pos in 0 until noteAdapt.itemCount) {
                val calendar = Calendar.getInstance().apply {
                    time.data.let { b -> set(b.getInt(YEAR), b.getInt(
                        MONTH
                    ), b.getInt(DAY), hour, minute) }
                }
                if (calendar <= Calendar.getInstance())
                    Toast.makeText(this, resources.getString(R.string.time_out), Toast.LENGTH_SHORT).show()
                else
                    (noteList.findViewHolderForAdapterPosition(pos) as NoteAdapter.NoteHolder?)?.onSetDeadline(calendar)
            }
        }
    }

    private fun changeHighlight(v: View) {
        when (v) {
            late, today, tomorrow, forever, subject_related -> {
                chosenCategory?.background?.setTintList(null)
                chosenCategory = v as Button
                chosenCategory!!.background.setTint(highlightColor)

                with(v != subject_related, {
                    subjectTabs.visibility = if (this) View.GONE else View.VISIBLE
                    scrollCat(v)
                    if (!this) { highlightSubjectWhenPossible() }
                })
            }
            else -> highlightSubjectWhenPossible()
        }
    }

    private fun highlightSubjectWhenPossible() {
        (subjectTabs.findViewHolderForAdapterPosition(s_position) as SubjectAdapter.TabHolder?)?.let {
            chosenSubject?.background?.setTintList(null)
            chosenSubject = it.itemView.tab as Button
            chosenSubject!!.background.setTint(highlightColor)
            scrollSub()
        } ?: Handler().postDelayed({ highlightSubjectWhenPossible() }, 50)
    }
    private fun scrollCat(v: View) {
        when {
            v.left < noteTabs.scrollX -> noteTabs.scrollX = v.left - v.width / 2
            v.right > noteTabs.scrollX + noteTabs.width -> noteTabs.scrollX = v.right - noteTabs.width + v.width / 2
            v.top < noteTabs.scrollY -> noteTabs.scrollY = v.top - v.height / 2
            v.bottom > noteTabs.scrollY + noteTabs.height -> noteTabs.scrollY = v.bottom - noteTabs.height + v.height / 2
        }
    }
    private fun scrollSub() {
        subjectTabs.scrollToPosition(s_position)
    }

    //ak aktivita zacala za ucelu vyhladania konkretnej poznamky
    private fun scrollToNote() {
        fun untilFound(n: Int) {
            if (noteList.findViewHolderForAdapterPosition(n) == null)
                Handler().postDelayed({untilFound(n)}, 50)
            else
                noteList.scrollToPosition(n)
        }
        intent.getLongExtra(TARGET_ID, -1).let { target ->
            val pos = if (target > -1) noteAdapt.getItemPositionById(target) else -1
            if (pos in 0 until noteAdapt.itemCount) {
                untilFound(pos)
                intent.removeExtra(TARGET_ID)
            }
            //ak je nastavena kategoria zoznamu podla casu a odbila polnoc alebo deadline, zaznam sa tu uz nenajde
            else if (target > -1)
                intent.removeExtra(TARGET_ID)
        }
    }
}

