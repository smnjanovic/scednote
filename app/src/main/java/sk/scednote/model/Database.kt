package sk.scednote.model

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import sk.scednote.NoteReminder
import sk.scednote.ScedNoteApp
import java.util.*
import kotlin.collections.ArrayList

/**
 * Trieda manipuluje s databazou SQLite. (prijem, tvorba, aktualizacia a odstranenie dat)
 */

class Database {
    //zona podtried a atrubutov
    companion object {
        // If you change the database schema, you must increment the database version.
        const val DATABASE_VERSION = 1
        const val DATABASE_NAME = "scednote.db"

        object TableSet {
            object Colors {
                override fun toString() = "design"
                const val TARGET = "target"
                const val H = "hue"
                const val S = "saturation"
                const val L = "lightness"
                const val A = "alpha"
            }

            object Subjects : BaseColumns {
                override fun toString() = "subjects"
                const val ABBREVIATION = "abbreviation"
                const val FULL_NAME = "full_name"
            }

            object Lessons : BaseColumns {
                override fun toString() = "lessons"
                const val SUBJECT = "sub_id"
                const val PRESENTATION = "presentation"
                const val LOCATION = "location"
                const val DAY = "day"
                const val START = "start"
                const val DURATION = "duration"
            }

            object Notes : BaseColumns {
                override fun toString() = "notes"
                const val SUBJECT = "sub_id"
                const val DETAIL = "detail"
                const val DEADLINE = "deadline"
            }
        }
    }
    private inner class DBHelp : SQLiteOpenHelper(ScedNoteApp.ctx, DATABASE_NAME, null, DATABASE_VERSION) {
        //implementacia interface -> udalosti
        override fun onCreate(db: SQLiteDatabase) {
            //konfiguracia farieb
            db.setLocale(Locale.getDefault())
            db.execSQL("CREATE TABLE ${TableSet.Colors}(" +
                    "${TableSet.Colors.TARGET} CHARACTER(20) NOT NULL PRIMARY KEY," +
                    "${TableSet.Colors.H} TINYINT(3) NOT NULL," +
                    "${TableSet.Colors.S} TINYINT(3) NOT NULL," +
                    "${TableSet.Colors.L} TINYINT(3) NOT NULL," +
                    "${TableSet.Colors.A} TINYINT(3) NOT NULL);")
            //evidencia predmetov
            db.execSQL("CREATE TABLE ${TableSet.Subjects}(" +
                    "${BaseColumns._ID} INTEGER PRIMARY KEY," +
                    "${TableSet.Subjects.ABBREVIATION} CHARACTER(4) NOT NULL UNIQUE," +
                    "${TableSet.Subjects.FULL_NAME} VARCHAR(30) NOT NULL);")
            //evidencia vyucovacich hodin
            db.execSQL("CREATE TABLE ${TableSet.Lessons}(" +
                    "${BaseColumns._ID} INTEGER PRIMARY KEY," +
                    "${TableSet.Lessons.SUBJECT} INTEGER NOT NULL," +
                    "${TableSet.Lessons.DAY} TINYINT NOT NULL," +
                    "${TableSet.Lessons.PRESENTATION} BOOLEAN NOT NULL," +
                    "${TableSet.Lessons.LOCATION} CHARACTER(12)," +
                    "${TableSet.Lessons.START} INTEGER NOT NULL," +
                    "${TableSet.Lessons.DURATION} INTEGER NOT NULL," +
                    "FOREIGN KEY (${TableSet.Lessons.SUBJECT}) REFERENCES ${
                    TableSet.Subjects}(${BaseColumns._ID}));"
            )
            //evidencia poznamok
            db.execSQL("CREATE TABLE ${TableSet.Notes}(" +
                    "${BaseColumns._ID} INTEGER PRIMARY KEY," +
                    "${TableSet.Notes.SUBJECT} INTEGER NOT NULL," +
                    "${TableSet.Notes.DETAIL} TEXT NOT NULL," +
                    "${TableSet.Notes.DEADLINE} INTEGER," +
                    "FOREIGN KEY (${TableSet.Notes.SUBJECT}) REFERENCES ${TableSet.Subjects}(${BaseColumns._ID}));")
        }
        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            db.execSQL("DROP TABLE IF EXISTS ${TableSet.Colors}; " +
                    "DROP TABLE IF EXISTS ${TableSet.Subjects}; " +
                    "DROP TABLE IF EXISTS ${TableSet.Lessons}; " +
                    "DROP TABLE IF EXISTS ${TableSet.Notes};")
            onCreate(db)
        }
        override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            onUpgrade(db, oldVersion, newVersion)
        }
    }

    private var helper = DBHelp()
    private val rd get() = helper.readableDatabase
    private val wrt get() = helper.writableDatabase

    //zavretie databazy
    fun close() {
        rd.close()
        wrt.close()
        helper.close()
    }

    private val currMillis: Long get() = System.currentTimeMillis()
    //dalsia polnoc
    private val midnight = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun <T> all(curs: Cursor, fn: (ArrayList<T>, Cursor)->Unit): ArrayList<T> {
        val ret = ArrayList<T>()
        while (curs.moveToNext())
            fn(ret, curs)
        curs.close()
        return ret
    }
    private fun <T> one(curs: Cursor, fn: (Cursor)->T): T? {
        curs.moveToFirst()
        val ret = if(curs.count == 0) null else fn(curs)
        curs.close()
        return ret
    }


    /**
     * getters
     */

    /**
     * ziska nastavenu farbu. Prednastavena farba je v resources, takze navratova hodnota je nenullova
     *
     * @param target Identifikator farby [String]
     * @return [Ahsl] farba v modeli HSL
     */
    fun getColor(target: String): Ahsl {
        val query = with(TableSet.Colors) {"SELECT $A, $H, $S, $L FROM $this WHERE $TARGET=?"}
        val default = with(ScedNoteApp) { Design.hex2hsl(res.getString(res.getIdentifier(target, "color", ctx.packageName))) }

        return one(rd.rawQuery(query, arrayOf(target))) {
            Ahsl(
                it.getInt(0),
                it.getInt(1),
                it.getInt(2),
                it.getInt(3)
            )
        } ?: default.also { setColor(target, it) }
    }

    /**
     * Nacita zoznam predmetov
     * @return [ArrayList] of [Subject] zoznam vsetkych predmetov
     */
    fun loadSubjects(): ArrayList<Subject> {
        val query = with(TableSet.Subjects) {"SELECT * FROM $this ORDER BY $ABBREVIATION"}
        return with (TableSet.Subjects) {
            all(rd.rawQuery(query, arrayOf())) { list, c ->
                list.add(
                    Subject(
                        c.getLong(0),
                        c.getString(1),
                        c.getString(2)
                    )
                )
            }
        }
    }

    /**
     * Vyhlada konkretny predmet podla jeho ID
     * @param id ID predmetu
     * @return [Subject] Predmet alebo null
     */
    fun getSubject(id: Long): Subject? {
        val query = with(TableSet.Subjects){"SELECT * FROM $this WHERE ${BaseColumns._ID}=$id"}
        return one(rd.rawQuery(query, arrayOf())) {
            Subject(
                it.getLong(
                    0
                ), it.getString(1), it.getString(2)
            )
        }
    }

    /**
     * Vyhlada konkretny predmet podla jeho skratky
     *
     * @param abb Skratka predmetu [String]
     * @return [Subject] Predmet alebo null
     */
    fun getSubject(abb: String): Subject? {
        val query = with(TableSet.Subjects) {"SELECT * FROM $this WHERE UPPER($ABBREVIATION)=?"}
        val arg = arrayOf(abb.toUpperCase(Locale.ROOT))
        return one(wrt.rawQuery(query, arg)) {
            Subject(it.getLong(0), it.getString(1), it.getString(2))
        }
    }

    /**
     * Vyhlada predmet podla celeho nazvu
     * @param fullName cely nazov údajne existujúceho predmetu
     * @return [Subject] Predmet alebo null
     */
    fun getSubjectByFullName(fullName: String): Subject? {
        val input = fullName.toLowerCase(Locale.ROOT).split("[^a-zA-Z0-9]+".toRegex()).joinToString("%")
        val query = "SELECT * FROM ${TableSet.Subjects} WHERE LOWER(${TableSet.Subjects.FULL_NAME}) LIKE ?"
        return one(rd.rawQuery(query, arrayOf(input))) {
            Subject(it.getLong(0), it.getString(1), it.getString(2))
        }
    }


    /**
     * Ziska data o vyucovacej hodine s danym ID
     * @param id ID hodiny [Long]
     * @return [Lesson] Hodina alebo null
     */
    fun getLesson(id: Long): Lesson? {
        val lID = BaseColumns._ID
        val s = TableSet.Subjects
        val query = with (TableSet.Lessons) {"SELECT l.$lID AS lesID,l.$DAY,l.$START," +
                "(l.$START+l.$DURATION-1) AS end,l.$PRESENTATION,s.$lID AS subID,s.${s.ABBREVIATION}," +
                "s.${s.FULL_NAME},l.$LOCATION FROM $this l JOIN $s s ON (s.$lID = l.$SUBJECT) WHERE l.$lID=$id"}

        return one(rd.rawQuery(query, arrayOf())) {
            val sub = Subject(
                it.getLong(5),
                it.getString(6),
                it.getString(7)
            )
            Lesson(
                id,
                Day[it.getInt(1)],
                it.getInt(2)..it.getInt(3),
                ScedSort[it.getInt(4)],
                sub,
                it.getString(8)
            )
        }
    }

    /**
     * Zisti najskorsi cas zaciatku vyucovania a najneskorsi cas konca vyucovania
     * @return [IntRange] čas od najskôršieho začiatku po najneskôrší koniec
     */
    fun getScedRange(): IntRange {
        val query = with(TableSet.Lessons) {"SELECT MIN($START) AS start, MAX($START + $DURATION - 1) AS end FROM $this"}
        return one(rd.rawQuery(query, arrayOf())){ if (it.isNull(0) || it.isNull(1)) 0..0 else it.getInt(0)..it.getInt(1) }!!
    }

    /**
     * Vrati vsetky vyucivacie hodiny v tabulke, prip. hodiny z konkretneho dna
     * @param day den [Day], pre ktory sa nacita rozvrh. Ak je hodnota null, nacita sa cely rozvrh
     */
    fun getScedule(day: Day? = null): ArrayList<Lesson> {
        val curs: Cursor
        with(TableSet.Lessons) {
            val id = BaseColumns._ID
            val sub = TableSet.Subjects
            val where = if (day != null) "WHERE l.$DAY=${day.position}" else ""
            curs = rd.rawQuery("SELECT l.${BaseColumns._ID} AS lesID,l.$DAY,l.$START,(l.$START+l.$DURATION-1) AS end," +
                    "l.$PRESENTATION,s.$id AS subID,s.${sub.ABBREVIATION},s.${sub.FULL_NAME},l.$LOCATION " +
                    "FROM $this l JOIN $sub s ON (s.$id = l.$SUBJECT) $where ORDER BY l.$DAY, l.$START", arrayOf())
        }

        return all(curs) {list, c ->
            val id = c.getLong(0)
            val day1: Day = Day[c.getInt(1)]
            val time = c.getInt(2)..c.getInt(3)
            val sub = Subject(
                c.getLong(5),
                c.getString(6),
                c.getString(7)
            )
            val sort = ScedSort[c.getInt(4)]
            val place = c.getString(8)
            list.add(Lesson(id, day1, time, sort, sub, place))
        }
    }

    private fun getLessonsInRange(day:Int, range:IntRange, except: Long = -1): ArrayList<Long> {
        with(TableSet.Lessons) {
            val id = BaseColumns._ID
            val leftIntruder = "${range.first} BETWEEN $START AND ($START + $DURATION - 1)"
            val rightIntruder = "${range.last} BETWEEN $START AND ($START + $DURATION - 1)"
            val innerIntruder = "$START BETWEEN ${range.first} AND ${range.last}"
            val where = "$id != $except AND $DAY = $day AND ($leftIntruder OR $rightIntruder OR $innerIntruder)"
            val query = "SELECT $id FROM $this WHERE $where ORDER BY $START"
            return all(rd.rawQuery(query, arrayOf())) {list, c -> list.add(c.getLong(0)) }
        }
    }

    /**
     * Vrati vsetky neobsadene casy v danom dni  (obcas sa neprihliada na predmety, ktore su mozno prave teraz aktualizovane)
     * @param day poradie dna v tyzdni
     * @param exception ID predmetu, pre ktorý platí vínimka (Ak sa aktualizuje, sam seba neobsadi)
     * @return [IntArray] Vrati mnozstvo zatial neobsadenych hodin
     */
    fun getFreeHours(day: Int, exception: Long = -1):IntArray {
        val query = with(TableSet.Lessons){"SELECT $START,$START+$DURATION-1 FROM $this WHERE $DAY=$day and ${BaseColumns._ID}!=$exception ORDER BY $START"}
        val ranges = all<IntRange>(rd.rawQuery(query, arrayOf())) {list, c -> list.add(c.getInt(0)..c.getInt(1)) }
        return ArrayList<Int>().apply {
            for (i in Lesson.OPENING_HOURS)
                if (ranges.isEmpty() || i !in ranges[0])
                    add(i)
                else if (ranges.isNotEmpty() && i == ranges[0].last)
                    ranges.removeAt(0)
        }.toIntArray()
    }

    /**
     * Vrati data k poznamke s danym ID
     */
    private fun getNote(id: Long): Note? {
        val query = with(TableSet.Notes) {"SELECT ${BaseColumns._ID}, $SUBJECT, $DETAIL, $DEADLINE FROM $this WHERE ${BaseColumns._ID}=$id"}
        return one (rd.rawQuery(query, arrayOf())) {
            val cal = if (it.isNull(3)) null else Calendar.getInstance().apply {
                timeInMillis
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            Note(it.getLong(0), getSubject(it.getLong(1))!!, it.getString(2), cal)
        }
    }

    /**
     * Vrati zoznam dat
     * @param sub_id ID poznamky
     * @return [ArrayList] of [Note] Zoznam úloh
     */
    fun getNotes (sub_id: Long): ArrayList<Note> {
        if (sub_id <= Note.NO_DATA) return ArrayList()

        val day = 24*60*60*1000
        val where = when (sub_id) {
            Note.DEADLINE_TODAY -> "${TableSet.Notes.DEADLINE} BETWEEN $currMillis AND $midnight"
            Note.DEADLINE_TOMORROW -> "${TableSet.Notes.DEADLINE} BETWEEN $midnight AND ${midnight + day}"
            Note.DEADLINE_THIS_WEEK -> "${TableSet.Notes.DEADLINE} BETWEEN $currMillis AND $currMillis+${7*day}"
            Note.DEADLINE_LATE -> "deadline < $currMillis"
            Note.DEADLINE_FOREVER -> "${TableSet.Notes.DEADLINE} IS NULL"
            else -> "${TableSet.Notes.SUBJECT} = $sub_id"
        }
        val select = with(TableSet.Notes) {"${BaseColumns._ID}, $SUBJECT, $DETAIL, $DEADLINE"}
        val query = with(TableSet.Notes) {"SELECT $select FROM $this WHERE $where ORDER BY $DEADLINE"}

        return all(rd.rawQuery(query, arrayOf())) { list: ArrayList<Note>, c ->
            val cal = if (c.isNull(3)) null else Calendar.getInstance().also {
                it.timeInMillis = c.getLong(3)
            }
            list.add(Note(c.getLong(0), getSubject(c.getLong(1))!!, c.getString(2), cal))
        }
    }

    /**
     * Ziska vsetky nadchadzajuce ulohy s deadlinom
     * @return [ArrayList] of [Note] Zoznam úloh
     */
    fun getDeadlinedNotes(): ArrayList<Note> {
        val sql = with (TableSet.Notes) {
            "SELECT ${BaseColumns._ID}, $SUBJECT, $DETAIL, $DEADLINE FROM $this WHERE $DEADLINE > $currMillis"
        }
        return all(rd.rawQuery(sql, arrayOf())) { list, c->
            val date = Calendar.getInstance().also {
                it.timeInMillis = c.getLong(3)
                it.set(Calendar.SECOND, 0)
                it.set(Calendar.MILLISECOND, 0)
            }
            list.add(Note(c.getLong(0), getSubject(c.getLong(1))!!, c.getString(2), date))
        }
    }




    /**
     * checkers
     */

    /**
     * vyberie vsetky po sebe iduce hodiny s rovnakymi vlastnostami
     * @return nasleduju za sebou identicke hodiny [ano / nie]
     */
    private fun hasSplitLessons(): Boolean {
        with(TableSet.Lessons) {
            val on = "r.$DAY = s.$DAY AND r.$START + r.$DURATION = s.$START AND r.$SUBJECT = " +
                    "s.$SUBJECT AND r.$LOCATION = s.$LOCATION AND r.$PRESENTATION = s.$PRESENTATION"
            val query = "SELECT r.${BaseColumns._ID}, s.${BaseColumns._ID} FROM $this r JOIN $this s ON ($on) ORDER BY r.$DAY, r.$START"
            return one(rd.rawQuery(query, arrayOf())) { true } ?: false
        }
    }

    /**
     * Zisti, ci je k predmetu pripnuta poznamka, alebo ci sa v nejakom case vyucuje
     * @param id ID kontrolovaneho predmetu
     */
    fun isSubjectObsolete(id: Long): Boolean {
        val count: Boolean
        val inLessons = "SELECT l.${TableSet.Lessons.SUBJECT} FROM ${TableSet.Lessons} l WHERE l.${TableSet.Lessons.SUBJECT} = s.${BaseColumns._ID}"
        val inNotes = "SELECT n.${TableSet.Notes.SUBJECT} FROM ${TableSet.Notes} n WHERE n.${TableSet.Notes.SUBJECT} = s.${BaseColumns._ID}"
        val query = "SELECT s.${BaseColumns._ID} FROM ${TableSet.Subjects} s " +
                "WHERE (EXISTS ($inLessons) OR EXISTS ($inNotes)) AND s.${BaseColumns._ID}=$id"

        with(rd.rawQuery(query, arrayOf())) {
            count = this.count == 0
            close()
        }
        return count
    }


    /**
     * inserts
     */

    /**
     * Nastavi alebo zmeni farbu pre dany ciel
     *
     * @param target Identifikátor farby
     * @param ahsl Farba, ktorú ukladám do databázy
     */
    fun setColor(target: String, ahsl: Ahsl) {
        wrt.execSQL(
            "INSERT OR REPLACE INTO ${TableSet.Colors} VALUES(?, ?, ?, ?, ?)",
            with(ahsl){arrayOf(target, "$h", "$s", "$l", "$a")}
        )
        ScedNoteApp.updateTableWidgets()
    }

    /**
     * Vlozi novy predmet do databazy
     *
     * @param abb Skratka vkladaneho suboru
     * @param full Cely názov vkladaného predmetu
     *
     * @return [Long] id vloženého objektu
     */
    fun insertSubject(abb: String, full: String): Long {
        val sub = getSubject(abb)
        return if (sub != null) updateSubject(sub.id, abb, full)
        else wrt.insert(TableSet.Subjects.toString(),null, ContentValues().apply{
            put(TableSet.Subjects.ABBREVIATION, abb.toUpperCase(Locale.ROOT))
            put(TableSet.Subjects.FULL_NAME, full)
        })
    }

    /**
     * Vlozi alebo aktualizuje vyucovaciu hodinu a zluci po sebe iduce duplicity
     *
     * @param les Vyučovacia hodina
     * @return [Long] ID vloženej alebo upravenej hodiny
     */
    fun insertOrUpdateLesson(les: Lesson): Long {
        val foundId = getLesson(les.id)?.id ?: -1
        val sub = getSubject(les.subject.abb)?.id ?: insertSubject(les.subject.abb, les.subject.full)
        removeLessonsInRange(les.day, les.time, les.id)
        //update
        if (foundId > 0) {
            wrt.update(TableSet.Lessons.toString(), ContentValues().apply {
                put(TableSet.Lessons.DAY, les.day.position)
                put(TableSet.Lessons.START, les.time.first)
                put(TableSet.Lessons.DURATION, les.time.count())
                put(TableSet.Lessons.PRESENTATION, les.sort.position)
                put(TableSet.Lessons.SUBJECT, sub)
                put(TableSet.Lessons.LOCATION, les.room.toUpperCase(Locale.ROOT))
            }, "${BaseColumns._ID}=${les.id}", arrayOf())
            ScedNoteApp.updateTableWidgets()
            return les.id
        }
        //or insert
        return wrt.insert(TableSet.Lessons.toString(), null, ContentValues().apply{
            put(TableSet.Lessons.SUBJECT, les.subject.id.toString())
            put(TableSet.Lessons.DAY, les.day.position)
            put(TableSet.Lessons.START, les.time.first)
            put(TableSet.Lessons.DURATION, les.time.count())
            put(TableSet.Lessons.PRESENTATION, les.sort.position)
            put(TableSet.Lessons.LOCATION, les.room.toUpperCase(Locale.ROOT))
        }).also { ScedNoteApp.updateTableWidgets() }
    }

    /**
     * Vlozi novu poznamku k predmetu
     * @param note Úloha [Note]
     * @return [Long] ID vloženej poznámky
     */
    fun insertNote(note: Note):Long {
        if (getNote(note.id) != null) throw Exception("Note already exists!")
        return (getSubject(note.sub.id) ?: getSubject(note.sub.abb))?.let {sub ->
            wrt.insert(TableSet.Notes.toString(), null, ContentValues().apply {
                put(TableSet.Notes.SUBJECT, sub.id)
                put(TableSet.Notes.DETAIL, note.info)
                note.deadline?.let { put(TableSet.Notes.DEADLINE, "${it.timeInMillis}") }
            }).also {newID ->
                ScedNoteApp.updateNoteWidgets()
                note.deadline?.let { NoteReminder.setReminder(Note(newID, note.sub, note.info, it)) }
            }
        } ?: throw Exception("Subject doesn't exist!")
    }


    /**
     * updates
     */

    /**
     * Aktualizuje data o predmete
     * @param id ID upravovaneho predmetu
     * @param abb Skratka upravovaneho predmetu
     * @param full Celý názov predmetu
     * @return [Long] ID predmetu
     */
    fun updateSubject(id: Long, abb: String, full: String): Long {
        with(TableSet.Subjects) {
            wrt.update(this.toString(), ContentValues().apply {
                put(ABBREVIATION, abb.substring(0..(abb.length-1).coerceAtMost(4)).toUpperCase(Locale.ROOT))
                put(FULL_NAME, full)
            }, "${BaseColumns._ID}=?", arrayOf(id.toString()))
        }
        ScedNoteApp.updateTableWidgets()
        return getSubject(abb)?.id ?: -1
    }

    /**
     * Zluci 2 duplicitne predmety, ak po sebe nasleduju
     *
     * @param updatingSub predmet ktorý sa menil a po zlúčení s druhým zanikne
     * @param validSub predmet, ktorý sa zachová a prevezme všetky poznámky a čas v rozvrhu
     * zo zanikajúceho predmetu
     *
     */
    fun mergeSubjects(updatingSub: Subject, validSub: Subject) {
        //obidva predmety musia existovat aby sa zlucili
        if (getSubject(updatingSub.id) != null && getSubject(validSub.id) != null) {

            NoteReminder.createNoteReminderChannel()

            //vyhladat notifikacie poznamok k predmetu, ktory prave zanikne, a zrusit ich
            val deadlinedNotes = with (TableSet.Notes) {
                "SELECT ${BaseColumns._ID} FROM $this WHERE $SUBJECT=${updatingSub.id} AND $DEADLINE " +
                        "IS NOT NULL AND $DEADLINE > $currMillis"
            }

            //ulozit id notifikacii, na ktorych sa zmena prejavi
            val notes = all<Long>(rd.rawQuery(deadlinedNotes, arrayOf())) { list, c ->
                list.add(c.getLong(0).apply { NoteReminder.cancelReminder(getNote(this)!!) })
            }

            //pripnut ovplyvnene poznamky k zachovalemmu predmetu
            wrt.update("${TableSet.Notes}", ContentValues().apply {
                put(TableSet.Notes.SUBJECT, validSub.id)
            }, "${TableSet.Notes.SUBJECT}=${updatingSub.id}", arrayOf())

            // aktualizovat notifikacie
            for (note in notes) getNote(note)?.let { NoteReminder.setReminder(it) }

            // hodiny zanikajuceho predmetu sa teraz stanu hodinami zachovaleho predmetu
            wrt.update("${TableSet.Lessons}", ContentValues().apply {
                put(TableSet.Lessons.SUBJECT, validSub.id)
            }, "${TableSet.Lessons.SUBJECT}=${updatingSub.id}", arrayOf())

            // presunut data so zanikajuceho predmetu na zachovaly predmet
            wrt.update("${TableSet.Subjects}", ContentValues().apply {
                if (updatingSub.abb.trim().isNotEmpty()) put(TableSet.Subjects.ABBREVIATION, updatingSub.abb)
                if (updatingSub.full.trim().isNotEmpty()) put(TableSet.Subjects.FULL_NAME, updatingSub.full)
            }, "${BaseColumns._ID}=${validSub.id}", arrayOf())

            //zanikajuci predmet moze konecne odist na vecny odpocinok
            removeSubject(updatingSub.id)

            //zlucenie identickych po sebe iducich hodin
            putLessonsTogether()
        }
        ScedNoteApp.updateTableWidgets()
        ScedNoteApp.updateNoteWidgets()
    }

    /**
     * hodiny s rovnakymi vlastnostami iduce po sebe bez prestavky sa zlucia dokopy
     *
     * @param day Deň v ktorom sa tento prípad ošetruje. Ak je hodnota null, kontrolujú sa všetky hodiny z celého týždňa
     */
    fun putLessonsTogether(day: Day? = null) {
        if (hasSplitLessons()) {
            val lessons = getScedule(day)
            for (index in lessons.indices) {
                if (index > 0) {
                    val prev = lessons[index - 1]
                    val recent = lessons[index]
                    if (prev == recent && prev.breaksBetween(recent))
                        insertOrUpdateLesson(
                            Lesson(
                                prev.id,
                                prev.day,
                                prev.time.first..recent.time.last,
                                prev.sort,
                                prev.subject,
                                prev.room
                            )
                        )
                }
            }
            ScedNoteApp.updateTableWidgets()
        }
    }

    /**
     * Aktualizuje ulohu pripnutu k predmetu
     * @param note Poznámka [Note] s novými platnými vlastnosťami
     * @return [Long] ID poznámky
     */
    fun updateNote (note: Note): Long {
        if (note.id > -1) {
            val oldNote = getNote(note.id)
            oldNote?.deadline?.let{ NoteReminder.cancelReminder(note) }

            wrt.update(TableSet.Notes.toString(), ContentValues().apply {
                put(TableSet.Notes.SUBJECT, note.sub.id)
                put(TableSet.Notes.DETAIL, note.info)
                note.deadline?.apply {
                    put(TableSet.Notes.DEADLINE, "$timeInMillis")
                    NoteReminder.setReminder(note)
                } ?: putNull(TableSet.Notes.DEADLINE)
            }, "${BaseColumns._ID} = ${note.id}", arrayOf())
            ScedNoteApp.updateNoteWidgets()
            return note.id
        }
        return -1
    }

    /**
     * removals
     */

    /**
     * Odstrani nadbytocne data
     * Nevyuzite predmety (ani v rozvrhu ani poznamka)
     */
    fun removeObsoleteData() {
        // uplynule poznamky
        wrt.delete("${TableSet.Notes}", "${TableSet.Notes.DEADLINE} <= $currMillis", arrayOf())
        // nevyuzite predmety
        val id = BaseColumns._ID
        val sub = TableSet.Lessons.SUBJECT
        val inLessons = "SELECT l.$sub FROM ${TableSet.Lessons} l WHERE l.$sub = s.$id"
        val inNotes = "SELECT n.$sub FROM ${TableSet.Notes} n WHERE n.$sub = s.$id"
        val subs = "SELECT s.$id FROM ${TableSet.Subjects} s WHERE NOT EXISTS ($inLessons) AND NOT EXISTS ($inNotes)"

        val ids = all<Long>(rd.rawQuery(subs, arrayOf())) { list, c ->
            list.add(c.getLong(0))
        }.joinToString(",")

        if (ids.isNotEmpty())
            wrt.delete("${TableSet.Subjects}", "${BaseColumns._ID} IN (${ids})", arrayOf())
    }

    /**
     * Odstranit predmet
     * @param id ID predmetu
     */
    fun removeSubject(id: Long) {
        val cond = arrayOf(id.toString())
        wrt.delete(TableSet.Notes.toString(), "${TableSet.Notes.SUBJECT}=?", cond)
        wrt.delete(TableSet.Lessons.toString(), "${TableSet.Lessons.SUBJECT}=?", cond)
        wrt.delete(TableSet.Subjects.toString(), "${BaseColumns._ID}=?", cond)
        ScedNoteApp.updateTableWidgets()
        ScedNoteApp.updateNoteWidgets()
    }

    /**
     * Trvalo Zrusit hodiny z rozvru, ktore obsadzuju dane časove obdobie v tyzdni
     */
    private fun removeLessonsInRange(day: Day, range: IntRange, exception: Long = -1) {
        val trash = getLessonsInRange(day.position, range, exception)
        if (trash.isNotEmpty()) {
            val cond = ArrayList<String>()
            val args = ArrayList<String>()
            for (t in trash) {
                cond.add("${BaseColumns._ID}=?")
                args.add(t.toString())
            }
            wrt.delete(TableSet.Lessons.toString(), cond.joinToString(" OR "), args.toTypedArray())
        }
    }

    /**
     * odstranit konkretnu hodinu ak existuje
     * @param id ID hodiny
     */
    fun removeLesson(id: Long) {
        wrt.delete(TableSet.Lessons.toString(), "${BaseColumns._ID}=?", arrayOf(id.toString()))
        ScedNoteApp.updateTableWidgets()
    }

    /**
     * Odstranit konkretnu poznamku ak existuje
     * @param id ID poznámky
     */
    fun removeNote (id: Long) {
        getNote(id)?.let{ NoteReminder.cancelReminder(it) }
        wrt.delete(TableSet.Notes.toString(), "${BaseColumns._ID}=$id", arrayOf())
        ScedNoteApp.updateNoteWidgets()
    }
}

