package sk.scednote.model

import sk.scednote.R
import sk.scednote.ScedNoteApp

/**
 * Obsahuje informaciu o sposobe typu hodiny (prednaska, cvicenie), v buducom vyvoji mozu pribudnut prax, a ine
 */

enum class ScedSort {
    COURSE, PRESENTATION;
    companion object {
        /**
         * Ziskanie predmetu v danom poradi
         *
         * @param n poradie prvku ScedSort
         * @return Vrati formu vyucovania [ScedSort]
         */
        operator fun get(n: Int): ScedSort {
            return when (n) {
                0 -> COURSE
                1 -> PRESENTATION
                else -> throw(IndexOutOfBoundsException("Index $n of <<enum>> ScedSort is Out of bounds!"))
            }
        }

        /**
         * Indexovany zoznam textových reprezentácií Inštancií
         */
        val sorts: Array<String> get() = arrayOf(COURSE.sort, PRESENTATION.sort)
    }

    /**
     * Textová reprezentácia Instancie Objektu ScedSort
     */
    val sort: String get() = ScedNoteApp.res.getString( when (this) {
        PRESENTATION -> R.string.lessonP
        COURSE -> R.string.lessonC
    })

    /**
     * Poradie Instancie objektu ScedSort
     */
    val position: Int get() = values().indexOf(this)
}