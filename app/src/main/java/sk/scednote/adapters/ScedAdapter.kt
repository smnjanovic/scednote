package sk.scednote.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.les_item.view.*
import sk.scednote.EditLesson
import sk.scednote.R
import sk.scednote.model.Database
import sk.scednote.model.data.Day
import sk.scednote.model.data.Lesson

open class ScedAdapter(context: Context, list: ArrayList<Lesson>? = null): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val cnt = context
    private val db = Database(cnt)
    var items = list?.also { notifyDataSetChanged() } ?: ArrayList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ScedHolder (LayoutInflater.from(parent.context).inflate(R.layout.les_item, parent, false), cnt)
    }
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as ScedHolder).bind(this, items[position], position)
    }
    override fun getItemCount(): Int {
        return items.size
    }

    fun removeItem(pos: Int): Boolean {
        if (items.size > 0 && pos in 0..items.size) {
            db.removeLesson(items[pos].id)
            items.removeAt(pos)
            notifyDataSetChanged()
            return true
        }
        return false
    }
    fun loadData(day: Day?) {
        items = db.getScedule(day)
        this.notifyDataSetChanged()
    }
    fun closeDb() {
        db.close()
    }

    private inner class ScedHolder (itemView: View, context: Context): RecyclerView.ViewHolder(itemView) {
        private val cnt = context
        private val time = itemView.time
        private val sub = itemView.subject
        private val sort = itemView.sort
        private val edit = itemView.edit_les
        private val del = itemView.del_les
        fun bind(adapter: ScedAdapter, les: Lesson, pos: Int) {
            time.text = ("${dig2(les.time.first)}:00 - ${dig2(les.time.last + 1)}:00")
            sub.text = les.subject.abb
            sort.text = les.sort.getSort(cnt)
            //tagy s indexom 0 obsahuju id hodiny
            del.setOnClickListener { adapter.removeItem(pos) }
            edit.setOnClickListener {
                with(Intent(cnt, EditLesson::class.java)) {
                    putExtra("les_id", les.id)
                    startActivity(cnt, this, null)
                }
            }
        }

        private fun dig2(n: Int): String {
            return "${n/10}${n%10}"
        }
    }
}