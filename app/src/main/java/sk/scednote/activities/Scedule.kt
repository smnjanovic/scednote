/**
 * Array Adapter podľa https://www.youtube.com/watch?v=Jo6Mtq7zkkg
 */

package sk.scednote.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.scedule.*
import sk.scednote.R
import sk.scednote.activities.EditLesson
import sk.scednote.adapters.ScedAdapter
import sk.scednote.model.data.Day
import kotlin.properties.Delegates


class Scedule : AppCompatActivity() {
    companion object {
        private const val recV = "recycler_view"
        private const val recP = "recycler_pos"
        private const val higL = "highlight_btn"
        private const val DAYS = "days"
        private const val DAY_TAB = "day_tab"
        private const val DAY_SCROLL = "day_scr"
        private const val ADDED_CHANGED = 1000
    }
    private lateinit var lesAdapt: ScedAdapter
    private lateinit var btns: Array<View>
    private lateinit var day: Day
    private var highlightColor by Delegates.notNull<Int>()

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
        setUpRecycleView(savedInstanceState)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        dayTabs.setOnScrollChangeListener { _, scrollX, _, _, _ ->
            with(getSharedPreferences(DAYS, Context.MODE_PRIVATE).edit()) {
                /*
                 * vdaka rovnakym rozmerom poloziek si staci vypocitat percentualnu poziciu scroll.
                 * Percentuálnu preto, lebo po otočení displeja má layout inú orientáciu toku dát.
                 */
                putFloat(DAY_SCROLL, scrollX.toFloat() / day_tab_layout.width.toFloat())
                apply()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ADDED_CHANGED && resultCode == Activity.RESULT_OK && data != null) {
            btns[data.getIntExtra(EditLesson.INTENT_DAY, 0)].performClick()
        }
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
                startActivityForResult(Intent(this, EditLesson::class.java).apply {
                    putExtra(EditLesson.INTENT_DAY, day.position)
                }, ADDED_CHANGED)
            }
        }
    }

    /**
     * RecyclerView - vytvorenie alebo obnovenie zoznamu
     * inspiracia: https://www.youtube.com/watch?v=_jU7vMw3Wcw
     */
    private fun setUpRecycleView(saved: Bundle? = null) {
        btns = arrayOf(Mon, Tue, Wed, Thu, Fri)
        day = Day[saved?.getInt(higL) ?: getSharedPreferences(
            DAYS, Context.MODE_PRIVATE).getInt(
            DAY_TAB, 0)]
        highlightColor = Color.parseColor("#f5ffffff")

        lesAdapt = ScedAdapter(saved?.getParcelableArrayList(recV))
        lesAdapt.onUpdate {
            try {
                startActivityForResult(Intent(this, EditLesson::class.java).apply {
                    putExtra(EditLesson.INTENT_LESSON, lesAdapt.getItemId((it.tag as ScedAdapter.ScedHolder).adapterPosition))
                    putExtra(EditLesson.INTENT_DAY, day.position)
                }, ADDED_CHANGED)
            }
            catch (ex: ClassCastException) {throw ClassCastException(ex.message + "Item is missing a tag!")}
        }
        lesAdapt.onDelete {
            try { lesAdapt.removeItem((it.tag as ScedAdapter.ScedHolder).adapterPosition) }
            catch (ex: ClassCastException) {throw ClassCastException(ex.message + "Item is missing a tag!")}
        }
        scedList.apply {
            layoutManager = LinearLayoutManager(this@Scedule)
            adapter = lesAdapt
            saved?.let { layoutManager?.onRestoreInstanceState(it.getParcelable(recP)) }
        }
        with(btns[day.position]){ saved?.let { changeHighlight(this) } ?: onClick(this) }
    }

    private fun changeHighlight(view: View) {
        if (view !in btns) return
        for (d in btns)
            with(d.background) { if (d != view) setTintList(null) else setTint(highlightColor) }
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