package sk.scednote.model.data

import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import kotlin.properties.Delegates

data class Lesson (val id: Long, val day: Day, val time: IntRange, val sort: ScedSort, val subject: Subject, val room: String) : Parcelable {
    constructor(id: Long, day_: Int, start: Int, end: Int, sort_: Int, sub_id:Long, sub_abb: String, sub_full: String, room: String)
            : this(id, Day[day_], start..end, ScedSort[sort_], Subject(sub_id, sub_abb, sub_full), room)

    constructor(parcel: Parcel) : this(
        parcel.readLong(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readLong(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: ""
    )


    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeInt(day.position)
        parcel.writeInt(time.first)
        parcel.writeInt(time.last)
        parcel.writeInt(sort.position)
        parcel.writeLong(subject.id ?: -1)
        parcel.writeString(subject.abb)
        parcel.writeString(subject.full)
        parcel.writeString(room)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Lesson> {
        val OPENING_HOURS = 7..21

        override fun createFromParcel(parcel: Parcel): Lesson {
            return Lesson(parcel)
        }

        override fun newArray(size: Int): Array<Lesson?> {
            return arrayOfNulls(size)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if(other !is Lesson) return false
        return day == other.day && sort == other.sort && room == other.room && subject == other.subject
    }

    /**
     * zistenie ci je medzi predmetmi nejaky casovy odstup
     */
    fun breaksBetween(les: Lesson): Boolean {
        return this.day == les.day && (this.time.first - 1 == les.time.last || this.time.last + 1 == les.time.first)
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + day.hashCode()
        result = 31 * result + time.hashCode()
        result = 31 * result + sort.hashCode()
        result = 31 * result + subject.hashCode()
        result = 31 * result + room.hashCode()
        return result
    }
}