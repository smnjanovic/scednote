package sk.scednote.scedule

import CellGroup
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.updateLayoutParams
import kotlinx.android.synthetic.main.tbl_cell.view.*
import sk.scednote.R
import sk.scednote.ScedNoteApp
import sk.scednote.model.Database
import sk.scednote.model.Design
import sk.scednote.model.data.Ahsl
import sk.scednote.model.data.Lesson
import sk.scednote.model.data.ScedSort
import java.util.*

class TimetableBuilder(private val table: LinearLayout, background: View? = null, database: Database = Database()) {
    private val context = ScedNoteApp.ctx
    private val data = database
    private val groups = TreeMap<String, CellGroup>().apply {
        this[Design.bg_b] = CellGroup(Design.bg_b, data.getColor(Design.bg_b))
        this[Design.bg_h] = CellGroup(Design.bg_h, data.getColor(Design.bg_h))
        this[Design.bg_p] = CellGroup(Design.bg_p, data.getColor(Design.bg_p))
        this[Design.bg_c] = CellGroup(Design.bg_c, data.getColor(Design.bg_c))
        this[Design.FREE] = CellGroup(null, Ahsl(35, 0, 0, 0))
        background?.let { this[Design.bg_b]!! += background }
    }
    //zotriedene stlpce podla sirok
    private val colSizeGroup = TreeMap<Int, ArrayList<ViewGroup>>()
    //udalost po kliknuti na hodinu
    private var editLesson: (Long)->Unit = fun(_){}
    //handler udalosti po kliknuti na hodinu
    private val onLessonClick = View.OnClickListener {
        if (it.tag is Long)
            editLesson(it.tag as Long)
    }
    val empty get() = groups[Design.bg_p]!!.empty && groups[Design.bg_c]!!.empty

    // definovanie vlastnej funkcie, ktora sa vykona po nejakej udalosti
    fun setOnLessonClick(fn: (Long)->Unit) { editLesson = fn }
    //vykreslenie tabulky1
    fun updateColors() {
        for ((target, group) in groups) {
            if (target != Design.FREE)
                group.updateColors(data.getColor(target))
        }
    }
    fun fillTable() {
        table.removeAllViews()
        for ((_, group) in groups)
            if (group != groups[Design.bg_b])
                group.clear()

        val range = data.getScedRange()
        if (range != 0..0 || range.count() > 0) {
            fillHeader(range)
            fillContent(range)
            for ((_, value) in groups)
                value.recolor()
        }
    }
    private fun addContent(row: ViewGroup, cell: LinearLayout, target: String, duration: Int) {
        row.addView(cell)
        groups[target]!! += cell
        colSizeGroup[duration]?.add(cell)
        if (colSizeGroup[duration] == null)
            colSizeGroup[duration] = ArrayList<ViewGroup>().also { it.add(cell) }
    }
    private fun fillHeader(range: IntRange) {
        val thead = LinearLayout(context, null, LinearLayout.HORIZONTAL)
        thead.gravity = Gravity.CENTER
        var next = range.first
        val step: Int

        val fullDuration = range.count()
        step = when (true) {
            fullDuration in 1..7 -> 1
            fullDuration in 8..11 || fullDuration > 11 && fullDuration % 3 == 1 -> 2
            else -> 3
        }

        while (next <= range.last) {
            val w = (range.last - next + 1).coerceAtMost(step)
            with(giveCell(thead)) {
                cell_abb.textAlignment = View.TEXT_ALIGNMENT_VIEW_START
                cell_abb.text = next.toString()
                cell_room.visibility = View.GONE
                addContent(thead, this, Design.bg_h, w)
            }
            next += step
        }
        table.addView(thead)
    }
    private fun fillContent(range: IntRange) {
        if (range.count() == 0) return
        //nekontrolujem poradie hodin. SQL ich nacitava v poradi podla stlpcov: den, hodina
        val lessons = data.getScedule()
        val days = arrayOf(
            LinearLayout(context, null, LinearLayout.HORIZONTAL),
            LinearLayout(context, null, LinearLayout.HORIZONTAL),
            LinearLayout(context, null, LinearLayout.HORIZONTAL),
            LinearLayout(context, null, LinearLayout.HORIZONTAL),
            LinearLayout(context, null, LinearLayout.HORIZONTAL))
        var dayOfPrevious = 0
        var hourAfterPrevious = range.first

        for (l in lessons) {
            //vyplnanie medzier medzi predoslou a sucasnou hodinou
            if (l.day.position - dayOfPrevious > 0) {
                //volny zvysok dna
                fillVoid(days[dayOfPrevious], (hourAfterPrevious..range.last).count())

                // volne cele dni pred sucasnou hodinou
                for (d in dayOfPrevious + 1 until l.day.position)
                    fillVoid(days[d], range.count())

                //volno pred zaciatkom 1. hodiny
                fillVoid(days[l.day.position], (range.first until l.time.first).count())
            }
            //medzera medzi hodinami v rovnakom dni
            else
                fillVoid(days[dayOfPrevious], (hourAfterPrevious until l.time.first).count())

            //pridanie hodiny
            dayOfPrevious = l.day.position
            fillLesson(days[dayOfPrevious], l)
            hourAfterPrevious = l.time.last + 1
        }

        //vyplnit prazdne riadky chybajuce do konca tyzdna
        fillVoid(days[dayOfPrevious], (hourAfterPrevious .. range.last).count())
        if (++dayOfPrevious < 5)
            for (d in dayOfPrevious .. 4)
                fillVoid(days[d], range.count())

        //povkladat riadky do tabulky
        for (r in days) {
            table.addView(r)
            r.gravity = Gravity.CENTER
        }
    }
    private fun fillVoid(row: LinearLayout, span: Int) {
        if (span > 0) {
            val view = giveCell(row)
            addContent(row, view, Design.FREE, span)
        }
    }
    private fun fillLesson(row: LinearLayout, les: Lesson) {
        val time = les.time.count()
        val target = if (les.sort == ScedSort.COURSE) Design.bg_c else Design.bg_p
        val cell = giveCell(row)

            cell.cell_abb.text = les.subject.abb
            cell.cell_room.text = les.room
            cell.tag = les.id
            cell.setOnClickListener(onLessonClick)
            addContent(row, cell, target, time)
    }
    private fun giveCell(parent: ViewGroup):LinearLayout {
        return LayoutInflater.from(parent.context).inflate(R.layout.tbl_cell, parent, false).cell_layout
    }

    //zmeria sirku tabulky a stlpcom v neviditelnom riadku nastavi rovnake sirky (poslednemu prida zvysok)
    fun scaleByWidth(tableWidth: Int) {
        val cellWidth = tableWidth / data.getScedRange().count()
        val wrap = LinearLayout.LayoutParams.WRAP_CONTENT
        for ((size, group) in colSizeGroup)
            for (cell in group)
                cell.layoutParams = LinearLayout.LayoutParams(wrap, wrap).apply { width = cellWidth * size }
    }
    fun recolor(color: Ahsl, target: String) {
        groups[target]?.recolor(color)
    }
    fun storecolor(color: Ahsl, target: String) {
        groups[target]?.let {
            data.setColor(target, color)
            it.storeColor(color)
        }
    }
    fun getHsl(target: String): Ahsl? {
        return groups[target]?.getHsl()
    }
    //zavriet databazu v ramci triedy
    fun close() {
        data.close()
    }
}