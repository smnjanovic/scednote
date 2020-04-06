package sk.scednote.model.data

import android.content.DialogInterface

data class BtnFn (val str: String, private val function: () -> Unit = fun(){}) {
    val fn = fun(dialog: DialogInterface, id: Int) { function() }
}