/**
 * Array Adapter podľa https://www.youtube.com/watch?v=Jo6Mtq7zkkg
 */

package sk.scednote.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.scedule.*
import sk.scednote.R
import sk.scednote.adapters.ScedAdapter
import sk.scednote.model.Day
import kotlin.properties.Delegates

/**
 * Tu sa vykonava predpriprava pozadia, prefarbenie buniek tabulky a pozadia,
 * vkladanie a transformacia obrazku na pozadi, a vysledny obrazok mozno dat na pozadie
 */

class Scedule : ShakeCompatActivity() {
    companion object {
        private const val recV = "recycler_view"
        private const val recP = "recycler_pos"
        private const val higL = "highlight_btn"
        const val DAYS = "days"
        private const val DAY_TAB = "day_tab"
        private const val DAY_SCROLL = "day_scr"
        private const val ADDED_CHANGED = 1000
    }
    private lateinit var lesAdapt: ScedAdapter
    private lateinit var btns: Array<View>
    private lateinit var day: Day
    private var highlightColor by Delegates.notNull<Int>()

    /**
     *  Vytvorenie tlacidla pre navrat na predoslu aktivitu
     *  Zdroj: https://devofandroid.blogspot.com/2018/03/add-back-button-to-action-bar-android.html
     *  @return true
     */
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    /**
     * Príprava aktivity
     * @param savedInstanceState záloha z predošlej aktivity zrušenej systémom
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.scedule)

        //vopred znamy den, ktory sa zobrazi
        intent?.extras?.getInt(DAYS)?.let {
            val size = Day.values().size
            with (getSharedPreferences(DAYS, Context.MODE_PRIVATE).edit()) {
                putInt(DAY_TAB, (it + size) % size)
                apply()
            }
            intent.removeExtra(DAYS)
        }

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

    /**
     * Výsledky iných aktivít
     * @param requestCode kód žiadosti
     * @param resultCode výsledný kód
     * @param data výsledné dáta
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ADDED_CHANGED && resultCode == Activity.RESULT_OK && data != null)
            btns[data.getIntExtra(EditLesson.INTENT_DAY, 0)].performClick()
        else lesAdapt.loadData(day)
    }

    /**
     * Otvorenie aktivity s presnym urcenim dna
     */
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.extras?.getInt(DAYS)?.let {
            val size = Day.values().size
            changeHighlight(btns[(it + size) % size])
            intent.removeExtra(DAYS)
        }
    }

    /**
     * Pokračovanie
     */
    override fun onResume() {
        super.onResume()
        //Vypocet scroll pozicie oneskorim az o 0 ms ;-). Vtedy je uz GUI viditelne.
        dayTabs.post { scrollToHighLightedTab() }
    }

    /**
     * @param outState Zdroj zálohy
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelableArrayList(recV, lesAdapt.items)
        outState.putParcelable(recP, scedList.layoutManager?.onSaveInstanceState())
        outState.putInt(higL, day.position)
    }

    /**
     * Zapamätať si naposledy zobrazený deň
     */
    override fun onStop() {
        super.onStop()
        with(getSharedPreferences(DAYS, Context.MODE_PRIVATE).edit()) {
            putInt(DAY_TAB, day.position)
            commit()
        }
    }

    /**
     * Prepinanie sa medzi dnami alebo pridanie hodiny
     * @view Tlačidlo na ktoré sa kliklo
     */
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
     * @param saved záloha súborov
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
            lesAdapt.removeItem((it.tag as ScedAdapter.ScedHolder).adapterPosition)
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
        dayTabs.post {
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
}
