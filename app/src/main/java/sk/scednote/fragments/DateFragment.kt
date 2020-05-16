package sk.scednote.fragments

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.DatePicker
import androidx.fragment.app.DialogFragment
import java.util.*

/**
 * Trieda zobrazuje dialog na nastavenie dátumu. Pred zobrazením do nej možno vložiť predpripravený dátum na úpravu
 */
class DateFragment : DialogFragment(), DatePickerDialog.OnDateSetListener {
    companion object {
        const val CAL = "CAL"
    }
    private var dateEvent: (Calendar)->Unit = fun(_){}
    //zachovatelne data posielane z aktivity
    val data: Bundle = Bundle()

    /**
     * Odlozenie nastavenia daneho datumu
     * @param cal dátum na uloženie
     */
    fun putCalendar(cal: Calendar) = data.putLong(CAL, cal.timeInMillis)

    /**
     * Udalost nastavitelna zvonka
     * @param fn Metóda, ktorá sa má vykonať po tom ako bol dátum nastavený
     */
    fun setOnChoice(fn: (Calendar)->Unit) {
        dateEvent = fn
    }

    /**
     * Vytvorenie dialogoveho okna, aplikovanie menitelnej udalosti
     * @param saved dáta, ktoré sa zachovali pred ukončením fragmentu systémom
     * @return [Dialog] Vráti dialóg na zobrazenie
     */
    override fun onCreateDialog(saved: Bundle?): Dialog {
        saved?.let { data.putAll(saved) }
        return activity?.let {activity ->
            val c = Calendar.getInstance()
            saved?.getLong(CAL)?.let { c.timeInMillis = it }
            DatePickerDialog(activity, this, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH))
        } ?: throw(NullPointerException("Activity must have been destroyed!"))
    }

    /**
     * pokus o nastavenie datumu
     * @param view Dialóg na výber dátumu
     * @param year Nastavenie roku
     * @param month Nastavenie mesiaca
     * @param day Nastavenie casu
     */
    override fun onDateSet(view: DatePicker?, year: Int, month: Int, day: Int) {
        val cal = Calendar.getInstance()
        cal.timeInMillis = data.getLong(TimeFragment.CAL, System.currentTimeMillis())
        cal.set(year, month, day)
        putCalendar(cal)
        dateEvent(cal)
    }

    /**
     * po otoceni displeja dialog ostane viditelny
     * @param outState balik zalohovanych dat
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putAll(data)
    }
}