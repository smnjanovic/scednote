package sk.scednote.scedule

import android.graphics.*
import android.util.Log
import sk.scednote.model.Database
import sk.scednote.model.Design
import sk.scednote.model.data.Ahsl
import sk.scednote.model.data.Lesson
import sk.scednote.model.data.ScedSort

class TimetableImage {
    private val data = Database()
    private val oneColWidth: Int
    private val width: Int
    private val headingHeight = Design.dpToPx(20)
    private val dayHeight = Design.dpToPx(35)
    private val height: Int = headingHeight + dayHeight * 5
    private val bitmap: Bitmap
    private val canvas: Canvas
    private var px = 0
    private var py = 0

    private val border = Paint().apply {
        color = Color.BLACK
        strokeWidth = 1F
        style = Paint.Style.STROKE
    }
    private val head = Paint().apply {
        color = Color.parseColor(Design.hsl2hex(data.getColor(Design.bg_h)))
        style = Paint.Style.FILL
    }
    private val free = Paint().apply {
        color = Color.parseColor(Design.hsl2hex(Ahsl(35, 0, 0, 0)))
        style = Paint.Style.FILL
    }
    private val presentation = Paint().apply {
        color = Color.parseColor(Design.hsl2hex(data.getColor(Design.bg_p)))
        style = Paint.Style.FILL
    }
    private val course = Paint().apply {
        color = Color.parseColor(Design.hsl2hex(data.getColor(Design.bg_c)))
        style = Paint.Style.FILL
    }

    private val headTxt = text(14, Design.bg_h).also { it.typeface = Typeface.DEFAULT_BOLD }
    private val presentationHeading = text(12, Design.bg_p).also { it.typeface = Typeface.DEFAULT_BOLD }
    private val presentationRoom = text(8, Design.bg_p)
    private val courseHeading = text(12, Design.bg_p).also { it.typeface = Typeface.DEFAULT_BOLD }
    private val courseRoom = text(9, Design.bg_p)

    init {
        val count = data.getScedRange().count()
        oneColWidth = 720 / count
        width = oneColWidth * count
        head.color = Color.parseColor(Design.hsl2hex(data.getColor(Design.bg_h)))
        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        canvas = Canvas(bitmap)
    }

    private fun text(n: Int, colorTarget: String): Paint {
        return Paint().apply {
            color = Color.parseColor(Design.hsl2hex(Design.customizedForeground(data.getColor(colorTarget))))
            style = Paint.Style.FILL
            textSize = Design.dpToPx(n).toFloat()
            isElegantTextHeight = true
            textAlign = Paint.Align.CENTER
        }
    }

    fun drawTable(): Bitmap {
        with(data.getScedRange()) {
            drawHeader(this)
            drawBody(this)
        }
        data.close()
        return bitmap
    }
    private fun drawHeader(range: IntRange) {
        px = 0
        py = 0
        val hours = range.count()

        var next = range.first
        val step = when (true) {
            hours in 1..7 -> 1
            hours in 8..11 || hours > 11 && hours % 3 == 1 -> 2
            else -> 3
        }

        while (next <= range.last) {
            val w = (range.last - next + 1).coerceAtMost(step) * oneColWidth
            val right = px + w
            val bottom = py + headingHeight
            val rect = Rect(px, py, right, bottom)
            canvas.drawRect(rect, head)
            canvas.drawRect(rect, border)
            val txt = "$next"
            canvas.drawText(txt, 0, txt.length, px + Design.dpToPx(10).toFloat(),  bottom - Design.dpToPx(5).toFloat(), headTxt)
            next += step
            px += w
        }
        py += headingHeight
    }
    private fun drawBody(range: IntRange) {
        px = 0
        if (range.count() == 0) return

        //nekontrolujem poradie hodin. SQL ich nacitava v poradi podla stlpcov: den, hodina
        val lessons = data.getScedule()
        var dayOfPrevious = 0
        var hourAfterPreviousLesson = range.first

        for (l in lessons) {
            //vyplnanie medzier medzi predoslou a sucasnou hodinou s dennym odstupom
            if (l.day.position - dayOfPrevious > 0) {
                //volny zvysok dna
                drawVoid((hourAfterPreviousLesson..range.last).count())
                px = 0
                py += dayHeight

                // volne cele dni medzi sucasnou a predoslou hodinou
                for (d in dayOfPrevious + 1 until l.day.position) {
                    drawVoid(range.count())
                    px = 0
                    py += dayHeight
                }

                //volno pred zaciatkom 1. hodiny
                drawVoid((range.first until l.time.first).count())
            }
            //medzera medzi 2 hodinami v rovnakom dni
            else
                drawVoid((hourAfterPreviousLesson until l.time.first).count())

            //pridanie hodiny
            dayOfPrevious = l.day.position
            drawLesson(l)
            hourAfterPreviousLesson = l.time.last + 1
        }

        //vyplnit prazdne riadky chybajuce do konca tyzdna
        drawVoid((hourAfterPreviousLesson .. range.last).count())
        px = 0
        py += dayHeight
        if (++dayOfPrevious < 5)
            for (d in dayOfPrevious .. 4) {
                drawVoid(range.count())
                px = 0
                py += dayHeight
            }
    }
    private fun drawVoid(span: Int) {
        val pw = span * oneColWidth
        val rect = Rect(px, py, px + pw, py + dayHeight)
        canvas.drawRect(rect, free)
        canvas.drawRect(rect, border)
        px += pw
    }
    private fun drawLesson(les: Lesson) {
        val pw = les.time.count() * oneColWidth
        val rect = Rect(px, py, px + pw, py + dayHeight)
        val h = if (les.sort == ScedSort.COURSE) courseHeading else presentationHeading
        val r = if (les.sort == ScedSort.COURSE) courseRoom else presentationRoom

        val abb = shortenText(les.subject.abb, h, rect).let { if (it.length > 4) it.substring(0..4) else it }
        val room = shortenText(les.room, r, rect)
        canvas.drawRect(rect, if (les.sort == ScedSort.COURSE) course else presentation)
        canvas.drawRect(rect, border)
        canvas.drawText(abb, 0, abb.length, px + pw / 2F, py + Design.dpToPx(15).toFloat(), h )
        canvas.drawText(room, 0, room.length, px + pw / 2F, py + dayHeight - Design.dpToPx(5).toFloat(), r )
        px += pw
    }

    private fun shortenText(text: String, textBox: Paint, fitTo: Rect): String {
        var tmpRect = Rect()
        textBox.getTextBounds(text, 0, text.length, tmpRect)

        var editStr = text
        var short = false

        while (fitTo.width() < tmpRect.width() && editStr.length > 1) {
            tmpRect = Rect()
            editStr = editStr.substring(0..editStr.length - 2)
            textBox.getTextBounds("${editStr}...", 0, editStr.length + 3, tmpRect)
            Log.d("moriak", "fitTo: ${fitTo.width()} vs. tmpRect: ${tmpRect.width()} $tmpRect")
            short = true
        }
        Log.d("moriak", "----------------------- end --------------------------")
        return if (short) "$editStr..." else editStr
    }
}