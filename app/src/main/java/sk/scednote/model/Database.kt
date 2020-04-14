package sk.scednote.model

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import sk.scednote.model.data.*
import java.util.*
import kotlin.collections.ArrayList

@Suppress("UNREACHABLE_CODE")
class Database(context: Context) {
    private var cont = context

    //zona podtried a atrubutov
    companion object {
        // If you change the database schema, you must increment the database version.
        const val DATABASE_VERSION = 1
        const val DATABASE_NAME = "scednote.db"
    }
    object TableSet {
        object Colors {
            const val TABLE_NAME = "design"
            const val TARGET = "target"
            const val H = "hue"
            const val S = "saturation"
            const val L = "lightness"
            const val A = "alpha"
        }
        object Subjects : BaseColumns {
            const val TABLE_NAME = "subjects"
            const val ABBREVIATION = "abbreviation"
            const val FULL_NAME = "full_name"
        }
        object Lessons : BaseColumns {
            const val TABLE_NAME = "lessons"
            const val SUBJECT = "sub_id"
            const val PRESENTATION = "presentation"
            const val LOCATION = "location"
            const val DAY = "day"
            const val START = "start"
            const val DURATION = "duration"
        }
        object Notes : BaseColumns {
            const val TABLE_NAME = "notes"
            const val SUBJECT = "sub_id"
            const val DETAIL = "detail"
            const val DEADLINE = "deadline"
        }
    }

    private inner class DBHelp(cnt: Context) : SQLiteOpenHelper(cnt, DATABASE_NAME, null, DATABASE_VERSION) {
        //implementacia interface -> udalosti
        override fun onCreate(db: SQLiteDatabase) {
            //konfiguracia farieb
            db.execSQL("CREATE TABLE ${TableSet.Colors.TABLE_NAME}(" +
                    "${TableSet.Colors.TARGET} CHARACTER(20) NOT NULL PRIMARY KEY," +
                    "${TableSet.Colors.H} TINYINT(3) NOT NULL," +
                    "${TableSet.Colors.S} TINYINT(3) NOT NULL," +
                    "${TableSet.Colors.L} TINYINT(3) NOT NULL," +
                    "${TableSet.Colors.A} TINYINT(3) NOT NULL);")
            //evidencia predmetov
            db.execSQL("CREATE TABLE ${TableSet.Subjects.TABLE_NAME}(" +
                    "${BaseColumns._ID} INTEGER PRIMARY KEY," +
                    "${TableSet.Subjects.ABBREVIATION} CHARACTER(4) NOT NULL UNIQUE," +
                    "${TableSet.Subjects.FULL_NAME} VARCHAR(30) NOT NULL);")
            //evidencia vyucovacich hodin
            db.execSQL("CREATE TABLE ${TableSet.Lessons.TABLE_NAME}(" +
                    "${BaseColumns._ID} INTEGER PRIMARY KEY," +
                    "${TableSet.Lessons.SUBJECT} INTEGER NOT NULL," +
                    "${TableSet.Lessons.DAY} TINYINT NOT NULL," +
                    "${TableSet.Lessons.PRESENTATION} BOOLEAN NOT NULL," +
                    "${TableSet.Lessons.LOCATION} CHARACTER(12)," +
                    "${TableSet.Lessons.START} INTEGER NOT NULL," +
                    "${TableSet.Lessons.DURATION} INTEGER NOT NULL," +
                    "FOREIGN KEY (${TableSet.Lessons.SUBJECT}) REFERENCES ${
                    TableSet.Subjects.TABLE_NAME}(${BaseColumns._ID}));"
            )
            //evidencia poznamok
            db.execSQL("CREATE TABLE ${TableSet.Notes.TABLE_NAME}(" +
                    "${BaseColumns._ID} INTEGER PRIMARY KEY," +
                    "${TableSet.Notes.SUBJECT} INTEGER NOT NULL," +
                    "${TableSet.Notes.DETAIL} TEXT NOT NULL," +
                    "${TableSet.Notes.DEADLINE} TEXT," +
                    "FOREIGN KEY (${TableSet.Notes.SUBJECT}) REFERENCES ${
                    TableSet.Subjects.TABLE_NAME}(${BaseColumns._ID}));")
        }
        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            db.execSQL("DROP TABLE IF EXISTS ${TableSet.Colors.TABLE_NAME}; " +
                    "DROP TABLE IF EXISTS ${TableSet.Subjects.TABLE_NAME}; " +
                    "DROP TABLE IF EXISTS ${TableSet.Lessons.TABLE_NAME}; " +
                    "DROP TABLE IF EXISTS ${TableSet.Notes.TABLE_NAME};")
            onCreate(db)
        }
        override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            onUpgrade(db, oldVersion, newVersion)
        }

    }

    private var helper = DBHelp(cont)
    private val rd get() = helper.readableDatabase
    private val wrt get() = helper.writableDatabase

    //zavretie databazy
    fun close() {
        rd.close()
        wrt.close()
        helper.close()
    }

    //farby
    fun setColor(target: String, ahsl: Ahsl) {
        wrt.execSQL(
            "INSERT OR REPLACE INTO ${TableSet.Colors.TABLE_NAME} VALUES(?, ?, ?, ?, ?)",
            arrayOf(target, ahsl.h.toString(), ahsl.s.toString(), ahsl.l.toString(), ahsl.a.toString())
        )
    }
    fun getColor(target: String): Ahsl {
        val ahsl: Ahsl
        with(TableSet.Colors) {
            rd.query(TABLE_NAME, arrayOf(A, H, S, L), "$TARGET=?", arrayOf(target), null, null, null, "1").apply {
                if (count == 0) { //plati iba raz
                    val id = cont.resources.getIdentifier(target, "color", cont.packageName)
                    ahsl = Design.hex2hsl(cont.resources.getString(id))
                    setColor(target, ahsl)
                }
                else {
                    moveToFirst()
                    ahsl = Ahsl(getInt(0), getInt(1), getInt(2), getInt(3))
                }
                close()
            }
        }
        return ahsl
    }

    //predmety
    fun insertSubject(abb: String, full: String): Long {
        val sub = getSubject(abb)
        return if (sub != null) updateSubject(sub.id!!, abb, full)
        else wrt.insert(TableSet.Subjects.TABLE_NAME,null, ContentValues().apply{
            put(TableSet.Subjects.ABBREVIATION, abb.toUpperCase(Locale.ROOT))
            put(TableSet.Subjects.FULL_NAME, full)
        })
    }
    fun updateSubject(id: Long, abb: String, full: String): Long {
        with(TableSet.Subjects) {
            wrt.update(TABLE_NAME, ContentValues().apply {
                put(ABBREVIATION, abb.substring(0..(abb.length-1).coerceAtMost(4)).toUpperCase(Locale.ROOT))
                put(FULL_NAME, full)
            }, "${BaseColumns._ID}=?", arrayOf(id.toString()))
        }
        return getSubject(abb)?.id ?: -1
    }
    fun loadSubjects(): ArrayList<Subject> {
        val subs = ArrayList<Subject>()
        val cursor: Cursor
        with (TableSet.Subjects) {
            cursor = rd.query(TABLE_NAME, arrayOf("*"), null, null, null, null, ABBREVIATION)
        }
        with(cursor) {
            while(cursor.moveToNext())
                subs.add(Subject(getLong(0), getString(1), getString(2)))
            close()
        }
        return subs
    }
    fun getSubject(id: Long): Subject? {
        val sub: Subject?
        rd.query(TableSet.Subjects.TABLE_NAME, arrayOf("*"), "${BaseColumns._ID}=?", arrayOf(id.toString()), null, null, null).apply {
            moveToFirst()
            sub = if (count == 0) null else Subject(getLong(0), getString(1), getString(2))
            close()
        }
        return sub
    }
    fun getSubject(abb: String): Subject? {
        val cur: Cursor
        val subject: Subject?
        with(TableSet.Subjects) {
            cur = wrt.query(TABLE_NAME, arrayOf("*"), "UPPER($ABBREVIATION)=?",
                arrayOf(abb.toUpperCase(Locale.ROOT)), null, null, null,
                null).also { it.moveToFirst() }
        }
        with(cur) {
            subject = if (count > 0) Subject(getLong(0), getString(1), getString(2)) else null
            close()
        }
        return subject
    }
    fun removeSubject(abb:String) {
        val id = getSubject(abb)?.id ?: -1
        if (id > -1) removeSubject(id)
    }
    fun removeSubject(id: Long) {
        val cond = arrayOf(id.toString())
        wrt.delete(TableSet.Notes.TABLE_NAME, "${TableSet.Notes.SUBJECT}=?", cond)
        wrt.delete(TableSet.Lessons.TABLE_NAME, "${TableSet.Lessons.SUBJECT}=?", cond)
        wrt.delete(TableSet.Subjects.TABLE_NAME, "${BaseColumns._ID}=?", cond)
    }

    //rozvrh
    fun getScedRange(): IntRange {
        if (isScedClear()) return 0..0
        val rng: IntRange
        with(TableSet.Lessons) {
            val cols = arrayOf("MIN($START) AS start", "MAX($START + $DURATION - 1) AS end")
            rd.query(TABLE_NAME, cols, "", arrayOf(),null, null, null).apply {
                moveToFirst()
                rng = getInt(0)..getInt(1)
                close()
            }
        }
        return rng
    }
    fun getScedule(day: Day? = null): ArrayList<Lesson> {
        val lessons = ArrayList<Lesson>()
        val curs: Cursor
        val les = TableSet.Lessons.TABLE_NAME
        val sub = TableSet.Subjects.TABLE_NAME
        with(TableSet.Lessons) {
            with(TableSet.Subjects) {
                curs = rd.query(
                    "$les l JOIN ($sub) s ON (s.${BaseColumns._ID} = l.$SUBJECT)",
                    arrayOf("l.${BaseColumns._ID} AS lesID", "l.$DAY", "l.$START",
                        "(l.$START + l.$DURATION - 1) AS end", "l.$PRESENTATION", "s.${BaseColumns._ID} AS subID",
                        "s.$ABBREVIATION", "s.$FULL_NAME", "l.$LOCATION"),
                    if (day != null) "l.$DAY=${day.position}" else "", arrayOf(), null, null, "l.$DAY, l.$START")
            }
        }
        with(curs) {
            while (moveToNext()) {
                lessons.add(
                    Lesson(
                        getLong(0), Day[getInt(1)],
                        getInt(2)..getInt(3),
                        ScedSort[getInt(4)],
                        Subject(getLong(5), getString(6), getString(7)),
                        getString(8)
                    )
                )
            }
            close()
        }
        return lessons
    }
    fun getLessonsInRange(day: Day, range: IntRange, except: Long = -1): ArrayList<Long>{
        return getLessonsInRange(day.position,range, except)
    }
    fun getLessonsInRange(day:Int, range:IntRange, except: Long = -1): ArrayList<Long> {
        val ids = ArrayList<Long>()
        with(TableSet.Lessons) {
            val ID = BaseColumns._ID
            val END = "($START + $DURATION - 1)"
            val p1 = "${range.first}"
            val pN = "${range.last}"

            val cond = "$ID != $except AND $DAY = $day AND ($p1 BETWEEN $START AND $END OR $pN BETWEEN $START AND $END OR $START BETWEEN $p1 AND $pN)"
            rd.query(TABLE_NAME, arrayOf(ID), cond, arrayOf(), null, null, null).apply {
                while (moveToNext())
                    ids.add(getLong(0))
                close()
            }
        }
        return ids
    }

    //lesson after lesson
    //select s1._id, s2._id FROM lessons s1, lessons s2 WHERE (s1.start + s1.duration) = s2.start AND s1.day = s2.day AND s1.presentation = s2.presentation AND s1.location = s2.location

    fun isScedClear(): Boolean {
        val ret: Boolean
        rd.query(TableSet.Lessons.TABLE_NAME, arrayOf("*"), "", arrayOf(), null, null, null).apply { ret = count == 0; close() }
        return ret
    }

    fun getBusyHoursCount(day: Int, exception: Long = -1): Int {
        val num: Int
        with(TableSet.Lessons) {
            rd.query(TABLE_NAME, arrayOf("SUM($DURATION)"), "$DAY=$day AND ${BaseColumns._ID} != $exception", arrayOf(), null, null, null).apply {
                moveToFirst()
                num = getInt(0)
                close()
            }
        }
        return num
    }

    fun isScedClear(day: Day, range: IntRange, exception: Long = -1): Boolean {
        return getLessonsInRange(day.position, range, exception).isEmpty()
    }
    fun isScedClear(day:Int, range: IntRange, exception: Long = -1): Boolean {
        return getLessonsInRange(day, range, exception).isEmpty()
    }
    fun removeLessonsInRange(day: Day, range: IntRange, exception: Long = -1) {
        removeLessonsInRange(day.position, range, exception)
    }
    fun removeLessonsInRange(day: Int, range: IntRange, exception: Long = -1) {
        val trash = getLessonsInRange(day, range, exception)
        if (trash.isNotEmpty()) {
            val cond = ArrayList<String>()
            val args = ArrayList<String>()
            for (t in trash) {
                cond.add("${BaseColumns._ID}=?")
                args.add(t.toString())
            }
            wrt.delete(TableSet.Lessons.TABLE_NAME, cond.joinToString(" OR "), args.toTypedArray())
        }
    }
    fun getLesson(id: Long): Lesson? {
        val les: Lesson?
        val abb = arrayOf(BaseColumns._ID, "${TableSet.Subjects.TABLE_NAME} s", "${TableSet.Lessons.TABLE_NAME} l")
        // na skratenie zapisu
        with(TableSet.Subjects) {
            with (TableSet.Lessons) {
                rd.query(
                    "${abb[2]} JOIN ${abb[1]} ON (s.${abb[0]} = l.$SUBJECT)",
                    arrayOf(
                        "l.${BaseColumns._ID} AS lesID", "l.$DAY", "l.$START", "(l.$START + l.$DURATION - 1) AS end",
                        "l.$PRESENTATION", "s.${abb[0]} AS subID", "s.$ABBREVIATION", "s.$FULL_NAME", "l.$LOCATION"),
                    "l.${abb[0]}=?", arrayOf(id.toString()), null, null, null).
                apply {
                    moveToNext()
                    if (count == 0) les = null
                    else {
                        val time = getInt(2) .. getInt(3)
                        val sub = Subject(getLong(5), getString(6), getString(7))
                        les = Lesson (id, Day[getInt(1)], time, ScedSort[getInt(4)], sub, getString(8))
                    }
                    close()
                }
            }
        }
        return les
    }
    fun insertLesson(les: Lesson): Long {
        if (les.id > -1 && getLesson(les.id) != null)
            return updateLesson(les)
        //id hodiny moze byt teraz akekolvek. Nepotrebujeme ho.
        removeLessonsInRange(les.day, les.time)
        return wrt.insert(TableSet.Lessons.TABLE_NAME, null, ContentValues().apply{
            put(TableSet.Lessons.SUBJECT, les.subject.id.toString())
            put(TableSet.Lessons.DAY, les.day.position)
            put(TableSet.Lessons.START, les.time.first)
            put(TableSet.Lessons.DURATION, les.time.count())
            put(TableSet.Lessons.PRESENTATION, les.sort.position)
            put(TableSet.Lessons.LOCATION, les.room.toUpperCase(Locale.ROOT))
        })
    }
    fun updateLesson(les: Lesson): Long {
        if (getLesson(les.id) == null) return -1
        //predmet sa vlozi a/alebo uchova
        val sID = getSubject(les.subject.abb)?.id ?: insertSubject(les.subject.abb, les.subject.full)
        removeLessonsInRange(les.day, les.time, les.id)
        wrt.update(TableSet.Lessons.TABLE_NAME, ContentValues().apply {
            put(TableSet.Lessons.DAY, les.day.position)
            put(TableSet.Lessons.START, les.time.first)
            put(TableSet.Lessons.DURATION, les.time.count())
            put(TableSet.Lessons.PRESENTATION, les.sort.position)
            put(TableSet.Lessons.SUBJECT, sID)
            put(TableSet.Lessons.LOCATION, les.room.toUpperCase(Locale.ROOT))
        }, "${BaseColumns._ID}=?", arrayOf(les.id.toString()))
        return les.id
    }

    /**
     * ak su 2 po sebe iduce hodiny bez aspon hodinovej prestavky medzi nimi, su v rovnakej
     * miestnosti, rovnaky den, z rovnakeho predmetu, rovnkeho typu zlucia sa do jednej
     */
    private fun getSplitLessons(): ArrayList<Lesson> {
        val ret = ArrayList<Lesson>()
        with(TableSet.Lessons) {
            val ON = "r.$DAY = s.$DAY AND r.$START + r.$DURATION = s.$START AND r.$SUBJECT = " +
                    "s.$SUBJECT AND r.$LOCATION = s.$LOCATION AND r.$PRESENTATION = s.$PRESENTATION"
            val SEL = arrayOf("r.${BaseColumns._ID}", "s.${BaseColumns._ID}")
            val FRM = "$TABLE_NAME r JOIN $TABLE_NAME s ON ($ON)"
            //val sql = "SELECT r.$ID, s.$ID FROM $TABLE_NAME r JOIN $TABLE_NAME s ON ($ON) ORDER BY r.$DAY, r.$START;"

            rd.query(FRM, SEL, "", arrayOf(), null, null, "r.$DAY, r.$START").apply {
                while (moveToNext()) {
                    val current = getLong(0)
                    if (ret.isEmpty() || current != ret.last().id) ret.add(getLesson(current)!!)
                    ret.add(getLesson(getLong(1))!!)
                }
                close()
            }
        }
        return ret
    }

    /**
     * ak su 2 po sebe iduce hodiny bez aspon 1-hodinovej prestavky, su v rovnakej
     * miestnosti, rovnaky den, z rovnakeho predmetu, rovnkeho typu zlucia sa do jednej
     * Navratova hodnota hovori, ze doslo k zluceniu aspon 2 hodin alebo nedoslo k ziadnym zmenam.
     */
    fun putLessonsTogether(): Boolean {
        val lessons = getSplitLessons()
        for (i in lessons)
        if (lessons.size == 0) return false
        while (lessons.size > 0) {
            //vybrat skupinu bezstratovo zlucitelnych predmetov (rovnake predmety) prva a posledna dvojica to bude vzdy
            val joinable = ArrayList<Lesson>()
            val lastHour = lessons.last().time.last
            do {
                val index = lessons.size - 1
                val last = lessons.removeAt(index)
                joinable.add(last)
                if (index == 0 || last != lessons[index-1]) break
            } while (true)
            //tu je jedna so skupin spojitelnych predmetov
            with(joinable.last()){
                updateLesson(Lesson(id, day, time.first..lastHour, sort, subject, room))
            }
        }
        return true
    }

    fun removeLesson(id: Long) {
        wrt.delete(TableSet.Lessons.TABLE_NAME, "${BaseColumns._ID}=?", arrayOf(id.toString()))
    }
    //poznamky
    //fun insertNote(note: )

}

