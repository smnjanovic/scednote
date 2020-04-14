package sk.scednote.model;

import android.app.AlertDialog
import android.content.Context
import sk.scednote.model.data.BtnFn;

object Popups {
    fun alert(cont: Context, msg: String, pos:BtnFn, neg: BtnFn? = null, neu: BtnFn? = null) {
        AlertDialog.Builder(cont).apply {
            setCancelable(false)
            setMessage(msg)
            setPositiveButton(pos.str, pos.fn)
            if (neg != null) setNegativeButton(neg.str, neg.fn)
            if (neu != null) setNeutralButton(neu.str, neu.fn)
            create()
            show()
        }
    }
}
