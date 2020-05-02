package sk.scednote.model

import android.os.Handler
import android.util.DisplayMetrics
import android.util.Log
import android.widget.ImageView
import androidx.core.view.updateLayoutParams
import sk.scednote.R
import sk.scednote.ScedNoteApp
import sk.scednote.model.data.Ahsl
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Tu prebiehaju vypoctove metody zobrazenia obrazku
 */
object Design {
    val bg_b = ScedNoteApp.res.getResourceEntryName(R.color.des_background)!!
    val bg_h = ScedNoteApp.res.getResourceEntryName(R.color.des_heading)!!
    val bg_p = ScedNoteApp.res.getResourceEntryName(R.color.des_presentations)!!
    val bg_c = ScedNoteApp.res.getResourceEntryName(R.color.des_courses)!!
    const val FREE = "CELLS_OF_NOTHING"

    private fun hex2dec(hex: String): Long {
        val len = hex.length
        val pattern = "[^0-9a-fA-F#]".toRegex()
        if (pattern.containsMatchIn(hex) || !(len in 4..9 && len != 6 && len != 8))
            return hex2dec("#FB0")
        //ak som sa dostal sem vstupny parameter ma spravny format
        fun extend(n: Int): Int {return n*16+n} //reprezentácia farby z 0-16 na 0-255
        //alfa
        var number: Long = when (len) {
            4 -> extend(hex[1].toString().toInt(16)).toLong()
            9 -> hex.substring(1..2).toInt(16).toLong()
            else -> 255
        }
        //farby
        for (i in 1..3) {
            val k = 3 - i + 1
            number *= 256
            number +=
                if (len in 4..5) extend(hex[len-k].toString().toInt(16))
                else hex.substring(len-2*k..len-2*k+1).toInt(16)
        }
        return number
    }
    fun hex2hsl(hex: String) : Ahsl {
        var number = hex2dec(hex)
        val b = (number % 256)/255.0; number /= 256
        val g = (number % 256)/255.0; number /= 256
        val r = (number % 256)/255.0; number /= 256
        val a = ((number % 256)*100/255.0).roundToInt()

        val cMax = max(max(r, g), b)
        val cMin = min(min(r, g), b)
        val delta = cMax - cMin

        fun dcmp (n1:Double ,n2:Double):Boolean {return abs(n1-n2) < 0.000001}

        val l = (cMax + cMin) / 2
        val s = if (!dcmp(cMax, cMin)) delta / (1 - abs(2*l - 1)) else 0.0
        val h = (60 * (when {
            dcmp(cMax, cMin) -> 0.0
            dcmp(cMax, r) -> ((g-b)/delta) % 6
            dcmp(cMax, g) -> ((b-r)/delta) + 2
            else -> ((r-g)/delta) + 4
        })).roundToInt()

        return Ahsl(a, h, (s * 100).roundToInt(), (l * 100).roundToInt())
    }
    fun hsl2hex(ahsl: Ahsl) :String {
        return hsl2hex(ahsl.h, ahsl.s, ahsl.l, ahsl.a)
    }
    fun hsl2hex(h: Int, s: Int, l:Int, a:Int) :String {
        fun to16(n: Int): String {
            val newHex = n.coerceIn(0, 255).toString(16).toUpperCase(Locale.ROOT)
            return if (n < 16) "0${newHex}" else newHex
        }
        fun to16(n: Double): String { return to16(n.roundToInt()) }

        val alpha = to16(a * 2.55)
        return if (s == 0) {
            val rgb = to16(l * 2.55)
            "#$alpha$rgb$rgb$rgb"
        } else {
            val c: Double = (1 - abs(0.02 * l - 1)) * s * 0.01
            val x: Double = c * (1- abs((h / 60.0) % 2 - 1))
            val m: Double = 0.01 * l - c / 2

            val r = (m + (if (h in 120..239) 0.0 else if (h < 60 || h >= 300) c else x)) * 255
            val g = (m + (if (h >= 240) 0.0 else if (h in 60..179) c else x)) * 255
            val b = (m + (if (h < 120) 0.0 else if (h in 180..299) c else x)) * 255

            "#${alpha}${to16(r)}${to16(g)}${to16(b)}"
        }
    }

    // Zdroj: https://gist.github.com/laaptu/786785
    fun dpToPx (n: Int) = (n * (ScedNoteApp.res.displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT)).roundToInt()
    fun pxToDp (n: Int) = (n / (ScedNoteApp.res.displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT)).roundToInt()

    /**
     * Monochromaticka farebna schema: Odtieň pozadia a farby textu je rovnaký. Snaha udržať kontrast
     */
    fun customizedForeground(background: Ahsl): Ahsl {
        val midContrast = if(background.s > 35 && background.h in 45..200) 35 else 50
        return Ahsl(100, background.h, background.s, if (background.l < midContrast) 85 else 15)
    }

    //smer pohybu obrazku ak je ho pomer stran je rozny od pomeru stran platna
    enum class OverSize { WIDTH, HEIGHT, NONE }
    //rozlozenie obrazku
    enum class ImgFit {
        COVER, CONTAIN, FILL, UNDEFINED;
        companion object {
            private val arr = arrayOf(COVER, CONTAIN, FILL, UNDEFINED)
            operator fun get(n: Int) = arr[n]
        }
        val position get() = arr.indexOf(this)
    }

    class ImageEditor(private val frmW: Int, private val frmH: Int, private val img: ImageView) {
        // praca s obrazkom
        private val imgW: Int get() = img.drawable?.intrinsicWidth ?: 0
        private val imgH: Int get() = img.drawable?.intrinsicHeight ?: 0
        private var width = imgW
        private var height = imgH
        val overSize: OverSize get() {
            if (imgW * imgH == 0 || img.drawable == null) return OverSize.NONE
            val widthRatio = imgW / frmW.toFloat()
            val heightRatio = imgH / frmH.toFloat()
            return when {
                widthRatio < heightRatio -> OverSize.HEIGHT
                widthRatio > heightRatio -> OverSize.WIDTH
                else -> OverSize.NONE
            }
        }
        val bounds: ClosedFloatingPointRange<Float> get() = when {
            overSize == OverSize.WIDTH && getFit() == ImgFit.COVER -> (frmW - width).toFloat()..0F
            overSize == OverSize.HEIGHT && getFit() == ImgFit.COVER -> (frmH - height).toFloat()..0F
            overSize == OverSize.WIDTH && getFit() == ImgFit.CONTAIN -> 0F..(frmH - height).toFloat()
            overSize == OverSize.HEIGHT && getFit() == ImgFit.CONTAIN -> 0F..(frmW - width).toFloat()
            else -> 0F..0F
        }

        val horizontalMovement get() = overSize == OverSize.WIDTH && getFit() == ImgFit.COVER ||
                overSize == OverSize.HEIGHT && getFit() == ImgFit.CONTAIN
        val verticalMovement get() = overSize == OverSize.HEIGHT && getFit() == ImgFit.COVER ||
                overSize == OverSize.WIDTH && getFit() == ImgFit.CONTAIN

        private fun computeWidth (p_fit: ImgFit) = when {
            overSize == OverSize.WIDTH && p_fit == ImgFit.COVER || overSize == OverSize.HEIGHT && p_fit == ImgFit.CONTAIN -> imgW * frmH / imgH
            else -> frmW
        }
        private fun computeHeight (p_fit: ImgFit) = when {
            overSize == OverSize.WIDTH && p_fit == ImgFit.CONTAIN || overSize == OverSize.HEIGHT && p_fit == ImgFit.COVER -> imgH * frmW / imgW
            else -> frmH
        }

        fun getFit(): ImgFit {
            return when {
                imgW * imgH == 0 -> ImgFit.UNDEFINED
                overSize == OverSize.NONE -> ImgFit.FILL
                overSize == OverSize.WIDTH -> if (width == frmW) ImgFit.CONTAIN else ImgFit.COVER
                overSize == OverSize.HEIGHT -> if (height == frmH) ImgFit.CONTAIN else ImgFit.COVER
                else -> ImgFit.UNDEFINED
            }
        }
        fun setFit(value: ImgFit) {
            if (value != ImgFit.UNDEFINED) {
                when (value) {
                    ImgFit.CONTAIN -> {
                        when (overSize) {
                            OverSize.WIDTH -> scaleByWidth(value)
                            OverSize.HEIGHT -> scaleByHeight(value)
                            else -> contain()
                        }
                    }
                    ImgFit.COVER -> {
                        when (overSize) {
                            OverSize.WIDTH -> scaleByHeight(value)
                            OverSize.HEIGHT -> scaleByWidth(value)
                            else -> contain()
                        }
                    }
                    else -> contain()
                }
                img.updateLayoutParams {
                    this.width = this@ImageEditor.width
                    this.height = this@ImageEditor.height
                }
                img.postDelayed({
                    Handler().postDelayed({
                        img.x = frmW / 2F - width / 2F
                        img.y = frmH / 2F - height / 2F
                    }, 0)
                }, 0)
            }
        }

        private fun scaleByWidth(p_fit: ImgFit) {
            height = computeHeight(p_fit)
            width = frmW
        }
        private fun scaleByHeight(p_fit: ImgFit) {
            width = computeWidth(p_fit)
            height = frmH
        }
        private fun contain() {
            width = frmW
            height = frmH
        }
    }
}