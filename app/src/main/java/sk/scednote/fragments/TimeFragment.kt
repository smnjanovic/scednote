package sk.scednote.fragments

import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.text.format.DateFormat
import android.widget.TimePicker
import androidx.fragment.app.DialogFragment
import java.util.*

class TimeFragment: DialogFragment(), TimePickerDialog.OnTimeSetListener {
    private var timeEvent: (TimePicker?, Int, Int)->Unit = fun(_, _, _){}
    val data = Bundle()

    fun setOnChoice(fn: (TimePicker?, Int, Int)->Unit) {
        timeEvent = fn
    }

    fun setOnChoice(reducedFn: (Int, Int)->Unit) {
        setOnChoice(fun (_: TimePicker?, hour: Int, minute: Int) {
            reducedFn(hour, minute)
        })
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        savedInstanceState?.let { data.putAll(it) }
        return activity?.let {
            val c = Calendar.getInstance()
            val hour = c.get(Calendar.HOUR_OF_DAY)
            val minute = c.get(Calendar.MINUTE)
            TimePickerDialog(it, this, hour, minute, DateFormat.is24HourFormat(it))
        } ?: throw(NullPointerException("Date Fragment must contain FragmentActivity!"))
    }

    override fun onTimeSet(view: TimePicker?, hourOfDay: Int, minute: Int) {
        timeEvent(view, hourOfDay, minute)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putAll(data)
    }

}