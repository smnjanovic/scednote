package sk.scednote.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.PersistableBundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.RotateAnimation
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.facebook.stetho.Stetho
import kotlinx.android.synthetic.main.activity_main.*
import sk.scednote.*
import sk.scednote.adapters.RecentNotesAdapter
import sk.scednote.fragments.Confirm
import sk.scednote.model.Database
import sk.scednote.scedule.TimetableBuilder

class Main : AppCompatActivity() {
    companion object {
        private const val CLEAN_UP = "CLEAN_UP"
    }
    private lateinit var scedule: TimetableBuilder
    private lateinit var data: Database
    private lateinit var noteAdapt: RecentNotesAdapter

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.cleanBtn -> {
                val confirm = Confirm.newInstance(resources.getString(R.string.clean_up_msg))
                confirm.setOnConfirm { _, _ -> cleanUp() }
                confirm.show(supportFragmentManager,
                    CLEAN_UP
                )
            }
            R.id.noteBtn -> startActivity(Intent(this, NoteList::class.java))
            R.id.subBtn -> startActivity(Intent(this, SubList::class.java))
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Stetho.initializeWithDefaults(this) //sluzi na pristup k databaze pomocou adresy: chrome://inspect/#devices

        data = Database()
        scedule = TimetableBuilder(timetable, frame, data)

        /**
         * https://stackoverflow.com/questions/11026234/how-to-check-if-the-current-activity-has-a-dialog-in-front
         */
        (supportFragmentManager.findFragmentByTag(CLEAN_UP) as Confirm?)?.setOnConfirm { _, _ -> cleanUp() }
        shotBtn.setOnClickListener { startActivity(Intent(this, Screenshot::class.java)) }
        scedBtn.setOnClickListener { startActivity(Intent(this, Scedule::class.java)) }
        reloader.setOnClickListener {
            RotateAnimation(0F, 360F, it.width/2F, it.height/2F).apply {
                this.duration = 750
                it.startAnimation(this)
            }
            Handler().postDelayed({
                noteAdapt.reload()
                empty.visibility = if (noteAdapt.itemCount > 0) View.GONE else View.VISIBLE
            }, 250)
        }


        recent.apply {
            layoutManager = LinearLayoutManager(this@Main)
            noteAdapt = RecentNotesAdapter(data, savedInstanceState)
            adapter = noteAdapt
            noteAdapt.setOnNoteNavigate { category, id ->
                startActivity(Intent(this@Main, NoteList::class.java).apply {
                    putExtra(NoteList.CATEGORY, category)
                    putExtra(NoteList.TARGET_ID, id)
                })
            }
            noteAdapt.setOnNotifyIfEmpty {
                if (noteAdapt.itemCount == 0) empty.visibility = View.GONE else View.VISIBLE
            }
        }
    }

    override fun onRestart() {
        super.onRestart()
        noteAdapt.reload()
        scedule.updateColors()
    }

    override fun onStart() {
        super.onStart()
        scedule.fillTable()
        scedule.setOnLessonClick {
            startActivity(Intent(this@Main, EditLesson::class.java).apply {
                putExtra(EditLesson.INTENT_LESSON, it)
            })
        }
    }

    override fun onResume() {
        super.onResume()
        shotBtn.visibility = if (scedule.empty) View.GONE else View.VISIBLE
        empty.visibility = if (noteAdapt.itemCount > 0) View.GONE else View.VISIBLE
        /**
         * upravi sirky stlpcov tak, aby respektovali minimalnu dlzku (aby po 8. hodine nenasledovala
         * hned 14. ak by bol medzi nimi prazdny obsah v kazdom riadku)
         */
        Handler().postDelayed({ scedule.scaleByWidth(timetable.measuredWidth) }, 100)
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        noteAdapt.storeBackup(outState)
    }

    override fun onDestroy() {
        scedule.close() // zavrie databazu v rozbehnutej instancii
        super.onDestroy()
    }

    private fun cleanUp() {
        data.removeObsoleteSubjects()
        Toast.makeText(this, resources.getString(R.string.obsolete_subjects_gone), Toast.LENGTH_SHORT).show()
    }
}

