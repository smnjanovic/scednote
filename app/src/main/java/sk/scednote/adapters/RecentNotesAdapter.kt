package sk.scednote.adapters

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.note_item_delete_only.view.*
import sk.scednote.activities.NoteList
import sk.scednote.R
import sk.scednote.model.Database
import sk.scednote.model.data.Note
import java.lang.ClassCastException
import java.util.*
import kotlin.collections.ArrayList

class RecentNotesAdapter(db: Database?, bdl: Bundle?): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        private const val NOT_HOLDER = "Clickable items need their holders asigned to their tags!"
        private const val BACKUP = "BACKUP"
    }

    val data = db ?: Database()
    val items: ArrayList<Note> = bdl?.getParcelableArrayList(BACKUP) ?: data.getWeekNotes()

    fun reload() {
        items.clear()
        items.addAll(data.getWeekNotes())
        notifyDataSetChanged()
    }
    /**
     * @long1 category
     * @long2 note ID
     */
    // funkcia ktora presmeruje uzivatela na aktivitu so zoznamom. treba ju nastavit v aktivite
    private var redirect:(Long, Long)-> Unit = fun (_, _) {}
    private var notifyIfEmpty: ()-> Unit = fun () {}

    fun setOnNoteNavigate(fn: (Long, Long)-> Unit) { redirect = fn }
    fun setOnNotifyIfEmpty(fn: ()-> Unit) { fn() }

    fun storeBackup(bdl: Bundle) {
        bdl.putParcelableArrayList(BACKUP, items)
    }

    val noteRedirect = View.OnClickListener {
        if (it.tag !is RecentNotesHolder) throw (ClassCastException(NOT_HOLDER))
        with (it.tag as RecentNotesHolder) {
            val item = items[adapterPosition]
            val nextMidnight = Calendar.getInstance().apply {
                set(
                    this.get(Calendar.YEAR),
                    this.get(Calendar.MONTH),
                    this.get(Calendar.DAY_OF_MONTH) + 1,
                    0,
                    0
                )
            }
            val midnightAfter = Calendar.getInstance().apply {
                set(
                    nextMidnight.get(Calendar.YEAR),
                    nextMidnight.get(Calendar.MONTH),
                    nextMidnight.get(Calendar.DAY_OF_MONTH) + 1,
                    0,
                    0
                )
            }
            val category = when {
                it == itemView.abb || item.deadline == null -> item.sub.id!!
                item.deadline!! < Calendar.getInstance() -> NoteList.DEADLINE_TIME_OUT
                item.deadline!! < nextMidnight -> NoteList.DEADLINE_TODAY
                item.deadline!! < midnightAfter -> NoteList.DEADLINE_TOMORROW
                else -> item.sub.id!!
            }
            redirect(category, item.id)
        }
    }

    val onDelete = View.OnClickListener {
        if (it.tag !is RecentNotesHolder) throw (ClassCastException(NOT_HOLDER))
        with(it.tag as RecentNotesHolder) {
            val pos = adapterPosition
            data.removeNote(items[pos].id)
            items.removeAt(pos)
            notifyItemRemoved(pos)
            notifyIfEmpty()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return RecentNotesHolder(LayoutInflater.from(parent.context).inflate(R.layout.note_item_delete_only, parent, false))
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as RecentNotesHolder).bind()
    }

    inner class RecentNotesHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val sub: TextView = itemView.abb
        private val date: TextView = itemView.datetime
        private val info: TextView = itemView.detail
        private val del: ImageButton = itemView.delete

        fun bind() {
            val item = items[adapterPosition]
            sub.text = item.sub.abb
            date.text = item.ddlItem
            info.text = item.info

            sub.tag = this
            date.tag = this
            info.tag = this
            del.tag = this

            sub.setOnClickListener(noteRedirect)
            date.setOnClickListener(noteRedirect)
            info.setOnClickListener(noteRedirect)
            del.setOnClickListener(onDelete)
        }
    }

}