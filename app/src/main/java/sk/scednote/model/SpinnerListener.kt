package sk.scednote.model

import android.view.View
import android.widget.AdapterView

class SpinnerListener : AdapterView.OnItemSelectedListener {
    private val sel_fn: (AdapterView<*>?, View?, Int, Long)->Unit
    private val nosel_fn: (AdapterView<*>?)->Unit

    /**
     * Vkladam funkcie ktore sa maju vykonat pri volbe niektorej alebo ziadnej moznosti v selektovacom zozname
     * funkcie ktore funguju ako vstup mozu a nemusia pouzivat parametre
     */
    constructor(onSelectItem: (AdapterView<*>?, View?, Int, Long)->Unit, onSelectNothing: ((AdapterView<*>?)->Unit)? = null) {
        sel_fn = onSelectItem
        nosel_fn = onSelectNothing ?: fun(_: AdapterView<*>?){}
    }
    constructor(onSelectItem: (AdapterView<*>?, View?, Int, Long)->Unit, onSelectNothing: ()->Unit)
            : this (onSelectItem, fun(_:AdapterView<*>?) { onSelectNothing() })
    constructor(onSelectItem: ()->Unit, onSelectNothing: ((AdapterView<*>?)->Unit)? = null)
            : this (fun(_:AdapterView<*>?, _:View?, _:Int, _:Long){onSelectItem()}, onSelectNothing)
    constructor(onSelectItem: ()->Unit, onSelectNothing: ()->Unit)
            : this (fun(_:AdapterView<*>?, _:View?, _:Int, _:Long){onSelectItem()}, onSelectNothing)

    override fun onNothingSelected(parent: AdapterView<*>?) {
        nosel_fn(parent)
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        sel_fn(parent, view, position, id)
    }
}