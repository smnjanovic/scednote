package sk.scednote.activities
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.notelist.*
import sk.scednote.NoteReminder
import sk.scednote.R
import sk.scednote.adapters.NoteAdapter
import sk.scednote.adapters.SubjectAdapter
import sk.scednote.fragments.DateFragment
import sk.scednote.fragments.TimeFragment
import sk.scednote.model.Note
import java.util.*

/**
 * Načíta zoznam úloh
 */
class NoteList : ShakeCompatActivity() {
    companion object {
        const val OPEN_FROM_WIDGET = "OPEN FROM WIDGET"
        const val TARGET_ID = "TARGET_ID"
        const val CATEGORY = "CATEGORY"

        private const val DIALOG_TARGET = "DIALOG_TARGET"
        private const val DATE_DIALOG = "DATE_DIALOG"
        private const val TIME_DIALOG = "TIME_DIALOG"
    }

    private var category: Long = Note.DEADLINE_TODAY
        set(id) {
            field = id
            supportActionBar?.let {
                it.title = when (id) {
                    Note.DEADLINE_TODAY -> resources.getString(R.string.today)
                    Note.DEADLINE_TOMORROW -> resources.getString(R.string.tomorrow)
                    Note.DEADLINE_THIS_WEEK -> resources.getString(R.string.this_week)
                    Note.DEADLINE_LATE -> resources.getString(R.string.late)
                    Note.DEADLINE_FOREVER -> resources.getString(R.string.forever)
                    else -> {
                        val pos = subAdapt.getPositionById(id)
                        if (pos in 0 until subAdapt.itemCount) {
                            subAdapt.marked = pos
                            subAdapt.getSubjectNameAt(pos)
                        }
                        else resources.getString(R.string.notes)
                    }
                }
                subjectTabs?.visibility = if (id > 0) View.VISIBLE else View.GONE
            }
        }

    private lateinit var subAdapt: SubjectAdapter
    private lateinit var noteAdapt: NoteAdapter

    /**
     * Vlozenie menu
     * @param menu Menu
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.note_cat_list, menu)
        return super.onCreateOptionsMenu(menu)
    }

    /**
     * funkcie menu poloziek
     * @param item Menu
     * @return [Boolean] true
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.today -> category = Note.DEADLINE_TODAY
            R.id.tomorrow -> category = Note.DEADLINE_TOMORROW
            R.id.recentList -> category = Note.DEADLINE_THIS_WEEK
            R.id.late -> category = Note.DEADLINE_LATE
            R.id.forever -> category = Note.DEADLINE_FOREVER
            R.id.subject_related -> {
                if (subAdapt.marked == -1) subAdapt.marked = 0
                category = subAdapt.getItemId(subAdapt.marked)
            }
            R.id.advance -> startActivity(Intent(this, NotificationAdvance::class.java))
        }
        if (item.itemId != R.id.advance)
            noteAdapt.loadData(category)
        return super.onOptionsItemSelected(item)
    }

    /**
     * Krok späť
     * @return Vždy vracia true
     */
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    /**
     * Nastavenia pred spustením aktivity
     * @param savedInstanceState záloha dát tejto aktivity, ktorú zastavil systém
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.notelist)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        NoteReminder.createNoteReminderChannel()
        setAdapters(savedInstanceState)
        setEvents()

        //zapamatanie si dialogu nastavenia datumu
        (supportFragmentManager.findFragmentByTag(DATE_DIALOG) as DateFragment?)?.let { onDateChosen(it) }
        (supportFragmentManager.findFragmentByTag(TIME_DIALOG) as TimeFragment?)?.let { onTimeChosen(it) }
    }

    /**
     * Ulozenie zoznamu a kategorie a aktivnej polozky
     * @param outState Záloha
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        (subjectTabs.adapter as SubjectAdapter).backupData(outState)
        (noteList.adapter as NoteAdapter).backupData(outState)
        outState.putLong(CATEGORY, category)
    }

    /**
     * Ak sa odkazujem na konkretnu poznamku, a este stale existuje v zozname, rolujem k nej
     */
    override fun onResume() {
        super.onResume()
        val id = intent.getLongExtra(TARGET_ID, -1)
        if (id > 0) {
            val pos = noteAdapt.getItemPositionById(id)
            if (pos in 0 until noteAdapt.itemCount)
                //operacia vo fronte
                noteList.post {
                    noteList.scrollToPosition(pos)
                    intent.removeExtra(TARGET_ID)
                }
        }
    }

    //nastavenie adapterov
    private fun setAdapters(saved: Bundle?) {
        subAdapt = SubjectAdapter(SubjectAdapter.ADAPTER_TYPE_TAB, saved)
        category = intent.getLongExtra(CATEGORY, saved?.getLong(CATEGORY) ?: Note.DEADLINE_TODAY)
        noteAdapt = NoteAdapter(category, saved)
        showIfEmpty()
        intent.removeExtra(CATEGORY)

        subjectTabs.apply {
            adapter = subAdapt
            layoutManager = LinearLayoutManager(this@NoteList, LinearLayoutManager.HORIZONTAL, false)
        }
        noteList.apply {
            layoutManager = LinearLayoutManager(this@NoteList)
            adapter = noteAdapt
            saved?.let { layoutManager?.onRestoreInstanceState(saved.getParcelable(NoteAdapter.RESTORED_DATA)) }
        }
    }

    private fun showIfEmpty() {
        when {
            noteAdapt.subjects.size == 0 -> {
                note_empty.visibility = View.VISIBLE
                note_empty.text = resources.getString(R.string.no_subject_no_note)
            }
            noteAdapt.itemCount == 0 -> {
                note_empty.visibility = View.VISIBLE
                note_empty.text = resources.getString(R.string.no_recent_notes)
            }
            else -> note_empty.visibility = View.GONE
        }
    }

    private fun setEvents() {
        //prepinanie predmetov
        subAdapt.onSwitch { view ->
            view.tag.also { tag ->
                if (tag is SubjectAdapter.TabHolder) {
                    category = subAdapt.getItemId(tag.adapterPosition)
                    subAdapt.getPositionById(category)
                    noteAdapt.loadData(category)
                    showIfEmpty()
                }
            }
        }

        //kliknutie na polozku predmet (pri kategorii "k predmetu")
        noteAdapt.onDateTimeChange {holder, calendar ->
            val date = DateFragment()
            date.putCalendar(calendar)
            date.data.putInt(DIALOG_TARGET, holder.adapterPosition)
            date.show(supportFragmentManager, DATE_DIALOG)
            onDateChosen(date)
        }
        if (category in 0 until subAdapt.itemCount) {
            subjectTabs.scrollToPosition(subAdapt.getPositionById(category))
        }
    }
    private fun onDateChosen(date: DateFragment) {
        date.setOnChoice { calendar ->
            val time = TimeFragment()
            time.putCalendar(calendar)
            time.data.putInt(DIALOG_TARGET, date.data.getInt(DIALOG_TARGET))
            time.show(supportFragmentManager, TIME_DIALOG)
            onTimeChosen(time)
        }
    }

    private fun onTimeChosen(time: TimeFragment) {
        time.setOnChoice { cal ->
            val pos = time.data.getInt(DIALOG_TARGET)
            when {
                cal <= Calendar.getInstance() ->
                    Toast.makeText(this, resources.getString(R.string.time_out), Toast.LENGTH_SHORT).show()
                pos in 0 until noteAdapt.itemCount ->
                    (noteList.findViewHolderForAdapterPosition(pos) as NoteAdapter.NoteHolder?)?.onSetDeadline(cal)
                else ->
                    Toast.makeText(this, R.string.note_not_on_list, Toast.LENGTH_SHORT).show()
            }
        }
    }
}

