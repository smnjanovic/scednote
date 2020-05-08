package sk.scednote.model

import sk.scednote.R
import sk.scednote.ScedNoteApp

/**
 * Obsahuje informaciu o sposobe typu hodiny (prednaska, cvicenie), v buducom vyvoji mozu pribudnut prax, a ine
 */

enum class ScedSort {
    COURSE, PRESENTATION;
    companion object {
        operator fun get(n: Int): ScedSort {
            return when (n) {
                0 -> COURSE
                1 -> PRESENTATION
                else -> throw(IndexOutOfBoundsException("Index $n of <<enum>> ScedSort is Out of bounds!"))
            }
        }
        val sorts: Array<String> get() = arrayOf(COURSE.sort, PRESENTATION.sort)
    }

    //vrati pouzitelny text
    val sort: String get() = ScedNoteApp.res.getString( when (this) {
        PRESENTATION -> R.string.lessonP
        COURSE -> R.string.lessonC
    })

    val position: Int get() = values().indexOf(this)
}