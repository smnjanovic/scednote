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
 *
 *  @param txt textove pole ktoreho vstup sa kontroluje. Moze byt null
 *  @param regex povolene znaky alebo kombinacie znakom stylom regular expression. Moze byt null
 *  @param rng povolena dlzka textu. Moze byt null
 *  @param btn tlacidlo, ktore je aktivne ak je format textu spravny a dlzka primerana. Moze byt null
 *  @param fn funkcia, ktora sa vykona navyse, pokial kontrola prebehla uspesne (trebars je k dispozicii
 *  viac textovych poli, ktore vo funkcii treba kontrolovať, ci su bez chýb. Až potom sa tlačidlo uvolní)
 */

open class TxtValid(
    private val txt: EditText? = null,
    private val regex: String? = null,
    private val rng: IntRange? = null,
    private val btn: View? = null,
    private val fn: ()->Boolean = defaultFn
): TextWatcher {
    //tlacidlo odoslat
    companion object {
        //zdroje textu bez kontextu
        private val defaultFn = fun(): Boolean { return true }
        private val CANNOT_BE_EMPTY = ScedNoteApp.res.getString(R.string.cannot_be_empty)
        private val TOO_SHORT = ScedNoteApp.res.getString(R.string.text_too_short)
        private val TOO_LONG = ScedNoteApp.res.getString(R.string.text_too_long)
        private val INVALID_INPUT = ScedNoteApp.res.getString(R.string.invalid_chars)
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

    /**
     * neprimerana dlzka, nepovolene znaky alebo neplatna kombinacia znakov sposobia, ze textove
     * pole bude mat nastavene error hlasenie pre uzivatela a tlacidlo na stlacenie sa deaktivuje
     * @param s Upraveny text
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
