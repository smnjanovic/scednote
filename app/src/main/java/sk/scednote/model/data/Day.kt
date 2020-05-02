package sk.scednote.model.data

import sk.scednote.R
import sk.scednote.ScedNoteApp

enum class Day {
    MON, TUE, WED, THU, FRI;
    companion object {
        private val days = arrayOf(MON, TUE, WED, THU, FRI)
        private fun exp(n: Int) = "Index $n of <<enum>> Day is Out of bounds!"
        operator fun get(n: Int): Day {
            try { return days[n] }
            catch (i: IndexOutOfBoundsException) { throw(java.lang.IndexOutOfBoundsException(exp(n))) }
        }
        val titles: Array<String> get() = arrayOf(MON.title, TUE.title, WED.title, THU.title, FRI.title)
    }
    val position: Int get() = values().indexOf(this)
    val title: String get() = ScedNoteApp.res.getString( when (this) {
        MON -> R.string.Mon
        TUE -> R.string.Tue
        WED -> R.string.Wed
        THU -> R.string.Thu
        FRI -> R.string.Fri
    })
}