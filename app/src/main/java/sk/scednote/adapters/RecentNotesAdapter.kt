package sk.scednote.adapters

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.note_item_delete_only.view.*
import sk.scednote.R
import sk.scednote.ScedNoteApp
import sk.scednote.model.Note
import java.util.*

/**
 * Adapter zobrazujuci zoznam uloh v terminoch v tomto tyzdni
 * @param bdl zalohovane dáta
 */
class RecentNotesAdapter(bdl: Bundle?): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        private const val NOT_HOLDER = "Clickable items need their holders asigned to their tags!"
        private const val BACKUP = "BACKUP"
    }

    val data = ScedNoteApp.database
    val items: ArrayList<Note> = bdl?.getParcelableArrayList(BACKUP) ?: data.getNotes(
        Note.DEADLINE_THIS_WEEK)

    /**
     * Načítanie čerstvých dát
     */
    fun reload() {
        items.clear()
        items.addAll(data.getNotes(Note.DEADLINE_THIS_WEEK))
        notifyDataSetChanged()
    }

    // funkcia ktora presmeruje uzivatela na aktivitu so zoznamom. treba ju nastavit v aktivite
    private var redirect:(Long, Long)-> Unit = fun (_, _) {}

    /**
     * čo sa má stať po kliknutí na položku
     * @param fn Funkcia
     */
    fun setOnNoteNavigate(fn: (Long, Long)-> Unit) { redirect = fn }

    /**
     * Zálohovanie dát
     */
    fun storeBackup(bdl: Bundle) {
        bdl.putParcelableArrayList(BACKUP, items)
    }

    //udalosť presmerovania na inú aktivitu
    val noteRedirect = View.OnClickListener {
        if (it.tag !is RecentNotesHolder) throw (ClassCastException(NOT_HOLDER))
        with (it.tag as RecentNotesHolder) {
            val item = items[adapterPosition]
            val nextMidnight = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val midnightAfter = Calendar.getInstance().apply {
                timeInMillis = nextMidnight.timeInMillis + 24 * 60 * 60 * 1000 - 1
            }
            val category = when {
                it == itemView.abb || item.deadline == null -> item.sub.id
                item.deadline!! < Calendar.getInstance() -> Note.DEADLINE_LATE
                item.deadline!! < nextMidnight -> Note.DEADLINE_TODAY
                item.deadline!! < midnightAfter -> Note.DEADLINE_TOMORROW
                else -> item.sub.id
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
        }
    }

    /**
     * Tvorba view holdera
     * @param parent Priamy predok
     * @param viewType Typ ViewHoldera
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return RecentNotesHolder(LayoutInflater.from(parent.context).inflate(R.layout.note_item_delete_only, parent, false))
    }

    /**
     * Vráti počet záznamov
     * @return Počet záznamov
     */
    override fun getItemCount() = items.size

    /**
     * @param holder ViewHolder
     * @param position Pozícia dát v zozname
     */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as RecentNotesHolder).bind()
    }

    inner class RecentNotesHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val sub: TextView = itemView.abb
        private val date: TextView = itemView.datetime
        private val info: TextView = itemView.detail
        private val del: ImageButton = itemView.delete

        /**
         * Vytvorenie viditelnej polozky reprezentujucej urcitu poznamku. Da sa len vymazat a odkazuje na aktivitu, kde sa nachadza
         */
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