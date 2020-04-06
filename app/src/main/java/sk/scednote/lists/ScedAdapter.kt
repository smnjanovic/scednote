package sk.scednote.lists

import android.annotation.SuppressLint
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

class ScedAdapter(context: Context): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val cnt = context
    private val db = Database(cnt)
    private var items = ArrayList<Lesson>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ScedHolder (
            LayoutInflater.from(parent.context).inflate(R.layout.les_item, parent, false),
            cnt
        )
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun removeItem(les: Lesson): Boolean {
        val pos = items.indexOf(les)
        if (pos > -1) {
            db.removeLesson(les.id)
            items.removeAt(pos)
            notifyItemRemoved(pos)
            notifyItemRangeChanged(pos, items.size)
            return true
        }
        return false
    }

    fun closeDb() {
        db.close()
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ScedHolder) {
            holder.bind(items[position], this)
        }
    }

    fun loadData(day: Day?) {
        items = db.getScedule(day)
        this.notifyDataSetChanged()
    }

    class ScedHolder constructor(itemView: View, context: Context): RecyclerView.ViewHolder(itemView) {
        private val cnt = context
        private val card = itemView

        private val time = itemView.time
        private val sub = itemView.subject
        private val sort = itemView.sort
        private val edit = itemView.edit_les
        private val del = itemView.del_les

        @SuppressLint("SetTextI18n")
        fun bind(les: Lesson, adapter: ScedAdapter) {
            time.text = "${les.time.first}:00 - ${les.time.last}:00"
            sub.text = les.subject?.abb ?: ""
            sort.text = les.sort.getSort(cnt)
            //tagy s indexom 0 obsahuju id hodiny
            del.setOnClickListener {
                adapter.removeItem(les)
            }
            edit.setOnClickListener {
                val intent = Intent(cnt, EditLesson::class.java)
                intent.putExtra("les_id", les.id)
                startActivity(cnt, intent, null)
            }
        }
    }
}