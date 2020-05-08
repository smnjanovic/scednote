package sk.scednote.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.les_item.view.*
import sk.scednote.R
import sk.scednote.model.Database
import sk.scednote.model.Day
import sk.scednote.model.Lesson

/**
 * Adapter nacitava zaznamy o vyucovacich hodinach podla vybraneho dna
 */
class ScedAdapter(list: ArrayList<Lesson>? = null): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val db = Database()
    var items = list?.also { notifyDataSetChanged() } ?: ArrayList()
    private var delete: View.OnClickListener? = null
    private var update: View.OnClickListener? = null

    /**
     * Nastavenie co sa ma stat ak tuknem na ikonu uprav
     */
    fun onUpdate(fn: (View) -> Unit) { update = View.OnClickListener { fn(it) } }

    /**
     * Nastavenie co sa ma stat ak tuknem na ikonu odstranit
     */
    fun onDelete(fn: (View) -> Unit) { delete = View.OnClickListener { fn(it) } }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ScedHolder (LayoutInflater.from(parent.context).inflate(R.layout.les_item, parent, false))
    }
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as ScedHolder).bind()
    }
    override fun getItemCount() = items.size
    override fun getItemId(position: Int) = if (position !in 0 until items.size) -1 else items[position].id

    /**
     * Odstranenie polozky
     */
    fun removeItem(pos: Int): Boolean {
        if (items.size > 0 && pos in 0..items.size) {
            db.removeLesson(items[pos].id)
            items.removeAt(pos)
            notifyItemRemoved(pos)
            return true
        }
        return false
    }

    /**
     * Nacitanie cerstvych dat
     */
    fun loadData(day: Day?) {
        items = db.getScedule(day)
        this.notifyDataSetChanged()
    }

    /**
     * Zavretie databazy
     */
    fun closeDb() {
        db.close()
    }

    inner class ScedHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val time = itemView.time
        private val sub = itemView.subject
        private val sort = itemView.sort
        private val edit = itemView.edit_les
        private val del = itemView.del_les

        /**
         * Tvorba viewHoldera
         */
        fun bind() {
            val les = items[adapterPosition]
            val f = les.time.first
            val l = les.time.last + 1

            time.text = ("${f/10}${f%10}:00 - ${l/10}${l%10}:00")
            sub.text = les.subject.abb
            sort.text = les.sort.sort

            del.tag = this
            edit.tag = this

            del.setOnClickListener(delete)
            edit.setOnClickListener(update)
        }
    }
}