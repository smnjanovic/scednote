import android.graphics.Color
import android.util.Log
import android.view.View
import kotlinx.android.synthetic.main.tbl_cell.view.*
import sk.scednote.model.Design
import sk.scednote.model.data.Ahsl
import java.util.ArrayList

class CellGroup(private val target: String? = null, color: Ahsl) {
    private val cells = ArrayList<View>()
    private var bgHsl = color
    private var fgHsl = Design.customizedForeground(color)
    private var bgHex = Design.hsl2hex(bgHsl)
    private var fgHex = Design.hsl2hex(fgHsl)

    operator fun plusAssign(view: Any?) {
        if (view is View)
            cells.add(view)
    }
    operator fun get(n: Int) = cells[n]

    val empty: Boolean get() = cells.size == 0
    fun updateColors(ahsl: Ahsl) {
        target?.let {
            bgHsl = ahsl
            fgHsl = Design.customizedForeground(bgHsl)
            bgHex = Design.hsl2hex(bgHsl)
            fgHex = Design.hsl2hex(fgHsl)
        }
    }
    fun clear() {
        cells.clear()
    }
    //uchovat farbu v databaze. Nenastavitelne farby maju target null
    fun storeColor(ahsl: Ahsl) {
        target?.let {
            bgHsl = ahsl
            fgHsl = Design.customizedForeground(ahsl)
            bgHex = Design.hsl2hex(bgHsl)
            fgHex = Design.hsl2hex(fgHsl)
        }
    }

    fun getHsl(): Ahsl {
        return bgHsl
    }
    //prefarbit ramec
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
