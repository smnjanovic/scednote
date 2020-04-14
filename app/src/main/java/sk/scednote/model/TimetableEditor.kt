package sk.scednote.model

import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.TableLayout
import android.widget.TableRow
import androidx.core.view.updateLayoutParams
import kotlinx.android.synthetic.main.grid_cell.view.*
import sk.scednote.R
import sk.scednote.model.data.Ahsl
import sk.scednote.model.data.Day
import sk.scednote.model.data.Lesson
import sk.scednote.model.data.ScedSort
import java.util.*

class TimetableEditor(private val context: Context, private val table: TableLayout, background: View? = null, database: Database = Database(context)) {
    private val data = database
    private val groups = TreeMap<String, CellGroup>()
    private val tplCols = ArrayList<View>()
    init {
        val tg = Design.Target(context)
        groups[Design.BACKGROUND] = CellGroup(Design.BACKGROUND, tg.bg_b)
        groups[Design.TABLE_HEAD] = CellGroup(Design.TABLE_HEAD, tg.bg_h)
        groups[Design.PRESENTATIONS] = CellGroup(Design.PRESENTATIONS, tg.bg_p)
        groups[Design.COURSES] = CellGroup(Design.COURSES, tg.bg_c)
        groups[Design.FREE] = CellGroup(Design.FREE)
        background?.let { groups[Design.BACKGROUND]!! += background}
    }

    private inner class CellGroup(private val group: String, private val target: String? = null) {
        private val cells = ArrayList<View>()
        private var bg_hsl = Ahsl(35,0,0,0)
        private var fg_hsl = Ahsl(0,0,0,0)
        private var bg_hex = "#59000000"
        private var fg_hex = "#00000000"
        val size: Int get() = cells.size

        init {
            target?.let {
                bg_hsl = data.getColor(it)
                fg_hsl = Design.customizedForeground(bg_hsl)
                bg_hex = Design.hsl2hex(bg_hsl)
                fg_hex = Design.hsl2hex(fg_hsl)
            }
        }
        operator fun plusAssign(view: Any?) {
            if (view is View)
                cells.add(view)
        }
        operator fun get(n: Int) = cells[n]

        fun clear() {
            cells.clear()
        }
        //uchovat farbu v databaze. Nenastavitelne farby maju target null
        fun storeColor(ahsl: Ahsl) {
            target?.let {
                data.setColor(target, ahsl)
                bg_hsl = ahsl
                fg_hsl = Design.customizedForeground(ahsl)
                bg_hex = Design.hsl2hex(bg_hsl)
                fg_hex = Design.hsl2hex(fg_hsl)
            }
        }
        fun getHsl(): Ahsl {
            return bg_hsl
        }

        //prefarbit ramec
        fun recolor(ahsl: Ahsl? = null) {
            if (cells.size > 0) {
                val color = ahsl ?: bg_hsl
                val bg = Color.parseColor(Design.hsl2hex(color))
                val fg = Color.parseColor(Design.hsl2hex(Design.customizedForeground(color)))
                for (cell in cells) {
                    cell.setBackgroundColor(bg)
                    cell.cell_abb.setTextColor(fg)
                    cell.cell_room.setTextColor(fg)
                }
            }
        }
    }

    //vykreslenie tabulky
    fun fillTable() {
        table.removeAllViews()
        tplCols.clear()
        for (group in groups)
            if (group.value != groups[Design.BACKGROUND])
                group.value.clear()

        val range = data.getScedRange()
        if (fillTemplateColumns(range)) {
            fillHeader(range)
            for (d in Day.values()) {
                val dayRow = TableRow(context)
                val lessons = data.getScedule(d)

                if (lessons.isEmpty())
                    fillVoid(dayRow, range.count())
                else {
                    var next = range.first
                    for (les in lessons) {
                        next += fillVoid(dayRow, (next until les.time.first).count()) //volno pred hodinou
                        next += fillLesson(dayRow, les) //hodina
                    }
                    fillVoid(dayRow, (next .. range.last).count()) //volno do konca dna
                }
                table.addView(dayRow)
            }

            for (d in groups)
                d.value.recolor()
        }
    }
    private fun fillTemplateColumns(range: IntRange): Boolean {
        if (range == 0..0 || range.count() == 0) return false
        val tplRow = TableRow(context)
        for ( i in range) {
            with(giveCell(tplRow)) {
                cell.visibility = View.INVISIBLE
                cell_layout.visibility = View.GONE
                tplRow.addView(cell)
                tplCols.add(cell)
            }
        }
        table.addView(tplRow)
        return true
    }
    private fun fillHeader(range: IntRange) {
        val thead = TableRow(context)
        var next = range.first
        val step: Int

        with (range.count()) {
            step = when (true) {
                this in 1..7 -> 1
                this in 8..11 || this > 11 && this % 3 == 1 -> 2
                else -> 3
            }
        }

        while (next <= range.last) {
            with(giveCell(thead)) {
                cell.layoutParams = TableRow.LayoutParams().apply {
                    span = (range.last - next + 1).coerceAtMost(step)
                }
                cell_abb.textAlignment = View.TEXT_ALIGNMENT_VIEW_START
                cell_abb.text = next.toString()
                cell_room.visibility = View.GONE
                thead.addView(this)
                groups[Design.TABLE_HEAD]!! += this
            }
            next += step
        }
        table.addView(thead)
    }
    private fun fillVoid(row: TableRow, span: Int): Int {
        if (span > 0) {
            with(giveCell(row).cell) {
                groups[Design.FREE]!! += this
                row.addView(this)
                this.layoutParams = TableRow.LayoutParams().apply { this.span = span }
            }
        }
        return span
    }
    private fun fillLesson(row: TableRow, les: Lesson): Int {
        val dur = les.time.count()
        with(giveCell(row).cell) {
            groups[if (les.sort == ScedSort.COURSE) Design.COURSES else Design.PRESENTATIONS]!! += this
            row.addView(this)
            this.layoutParams = TableRow.LayoutParams().apply { span = dur }
            cell_abb.text = les.subject.abb
            cell_room.text = les.room
        }
        return dur
    }
    private fun giveCell(parent: TableRow):View {
        return LayoutInflater.from(parent.context).inflate(R.layout.grid_cell, parent, false)
    }

    //zmeria sirku tabulky a stlpcom v neviditelnom riadku nastavi rovnake sirky (poslednemu prida zvysok)
    fun scaleByWidth(): Boolean {
        val count = tplCols.size
        if (count == 0) return false // tabulka nie je ani vykreslena

        val singleWidth = table.measuredWidth / count
        val rightVoid = table.measuredWidth % count

        //rovnaka pevna sirka pre vsetky stlpce (posledny: +zvysok)
        for (col in tplCols)
            col.updateLayoutParams { width = singleWidth }
        tplCols.lastOrNull()?.apply { this.updateLayoutParams { width += rightVoid } }
        return true
    }

    fun recolor(color: Ahsl, target: String) {
        groups[target]?.recolor(color)
    }
    fun storecolor(color: Ahsl, target: String) {
        groups[target]?.storeColor(color)
    }

    fun getHsl(target: String): Ahsl? {
        return groups[target]?.getHsl()
    }

    //zavriet databazu v ramci triedy
    fun close() {
        data.close()
    }
}