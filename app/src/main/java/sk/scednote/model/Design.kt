package sk.scednote.model

import sk.scednote.model.data.Ahsl
import kotlin.math.*

/**
 * Tu prebiehaju vypoctove metody zobrazenia obrazku
 */
object Design {
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
            var k = 3 - i + 1
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

        var l = (cMax + cMin) / 2
        var s = if (!dcmp(cMax, cMin)) delta / (1 - abs(2*l - 1)) else 0.0
        var h = (60 * (when {
            dcmp(cMax, cMin) -> 0.0
            dcmp(cMax, r) -> ((g-b)/delta) % 6
            dcmp(cMax, g) -> ((b-r)/delta) + 2
            else -> ((r-g)/delta) + 4
        })).roundToInt()

        return Ahsl(
            a,
            h,
            (s * 100).roundToInt(),
            (l * 100).roundToInt()
        )
    }

    fun hsl2hex(ahsl: Ahsl) :String {
        return hsl2hex(ahsl.h, ahsl.s, ahsl.l, ahsl.a)
    }

    fun hsl2hex(h: Int, s: Int, l:Int, a:Int) :String {
        fun to16(n: Int): String {
            return (
                if (n < 16) "0${n.coerceAtLeast(0).toString(16)}"
                else n.coerceAtMost(255).toString(16)
            ).toUpperCase()
        }
        fun to16(n: Double): String { return to16(n.roundToInt()) }

        val alpha = to16(a * 2.55)
        if (s == 0) {
            val rgb = to16(l * 2.55)
            return "#$alpha$rgb$rgb$rgb"
        }
        else {
            val c: Double = (1 - abs(0.02 * l - 1)) * s * 0.01
            val x: Double = c * (1- abs((h / 60.0) % 2 - 1))
            val m: Double = 0.01 * l - c / 2

            val r = (m + (if (h in 120..239) 0.0 else if (h < 60 || h >= 300) c else x)) * 255
            val g = (m + (if (h >= 240) 0.0 else if (h in 60..179) c else x)) * 255
            val b = (m + (if (h < 120) 0.0 else if (h in 180..299) c else x)) * 255

            return "#${alpha}${to16(r)}${to16(g)}${to16(b)}"
        }
    }

    //este doplnim funkcie ktore budu pocitat rozlozenie obrazku na stranke

    //smer pohybu obrazku podla sirky a vysky
    //nastavenie zvacsenia obrazku vratane jeho pozicie
}