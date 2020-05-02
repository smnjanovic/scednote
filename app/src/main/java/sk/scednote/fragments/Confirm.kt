package sk.scednote.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import sk.scednote.R

/**
 * https://www.youtube.com/watch?v=r_87U6oHLFc
 */

class Confirm: DialogFragment() {
    companion object {
        private const val MSG = "MSG"
        private const val OK = "OK"
        private const val KO = "KO"
        fun newInstance(msg: String, confirm: String? = null, cancel: String? = null): Confirm {
            return Confirm().also {
                it.arguments = Bundle().apply {
                    putString(MSG, msg)
                    putString(OK, confirm)
                    putString(KO, cancel)
                }
            }
        }
    }

    private var onConfirm:(DialogInterface, Int)->Unit = fun(_,_) {}
    private var onCancel:(DialogInterface, Int)->Unit = fun(_,_) {}
    private lateinit var msg: String
    private lateinit var confirm: String
    private lateinit var cancel: String

    override fun onCreateDialog(saved: Bundle?): Dialog {
        return activity?.let {
            confirm = arguments?.getString(OK) ?: saved?.getString(
                OK
            ) ?: it.resources.getString(R.string.continue_)
            cancel = arguments?.getString(KO) ?: saved?.getString(
                KO
            ) ?: it.resources.getString(R.string.cancel)
            msg = arguments?.getString(MSG) ?: saved?.getString(
                MSG
            ) ?: "$confirm?"

             AlertDialog.Builder(it).apply {
                setMessage(msg)
                setPositiveButton(confirm) { d, w -> onConfirm(d, w) }
                setNegativeButton(cancel) { d, w -> onCancel(d, w) }
             }.create()
        } ?: throw( NullPointerException("Activity must exist!"))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.apply {
            putString(MSG, msg)
            putString(OK, confirm)
            putString(KO, cancel)
        }
    }

    fun setOnConfirm(fn: (DialogInterface, Int) -> Unit) {
        onConfirm = fn
    }

    fun setOnCancel(fn: (DialogInterface, Int) -> Unit) {
        onCancel = fn
    }
}
