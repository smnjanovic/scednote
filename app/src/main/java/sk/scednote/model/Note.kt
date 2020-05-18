package sk.scednote.model

import android.os.Parcel
import android.os.Parcelable
import java.util.*

/**
 * Trieda obaluje data o ulohach k predmetom.
 *
 * @param id ID ulohy
 * @param sub Predmet
 * @param info obsah ulohy
 * @param deadline termín dokončenia úlohy
 */
data class Note (val id: Long, val sub: Subject, val info: String, var deadline: Calendar? = null) :
    Parcelable {
    companion object CREATOR : Parcelable.Creator<Note> {
        const val DEADLINE_TODAY: Long = 0
        const val DEADLINE_TOMORROW: Long = -1
        const val DEADLINE_THIS_WEEK: Long = -2
        const val DEADLINE_LATE: Long = -3
        const val DEADLINE_FOREVER: Long = -4
        const val NO_DATA: Long = -5

        override fun createFromParcel(parcel: Parcel) = Note(parcel)
        override fun newArray(size: Int): Array<Note?> = arrayOfNulls(size)
    }

    constructor(parcel: Parcel) : this(
        parcel.readLong(),
        parcel.readParcelable(Subject::class.java.classLoader)!!,
        parcel.readString()!!,
        parcel.readLong().let {millis ->
            if (millis == (0).toLong()) null
            else Calendar.getInstance().also {cal -> cal.timeInMillis = millis }
        }
    )

    /**
     * Prevedenie datumu na Textovy retazec
     */
    val ddlItem: String get() = deadline?.let {
        fun dig2(n: Int) = "${n/10}${n%10}"
        val year = dig2(deadline!!.get(Calendar.YEAR))
        val month = dig2(deadline!!.get(Calendar.MONTH) + 1)
        val day = dig2(deadline!!.get(Calendar.DAY_OF_MONTH))
        val hour = dig2(deadline!!.get(Calendar.HOUR_OF_DAY))
        val minute = dig2(deadline!!.get(Calendar.MINUTE))
        "$day.$month.$year $hour:$minute"
    } ?: ""

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeParcelable(sub, flags)
        parcel.writeString(info)
        parcel.writeLong(deadline?.timeInMillis ?: 0)
    }

    override fun describeContents() = 0
}