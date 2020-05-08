package sk.scednote.scedule

import android.graphics.Color
import android.view.View
import kotlinx.android.synthetic.main.tbl_cell.view.*
import sk.scednote.model.Ahsl
import sk.scednote.model.Design
import java.util.*

/**
 * Trieda ziskava, uchovava, aktualizuje farby z databazy SQL.
 * Viditelne menia farby pozadia danej bunke
 */

class CellGroup(private val target: String? = null, color: Ahsl) {
    private val cells = ArrayList<View>()
    private var bgHsl = color
    private var fgHsl = Design.customizedForeground(color)
    private var bgHex = Design.hsl2hex(bgHsl)
    private var fgHex = Design.hsl2hex(fgHsl)

    /**
     * Pridanie malovatelnej grafiky
     */
    operator fun plusAssign(view: Any?) {
        if (view is View)
            cells.add(view)
    }

    /**
     * Ziskanie n-tej farby
     */
    operator fun get(n: Int) = cells[n]

    //kontrola ci je ake polia prefarbovat
    val empty: Boolean get() = cells.size == 0

    /**
     * nastavena farba uz mozno neplati, metoda vezme z databazy aktualne nastavenu. Je to
     * vyuzite hlavne po navrate z aktivity Screenshot na hlavnu aktivitu, kte zostali este
     * stare farby buniek
     */
    fun accessNewColors(ahsl: Ahsl) {
        target?.let {
            bgHsl = ahsl
            fgHsl = Design.customizedForeground(bgHsl)
            bgHex = Design.hsl2hex(bgHsl)
            fgHex = Design.hsl2hex(fgHsl)
        }
    }

    /**
     * vycistit zoznam farebnych blokov
     */
    fun clear() {
        cells.clear()
    }

    /**
     * Ulozit zmenu farby do tatabazy
     */
    fun storeColor(ahsl: Ahsl) {
        target?.let {
            bgHsl = ahsl
            fgHsl = Design.customizedForeground(ahsl)
            bgHex = Design.hsl2hex(bgHsl)
            fgHex = Design.hsl2hex(fgHsl)
        }
    }

    /**
     * ziskanie farby v hsl
     */
    fun getHsl(): Ahsl {
        return bgHsl
    }

    /**
     * Zmena farby pohladu, (bez aktualizacie v databaze pre lepsi vykon)
     */
    fun recolor(ahsl: Ahsl? = null) {
        if (cells.size > 0) {
            val color = ahsl ?: bgHsl
            val bg = Color.parseColor(Design.hsl2hex(color))
            val fg = Color.parseColor(Design.hsl2hex(Design.customizedForeground(color)))
            for (cell in cells) {
                cell.setBackgroundColor(bg)
                cell.cell_abb?.setTextColor(fg)
                cell.cell_room?.setTextColor(fg)
            }
        }
    }
}
