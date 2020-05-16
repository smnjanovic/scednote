package sk.scednote.scedule

import android.content.Context
import android.graphics.*
import android.view.WindowManager
import sk.scednote.ScedNoteApp
import sk.scednote.model.*
import kotlin.math.roundToInt

/**
 * Trieda vytvorí tabuľku s rozvrhom vo forme bitmapy.
 */

class TimetableImage {
    private lateinit var bitmap: Bitmap
    private lateinit var canvas: Canvas
    private lateinit var lessons: ArrayList<Lesson>
    private var px = 0
    private var py = 0

    private val frmW: Int
    private val frmH: Int get() = frmW * 16 / 33
    private val headingHeight = Design.dp(20)
    private lateinit var range: IntRange
    private val colW: Int get() = frmW / range.count().coerceAtLeast(1)
    private val colH: Int get() = (frmH - headingHeight) / Day.values().size

    private lateinit var bgH: String
    private lateinit var bgP: String
    private lateinit var bgC: String

    private lateinit var fgH: String
    private lateinit var fgP: String
    private lateinit var fgC: String

    private val border = Paint().apply {
        color = Color.BLACK
        strokeWidth = 1F
        style = Paint.Style.STROKE
    }
    private val free = fill(Design.hsl2hex(Ahsl(35, 0, 0, 0)))
    private lateinit var head: Paint
    private lateinit var headTxt: Paint
    private lateinit var presentation: Paint
    private lateinit var presentationHeading: Paint
    private lateinit var presentationRoom: Paint
    private lateinit var course: Paint
    private lateinit var courseHeading: Paint
    private lateinit var courseRoom: Paint

    init {
        val point = Point()
        (ScedNoteApp.ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getRealSize(point)
        frmW = point.x.coerceAtMost(point.y)
    }

    private fun setUp () {
        val db = ScedNoteApp.database
        val h = db.getColor(Design.bg_h)
        val p = db.getColor(Design.bg_p)
        val c = db.getColor(Design.bg_c)
        range = db.getScedRange()
        lessons = db.getScedule()

        bgH = Design.hsl2hex(h)
        fgH = Design.hsl2hex(Design.customizedForeground(h))
        bgP = Design.hsl2hex(p)
        fgP = Design.hsl2hex(Design.customizedForeground(p))
        bgC = Design.hsl2hex(c)
        fgC = Design.hsl2hex(Design.customizedForeground(c))

        head = fill(bgH)
        presentation = fill(bgP)
        course = fill(bgC)

        val bold = Typeface.DEFAULT_BOLD
        val abbSize = (colH * (10 / 50F)).roundToInt()
        val roomSize = (colH * (8 / 50F)).roundToInt()

        headTxt = text(14, fgH).also { it.typeface = bold }
        presentationHeading = text(abbSize, fgP).also { it.typeface = bold }
        presentationRoom = text(roomSize, fgP)
        courseHeading = text(abbSize, fgC).also { it.typeface = bold }
        courseRoom = text(roomSize, fgC)

        bitmap = Bitmap.createBitmap(frmW, frmH, Bitmap.Config.ARGB_8888)
        canvas = Canvas(bitmap)
    }

    private fun fill(hex: String) = Paint().apply {
        color = Color.parseColor(hex)
        style = Paint.Style.FILL
    }
    private fun text(n: Int, hex: String) = Paint().apply {
        color = Color.parseColor(hex)
        style = Paint.Style.FILL
        textSize = Design.dp(n).toFloat()
        isElegantTextHeight = true
        textAlign = Paint.Align.CENTER
    }

    /**
     * Nakresli tabulku s rozvrhom a vrati bitmapu
     *
     * @return [Bitmap] returns drawn image of scedule in the table
     */
    fun drawTable(): Bitmap {
        setUp()
        drawHeader()
        drawBody()
        return bitmap
    }
    private fun drawHeader() {
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
            val w = (range.last - next + 1).coerceAtMost(step) * colW
            val right = px + w
            val bottom = py + headingHeight
            val rect = Rect(px, py, right, bottom)
            canvas.drawRect(rect, head)
            canvas.drawRect(rect, border)
            val txt = "$next"
            canvas.drawText(txt, 0, txt.length, px + Design.dp(10).toFloat(),  bottom - Design.dp(5).toFloat(), headTxt)
            next += step
            px += w
        }
        py += headingHeight
    }
    private fun drawBody() {
        px = 0
        if (range.count() == 0) return

        if (lessons.isEmpty()) {
            canvas.drawRect(
                Rect(0, 0, frmW, frmW * 16 / 33),
                fill(Design.hsl2hex(Ahsl(35, 0, 0, 0)))
            )
        }
        else {
            //nekontrolujem poradie hodin. SQL ich nacitava v poradi podla stlpcov: den, hodina
            var dayOfPrevious = 0
            var hourAfterPreviousLesson = range.first

            for (l in lessons) {
                //vyplnanie medzier medzi predoslou a sucasnou hodinou s dennym odstupom
                if (l.day.position - dayOfPrevious > 0) {
                    //volny zvysok dna
                    drawVoid((hourAfterPreviousLesson..range.last).count())
                    px = 0
                    py += colH

                    // volne viacere (cele) dni medzi sucasnou a predoslou hodinou
                    for (d in dayOfPrevious + 1 until l.day.position) {
                        drawVoid(range.count())
                        px = 0
                        py += colH
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
            py += colH
            if (++dayOfPrevious < 5)
                for (d in dayOfPrevious .. 4) {
                    drawVoid(range.count())
                    px = 0
                    py += colH
                }
        }

    }
    private fun drawVoid(span: Int) {
        val pw = span * colW
        val rect = Rect(px, py, px + pw, py + colH)
        canvas.drawRect(rect, free)
        canvas.drawRect(rect, border)
        px += pw
    }
    private fun drawLesson(les: Lesson) {
        val pw = les.time.count() * colW
        val rect = Rect(px, py, px + pw, py + colH)
        val h = if (les.sort == ScedSort.COURSE) courseHeading else presentationHeading
        val r = if (les.sort == ScedSort.COURSE) courseRoom else presentationRoom

        val abb = shortenText(les.subject.abb, h, rect).let { if (it.length > 4) it.substring(0..4) else it }
        val room = shortenText(les.room, r, rect)
        canvas.drawRect(rect, if (les.sort == ScedSort.COURSE) course else presentation)
        canvas.drawRect(rect, border)
        canvas.drawText(abb, 0, abb.length, px + pw / 2F, py + (colH *15F / 33F), h )
        canvas.drawText(room, 0, room.length, px + pw / 2F, py + colH - colH * (0.1F), r )
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
            short = true
        }
        return if (short) "$editStr..." else editStr
    }
}