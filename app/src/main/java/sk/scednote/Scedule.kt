/**
 * Array Adapter podľa https://www.youtube.com/watch?v=Jo6Mtq7zkkg
 */

package sk.scednote

import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.scedule.*
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import sk.scednote.lists.ScedAdapter
import sk.scednote.model.Database
import sk.scednote.model.data.Day
import java.lang.NullPointerException

class Scedule : AppCompatActivity() {
    private val db = Database(this)
    private lateinit var lesAdapt: ScedAdapter
    private var dayTab: Day? = null

    fun setUpRecycleView(ctx: Context) {
        lesAdapt = ScedAdapter(ctx)
        scedList.apply {
            layoutManager = LinearLayoutManager(ctx)
            adapter = lesAdapt
        }
    }

    /*
        Vytvorenie tlacidla pre navrat na predoslu aktivitu
        Zdroj: https://devofandroid.blogspot.com/2018/03/add-back-button-to-action-bar-android.html
    */
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.scedule)
        setUpRecycleView(this)

        //vratit spat
        val actionbar = supportActionBar
        actionbar?.setTitle(R.string.sced_tit)
        actionbar?.setDisplayHomeAsUpEnabled(true)


        //prepinanie dni
        for (d in arrayOf(Mon, Tue, Wed, Thu, Fri))
            d.setOnClickListener { setActive(it) }

        addLesson.setOnClickListener {
            if (dayTab != null)
                startActivity(Intent(this, EditLesson::class.java).apply { putExtra("day_num", dayTab) })
        }

    }

    override fun onDestroy() {
        lesAdapt.closeDb()
        super.onDestroy()
    }

    private fun setActive (day: View?) {
        val days = arrayOf<View>(Mon, Tue, Wed, Thu, Fri)
        for (i in days) {
            if (i != day) {
                i.background.setTintList(null)
            }
            else {
                dayTab = Day[days.indexOf(day)]
                lesAdapt.loadData(dayTab)
            }
        }
        day?.background?.setTint(Color.parseColor("#f5ffffff"))
    }
}
