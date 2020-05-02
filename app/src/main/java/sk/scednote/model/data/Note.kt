package sk.scednote.model.data

import android.os.Parcel
import android.os.Parcelable
import java.util.*

data class Note (val id: Long, val sub: Subject, val info: String, var deadline: Calendar? = null) :
    Parcelable {
    companion object CREATOR : Parcelable.Creator<Note> {
        private const val Y = "%Y"
        private const val m = "%m"
        private const val d = "%d"
        private const val H = "%H"
        private const val M = "%M"

        override fun createFromParcel(parcel: Parcel): Note {
            return Note(parcel)
        }

        override fun newArray(size: Int): Array<Note?> {
            return arrayOfNulls(size)
        }
    }
    constructor(p_id: Long, p_sub: Subject, p_info: String, c_year: Int, c_month: Int, c_dayOfMonth:Int, c_hour:Int, c_minute:Int) :
            this(p_id, p_sub, p_info, Calendar.getInstance().apply { set(c_year, c_month, c_dayOfMonth - 1, c_hour, c_minute) })

    constructor(parcel: Parcel) : this(
        parcel.readLong(),
        parcel.readParcelable(Subject::class.java.classLoader)!!,
        parcel.readString()!!,
        (fun (): Calendar?{
            return if (parcel.dataSize() == 3) null else {
                Calendar.getInstance().apply {
                    set(Calendar.YEAR, parcel.readInt())
                    set(Calendar.MONTH, parcel.readInt())
                    set(Calendar.DAY_OF_MONTH, parcel.readInt())
                    set(Calendar.HOUR, parcel.readInt())
                    set(Calendar.MINUTE, parcel.readInt())
                }
            }
        })()
    )

    private fun stringify(n: Int, leadingZero: Boolean): String {
        return if (leadingZero) "${n/10}${n%10}" else "$n"
    }

    private fun getDeadlineString(format: String, leadingZero: Boolean = true): String {
        if (deadline == null) return ""
        else {
            val year = stringify(deadline!!.get(Calendar.YEAR), leadingZero)
            val month = stringify(deadline!!.get(Calendar.MONTH) + 1, leadingZero)
            val day = stringify(deadline!!.get(Calendar.DAY_OF_MONTH), leadingZero)
            val hour = stringify(deadline!!.get(Calendar.HOUR_OF_DAY), leadingZero)
            val minute = stringify(deadline!!.get(Calendar.MINUTE), true)
            return format.replace(Y, year).replace(m, month).replace(d, day).replace(H, hour).replace(M, minute)
        }
    }

    val ddlSql: String get() = getDeadlineString("$Y-$m-$d $H:$M", true)
    val ddlItem: String get() = getDeadlineString("$d.$m.$Y $H:$M", false)

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeParcelable(sub, flags)
        parcel.writeString(info)
        if (deadline != null) {
            parcel.writeInt(deadline!!.get(Calendar.YEAR))
            parcel.writeInt(deadline!!.get(Calendar.MONTH))
            parcel.writeInt(deadline!!.get(Calendar.DAY_OF_MONTH))
            parcel.writeInt(deadline!!.get(Calendar.HOUR_OF_DAY))
            parcel.writeInt(deadline!!.get(Calendar.MINUTE))
        }
    }

    override fun describeContents(): Int {
        return 0
    }
}