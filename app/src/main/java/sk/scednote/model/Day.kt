package sk.scednote.model

import sk.scednote.R
import sk.scednote.ScedNoteApp

/**
 * Pracovne dni
 */

enum class Day {
    MON, TUE, WED, THU, FRI;
    companion object {
        private val days = arrayOf(
            MON,
            TUE,
            WED,
            THU,
            FRI
        )
        private fun exp(n: Int) = "Index $n of <<enum>> Day is Out of bounds!"

        /**
         * Ziska den v tyzdni
         * @param n [Int] poradie dna v tyzdni
         * @return [Day] den
         */
        operator fun get(n: Int): Day {
            try { return days[n] }
            catch (i: IndexOutOfBoundsException) { throw(java.lang.IndexOutOfBoundsException(
                exp(n)
            )) }
        }
        val titles: Array<String> get() = arrayOf(MON.title, TUE.title, WED.title, THU.title, FRI.title)
    }

    //vratenie pozicie predmetu
    val position: Int get() = values().indexOf(this)
    //vratenie nazvu predmetu
    val title: String get() = ScedNoteApp.res.getString( when (this) {
        MON -> R.string.Mon
        TUE -> R.string.Tue
        WED -> R.string.Wed
        THU -> R.string.Thu
        FRI -> R.string.Fri
    })
}