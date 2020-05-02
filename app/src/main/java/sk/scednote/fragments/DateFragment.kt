package sk.scednote.fragments

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.DatePicker
import androidx.fragment.app.DialogFragment
import java.lang.NullPointerException
import java.util.*

class DateFragment : DialogFragment(), DatePickerDialog.OnDateSetListener {
    private var dateEvent: (DatePicker?, Int, Int, Int)->Unit = fun(_, _, _, _){}
    //zachovatelne data posielane z aktivity
    val data: Bundle = Bundle()

    fun setOnChoice(fn: (DatePicker?, Int, Int, Int)->Unit) {
        dateEvent = fn
    }

    fun setOnChoice(reducedFn: (Int, Int, Int)->Unit) {
        setOnChoice(fun (_: DatePicker?, year: Int, month: Int, day: Int) {
            reducedFn(year, month, day)
        })
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        savedInstanceState?.let { data.putAll(savedInstanceState) }
        return activity?.let {
            val c = Calendar.getInstance()
            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH)
            val day = c.get(Calendar.DAY_OF_MONTH)
            DatePickerDialog(it, this, year, month, day)
        } ?: throw(NullPointerException("Date Fragment is missing must contain activity!"))
    }

    override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
        dateEvent(view, year, month, dayOfMonth)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putAll(data)
    }
}