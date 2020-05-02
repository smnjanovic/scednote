package sk.scednote.adapters

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.sub_item.view.*
import kotlinx.android.synthetic.main.tab_button.view.*
import sk.scednote.R
import sk.scednote.model.Database
import sk.scednote.model.data.Subject

open class SubjectAdapter(private val itemType: Int, subjects: ArrayList<Subject>? = null): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        const val VIEW_TYPE_TAB = 1000  // tlacidlo v navigacii
        const val VIEW_TYPE_ITEM = 1001 // polozka v zozname
    }

    private val data = Database()
    private var items: ArrayList<Subject> = subjects ?: data.loadSubjects()

    //kliknutie na ikonu edit - uprava predmetu
    private var editEvent: View.OnClickListener? = null
    //kliknutie na polozku v navigacii
    private var choiceEvent: View.OnClickListener? = null
    private var deleteEvent: View.OnClickListener? = null

    //editor uprav sa nastavi len raz
    fun setEditEvent(edit: View.OnClickListener) {
        editEvent = editEvent ?: edit
    }
    fun setDeleteEvent(delete: View.OnClickListener) {
        deleteEvent = deleteEvent ?: delete
    }
    fun setChoiceEvent(choice: View.OnClickListener) {
        choiceEvent = choiceEvent ?: choice
    }
    fun backupData(bundle: Bundle, key: String) {
        bundle.putParcelableArrayList(key, items)
    }

    fun updatedRecord(id: Long) {
        val position = getPositionById(id)
        if (position in 0 until itemCount) {
            data.getSubject(id)?.let { items[position] = it }
            notifyItemChanged(position)
        }
    }
    //zaznam bol vlozeny, dostal som jeho id
    fun insertedRecord(id: Long) {
        data.getSubject(id)?.let {
            items.add(it)
            notifyItemInserted(items.size)
        }
    }

    //vymazavame priamo v aktivite
    fun deleteRecord(pos:Int) {
        if (pos in items.indices) {
            data.removeSubject(items[pos].id!!)
            items.removeAt(pos)
            notifyItemRemoved(pos)
        }
    }
    fun reload() {
        items = data.loadSubjects().also { notifyDataSetChanged() }
    }

    fun getPositionById(id: Long): Int {
        for (i in items.indices) {
            if (items[i].id == id) {
                return i
            }
        }
        return -1
    }

    fun close() {
        data.close()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when(itemType) {
            VIEW_TYPE_ITEM -> ItemHolder(inflater.inflate(R.layout.sub_item, parent, false))
            VIEW_TYPE_TAB -> TabHolder(inflater.inflate(R.layout.tab_button, parent, false))
            else -> throw Exception("NO SUCH TYPE ON SUBJECT ADAPTER!")
        }
    }
    override fun getItemCount(): Int {
        return items.size
    }
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is SubjectHolder) holder.bind()
    }

    override fun getItemId(position: Int): Long {
        return if (position in items.indices) items[position].id ?: -1 else -1
    }

    abstract inner class SubjectHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        abstract fun bind()
        fun getSubject(): Subject {
            return items[adapterPosition]
        }
    }
    inner class ItemHolder(itemView: View): SubjectHolder(itemView) {
        private val abbreviation = itemView.abb
        private val fullSubName = itemView.full
        private val pencil = itemView.edit
        private val trash = itemView.delete

        override fun bind() {
            val sub = items[adapterPosition]
            abbreviation.text = sub.abb
            fullSubName.text = sub.full
            pencil.setOnClickListener(editEvent)
            trash.setOnClickListener(deleteEvent)
            pencil.tag = this
            trash.tag = this
        }

        fun isObsolete(): Boolean {
            return data.isSubjectObsolete(items[adapterPosition].id!!)
        }
    }
    inner class TabHolder(itemView: View): SubjectHolder(itemView) {
        private val button = itemView.tab
        override fun bind() {
            with (button) {
                text = items[adapterPosition].abb
                tag = this@TabHolder
                setOnClickListener(choiceEvent)
            }
        }
    }
}