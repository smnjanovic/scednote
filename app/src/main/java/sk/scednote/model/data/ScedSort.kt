package sk.scednote.model.data

import android.content.Context
import sk.scednote.R
import java.lang.IndexOutOfBoundsException

enum class ScedSort {
    FREE, COURSE, PRESENTATION;

    companion object {
        operator fun get(n: Int): ScedSort {
            if (n !in 0..2) throw(IndexOutOfBoundsException("Index $n of <<enum>> ScedSort is Out of bounds!"))
            return arrayOf(COURSE, PRESENTATION, FREE)[n]
        }

        fun getSorts(context: Context): Array<String> {
            return arrayOf(PRESENTATION.getSort(context),COURSE.getSort(context))
        }

        fun getSortByName(ctx: Context, str: String): ScedSort? {
            val sorts = ScedSort.values()
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
            ScedSort.PRESENTATION -> R.string.lessonP
            ScedSort.COURSE -> R.string.lessonC
            else -> R.string.lessonF
        })
    }

    val position: Int get() = ScedSort.values().indexOf(this)
}