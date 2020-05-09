package sk.scednote.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import sk.scednote.R

/**
 * Dialogove okno, ktore vyskoci pred vaznym rozhodnutim uzivatela, napr. odstranenie niecoho, co vedie
 * k odstraneniu cohosi dalsieho, alebo ak dojde k comukolvek nenavratnemu
 * https://www.youtube.com/watch?v=r_87U6oHLFc
 */

class Confirm: DialogFragment() {
    companion object {
        private const val MSG = "MSG"
        private const val OK = "OK"
        private const val KO = "KO"

        /**
         * Vytvori sa nova instancia dialogu s obsahom textu podla parametrov
         * @param msg správa pre uživateľa
         * @param confirm nápis na tlačidle pre potvrdenie
         * @param cancel nápis na tlačidle pre zrušenie
         * @return Confirm Nová inštancia s nastaveným obsahom uvedeným v parametroch
         */
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

    /**
     * Priprava dialogu
     * @param saved uchované dáta s predošlého zavretia
     * @return [Dialog] Dialóg na zobrazenie
     */
    override fun onCreateDialog(saved: Bundle?): Dialog {
        return activity?.let {
            confirm = arguments?.getString(OK) ?: saved?.getString(OK) ?: it.resources.getString(R.string.continue_)
            cancel = arguments?.getString(KO) ?: saved?.getString(KO) ?: it.resources.getString(R.string.cancel)
            msg = arguments?.getString(MSG) ?: saved?.getString(MSG) ?: "$confirm?"

             AlertDialog.Builder(it).apply {
                setMessage(msg)
                setPositiveButton(confirm) { d, w -> onConfirm(d, w) }
                setNegativeButton(cancel) { d, w -> onCancel(d, w) }
             }.create()
        } ?: throw( NullPointerException("Activity must exist!"))
    }

    /**
     * ulozenie dat k dialogu
     * @param outState priestor na uloženie zálohy dát
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.apply {
            putString(MSG, msg)
            putString(OK, confirm)
            putString(KO, cancel)
        }
    }

    /**
     * udalost, ktora sa vykona ak uzivatel potvrdi, ze chce pokracovat
     * @param fn Funkcia, čo sa má stať, keď uživateľ klikne potvrdiť
     */
    fun setOnConfirm(fn: (DialogInterface, Int) -> Unit) {
        onConfirm = fn
    }

    /**
     * udalost, ktora sa vykona ak uzivatel zrusi, to co robil
     * @param fn Funkcia, čo sa má stať, keď užívateľ klikne zrušiť
     */
    fun setOnCancel(fn: (DialogInterface, Int) -> Unit) {
        onCancel = fn
    }
}
