package sk.scednote.activities

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.les_edit.*
import sk.scednote.R
import sk.scednote.ScedNoteApp
import sk.scednote.events.TxtValid
import sk.scednote.fragments.Confirm
import sk.scednote.model.Day
import sk.scednote.model.Lesson
import sk.scednote.model.ScedSort
import sk.scednote.model.Subject
import kotlin.properties.Delegates

class EditLesson : AppCompatActivity() {
    companion object {
        const val INTENT_LESSON = "les_id"
        const val INTENT_DAY = "day_num"
        const val BUNDLE_DAY = "day"
        const val BUNDLE_START = "start"
        const val BUNDLE_DUR = "dur"
        const val BUNDLE_SORT = "sort"
        const val BUNDLE_SUB = "sub_id"
        const val BUNDLE_ROOM = "room"
        const val REQUIRED_SUBJECT = "REQUIRED_FOR_LESSON"
    }
    private var les: Lesson? = null
    private var subjects = ArrayList<Subject>()
    private var busyTint by Delegates.notNull<Int>()

    private var data = ScedNoteApp.database
    private val freeHours get() = data.getFreeHours(daySel.selectedItemPosition.coerceIn(0, 4), les?.id ?: -1)

    private inner class Event : AdapterView.OnItemSelectedListener, View.OnClickListener {
        override fun onNothingSelected(parent: AdapterView<*>?) {}
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            if (view?.isVisible != true) return
            when(parent) {
                daySel, durSel -> highlightBusy()
                startSel -> {
                    val max = (getValidRange().first .. Lesson.OPENING_HOURS.last).count()
                    val pos = durSel.selectedItemPosition.coerceAtMost(max - 1)
                    rebuildDurSel()
                    durSel.setSelection(pos)
                    highlightBusy()
                }
                subSel -> {
                    if (position == subjects.size)
                        startActivityForResult(Intent(this@EditLesson, EditSubject::class.java).apply {
                            putExtra(REQUIRED_SUBJECT, true)
                        }, EditSubject.SUB_REQUEST_CODE)
                }
            }
        }

        /**
         * Kliknutia
         * @param v kliknutý objekt
         */
        override fun onClick(v: View?) {
            when(v) {
                abort -> finish()
                confirm -> {
                    if(isBusy()) {
                        Confirm.newInstance(resources.getString(R.string.les_collision)).apply {
                            setOnConfirm { _, _ -> submit() }
                            show(supportFragmentManager, "ALERT") //tag pre zapamatanie ze mam na obrazovke dialog. Vyuzivane fragmentom
                        }
                    }
                    else submit()
                }
                delete -> {
                    les?.let {
                        data.removeLesson(it.id)
                        finish()
                    }
                }
            }
        }
    }

    /**
     * Navrat
     * @return [Boolean] true
     */
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    /**
     * priprava aktivity
     * @param savedInstanceState zaloha
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.les_edit)

        les = data.getLesson(intent.getLongExtra(INTENT_LESSON, -1))
        subjects = data.loadSubjects()
        busyTint = Color.parseColor("#77FFFFFF")

        val fragments = supportFragmentManager.fragments
        for (f in fragments)
            if (f is Confirm)
                f.setOnConfirm { _, _ -> submit() }

        //tlacidlo spat + nazov
        supportActionBar?.let {
            it.setTitle(if (les != null) R.string.les_edit_tit else R.string.les_new_tit)
            it.setDisplayHomeAsUpEnabled(true)
        }
        setValuesAndEvents(savedInstanceState)
    }

    /**
     * Ak bol pocas vytvarania hodiny vytvoreny novy predmet, tak ho treba v Spinneri zvolit
     * @param requestCode kod ziadosti
     * @param resultCode kod vysledku
     * @param intent vysledne data
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (requestCode == EditSubject.SUB_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                val id = intent?.getLongExtra(BUNDLE_SUB, -1) ?: -1
                if (id > -1) {
                    subjects = data.loadSubjects()
                    subSel.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, parseSubjects())
                    subSel.setSelection(getSubPos(id))
                }
                else subSel.setSelection(0) // vzdy bude existovat polozka novy - vzdy bude existovat nulty prvok
            }
            if (subjects.size == 0) finish()
        }
    }

    /**
     * Ulozenie dat pred ukoncenim aplikacie systemom
     * @param outState záloha na uloženie
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.apply {
            putInt(BUNDLE_DAY, daySel.selectedItemPosition)
            putInt(BUNDLE_START, Lesson.OPENING_HOURS.first + startSel.selectedItemPosition)
            putInt(BUNDLE_DUR, durSel.selectedItemPosition + 1)
            putInt(BUNDLE_SORT, sortSel.selectedItemPosition)
            val pos = subSel.selectedItemPosition
            putLong(BUNDLE_SUB, if (pos !in subjects.indices) -1 else subjects[pos].id)
            putString(BUNDLE_ROOM, locationSet.text.toString())
        }
    }

    /**
     * Zavretie databáz
     */
    override fun onDestroy() {
        data.close()
        super.onDestroy()
    }

    private fun parseSubjects(): ArrayList<String> {
        return ArrayList<String>().apply {
            for (s in subjects)
                add("${s.abb} - ${s.full}")
            add(resources.getString(R.string.nw))
        }
    }
    private fun parseLesson(): Lesson {
        val id = les?.id ?: -1
        val day = Day[daySel.selectedItemPosition]
        val time = getValidRange()
        val sort = ScedSort[sortSel.selectedItemPosition]
        val sub = subjects[subSel.selectedItemPosition]
        val room = locationSet.text.toString()
        return Lesson(id, day, time, sort, sub, room)
    }
    private fun getSubPos(n: Long):Int {
        if (n > 0) for(i in subjects.indices) if (subjects[i].id == n) return i % subjects.size
        return 0
    }
    private fun getValidRange(): IntRange {
        val first = (Lesson.OPENING_HOURS.first + startSel.selectedItemPosition).coerceIn(
            Lesson.OPENING_HOURS)
        val last = (first + durSel.selectedItemPosition).coerceIn(Lesson.OPENING_HOURS)
        return first..last
    }

    /**
     * Podla vybraneho zaciatku hodiny sa prenastavi jej maximalne trvanie aby boli dodrzane otvaracie hodiny
     */
    fun rebuildDurSel() {
        val rng = startSel.selectedItemPosition + Lesson.OPENING_HOURS.first .. Lesson.OPENING_HOURS.last
        durSel.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, (1..rng.count()).joinToString(",").split(","))
    }

    private fun isBusy(): Boolean {
        val rng = getValidRange()
        val free = freeHours
        for (i in rng)
            if (i !in free)
                return true
        return false
    }
    private fun highlightBusy() {
        val free = freeHours
        val range = getValidRange()
        val full = free.isEmpty()

        markAsBusy(daySel.selectedView, full)
        markAsBusy(startSel.selectedView, full || range.first !in free)
        markAsBusy(durSel.selectedView, full || isBusy())
    }
    private fun markAsBusy(view: View?, busy: Boolean = true) {
        view?.apply {
            if (busy) {
                taken_over.visibility = View.VISIBLE
                setBackgroundColor(Color.RED)
                background.setTint(busyTint)
                background.setTintMode(PorterDuff.Mode.SRC_ATOP)
            }
            else {
                taken_over.visibility = View.GONE
                background = null
            }
        }
    }

    private fun setValuesAndEvents(saved: Bundle?) {
        //vyplnit selekty
        val layout = android.R.layout.simple_list_item_1
        daySel.adapter = ArrayAdapter(this, layout, Day.titles)
        startSel.adapter = ArrayAdapter(this, layout, ArrayList<String>().apply {
            for (n in Lesson.OPENING_HOURS)
                add("${n/10}${n%10}:00")
        })
        rebuildDurSel()
        subSel.adapter = ArrayAdapter(this, layout, parseSubjects())
        sortSel.adapter = ArrayAdapter(this, layout, ScedSort.sorts)

        //vybrat moznosti

        daySel.setSelection(saved?.getInt(BUNDLE_DAY) ?: les?.day?.position ?: intent.getIntExtra(INTENT_DAY, 0))
        startSel.setSelection(saved?.getInt(BUNDLE_START) ?: (les?.time?.first ?: Lesson.OPENING_HOURS.first) - Lesson.OPENING_HOURS.first)
        durSel.setSelection(saved?.getInt(BUNDLE_DUR) ?: ((les?.time?.count() ?: 1) - 1))
        sortSel.setSelection(saved?.getInt(BUNDLE_SORT) ?: les?.sort?.position?.coerceAtLeast(0) ?: 0)
        subSel.setSelection(getSubPos(saved?.getLong(BUNDLE_SUB) ?: les?.subject?.id ?: 0))
        locationSet.setText(saved?.getString(BUNDLE_ROOM) ?: les?.room ?: "")

        if (les == null || les!!.id <= 0)
            delete.visibility = View.GONE

        val event = Event()
        //udalosti
        locationSet.addTextChangedListener(TxtValid(locationSet, "[a-zA-ZÀ-ž0-9]", 0..9, confirm))
        daySel.onItemSelectedListener = event
        startSel.onItemSelectedListener = event
        durSel.onItemSelectedListener = event
        sortSel.onItemSelectedListener = event
        subSel.onItemSelectedListener = event
        locationSet.setOnClickListener(event)
        abort.setOnClickListener(event)
        confirm.setOnClickListener(event)
        delete.setOnClickListener(event)
    }

    /**
     * Odoslanie zmien a navrat na predoslu aktivitu
     */
    fun submit() {
        val inputLesson = parseLesson()
        data.insertOrUpdateLesson(inputLesson)
        data.putLessonsTogether()
        callingActivity?.let {
            setResult(Activity.RESULT_OK, Intent().apply {
                putExtra(INTENT_DAY, inputLesson.day.position)
            })
        }
        finish()
    }
}