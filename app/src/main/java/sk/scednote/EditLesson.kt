package sk.scednote

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.les_edit.*
import sk.scednote.lists.TxtValid
import sk.scednote.model.Database
import sk.scednote.model.Popups
import sk.scednote.model.data.*

class EditLesson : AppCompatActivity() {
    companion object {
        private val new_subject = 1
        private val openingHours = 7..21
    }
    private lateinit var data: Database
    private var subjects = ArrayList<Subject>()
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
        free = notTakenOver()
        les = data.getLesson(intent.getLongExtra("les_id", -1))

        Log.d("moriak", "${if (les != null) les!!.id else "null"}")

        //tlacidlo spat + nazov
        val actionBar = supportActionBar
        actionBar?.setTitle(if (les != null) R.string.les_edit_tit else R.string.les_new_tit)
        actionBar?.setDisplayHomeAsUpEnabled(true)

        subjects = data.loadSubjects()

        //vyplnit selekty
        daySel.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, Day.getTitles(this))
        startSel.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, rangeToHours())
        durSel.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, untilTheEnd())
        subSel.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, parseSubjects())
        sortSel.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, ScedSort.getSorts(this))

        //vyber moznosti
        daySel.setSelection(savedInstanceState?.getInt("day") ?: les?.day?.position ?: intent.getIntExtra("day_num", 0))
        startSel.setSelection(savedInstanceState?.getInt("start") ?: (les?.time?.first ?: openingHours.first) - openingHours.first)
        durSel.setSelection(savedInstanceState?.getInt("dur") ?: les?.time?.count() ?: 0)
        subSel.setSelection(getSubPos(savedInstanceState?.getLong("sub_id") ?: les?.subject?.id ?: 0))
        locationSet.setText(savedInstanceState?.getString("room") ?: les?.room ?: "")

        //udalosti k selektom
        daySel.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                free = notTakenOver()
            }
        }
        startSel.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            //koniec vyucovacej hodiny nesmie prekrocit casovu hranicu openingHours
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                start = openingHours.first + position
                max_dur = (start..openingHours.last).count()
                val pos = durSel.selectedItemPosition.coerceAtMost(max_dur - 1)
                durSel.adapter = ArrayAdapter<String>(this@EditLesson, android.R.layout.simple_list_item_1, untilTheEnd())
                durSel.setSelection(pos)
                free = notTakenOver()
            }
        }
        durSel.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                free = notTakenOver()
            }
        }
        subSel.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position == subjects.size) {
                    val next = Intent(this@EditLesson, EditSubject::class.java)
                    next.putExtra("subject-before-lesson", subjects.isEmpty())
                    startActivityForResult(next, new_subject)
                }
            }
        }
        locationSet.addTextChangedListener(TxtValid(this, "[a-zA-ZÀ-ž0-9]", 0..9, locationSet, confirm))
        abort.setOnClickListener { finish() }

        //potvrdenie a vytvorenie hodiny
        confirm.setOnClickListener {
            val time = start..start+durSel.selectedItemPosition
            val day = Day[daySel.selectedItemPosition]
            val array = data.getLessonsInRange(day, time)

            //ak je cas vyhradeny pre cosi ine, upozorni uzivatela a navrhne uvolnit cas vymazom hodiny
            if(array.isNotEmpty()) {
                val abbs = ArrayList<String>()
                for(arr in array) {
                    val lesson = data.getLesson(arr) ?: continue
                    lesson.subject?.apply {
                        abbs.add("${lesson.time.first}:00 - ${lesson.time.last + 1}:00 $abb - $full")
                    }
                }
                Popups.alert(
                    this,
                    resources.getString(R.string.les_collision).replace("{list}", abbs.joinToString("\n")),
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
        finish()
    }

    /**
     * Po vytv. noveho predmetu k novej hodine
     */
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
            val pos = subSel.selectedItemPosition
            putLong("sub_id", if (pos !in subjects.indices) -1 else subjects[pos].id ?: -1)
            putString("room", locationSet.text.toString())
        }
    }

    /**
     * vytvori pole casov [ h ] v danom rozsahu
     */
    private fun rangeToHours(): ArrayList<String> {
        val arr = ArrayList<String>()
        for (n in openingHours)
            arr.add("@@:00".replaceFirst('@', (n / 10).toString()[0]).replaceFirst('@', (n % 10).toString()[0]))
        return arr
    }

    /**
     * vrati pole hodin kolko ostava do zatvorenia budovy
     */
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

    private fun notTakenOver(): Boolean {
        taken_over.visibility = if (data.isScedClear(daySel.selectedItemPosition, start .. start + durSel.selectedItemPosition)) View.GONE else View.VISIBLE
        return taken_over.visibility == View.GONE
    }

    private fun getSubPos(n: Long):Int {
        if (n > 0) for(i in subjects.indices) if (subjects[i].id == n) return i % subjects.size
        return 0
    }

}
