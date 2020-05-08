package sk.scednote.fragments

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.DatePicker
import androidx.fragment.app.DialogFragment
import java.util.*

class DateFragment : DialogFragment(), DatePickerDialog.OnDateSetListener {
    companion object {
        const val CAL = "CAL"
    }
    private var dateEvent: (DatePicker?, Int, Int, Int)->Unit = fun(_, _, _, _){}
    //zachovatelne data posielane z aktivity
    val data: Bundle = Bundle()

    /**
     * Odlozenie nastavenia daneho datumu
     */
    fun putCalendar(cal: Calendar) = data.putLong(CAL, cal.timeInMillis)

    /**
     * Vytiahne ulozeny kalendar
     */
    fun getCalendar() = Calendar.getInstance().also { it.timeInMillis = data.getLong(TimeFragment.CAL, 0) }

    /**
     * Udalost nastavitelna zvonka
     */
    fun setOnChoice(fn: (DatePicker?, Int, Int, Int)->Unit) {
        dateEvent = fn
    }

    /**
     * Udalost nastavitelna zvonka
     */
    fun setOnChoice(reducedFn: (Int, Int, Int)->Unit) {
        setOnChoice(fun (_: DatePicker?, year: Int, month: Int, day: Int) {
            reducedFn(year, month, day)
        })
    }

    /**
     * Vytvorenie dialogoveho okna, aplikovanie menitelnej udalosti
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
     */
    override fun onDateSet(view: DatePicker?, year: Int, month: Int, day: Int) {
        val cal = Calendar.getInstance()
        cal.timeInMillis = data.getLong(CAL)
        cal.set(year, month, day)
        data.putLong(CAL, cal.timeInMillis)
        //ak bol cas nastaveny uz predtym, system ho nesmie zabudnut aj ked nastavujem iba datum
        dateEvent(view, year, month, day)
    }

    /**
     * po otoceni displeja dialog ostane viditelny
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putAll(data)
    }
}