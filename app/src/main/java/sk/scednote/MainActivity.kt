package sk.scednote

import android.app.AlertDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.BaseColumns
import android.provider.ContactsContract
import android.util.Log
import android.view.View
import com.facebook.stetho.Stetho
import kotlinx.android.synthetic.main.activity_main.*
import sk.scednote.model.Database
import sk.scednote.model.data.Ahsl
import sk.scednote.model.data.BtnFn
import sk.scednote.model.data.Day
import java.util.*

class MainActivity : AppCompatActivity() {
    //treba nastavit na true vzdy pri zaniku aktivity, ked viem ze moze dojst k zmene rozvrhu
    private var scedUpdate : Boolean = false

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
    }

    /**
     * funkcia priradena tlacidlam v xml. Kazde tlacidlo vo funkcii spusta inu aktivitu
     */
    fun leave(view: View) {
        startActivity(Intent(this, when(view) {
            shotBtn -> Screenshot::class.java
            noteBtn -> NoteList::class.java
            scedBtn -> Scedule::class.java
            else -> return
        }))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Stetho.initializeWithDefaults(this);
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (!scedUpdate) {
            //ulozit nastavenie grafiky rozvrhu
        }
    }

    override fun onResume() {
        super.onResume()
        val data = Database(this)
        val arr = data.getLessonsInRange(Day[0], 10..16)
    }
}
