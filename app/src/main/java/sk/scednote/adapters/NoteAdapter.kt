package sk.scednote.adapters

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.android.synthetic.main.note_item.view.*
import sk.scednote.activities.NoteList
import sk.scednote.R
import sk.scednote.model.Database
import sk.scednote.events.TxtValid
import sk.scednote.model.data.Note
import java.util.*

class NoteAdapter(
    cat: Long = NoteList.DEADLINE_TODAY,
    values: ArrayList<Note>? = null,
    private var lastEditData: Note? = null,
    private var newItemData: Note? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        const val TAG_MISSING_OR_INVALID = "The tag of the View must be missing or invalid!"
        const val RESTORED_DATA = "NOTE_BACKUP"
        const val LAST_EDIT = "LAST_EDIT"

        const val NEW_ITEM = "NEW_ITEM"
        private const val VT_OLD = 1000

        private const val VT_NEW = 1001
    }

    private val data = Database()
    private var category = cat
    private val items = values ?: data.getNotes(cat).apply {
        //ak je category kladne cele cislo, jedna sa o id predmetu
        if (category > 0) {
            //vytvorenie prazdnej polozky, ktora ma za ulohu vytvarat nove poznamky
            data.getSubject(category)?.let { add(Note(-1, it, "")) } ?:
            throw Exception("Unexcepted error! Database contains garbage data in the 'notes' " +
                    "table. No such subject matching the id of '$category' exists!")
        }
    }
    private var lastEditedItem: OldNoteHolder? = null
    private var newItem: NewNoteHolder? = null

    /**
     * handlery udalosti
     */

    private var dateSetterFunction: (NoteHolder, Calendar)->Unit = fun(_, _){}
    fun onDateTimeChange(fn: (NoteHolder, Calendar)->Unit) {
        dateSetterFunction = fn
    }

    private val dateSetter = View.OnClickListener {
        if (it.tag !is NoteHolder) throw ClassCastException(TAG_MISSING_OR_INVALID)
        with (it.tag as NoteHolder) {
            dateSetterFunction(this, items[this.adapterPosition].deadline ?: Calendar.getInstance())
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
        (it.tag as NewNoteHolder).apply { insertRecord(adapterPosition, parseNote()) }
    }
    private val updateAttempt = View.OnClickListener {view ->
        if (view.tag !is OldNoteHolder) throw Exception(TAG_MISSING_OR_INVALID)
        (view.tag as OldNoteHolder).apply {
            if (itemView.editDetail.error == null)
                updateRecord(adapterPosition, parseNote())
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

    /**
     * Manipulacia s trvalym uloziskom dat a viditelnym zoznamom
     */

    private fun insertRecord(position: Int, note: Note) {
        val id = data.insertNote(note)
        if (id > -1) {
            items[position] = Note(id, note.sub, note.info, note.deadline)
            items.add(Note(-1, note.sub, ""))
            notifyItemChanged(position)
            notifyItemInserted(position+1)
        }
    }
    private fun updateRecord(position: Int, note: Note) {
        if (note.id > 0) {
            data.updateNote(note)
            items[position] = note
            notifyItemChanged(position)
        }
    }
    private fun removeRecord(position: Int) {
        if (getItemViewType(position) == VT_OLD) {
            data.removeNote(items[position].id)
            items.removeAt(position)
            notifyItemRemoved(position)
        }
    }
    fun loadData(cat: Long) {
        if (cat > NoteList.NO_DATA) {
            items.clear()
            newItem = null
            lastEditedItem = null
            items.addAll(data.getNotes(cat))
            category = cat
            if (cat > 0)
                data.getSubject(cat)?.let { items.add(Note(-1, it, "")) }
            notifyDataSetChanged()
        }
    }
    fun backupData(b: Bundle) {
        b.putParcelableArrayList(RESTORED_DATA, items)
        b.putParcelable(LAST_EDIT, lastEditedItem?.parseNote())
        // nie pre kazdu kategoriu smiem pridavat poznamku (nemoze pridat oneskorenu poznamku napr.)
        b.putParcelable(NEW_ITEM, newItem?.parseNote())
    }
    fun getItemPositionById(id: Long): Int {
        for (i in items.indices)
            if (items[i].id == id)
                return i
        return -1
    }

    fun close() {
        data.close()
    }

    /**
     * kod adapteru
     */

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when(viewType) {
            VT_OLD -> OldNoteHolder(inflater.inflate(R.layout.note_item, parent, false))
            VT_NEW -> NewNoteHolder(inflater.inflate(R.layout.note_item, parent, false))
            else -> throw Exception("NO SUCH TYPE ON SUBJECT ADAPTER!")
        }
    }
    override fun getItemCount(): Int {
        return items.size
    }
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is NoteHolder) holder.bind()
    }
    override fun getItemId(position: Int): Long {
        return items[position].id
    }
    override fun getItemViewType(position: Int): Int {
        return if (getItemId(position) < 1) VT_NEW else VT_OLD
    }

    /**
     * ViewHoldery
     */
    abstract inner class NoteHolder(viewItem: View) : RecyclerView.ViewHolder(viewItem) {
        private val sub: TextView = viewItem.sub_abb

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
        }

        //z viewholdera spakujem data do prislusneho datove objektu v ktorom to poslem do databazy
        fun parseNote(): Note {
            return with(items[adapterPosition]) { Note(id, sub, (if (editMode) detailEdit else detailRead).text.toString(), deadline) }
        }

        //existujuce poznamky, ak nie su v rezime uprav budu musiet trvalo ulozit zmeny
        open fun onSetDeadline(calendar: Calendar) {
            show(clearDate)
            with(items[adapterPosition]) {
                deadline = calendar
                datetime.text = ddlItem
            }
            //pokracovanie v potomkoch
        }

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
    inner class OldNoteHolder(viewItem: View): NoteHolder(viewItem) {
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

        override fun onSetDeadline(calendar: Calendar) {
            super.onSetDeadline(calendar)
            if (readMode) {
                items[adapterPosition].deadline = calendar
                updateRecord(adapterPosition, parseNote())
            }
        }

        override fun onUnsetDeadline() {
            super.onUnsetDeadline()
            if (readMode) updateRecord(adapterPosition, parseNote())
        }
    }
    inner class NewNoteHolder(viewItem: View): NoteHolder(viewItem) {
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
        }

        fun clearInput() {
            detailEdit.setText("")
            detailRead.text = ""
            onUnsetDeadline()
        }
    }
}