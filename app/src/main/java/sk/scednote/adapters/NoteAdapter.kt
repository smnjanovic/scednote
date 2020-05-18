package sk.scednote.adapters

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.android.synthetic.main.note_item.view.*
import sk.scednote.R
import sk.scednote.ScedNoteApp
import sk.scednote.events.TxtValid
import sk.scednote.model.Note
import sk.scednote.model.Subject
import java.util.*
import java.util.Calendar.getInstance
import kotlin.collections.ArrayList

/**
 * Model nacita zoznam uloh podla vybranej kategorie a vytvara pohlady (MVC).
 * Komunikuje s databazou, t.j. vytvara, aktualizuje a vymazava data
 * @param cat kategória alebo ID predmetu
 * @param bundle záloha dát
 */
class NoteAdapter(cat: Long = Note.DEADLINE_TODAY, bundle: Bundle?) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        const val TAG_MISSING_OR_INVALID = "The tag of the View must be missing or invalid!"
        const val RESTORED_DATA = "NOTE_BACKUP"
        const val LAST_EDIT = "LAST_EDIT"

        const val NEW_ITEM = "NEW_ITEM"
        private const val VT_OLD = 1000 //existujuca poznamka - vymazat, aktualizovat
        private const val VT_NEW = 1001 //nova poznamka - vymazat
    }


    private val data = ScedNoteApp.database
    private var category = cat
    val subjects = data.loadSubjects()

    private var lastEditData: Note? = bundle?.getParcelable(LAST_EDIT)
    private var newItemData: Note? = bundle?.getParcelable(NEW_ITEM)

    private val items = bundle?.getParcelableArrayList(RESTORED_DATA) ?: data.getNotes(cat)

    private var lastEditedItem: OldNoteHolder? = null
    private var newItem: NewNoteHolder? = null

    init { addNewItemIfValid() }

    /*
     * handlery udalosti
     */

    private var dateSetterFunction: (NoteHolder, Calendar)->Unit = fun(_, _){}

    /**
     * @param fn Čo sa ná stať po kliknutí na tlačidlo nastavovania času
     */
    fun onDateTimeChange(fn: (NoteHolder, Calendar)->Unit) {
        dateSetterFunction = fn
    }

    private val dateSetter = View.OnClickListener {
        if (it.tag !is NoteHolder) throw ClassCastException(TAG_MISSING_OR_INVALID)
        with (it.tag as NoteHolder) {
            dateSetterFunction(this, items[this.adapterPosition].deadline ?: getInstance())
        }
    }

    private val dateUnsetter = View.OnClickListener {
        if (it.tag !is NoteHolder) throw Exception(TAG_MISSING_OR_INVALID)
        val holder = it.tag as NoteHolder
        holder.onUnsetDeadline()
    }
    private val editAttempt = View.OnClickListener {view ->
        if (view.tag !is NoteHolder) throw Exception(TAG_MISSING_OR_INVALID)
        val holder = (view.tag as NoteHolder)
        holder.editMode = true

        lastEditedItem?.let {
            if (it.itemView.editDetail.error == null)
                updateRecord(it.adapterPosition, it.parseNote())
            it.readMode = true
        }
        lastEditedItem = if (holder !is NewNoteHolder) holder as OldNoteHolder else null
    }
    private val insertAttempt = View.OnClickListener {
        if (it.tag !is NewNoteHolder) throw Exception(TAG_MISSING_OR_INVALID)
        (it.tag as NewNoteHolder).apply {
            subject?.let {
                insertRecord(adapterPosition, parseNote())
                itemView.editDetailText.clearFocus()
            } ?: Toast.makeText(ScedNoteApp.ctx, ScedNoteApp.res.getString(R.string.no_subject_no_note),
                Toast.LENGTH_SHORT).show()
        }
    }
    private val updateAttempt = View.OnClickListener {view ->
        if (view.tag !is OldNoteHolder) throw Exception(TAG_MISSING_OR_INVALID)
        (view.tag as OldNoteHolder).apply {
            when {
                itemView.editDetail.error != null -> Toast.makeText(ScedNoteApp.ctx, ScedNoteApp.res.
                    getString(R.string.invalid_chars), Toast.LENGTH_SHORT).show()
                items[adapterPosition].deadline?.timeInMillis?.let { System.currentTimeMillis() + 1500 < it } == false ->
                    Toast.makeText(ScedNoteApp.ctx, ScedNoteApp.res.getString(R.string.time_out), Toast.LENGTH_SHORT).show()
                else -> updateRecord(adapterPosition, parseNote())
            }
            readMode = true
            lastEditedItem = null
        }
    }
    private val deleteAttemt = View.OnClickListener {
        if (it.tag !is OldNoteHolder) throw Exception(TAG_MISSING_OR_INVALID)
        val holder = (it.tag as OldNoteHolder)
        if (holder == lastEditedItem) lastEditedItem = null
        removeRecord(holder.adapterPosition)
    }
    private val clearAttempt = View.OnClickListener {
        if (it.tag !is NewNoteHolder) throw Exception(TAG_MISSING_OR_INVALID)
        (it.tag as NewNoteHolder).clearInput()
    }

    //pridanie doplnkoveho prvku z ktoreho vytvaram novu poznamku (ak to dana kategoria dovoli)
    private fun addNewItemIfValid() {
        fun add (sub: Subject) {
            if (items.size == 0 || getItemViewType(items.size - 1) != VT_NEW) {
                items.add(Note(-1, sub, ""))
            }
        }

        if (subjects.size > 0) {
            when(category) {
                 Note.DEADLINE_TODAY, Note.DEADLINE_TOMORROW, Note.DEADLINE_THIS_WEEK, Note.DEADLINE_FOREVER ->
                    add(Subject(-1, "", ""))
                 else -> if (category > 0) data.getSubject(category)?.let { add(it) }
            }
        }
    }

    //kontrola ci pridany alebo upraveny predmet este patri do tejto kategorie
    private fun belongs(note: Note): Boolean {
        return category > 0 && category == note.sub.id || when (category) {
            Note.DEADLINE_TODAY -> {
                note.deadline?.let{
                    with (getInstance()) {
                        val now = this.timeInMillis
                        val midnight = this.apply {
                            add(Calendar.DAY_OF_MONTH, 1)
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                        }.timeInMillis
                        it.timeInMillis in now until midnight
                    }
                } ?: false
            }
            Note.DEADLINE_TOMORROW -> {
                note.deadline?.let{
                    val tomorrowStart = getInstance().apply {
                        add(Calendar.DAY_OF_MONTH, 1)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                    }
                    it.timeInMillis in tomorrowStart.timeInMillis until tomorrowStart.apply { add(Calendar.DAY_OF_MONTH, 1) }.timeInMillis
                } ?: false
            }
            Note.DEADLINE_THIS_WEEK -> {
                note.deadline?.let{
                    with (getInstance()) {
                        it.timeInMillis in this.timeInMillis until this.apply { add(Calendar.DAY_OF_MONTH, 7) }.timeInMillis
                    }
                } ?: false
            }
            Note.DEADLINE_FOREVER -> note.deadline?.let{ false } ?: true
            else -> false
        }
    }

    /**
     * Manipulacia s trvalym uloziskom dat a viditelnym zoznamom
     */

    private fun insertRecord(position: Int, note: Note) {
        val id = data.insertNote(note)
        if (id > -1) {
            if (belongs(note)) {
                items[position] = Note(id, note.sub, note.info, note.deadline)
                items.add(Note(-1, note.sub, ""))
                notifyItemChanged(position)
                notifyItemInserted(position+1)
            }
            else {
                notifyItemChanged(position)
                Toast.makeText(ScedNoteApp.ctx, ScedNoteApp.res.getString(R.string.category_expired), Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun updateRecord(position: Int, note: Note) {
        if (note.id > 0) {
            data.updateNote(note)
            if (belongs(note)) {
                items[position] = note
                notifyItemChanged(position)
            }
            else {
                items.removeAt(position)
                notifyItemRemoved(position)
                Toast.makeText(ScedNoteApp.ctx, ScedNoteApp.res.getString(R.string.category_expired), Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun removeRecord(position: Int) {
        if (getItemViewType(position) == VT_OLD) {
            data.removeNote(items[position].id)
            items.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    /**
     * Nacitanie dat podla novej kategorie
     * @param cat zvolená kategória alebo ID predmetu
     */
    fun loadData(cat: Long) {
        if (cat > Note.NO_DATA) {
            items.clear()
            newItem = null
            lastEditedItem = null
            items.addAll(data.getNotes(cat))
            category = cat
            addNewItemIfValid()
            notifyDataSetChanged()
        }
    }

    /**
     * Zalohovanie dat (pred otocenim displeja alebo zavretim aplikacie po dlhej necinnosti)
     * @param b Zdroj zálohy
     */
    fun backupData(b: Bundle) {
        b.putParcelableArrayList(RESTORED_DATA, items)
        b.putParcelable(LAST_EDIT, lastEditedItem?.parseNote())
        // nie pre kazdu kategoriu smiem pridavat poznamku (nemoze pridat oneskorenu poznamku napr.)
        b.putParcelable(NEW_ITEM, newItem?.parseNote())
    }

    /**
     * najde na ktorej pozicii sa predmet s danym id nachadza (zlozita funkcia O(n))
     * @param id ID poznámky
     * @return pozícia poznámky s id [id]
     */
    fun getItemPositionById(id: Long): Int {
        for (i in items.indices)
            if (items[i].id == id)
                return i
        return -1
    }

    /**
     * Zavretie databazy
     */
    fun close() {
        data.close()
    }

    /**
     * kod adapteru
     */

    /**
     * @param parent priamy predok
     * @param viewType typ ViewHoldera
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when(viewType) {
            VT_OLD -> OldNoteHolder(inflater.inflate(R.layout.note_item, parent, false))
            VT_NEW -> NewNoteHolder(inflater.inflate(R.layout.note_item, parent, false))
            else -> throw Exception("NO SUCH TYPE ON SUBJECT ADAPTER!")
        }
    }

    /**
     * @return [Int] Počet prvkov
     */
    override fun getItemCount() = items.size

    /**
     * Tvorba alebo zmena obsahu šablóny pre položku v indexe [position]
     * @param holder ViewHolder
     * @param position index
     */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is NoteHolder) holder.bind()
    }

    /**
     * @param position idnex
     * @return [Long] ID poznámky
     */
    override fun getItemId(position: Int) = items[position].id

    /**
     * @param position index
     * @return [Int] typ ViewHoldera
     */
    override fun getItemViewType(position: Int) = if (items[position].id < 1) VT_NEW else VT_OLD

    /**
     * ViewHoldery
     */
    abstract inner class NoteHolder(viewItem: View) : RecyclerView.ViewHolder(viewItem) {
        protected val sub: TextView = viewItem.sub_abb
        protected val subChoice: Spinner = viewItem.sub_choice

        protected val datetime: TextView = viewItem.datetimeDisplay
        private val addDate: ImageButton = viewItem.addDate
        private val clearDate: ImageButton = viewItem.clearDate

        protected val detailRead: TextView = viewItem.detail
        private val detailEditLayout: TextInputLayout = viewItem.editDetail
        protected val detailEdit: TextInputEditText = viewItem.editDetailText

        protected val btnSave: ImageButton = viewItem.saveNote
        private val btnEdit: ImageButton = viewItem.editNote
        protected val btnDelete: ImageButton = viewItem.removeNote

        var editMode: Boolean
            get() {
                val hidden = arrayOf(detailRead.visibility, btnEdit.visibility)
                val shown = arrayOf(detailEditLayout.visibility, btnSave.visibility)
                return View.VISIBLE !in hidden && View.GONE !in shown
            }
            set(boolean) {
                if (!boolean) readMode = true
                else setEditable()
            }

        var readMode: Boolean
            get() = !editMode
            set(boolean) {
                if (!boolean) editMode = true
                else setReadable()
            }


        private fun hide(view: View) {
            view.visibility = View.GONE
        }
        private fun show(view: View) {
            view.visibility = View.VISIBLE
        }

        /**
         * Vytvori 2D pohlad pre novy záznam, vyplní dáta, nastaví udalosti spolocne pre obidva
         * typy ViewHoldera (novy na vytvorenie / stary na upravu).
         */
        open fun bind() {
            val item = items[adapterPosition]
            sub.text = item.sub.abb
            datetime.text = item.ddlItem
            detailRead.text = item.info
            detailEdit.setText(item.info)
            item.deadline?.let { show(clearDate) } ?: hide(clearDate)

            //tagy
            datetime.tag = this
            addDate.tag = this
            clearDate.tag = this
            detailRead.tag = this
            detailEdit.tag = this
            detailEditLayout.tag = this
            btnSave.tag = this
            btnEdit.tag = this
            btnDelete.tag = this

            //udalosti
            addDate.setOnClickListener(dateSetter)
            clearDate.setOnClickListener(dateUnsetter)

            //regex znemoznuje zacinat text prazdnym znakom a ukoncit ho viac ako 1 prazdnym znakom
            val rgx = "(^[^\\s]\\s?+$)|(^[^\\s]+(\\s|.)*[^\\s]\\s?$)"
            detailEdit.addTextChangedListener(
                TxtValid(
                    detailEdit,
                    rgx,
                    1..256,
                    btnSave
                )
            )

            btnEdit.setOnClickListener(editAttempt)
            detailRead.setOnClickListener(editAttempt)
        }

        private fun setEditable() {
            hide(detailRead)
            hide(btnEdit)
            show(detailEditLayout)
            show(btnSave)
            detailEdit.setText(detailRead.text)
            btnSave.isEnabled = detailEdit.error == null && detailEdit.text!!.trim().isNotEmpty()
        }
        private fun setReadable() {
            hide(detailEditLayout)
            hide(btnSave)
            show(detailRead)
            show(btnEdit)
            detailEdit.error?.let { detailRead.text = detailEdit.text }
            detailEdit.clearFocus()
        }

        /**
         * z dat z viewholdera vytvorim novy objekt Note
         * @return [Note] úloha v aktuálnom stave
         */
        open fun parseNote() = with(items[adapterPosition]) {
            Note(id, sub, (if (editMode) detailEdit else detailRead).text.toString(), deadline)
        }

        /**
         * nastavi a zobrazi datum v danej polozke
         * @param calendar Dátum
         */
        open fun onSetDeadline(calendar: Calendar) {
            show(clearDate)
            with(items[adapterPosition]) {
                deadline = calendar
                datetime.text = ddlItem
            }
        }

        /**
         * odstrani datum v danej polozke
         */
        open fun onUnsetDeadline() {
            hide(clearDate)
            with(items[adapterPosition]) {
                deadline = null
                datetime.text = ""
            }
            show(addDate)
            hide(clearDate)
        }
    }

    /**
     * ViewType: Old
     * metody a udalosti unikatne pre polozky existujucich poznamok
     * @param viewItem priamy predok obsahujúci tento layout
     */
    inner class OldNoteHolder(viewItem: View): NoteHolder(viewItem) {
        /**
         * Doplnenie NoteHolder: Co sa stane ak dam zmeny potvrdit alebo stlacim ikonu pre odstranenie suboru
         */
        override fun bind() {
            super.bind()
            btnDelete.setOnClickListener(deleteAttemt)
            btnSave.setOnClickListener(updateAttempt)
            //zachovanie upravovaneho obsahu po otoceni
            if (lastEditData != null && lastEditData!!.id == items[adapterPosition].id) {
                editMode = true
                datetime.text = lastEditData!!.ddlItem
                detailRead.text = lastEditData!!.info
                detailEdit.setText(lastEditData!!.info)
                lastEditedItem = this
                lastEditData = null
            }
            else readMode = true
        }

        /**
         * Ak existujuca poznamka nie je v rezime uprav, okamzite po zmene datumu sa zmena prejavi
         * aj v databaze
         * @param calendar Dátum
         */
        override fun onSetDeadline(calendar: Calendar) {
            super.onSetDeadline(calendar)
            if (readMode)
                updateRecord(adapterPosition, parseNote())
        }

        /**
         * Ak existujuca poznamka nie je v rezime uprav, okamzite po odstraneni datumu sa zmena prejavi
         * aj v databaze
         */
        override fun onUnsetDeadline() {
            super.onUnsetDeadline()
            if (readMode)
                updateRecord(adapterPosition, parseNote())
        }
    }

    /**
     * ViewType: NEW
     *
     * Funkcie pre view holder vznikajucej polozky
     * @param viewItem Priamy predok obsahujúci layout šablóny
     */
    inner class NewNoteHolder(viewItem: View): NoteHolder(viewItem) {
        /**
         * kontrola ci existuje predmet ku ktoremu bude nova poznamka pripnuta
         * vrati null ak neexistuje a Subject ak existuje
         */
        val subject: Subject? get() = when {
            subjects.size == 0 -> null
            category > 0 -> items[adapterPosition].sub
            else -> subjects[subChoice.selectedItemPosition]
        }
        /**
         * Doplnenie NoteHolder: Co sa stane ak dam zmeny potvrdit alebo stlacim ikonu odstranenia
         */
        override fun bind() {
            super.bind()
            btnDelete.setOnClickListener(clearAttempt)
            btnSave.setOnClickListener(insertAttempt)
            editMode = true
            newItem = this
            //obnovenie hodnot po otoceni displeja
            newItemData?.let {note ->
                datetime.text = note.ddlItem
                detailEdit.setText(note.info)
            }
            newItemData = null

            subChoice.adapter = ArrayAdapter(ScedNoteApp.ctx, android.R.layout.simple_list_item_1, ArrayList<String>().apply {
                for (s in subjects)
                    add(s.abb)
            })

            sub.visibility = if (category <= 0) View.GONE else View.VISIBLE
            subChoice.visibility = if (category > 0) View.GONE else View.VISIBLE
        }

        /**
         * Neodstrani polozku, ktora neexistuje, iba ju ocisti od obsahu
         */
        fun clearInput() {
            detailEdit.setText("")
            detailRead.text = ""
            if (category > 0 || category != Note.DEADLINE_FOREVER) onUnsetDeadline()
        }

        /**
         * Iny sposob ako vytvorit poznamku na zaklade dat vo view holderi.
         * @return [Note] Úloha v aktuálnom stave
         */
        override fun parseNote() = Note(
            -1, subject ?: Subject(-1, "", ""),
            detailEdit.text.toString(), items[adapterPosition].deadline
        )
    }
}