package sk.scednote

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
import sk.scednote.model.TxtValid
import sk.scednote.model.Database
import sk.scednote.model.Popups
import sk.scednote.model.SpinnerListener
import sk.scednote.model.data.*

class EditLesson : AppCompatActivity() {
    companion object {
        private val new_subject = 1
        private val openingHours = 7..21
    }
    private lateinit var data: Database
    private var subjects = ArrayList<Subject>()
    private var contentLoaded = false
    private var les: Lesson? = null

    private var start = openingHours.first
    private var max_dur = openingHours.count()
    private var free: Boolean = true

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.les_edit)

        //pripojit k databaze
        data = Database(this)
        isFree()
        les = data.getLesson(intent.getLongExtra("les_id", -1))
        subjects = data.loadSubjects()

        //tlacidlo spat + nazov
        supportActionBar?.let {
            it.setTitle(if (les != null) R.string.les_edit_tit else R.string.les_new_tit)
            it.setDisplayHomeAsUpEnabled(true)
        }

        //vyplnit selekty
        daySel.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, Day.getTitles(this))
        startSel.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, rangeToHours())
        durSel.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, untilTheEnd())
        subSel.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, parseSubjects())
        sortSel.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, ScedSort.getSorts(this))

        //vybrat moznosti
        daySel.setSelection(savedInstanceState?.getInt("day") ?: les?.day?.position ?: intent.getIntExtra("day_num", 0))
        startSel.setSelection(savedInstanceState?.getInt("start") ?: (les?.time?.first ?: openingHours.first) - openingHours.first)
        durSel.setSelection(savedInstanceState?.getInt("dur") ?: (les?.time?.count() ?: 1) - 1)
        sortSel.setSelection(savedInstanceState?.getInt("sort") ?: les?.sort?.position?.coerceAtLeast(0) ?: 0)
        subSel.setSelection(getSubPos(savedInstanceState?.getLong("sub_id") ?: les?.subject?.id ?: 0))
        locationSet.setText(savedInstanceState?.getString("room") ?: les?.room ?: "")

        //udalosti k selektom
        locationSet.addTextChangedListener(TxtValid(this, "[a-zA-ZÀ-ž0-9]", 0..9, locationSet, confirm))
        //pole bude vyznacene cervenou ak je tento den uz vytazeny
        daySel.onItemSelectedListener = SpinnerListener(fun(_:AdapterView<*>?, view: View?, pos: Int, _:Long){
            if (view != null && data.getBusyHoursCount(pos.coerceIn(0, 4), les?.id ?: -1) == openingHours.count()) {
                free = false
                restrictedOrNot(view)
            }
        })
        startSel.onItemSelectedListener = SpinnerListener(fun(_: AdapterView<*>?, view: View?, position: Int, _:Long) {
            if (startSel.isVisible) {
                start = openingHours.first + position
                max_dur = (start..openingHours.last).count()
                val pos = durSel.selectedItemPosition.coerceAtMost(max_dur - 1)
                durSel.adapter = ArrayAdapter(this@EditLesson, android.R.layout.simple_list_item_1, untilTheEnd())
                durSel.setSelection(pos)
                isFree()
                view?.apply {
                    if (!free) {
                        setBackgroundResource(R.color.colorPrimary)
                        background.setTint(Color.parseColor("#77FFFFFF"))
                        backgroundTintMode = PorterDuff.Mode.SRC_ATOP
                    }
                    else
                        view.background = null
                }
            }
        })
        durSel.onItemSelectedListener = SpinnerListener(fun(_:AdapterView<*>?, view: View?, _: Int, _: Long) {
            if(durSel.isVisible) {
                isFree()
                view?.apply {restrictedOrNot(view)}
            }
        })
        subSel.onItemSelectedListener = SpinnerListener(fun(_:AdapterView<*>?,_:View?, pos: Int, _:Long) {
            if (pos == subjects.size && subSel.isVisible) {
                val next = Intent(this@EditLesson, EditSubject::class.java)
                next.putExtra("subject-before-lesson", subjects.isEmpty())
                startActivityForResult(next, new_subject)
            }
        })
        abort.setOnClickListener { finish() }

        //potvrdenie a vytvorenie hodiny
        confirm.setOnClickListener {
            val time = start..start+durSel.selectedItemPosition
            val day = Day[daySel.selectedItemPosition]
            val array = data.getLessonsInRange(day, time, les?.id ?: -1)

            //Ak v danom case uz hodina existuje, bude nahradena. Uzivatel bude vsak vopred upozorneny
            if(array.isNotEmpty()) {
                val abbs = ArrayList<String>()
                for(arr in array) {
                    val lesson = data.getLesson(arr) ?: continue
                    lesson.subject.apply {
                        abbs.add("${lesson.time.first}:00 - ${lesson.time.last + 1}:00 $abb - $full")
                    }
                }
                Popups.alert(
                    this,
                    resources.getString(R.string.les_collision),
                    BtnFn(resources.getString(R.string.continue_), fun(){ insertLesson() }),
                    BtnFn(resources.getString(R.string.back))
                )
            }
            else insertLesson()
        }
    }

    private fun insertLesson(){
        data.insertLesson(Lesson(
            les?.id ?: -1,
            Day[daySel.selectedItemPosition],
            start .. start + durSel.selectedItemPosition,
            ScedSort[sortSel.selectedItemPosition],
            subjects[subSel.selectedItemPosition],
            locationSet.text.toString()
        ))
        //ak doslo k vytvoreniu 2 - viac po sebe iducich vyuc. hodin, kt. su duplicitami, stanu sa jednou
        data.putLessonsTogether()
        finish()
    }
    // Po vytv. noveho predmetu
    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (requestCode == new_subject && resultCode == Activity.RESULT_OK) {
            val id = intent?.getLongExtra("sub_id", -1) ?: -1
            if (id > -1) {
                subjects = data.loadSubjects()
                subSel.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, this.parseSubjects())
            }
            subSel.setSelection(getSubPos(id))
        }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.apply {
            putInt("day", daySel.selectedItemPosition)
            putInt("start", openingHours.first + startSel.selectedItemPosition)
            putInt("dur", durSel.selectedItemPosition + 1)
            putInt("sort", sortSel.selectedItemPosition)
            val pos = subSel.selectedItemPosition
            putLong("sub_id", if (pos !in subjects.indices) -1 else subjects[pos].id ?: -1)
            putString("room", locationSet.text.toString())
        }
    }

    // vytvori pole casov [ h ] v danom rozsahu
    private fun rangeToHours(): ArrayList<String> {
        val arr = ArrayList<String>()
        for (n in openingHours)
            arr.add("@@:00".replaceFirst('@', (n / 10).toString()[0]).replaceFirst('@', (n % 10).toString()[0]))
        return arr
    }
    // vrati pole hodin kolko ostava do zatvorenia budovy
    private fun untilTheEnd(): ArrayList<String> {
        val arr = ArrayList<String>()
        for (i in 0..openingHours.last - start)
            arr.add((i + 1).toString())
        return arr
    }
    private fun parseSubjects(): ArrayList<String> {
        val str = ArrayList<String>()
        for (s in subjects)
            str.add("${s.abb} - ${s.full}")
        str.add(resources.getString(R.string.nw))
        return str
    }
    private fun isFree(): Boolean {
        val day = daySel.selectedItemPosition
        val rng = start .. start + durSel.selectedItemPosition
        taken_over.visibility =
            if (data.isScedClear(day, rng, les?.id ?: -1)) View.GONE else View.VISIBLE
        free = taken_over.visibility == View.GONE
        return free
    }
    private fun getSubPos(n: Long):Int {
        if (n > 0) for(i in subjects.indices) if (subjects[i].id == n) return i % subjects.size
        return 0
    }

    /**
     * farebne oznacenie pozadia moznosti ktora by mohla ohrozit ostatne predmety
     */
    private fun restrictedOrNot(view: View) {
        with(view) {
            if (!free) setBackgroundResource(R.color.colorPrimary) else background = null
            if (background != null) with(background) {
                setTint(Color.parseColor("#77FFFFFF"))
                setTintMode(PorterDuff.Mode.SRC_ATOP)
            }
        }
    }
}
