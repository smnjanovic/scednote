package sk.scednote.fragments

import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.text.format.DateFormat
import android.widget.TimePicker
import androidx.fragment.app.DialogFragment
import java.util.*

/**
 * Trieda zobrazuje dialog na nastavenie času. Pred zobrazením do nej možno vložiť predpripravený dátum na úpravu
 */
class TimeFragment: DialogFragment(), TimePickerDialog.OnTimeSetListener {
    companion object {
        const val CAL = "CAL"
    }
    private var timeEvent: (Calendar)->Unit = fun(_){}
    val data = Bundle()

    /**
     * Táto funkcia umožňuje nastaviť, čo sa má stať, keď užívateľ zmení čas
     *
     * @param fn
     */
    fun setOnChoice(fn: (Calendar)->Unit) {
        timeEvent = fn
    }

    /**
     * Vlozenie kalendarac
     *
     * @param calendar vloženie existujueho dátumu [Calendar] do pamäti na ďaľšie úpravy
     */
    fun putCalendar(calendar: Calendar) = data.putLong(CAL, calendar.timeInMillis)

    /**
     * Vytvorenie dialogu na zobrazenie
     *
     * @param saved Zachované dáta fragmentu, ktorý bol systémom zavretý
     * @return [Dialog] Vráti diaóg
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
     *
     * @param view box na nastavovanie času
     * @param hourOfDay nastavená hodina, nastavená minúta
     */
    override fun onTimeSet(view: TimePicker?, hourOfDay: Int, minute: Int) {
        //nastavenie hodnôt
        val cal = Calendar.getInstance().also {
            it.timeInMillis = data.getLong(CAL, System.currentTimeMillis())
        }
        cal.set(Calendar.HOUR_OF_DAY, hourOfDay)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        putCalendar(cal)
        timeEvent(cal)
    }

    /**
     * doteraz zapamatane data si fragment ulozi v pamati
     * @param outState dáta na odloženie pred zrušením fragmentu systémom
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putAll(data)
    }
}