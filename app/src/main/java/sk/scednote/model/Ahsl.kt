package sk.scednote.model

/**
 * Uchovava data o farbe reprezentovane v modeli hsl s alfa kanalom
 */
data class Ahsl(private val alpha :Int, private val hue :Int, private val saturation: Int, private val lightness :Int) {
    val h = (if (hue < 0) 360 + hue % 360 else hue % 360)
    val s = saturation.coerceIn(0, 100)
    val l = lightness.coerceIn(0, 100)
    val a = alpha.coerceIn(0, 100)
}