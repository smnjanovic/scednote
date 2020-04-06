package sk.scednote

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class NoteList : AppCompatActivity() {

    private var needsUpdate : Boolean = false

    /*
        Vytvorenie tlacidla pre navrat na predoslu aktivitu
        Zdroj: https://devofandroid.blogspot.com/2018/03/add-back-button-to-action-bar-android.html
    */
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        needsUpdate = true
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.notelist)

        //vratit spat
        val actionbar = supportActionBar
        actionbar?.title = resources.getString(R.string.note_tit)
        actionbar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (!needsUpdate) {
            //ulozit grafiku rozvrhu
        }
    }
}
