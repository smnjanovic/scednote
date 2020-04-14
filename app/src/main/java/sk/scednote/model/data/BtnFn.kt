package sk.scednote.model.data

/**
 * Dáta pre dialógové okno Alert
 * 1. parameter -> text tlacidla
 * 2. parameter -> funkcia, ktoru vykona
 */

import android.content.DialogInterface

data class BtnFn (val str: String, val fn: (DialogInterface, Int) -> Unit = fun(_:DialogInterface, _:Int) {}) {
    constructor(label: String, func: ()->Unit): this(label, fun(_: DialogInterface, _: Int) { func() })
}