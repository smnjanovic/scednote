package sk.scednote.model

import android.os.Parcel
import android.os.Parcelable


/**
 * Data o vyucovacej hodine
 *
 * @param id ID hodiny [Long]
 * @param day Ďeň vyučovania [Day]
 * @param time Časový rozsah vyučovania [IntRange]
 * @param sort Forma vyučovania [ScedSort]
 * @param subject Predmet [Subject]
 * @param room miestnosť [String]
 */
data class Lesson (val id: Long, val day: Day, val time: IntRange, val sort: ScedSort, val subject: Subject, val room: String) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readLong(),
        Day[parcel.readInt()],
        parcel.readInt() .. parcel.readInt(),
        ScedSort[parcel.readInt()],
        Subject(
            parcel.readLong(),
            parcel.readString() ?: "",
            parcel.readString() ?: ""
        ),
        parcel.readString() ?: ""
    )


    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeInt(day.position)
        parcel.writeInt(time.first)
        parcel.writeInt(time.last)
        parcel.writeInt(sort.position)
        parcel.writeLong(subject.id)
        parcel.writeString(subject.abb)
        parcel.writeString(subject.full)
        parcel.writeString(room)
    }

    override fun describeContents() = 0

    companion object CREATOR : Parcelable.Creator<Lesson> {
        val OPENING_HOURS = 7..21

        override fun createFromParcel(parcel: Parcel): Lesson {
            return Lesson(parcel)
        }

        override fun newArray(size: Int): Array<Lesson?> {
            return arrayOfNulls(size)
        }
    }

    /**
     * Porovnanie hodin, ci su rovnake (nie ci su v rovnakom case)
     *
     * @param other dalsi objekt malo by sa jednat o vyucovaciu hodinu
     * @throws IllegalArgumentException porovnavat smiem iba Hodinu s hodinou, prip. null
     */
    override fun equals(other: Any?): Boolean {
        if(other is Lesson?) return day == other?.day && sort == other?.sort && room == other?.room && subject == other?.subject
        throw IllegalArgumentException("The type of an argument must be Lesson!")
    }

    /**
     * zistenie ci je medzi 2 predmetmi nejaky casovy odstup
     *
     * @param les Kontrola ci tato hodina a hodina v parametri nasleduju tesne za sebou (bez prestavky medzi nimi)
     * @return [Boolean] je medzi nimi odstup aspon o 1 hodinu false inak true
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