package sk.scednote.model

import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import android.util.Log
import sk.scednote.model.data.*
import java.util.*

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

    inner class DBHelp(cnt: Context) : SQLiteOpenHelper(cnt, DATABASE_NAME, null, DATABASE_VERSION) {
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
            // This database is only a cache for online data, so its upgrade policy is
            // to simply to discard the data and start over
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
    fun getColor(target: String): Ahsl? {
        val col = TableSet.Colors
        val cur = rd.query(col.TABLE_NAME, arrayOf(col.A, col.H, col.S, col.L),
            "${col.TARGET}=?", arrayOf(target), null, null, null, "1")
        cur.moveToFirst()
        val ahsl: Ahsl? =
            if (cur.count > 0) Ahsl(cur.getInt(0), cur.getInt(1), cur.getInt(2), cur.getInt(3))
            else null
        cur.close()
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
        wrt.update(TableSet.Subjects.TABLE_NAME, ContentValues().apply {
                put(TableSet.Subjects.ABBREVIATION, (if(abb.length > 5) abb.substring(0..4) else abb).toUpperCase(Locale.ROOT))
                put(TableSet.Subjects.FULL_NAME, full)
            }, "${BaseColumns._ID}=?", arrayOf(id.toString()))
        return getSubject(abb)?.id ?: -1
    }
    fun loadSubjects(): ArrayList<Subject> {
        val cur = rd.rawQuery("SELECT * FROM ${TableSet.Subjects.TABLE_NAME}", arrayOf())
        val arr = ArrayList<Subject>()
        while(cur.moveToNext())
            arr.add(Subject(cur.getLong(0), cur.getString(1), cur.getString(2)))
        cur.close()
        return arr
    }
    fun getSubject(id: Long): Subject? {
        val cur = wrt.rawQuery(
            "SELECT * FROM ${TableSet.Subjects.TABLE_NAME} WHERE ${BaseColumns._ID}=?",
            arrayOf(id.toString())
        ).also { it.moveToFirst() }
        val subject = if (cur.count == 0) null else Subject(cur.getLong(0), cur.getString(1), cur.getString(2))
        cur.close()
        return subject
    }
    fun getSubject(abb: String): Subject? {
        val cur = wrt.rawQuery(
            "SELECT * FROM ${TableSet.Subjects.TABLE_NAME} WHERE UPPER(${TableSet.Subjects.ABBREVIATION})=?",
            arrayOf(abb.toUpperCase(Locale.ROOT))
        ).also { it.moveToFirst() }
        val subject = if (cur.count == 0) null else Subject(cur.getLong(0), cur.getString(1), cur.getString(2))
        cur.close()
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
        val curs = wrt.rawQuery("SELECT MIN(${TableSet.Lessons.START}) AS start," +
                " MAX(${TableSet.Lessons.START} + ${TableSet.Lessons.DURATION} - 1) AS end" +
                " FROM ${TableSet.Lessons.TABLE_NAME}", arrayOf())
        if (curs.moveToFirst()) {
            val range = curs.getInt(0)..curs.getInt(1)
            curs.close()
            return range
        }
        return 0..0
    }
    fun getScedule(day: Day? = null): ArrayList<Lesson> {
        val lessons = ArrayList<Lesson>()
        val curs = rd.rawQuery(
            "SELECT " +
                    "l.${BaseColumns._ID} AS lesID," +
                    "l.${TableSet.Lessons.DAY}, " +
                    "l.${TableSet.Lessons.START}," +
                    "(l.${TableSet.Lessons.START} + l.${TableSet.Lessons.DURATION} - 1) AS end_," +
                    "l.${TableSet.Lessons.PRESENTATION}," +
                    "l.${TableSet.Lessons.LOCATION}," +
                    "s.${BaseColumns._ID} AS subID," +
                    "s.${TableSet.Subjects.ABBREVIATION}," +
                    "s.${TableSet.Subjects.FULL_NAME} " +
                    "FROM ${TableSet.Lessons.TABLE_NAME} l JOIN (${TableSet.Subjects.TABLE_NAME}) s " +
                    "ON (s.${BaseColumns._ID} = l.${TableSet.Lessons.SUBJECT}) " +
                    if (day != null) "WHERE l.${TableSet.Lessons.DAY}=? " else "" +
                    "ORDER BY l.${TableSet.Lessons.DAY}, l.${TableSet.Lessons.START}",
            if (day != null) arrayOf(day.position.toString()) else arrayOf()
        )

        while (curs.moveToNext())
            lessons.add(Lesson(
                curs.getLong(0),
                Day[curs.getInt(1)],
                curs.getInt(2)..curs.getInt(3),
                ScedSort[curs.getInt(4)],
                Subject(curs.getLong(6), curs.getString(7), curs.getString(8)),
                curs.getString(5)
            ))
        curs.close()
        return lessons
    }
    fun getLessonsInRange(day: Day, range: IntRange): ArrayList<Long>{
        return getLessonsInRange(day.position,range)
    }
    fun getLessonsInRange(day:Int, range:IntRange): ArrayList<Long> {
        val sql = "SELECT ${BaseColumns._ID} FROM ${TableSet.Lessons.TABLE_NAME} WHERE ${TableSet.Lessons.DAY}=? " +
                "AND (${TableSet.Lessons.START} BETWEEN ? AND ? OR (${TableSet.Lessons.START} + " +
                "${TableSet.Lessons.DURATION} - 1) BETWEEN ? AND ?)"
        val args = arrayOf(
            day.toString(),
            range.first.toString(), (range.last).toString(),
            range.first.toString(), (range.last).toString()
        )
        var res = sql
        for (i in args)
            res = res.replaceFirst("[?]".toRegex(), i)
        val curs = rd.rawQuery(sql, args)
        val arr = ArrayList<Long>()
        while (curs.moveToNext())
            arr.add(curs.getLong(0))
        curs.close()
        Log.d("moriak", "SQL: ${res}")
        Log.d("moriak", range.first.toString() + ".." + range.last.toString())
        Log.d("moriak", "records: ${curs.count}")
        return arr
    }
    fun isScedClear(): Boolean {
        val curs = rd.rawQuery("SELECT * FROM ${TableSet.Lessons.TABLE_NAME}", arrayOf())
        val ret = curs.count == 0
        curs.close()
        return ret
    }
    fun isScedClear(day: Day, range: IntRange): Boolean {
        return getLessonsInRange(day, range).isEmpty()
    }
    fun isScedClear(day:Int, range: IntRange): Boolean {
        return getLessonsInRange(day, range).isEmpty()
    }
    fun removeLessonsInRange(day: Day, range: IntRange) {
        removeLessonsInRange(day.position, range)
    }
    fun removeLessonsInRange(day: Int, range: IntRange) {
        val trash = getLessonsInRange(day, range)
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
        val cur = rd.rawQuery(
            "SELECT " +
                    "l.${BaseColumns._ID} AS lesID," +
                    "l.${TableSet.Lessons.DAY}, " +
                    "l.${TableSet.Lessons.START}," +
                    "(l.${TableSet.Lessons.START} + l.${TableSet.Lessons.DURATION} - 1) AS end," +
                    "l.${TableSet.Lessons.PRESENTATION}," +
                    "l.${TableSet.Lessons.LOCATION}," +
                    "s.${BaseColumns._ID} AS subID," +
                    "s.${TableSet.Subjects.ABBREVIATION}," +
                    "s.${TableSet.Subjects.FULL_NAME} " +
                    "FROM ${TableSet.Lessons.TABLE_NAME} l JOIN (${TableSet.Subjects.TABLE_NAME}) s " +
                    "ON (s.${BaseColumns._ID} = l.${TableSet.Lessons.SUBJECT}) " +
                    "WHERE l.${BaseColumns._ID}=?", arrayOf(id.toString())
        )
        cur.moveToNext()
        val lesson = if (cur.count == 0) null else Lesson(
            id, Day[cur.getInt(1)],
            cur.getInt(2) until cur.getInt(3),
            ScedSort[cur.getInt(4)],
            Subject(cur.getLong(6), cur.getString(7), cur.getString(8)),
            cur.getString(5)
        )
        cur.close()
        return lesson
    }
    fun insertLesson(les: Lesson): Long {
        if (les.subject == null)
            return -1
        if (les.id > -1 && getSubject(les.id) != null)
            return updateLesson(les)

        //id hodiny moze byt teraz akekolvek. Nepotrebujeme ho.
        removeLessonsInRange(les.day, les.time)
        return wrt.insert(TableSet.Lessons.TABLE_NAME, null, ContentValues().apply{
            put(TableSet.Lessons.SUBJECT, les.subject.id.toString())
            put(TableSet.Lessons.DAY, les.day.position)
            put(TableSet.Lessons.START, les.time.first)
            put(TableSet.Lessons.DURATION, les.time.count())
            put(TableSet.Lessons.PRESENTATION, les.sort.position)
            put(TableSet.Lessons.LOCATION, les.room)
        })
    }
    fun updateLesson(les: Lesson): Long {
        if (getLesson(les.id) == null || les.subject == null) return -1
        val sID = insertSubject(les.subject.abb, les.subject.full)
        wrt.update(TableSet.Lessons.TABLE_NAME, ContentValues().apply {
            put(TableSet.Lessons.DAY, les.day.position)
            put(TableSet.Lessons.START, les.time.first)
            put(TableSet.Lessons.DURATION, les.time.count())
            put(TableSet.Lessons.PRESENTATION, les.sort.position)
            put(TableSet.Lessons.SUBJECT, sID)
            put(TableSet.Lessons.LOCATION, les.room)
        }, "${BaseColumns._ID}=?", arrayOf(les.id.toString()))
        return les.id
    }
    fun removeLesson(id: Long) {
        wrt.delete(TableSet.Lessons.TABLE_NAME, "${BaseColumns._ID}=?", arrayOf(id.toString()))
    }

    //poznamky
    //fun insertNote(note: )
}

