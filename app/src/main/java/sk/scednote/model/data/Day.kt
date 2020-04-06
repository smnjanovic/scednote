package sk.scednote.model.data

import android.content.Context
import sk.scednote.R.string.*
import java.lang.IndexOutOfBoundsException

enum class Day {
    MON, TUE, WED, THU, FRI;
    companion object {
        operator fun get(n: Int): Day {
            if (n !in 0..4) throw(IndexOutOfBoundsException("Index $n of <<enum>> Day is Out of bounds!"))
            return arrayOf(MON, TUE, WED, THU, FRI)[n]
        }
        fun getTitles(context: Context): Array<String> {
            return arrayOf(
                MON.getTitle(context),
                TUE.getTitle(context),
                WED.getTitle(context),
                THU.getTitle(context),
                FRI.getTitle(context)
            )
        }

        fun getDayByName(ctx: Context, str: String): Day? {
            val days = Day.values()
            for (d in days)
                if (d.getTitle(ctx) == str)
                    return d
            return null
        }
    }
    val position: Int get() = Day.values().indexOf(this)

    fun getTitle(context: Context): String {
        return context.resources.getString( when (this) {
            MON -> Mon; TUE -> Tue; WED -> Wed; THU -> Thu; FRI -> Fri
        })
    }
}