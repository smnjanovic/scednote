package sk.scednote.fragments

import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.text.format.DateFormat
import android.widget.TimePicker
import androidx.fragment.app.DialogFragment
import java.util.*

/**
 * Trieda zobrazuje dialog, mozno pomocou nej z vonka nastavit, co sa ma vykonat, po stlaceni daneho
 * tlacidla a obsah aky bude mat
 */
class TimeFragment: DialogFragment(), TimePickerDialog.OnTimeSetListener {
    companion object {
        const val CAL = "CAL"
    }
    private var timeEvent: (TimePicker?, Int, Int)->Unit = fun(_, _, _){}
    val data = Bundle()

    /**
     * Udalost upravitelna externe
     */
    fun setOnChoice(fn: (TimePicker?, Int, Int)->Unit) {
        timeEvent = fn
    }

    /**
     * Vlozenie kalendarac
     */
    fun putCalendar(calendar: Calendar) = data.putLong(CAL, calendar.timeInMillis)

    /**
     * Vlozenie kalendarac
     */
    fun getCalendar() = Calendar.getInstance().also { it.timeInMillis = data.getLong(CAL, 0) }

    /**
     * Udalost upravitelna externe
     */
    fun setOnChoice(reducedFn: (Int, Int)->Unit) {
        setOnChoice(fun (_: TimePicker?, hour: Int, minute: Int) {
            reducedFn(hour, minute)
        })
    }

    /**
     * Vytvorenie dialogu na zobrazenie
     */
    override fun onCreateDialog(saved: Bundle?): Dialog {
        saved?.let { data.putAll(it) }
        return activity?.let {activity ->
            val c = Calendar.getInstance()
            saved?.getLong(CAL)?.let { c.timeInMillis = it }
            val hour = c.get(Calendar.HOUR_OF_DAY)
            val minute = c.get(Calendar.MINUTE)
            TimePickerDialog(activity, this, hour, minute, DateFormat.is24HourFormat(activity))
        } ?: throw(NullPointerException("Date Fragment must contain FragmentActivity!"))
    }

    /**
     * Nastavenie casu (vratane datumu, pre pripad ze nastavujem oboje)
     */
    override fun onTimeSet(view: TimePicker?, hourOfDay: Int, minute: Int) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, hourOfDay)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        data.getLong(CAL, cal.timeInMillis)
        timeEvent(view, hourOfDay, minute)
    }

    /**
     * doteraz zapamatane data si fragment ulozi v pamati
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putAll(data)
    }

}