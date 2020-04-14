package sk.scednote.model.data

import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import kotlin.properties.Delegates

data class Lesson (
    val id: Long,
    val day: Day,
    val time: IntRange,
    val sort: ScedSort,
    val subject: Subject,
    val room: String
) : Parcelable {
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
}