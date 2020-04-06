package sk.scednote.model.data

import android.text.format.DateUtils

data class Note (val id: Long, val sub: Subject, val info: String, val date: DateUtils?)