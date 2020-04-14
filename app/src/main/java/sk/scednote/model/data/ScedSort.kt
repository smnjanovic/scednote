package sk.scednote.model.data

import android.content.Context
import sk.scednote.R
import java.lang.IndexOutOfBoundsException

enum class ScedSort {
    COURSE, PRESENTATION;

    companion object {
        operator fun get(n: Int): ScedSort {
            if (n !in 0..2) throw(IndexOutOfBoundsException("Index $n of <<enum>> ScedSort is Out of bounds!"))
            return arrayOf(COURSE, PRESENTATION)[n]
        }

        fun getSorts(context: Context): Array<String> {
            return arrayOf(COURSE.getSort(context), PRESENTATION.getSort(context))
        }

        fun getSortByName(ctx: Context, str: String): ScedSort? {
            val sorts = values()
            for (s in sorts)
                if (s.getSort(ctx) == str)
                    return s
            return null
        }
    }

    /**
     * prednáška, cvičenie, či voľno? Vyberie text zo suboru xml
     */
    fun getSort(context: Context): String {
        return context.resources.getString( when (this) {
            PRESENTATION -> R.string.lessonP
            COURSE -> R.string.lessonC
        })
    }

    val position: Int get() = values().indexOf(this)
}