package sk.scednote.events

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import sk.scednote.R
import sk.scednote.ScedNoteApp

/**
 *  Posluchac textoveho vstupu, ktory ma pristup k tlacidlu, ktore odosiela formular moze kontrolovat
 *  stav viacerych textovych vstupov sucasne. Ak ktorykolvek z textovych poli nesplna svoje stanovene
 *  podmienky, tlacidlo sa zablokuje a formulár nie je možné odoslať. Ak tlacidlo nie je definovane,
 *  vykona sa kontrola len obycajna kontrola textu
 */

open class TxtValid(
    private val txt: EditText? = null, // textove pole ktoreho vstup sa kontroluje
    private val regex: String? = null, // povolene znaky alebo kombinacie znakom stylom regular expression
    private val rng: IntRange? = null, // povolena dlzka textu
    private val btn: View? = null, // tlacidlo, ktore je aktivne ak je format textu spravny a dlzka primerana
    /**
     * funkcia, ktora sa vykona navyse, pokial kontrola prebehla uspesne (trebars je viac textovych
     * poli, ktore vo funkcii skontrolujem, ci su bez erroru ak ano tlacdidlo je volne na stlacenie)
     */
    private val fn: ()->Boolean = defaultFn
): TextWatcher {
    //tlacidlo odoslat
    companion object {
        //zdroje textu bez kontextu
        private val defaultFn = fun(): Boolean { return true }
        val CANNOT_BE_EMPTY = ScedNoteApp.res.getString(R.string.cannot_be_empty)
        val TOO_SHORT = ScedNoteApp.res.getString(R.string.text_too_short)
        val TOO_LONG = ScedNoteApp.res.getString(R.string.text_too_long)
        val INVALID_INPUT = ScedNoteApp.res.getString(R.string.invalid_chars)
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

    /**
     * neprimerana dlzka, nepovolene znaky alebo neplatna kombinacia znakov sposobia, ze textove
     * pole bude mat nastavene error hlasenie pre uzivatela a tlacidlo na stlacenie sa deaktivuje
     */
    override fun afterTextChanged(s: Editable?) {
        //neplatne znaky
        val illegal = regex?.let { s?.toString()?.replace(it.toRegex(), "")?.isNotEmpty() ?: false } ?: false
        //nedodrzana dlzka znakov
        val outRng =  rng?.let { (s?.length?:0) !in it } ?: false

        val len = s?.trim()?.length ?: 0
        when {
            illegal -> txt?.error =
                INVALID_INPUT
            outRng -> {
                txt?.error = when {
                    len == 0 && rng!!.first == 1 -> if (btn != null) null else CANNOT_BE_EMPTY
                    len < rng!!.first -> TOO_SHORT
                    else -> TOO_LONG
                }
            }
            else -> txt?.error = null
        }
        btn?.isEnabled = !illegal && !outRng && fn()
    }
}
