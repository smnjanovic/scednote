package sk.scednote.adapters

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.sub_item.view.*
import kotlinx.android.synthetic.main.tab_button.view.*
import sk.scednote.R
import sk.scednote.model.Database
import sk.scednote.model.Subject

/**
 * Adapter na zobrazenie suborov
 * Polozky mozu byt zobrazovane ako polozky menu alebo ako zoznam upravitelnych predmetov
 * @property adapterType Typ adaptera (na výber: menu položky na čítanie, zoznam s povolením úprav)
 * @param bundle zachované dáta pri obnove aktivity, ktorá adapter používa
 */
open class SubjectAdapter(private val adapterType: Int, bundle: Bundle?): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        const val ADAPTER_TYPE_TAB = 1000  // tlacidlo v navigacii
        const val ADAPTER_TYPE_ITEM = 1001 // polozka v zozname
        private const val HIGHLIGHTED_SUBJECT_POSITION = "HIGHLIGHTED SUBJECT"
        private const val SUBJECT_LIST = "SUBJECT LIST"
        private val HIGHLIGHT = Color.parseColor("#aaffffff")
    }

    private val data = Database()
    private var items: ArrayList<Subject> = bundle?.getParcelableArrayList(SUBJECT_LIST) ?: data.loadSubjects()

    var markedHolder: TabHolder? = null
    var marked: Int = bundle?.getInt(HIGHLIGHTED_SUBJECT_POSITION) ?: -1
        set(value) {
            markedHolder?.itemView?.tab?.background?.setTintList(null)
            field = value
            if (field in items.indices) notifyItemChanged(field)
        }

    private var choiceEvent: View.OnClickListener? = null
    private var editEvent: View.OnClickListener? = null
    private var deleteEvent: View.OnClickListener? = null

    /**
     * Nastavenie funkcie, co sa ma stat, ak sa v menu prekliknem na dalsi predmet
     * @param switch Co sa ma stat, s kliknutým objektom
     */
    fun onSwitch(switch: (View) -> Unit) {
        choiceEvent = View.OnClickListener(switch)
    }

    /**
     * Co sa stane ak sa pokusim upravit dany predmet
     *
     * @param edit funkcia, co sa ma stat, s pohladom vo formalnom parametri
     */
    fun onEdit(edit: (View) -> Unit) {
        editEvent = View.OnClickListener(edit)
    }

    /**
     * Co sa stane ak sa pokusim vymazat dany predmet
     *
     * @param delete funkcia, co sa ma stat, s pohladom vo formalnom parametri
     */
    fun onDelete(delete: (View) -> Unit) {
        deleteEvent = View.OnClickListener(delete)
    }

    /**
     * Zalohovanie dat adaptera
     * @param bundle funkcia, balik kam sa ulozia zalohovane subory
     */
    fun backupData(bundle: Bundle) {
        bundle.putParcelableArrayList(SUBJECT_LIST, items)
        bundle.putInt(HIGHLIGHTED_SUBJECT_POSITION, marked)
    }

    /**
     * Aktualizovany predmet sa zmeni vo viditelnom zozname
     *
     * @param id id aktualizovaneho predmetu
     */
    fun updatedRecord(id: Long) {
        val position = getPositionById(id)
        if (position in 0 until itemCount) {
            data.getSubject(id)?.let { items[position] = it }
            notifyItemChanged(position)
        }
    }
    /**
     * Pridany predmet sa prida do viditelneho zoznamu
     * @param id id pridaneho predmetu
     */
    fun insertedRecord(id: Long) {
        data.getSubject(id)?.let {
            items.add(it)
            notifyItemInserted(items.size)
        }
    }

    /**
     * Predmet zmizne z viditelneho zoznamu
     * @param pos pozicia v zozname
     */
    fun deleteRecord(pos:Int) {
        if (pos in items.indices) {
            data.removeSubject(items[pos].id)
            items.removeAt(pos)
            notifyItemRemoved(pos)
        }
    }

    /**
     * Nacitanie cerstvych dat
     */
    fun reload() {
        items = data.loadSubjects().also { notifyDataSetChanged() }
    }

    /**
     * Ziskanie pozicie predmetu s danym id
     * @param id ID položky ktorej pozíiu hľadám
     * @return [Int] index prvku v zozname
     */
    fun getPositionById(id: Long): Int {
        for (i in items.indices) {
            if (items[i].id == id) {
                return i
            }
        }
        return -1
    }

    /**
     * Ziska nazov predmetu na danej pozicii
     * @param position Získanie názvu predmetu na danej pozícii
     */
    fun getSubjectNameAt(position: Int) = items[position].full

    fun close() {
        data.close()
    }

    /**
     * Tvorba ViewHoldera
     * @param parent v com sa viewHolder nachadza
     * @param viewType o aky typ ViewHoldera sa jedná
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when(adapterType) {
            ADAPTER_TYPE_ITEM -> ItemHolder(inflater.inflate(R.layout.sub_item, parent, false))
            ADAPTER_TYPE_TAB -> TabHolder(inflater.inflate(R.layout.tab_button, parent, false))
            else -> throw Exception("NO SUCH TYPE ON SUBJECT ADAPTER!")
        }
    }

    /**
     * @return počet záznamov
     */
    override fun getItemCount() = items.size

    /**
     * po pridaní viewHoldera
     * @param holder Typ ViewHolder alebo jeho potomok
     * @param position pozícia, v zozname
     */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is SubjectHolder) holder.bind()
    }

    /**
     * ziskanie ID záznamu v zozname
     * @param position index
     */
    override fun getItemId(position: Int) = if (position in items.indices) items[position].id else -1

    abstract inner class SubjectHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        abstract fun bind()
    }

    /**
     * Pohlad pre polozku umoznujucu upravu a odstranenie predmetu
     * @param itemView rodičovský 2D objekt s určitým obsahom
     */
    inner class ItemHolder(itemView: View): SubjectHolder(itemView) {
        private val abbreviation = itemView.abb
        private val fullSubName = itemView.full
        private val pencil = itemView.edit
        private val trash = itemView.delete

        /**
         * Nastavenie obsahu vytvárenemu Template-u
         */
        override fun bind() {
            val sub = items[adapterPosition]
            abbreviation.text = sub.abb
            fullSubName.text = sub.full
            pencil.setOnClickListener(editEvent)
            trash.setOnClickListener(deleteEvent)
            pencil.tag = this
            trash.tag = this
        }

        /**
         * Zisti ci je dany predmet nadbytocnym (nie je v rozvrhu, nie su k nemu ziadne ulohy)
         */
        fun isObsolete() = data.isSubjectObsolete(items[adapterPosition].id)
    }

    /**
     * pohlad pre neupravitelnu polozku v menu
     * @param itemView Template, ktorý ViewHolder nadobudne
     */
    inner class TabHolder(itemView: View): SubjectHolder(itemView) {
        private val button = itemView.tab

        /**
         * vyplnit tlacidlo skratkou, prip. ho zvyraznit ak bolo posledne stlacene
         */
        override fun bind() {
            with (button) {
                text = items[adapterPosition].abb
                setOnClickListener(choiceEvent)
                tag = this@TabHolder
                if (adapterPosition == marked) {
                    this.background.setTint(HIGHLIGHT)
                    markedHolder = this@TabHolder
                }
            }
        }
    }
}