package sk.scednote.model.data

import android.os.Parcel
import android.os.Parcelable

data class Subject(val id: Long?, val abb: String, val full: String) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readValue(Long::class.java.classLoader) as? Long,
        parcel.readString() ?: "",
        parcel.readString() ?: ""
    ) {
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if(other !is Subject) return false
        return abb == other.abb && full == other.full
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeValue(id)
        parcel.writeString(abb)
        parcel.writeString(full)
    }

    override fun describeContents(): Int {
        return 0
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