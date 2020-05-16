package sk.scednote.activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.RotateAnimation
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.facebook.stetho.Stetho
import kotlinx.android.synthetic.main.activity_main.*
import sk.scednote.R
import sk.scednote.ScedNoteApp
import sk.scednote.adapters.RecentNotesAdapter
import sk.scednote.fragments.Confirm
import sk.scednote.scedule.TimetableBuilder

/**
 * Hlavná aktivita zobrazí rozvrh a úlohy s blížiacim sa termínom
 */
class Main : ShakeCompatActivity() {
    companion object {
        private const val CLEAN_UP = "CLEAN_UP"
    }
    private lateinit var scedule: TimetableBuilder
    private lateinit var noteAdapt: RecentNotesAdapter

    /**
     * Vytvorenie menu
     * @param menu Menu do ktorého sa vložia položky menu
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    /**
     * Reakcia jednotlivých položiek menu na ich voľbu
     * @param item Položka
     * @return [Boolean] True
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.cleanBtn -> {
                val confirm = Confirm.newInstance(resources.getString(R.string.clean_up_msg))
                confirm.setOnConfirm { _, _ -> cleanUp() }
                confirm.show(supportFragmentManager, CLEAN_UP)
            }
            R.id.noteBtn -> startActivity(Intent(this, NoteList::class.java))
            R.id.subBtn -> startActivity(Intent(this, SubList::class.java))
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    /**
     * Príprava aktivity
     * @param savedInstanceState uložená záloha pri obnove
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        parent?.finish()
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        scedule = TimetableBuilder(timetable, frame)
        //sluzi na pristup k databaze pomocou adresy: chrome://inspect/#devices
        Stetho.initializeWithDefaults(this)

        //https://stackoverflow.com/questions/11026234/how-to-check-if-the-current-activity-has-a-dialog-in-front
        (supportFragmentManager.findFragmentByTag(CLEAN_UP) as Confirm?)?.setOnConfirm { _, _ -> cleanUp() }
        shotBtn.setOnClickListener { startActivity(Intent(this, Screenshot::class.java)) }
        scedBtn.setOnClickListener { startActivity(Intent(this, Scedule::class.java)) }
        reloader.setOnClickListener {
            RotateAnimation(0F, 360F, it.width/2F, it.height/2F).apply {
                this.duration = 750
                it.startAnimation(this)
            }
            it.postOnAnimation {
                noteAdapt.reload()
                empty.visibility = if (noteAdapt.itemCount > 0) View.GONE else View.VISIBLE
            }
        }

        recentList.apply {
            layoutManager = LinearLayoutManager(this@Main)
            noteAdapt = RecentNotesAdapter(savedInstanceState)
            adapter = noteAdapt
            noteAdapt.setOnNoteNavigate { category, id ->
                startActivity(Intent(this@Main, NoteList::class.java).apply {
                    putExtra(NoteList.CATEGORY, category)
                    putExtra(NoteList.TARGET_ID, id)
                })
            }
        }
    }

    /**
     * Znovunačítanie farieb tabuľky rozvrhu a zoznamu úloh v priebehu týždňa
     */
    override fun onRestart() {
        super.onRestart()
        noteAdapt.reload()
        scedule.accessNewColors()
    }

    /**
     * Načítanie obsahu tabuľky a nastavenie veľkosti stĺpcov podľa jej šírky
     * Kliknutie na vyučovacie hodiny umožní vykonať úpravu hodiny
     */
    override fun onStart() {
        super.onStart()
        scedule.fillTable()
        scedule.setOnLessonClick {
            startActivity(Intent(this@Main, EditLesson::class.java).apply {
                putExtra(EditLesson.INTENT_LESSON, it)
            })
        }
        shotBtn.visibility = if (scedule.empty) View.GONE else View.VISIBLE
        empty.visibility = if (noteAdapt.itemCount > 0) View.GONE else View.VISIBLE
        frame.post { scedule.scaleByWidth(timetable.measuredWidth) }
    }

    /**
     * Zálohovanie dát
     * @param outState balík zálohy
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        noteAdapt.storeBackup(outState)
    }

    /**
     *  zavretie bežiacej databázy
     */
    override fun onDestroy() {
        scedule.close()
        noteAdapt.close()
        super.onDestroy()
    }

    private fun cleanUp() {
        ScedNoteApp.database.removeObsoleteData()
        Toast.makeText(this, resources.getString(R.string.obsolete_subjects_gone), Toast.LENGTH_SHORT).show()
    }
}

