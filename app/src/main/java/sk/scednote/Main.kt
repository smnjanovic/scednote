package sk.scednote

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.facebook.stetho.Stetho
import kotlinx.android.synthetic.main.activity_main.*
import sk.scednote.model.TimetableEditor

class Main : AppCompatActivity() {
    private lateinit var editor: TimetableEditor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Stetho.initializeWithDefaults(this) //pristup k databaze "chrome://inspect/#devices"
        editor = TimetableEditor(this, timetable)
    }

    override fun onStart() {
        super.onStart()
        editor.fillTable()
    }

    override fun onResume() {
        super.onResume()
        /* upravi sirky stlpcov tak, aby respektovali minimalnu dlzku (aby po 8. hodine nenasledovala
           hned 14. ak by bol medzi nimi prazdny obsah v kazdom riadku) */
        Handler().postDelayed({ editor.scaleByWidth() }, 100)
    }

    override fun onDestroy() {
        editor.close() // zavrie databazu v rozbehnutej instancii
        super.onDestroy()
    }

    //funkcia priradena tlacidlam v xml. Kazde tlacidlo vo funkcii spusta inu aktivitu
    fun leave(view: View) {
        startActivity(Intent(this, when(view) {
            shotBtn -> Screenshot::class.java
            noteBtn -> NoteList::class.java
            scedBtn -> Scedule::class.java
            else -> return
        }))
    }
}

