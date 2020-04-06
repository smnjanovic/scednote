package sk.scednote.lists

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import sk.scednote.R

class TxtValid(context: Context, regex: String, range: IntRange?, text: EditText, button: Button?):
    TextWatcher {
    private val rgx = regex.toRegex()
    private val rng = range

    private val ctx = context
    private val btn = button
    private val txt = text

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    override fun afterTextChanged(s: Editable?) {}
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        if (s == null) {
            if (rng!!.first > 0)
                txt.error = ctx.resources.getString(R.string.cannot_be_empty)
        }
        else {
            val illegal = s.replace(rgx, "").isNotEmpty()
            val outRng =  rng != null && !rng.contains(s.length)
            btn?.isEnabled = !(illegal || outRng)

            txt.error = when(true) {
                outRng -> ctx.resources.getString(
                    if(rng!!.first == 1 && s.isEmpty()) R.string.cannot_be_empty
                    else if (s. length < rng.first) R.string.text_too_short
                    else R.string.text_too_long
                )
                illegal -> ctx.resources.getString(R.string.invalid_chars)
                else -> return
            }
        }
    }
}
