package sk.scednote.model

import android.os.Parcel
import android.os.Parcelable


/**
 * Uchovava data o urcitom predmete
 */

data class Subject(val id: Long, val abb: String, val full: String) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readLong(),
        parcel.readString() ?: "",
        parcel.readString() ?: ""
    )

    /**
     * porovnava, ci sa jedna o 2 rovnake predmety
     */
    override fun equals(other: Any?): Boolean {
        return other != null && other is Subject && abb == other.abb && full == other.full
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeValue(id)
        parcel.writeString(abb)
        parcel.writeString(full)
    }

    override fun describeContents() = 0

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + abb.hashCode()
        result = 31 * result + full.hashCode()
        return result
    }

    companion object CREATOR : Parcelable.Creator<Subject> {
        override fun createFromParcel(parcel: Parcel): Subject {
            return Subject(parcel)
        }

        override fun newArray(size: Int): Array<Subject?> {
            return arrayOfNulls(size)
        }
    }
}