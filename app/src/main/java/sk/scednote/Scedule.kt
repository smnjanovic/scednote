/**
 * Array Adapter podľa https://www.youtube.com/watch?v=Jo6Mtq7zkkg
 */

package sk.scednote

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.scedule.*
import sk.scednote.adapters.ScedAdapter
import sk.scednote.model.data.Day


class Scedule : AppCompatActivity() {
    companion object {
        private const val recV = "recycler_view"
        private const val recP = "recycler_pos"
        private const val higL = "highlight_btn"
        private const val DAYS = "days"
        private const val DAY_TAB = "day_tab"
        private const val DAY_SCROLL = "day_scr"
    }
    private lateinit var lesAdapt: ScedAdapter
    private lateinit var btns: Array<View>
    private lateinit var day: Day

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
        setUpRecycleView(this, savedInstanceState)

        supportActionBar?.also {
            it.setTitle(R.string.sced_tit)
            it.setDisplayHomeAsUpEnabled(true)
        }

        dayTabs.setOnScrollChangeListener(object : View.OnScrollChangeListener {
            override fun onScrollChange(v: View?, scrollX: Int, scrollY: Int, oldScrollX: Int, oldScrollY: Int) {
                with(getSharedPreferences(DAYS, Context.MODE_PRIVATE).edit()) {
                    putFloat(DAY_SCROLL, scrollX.toFloat() / day_tab_layout.width.toFloat())
                    commit()
                }
            }

        })
    }
    override fun onRestart() {
        super.onRestart()
        onClick(btns[day.position])
    }

    override fun onResume() {
        super.onResume()
        //Vypocet scroll pozicie oneskorim az o 0 ms ;-). Vtedy je uz GUI viditelne.
        Handler().postDelayed({ scrollToHighLightedTab() }, 0)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelableArrayList(recV, lesAdapt.items)
        outState.putParcelable(recP, scedList.layoutManager?.onSaveInstanceState())
        outState.putInt(higL, day.position)
    }
    // zapamatat posledny zvoleny den
    override fun onStop() {
        super.onStop()
        with(getSharedPreferences(DAYS, Context.MODE_PRIVATE).edit()) {
            putInt(DAY_TAB, day.position)
            commit()
        }
    }
    override fun onDestroy() {
        lesAdapt.closeDb()
        super.onDestroy()
    }

    fun onClick (view: View) {
        when(view) {
            in btns -> {
                changeHighlight(view)
                lesAdapt.loadData(Day[btns.indexOf(view)])
            }
            addLesson -> {
                startActivity(Intent(this, EditLesson::class.java).apply { putExtra("day_num", day.position) })
            }
        }
    }

    /**
     * RecyclerView - vytvorenie alebo obnovenie zoznamu
     * inspiracia: https://www.youtube.com/watch?v=_jU7vMw3Wcw
     */
    private fun setUpRecycleView(ctx: Context, saved: Bundle? = null) {
        btns = arrayOf(Mon, Tue, Wed, Thu, Fri)
        day = Day[saved?.getInt(higL) ?: getSharedPreferences(DAYS, Context.MODE_PRIVATE).getInt(DAY_TAB, 0)]
        lesAdapt = ScedAdapter(ctx, saved?.getParcelableArrayList(recV))
        scedList.apply {
            layoutManager = LinearLayoutManager(ctx)
            adapter = lesAdapt
            saved?.let {
                layoutManager?.onRestoreInstanceState(saved.getParcelable(recP))
            }
        }
        with(btns[day.position]){
            if (saved == null) onClick(this) else changeHighlight(this)
        }
    }
    private fun changeHighlight(view: View) {
        if (view !in btns) return
        for (d in btns)
            with(d.background) {
                if (d != view) setTintList(null) else setTint(Color.parseColor("#f5ffffff"))
            }
        day = Day[btns.indexOf(view)]
        scrollToHighLightedTab()
    }

    private fun scrollToHighLightedTab() {
        val view = btns[day.position]
        if (dayTabs.width < day_tab_layout.width) {
            if (dayTabs.scrollX > view.left)
                dayTabs.scrollX = view.left - view.width/2
            if(view.right > dayTabs.width + dayTabs.scrollX)
                dayTabs.scrollX = view.right - dayTabs.width + view.width/2
        }
        if (dayTabs.height < day_tab_layout.height) {
            if (dayTabs.scrollY > view.top)
                dayTabs.scrollY = view.top - view.height/2
            if(view.bottom > dayTabs.height + dayTabs.scrollY)
                dayTabs.scrollY = view.bottom - dayTabs.height + view.height/2
        }
    }

}
