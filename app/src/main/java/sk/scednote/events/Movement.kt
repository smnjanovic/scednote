package sk.scednote.events

import android.view.MotionEvent
import android.view.View

class Movement: View.OnTouchListener {
    companion object {
        private var lastPosX = 0F
        private var lastPosY = 0F
        private var lastEventAction = 0
    }

    //koli zvacseniu grafiky sa objekt nebude posuvat rovnakym tempom ako prst / kurzor po obrazokve
    private var sclX = 1F
    private var sclY = 1F

    private var bdsX = 0F .. 0F
    private var bdsY = 0F .. 0F

    //nastavenie hranic rozsahu a akykolvek uzitocny algoritmus pred pokusom o posun objektu
    private var prepareToMove: (Movement)->Unit = fun(_){}
    fun setUp(fn: (Movement)->Unit): Movement {
        prepareToMove = fn
        return this
    }
    fun setBoundsX(bounds: ClosedFloatingPointRange<Float>) { bdsX = bounds }
    fun setBoundsY(bounds: ClosedFloatingPointRange<Float>) { bdsY = bounds }
    fun mentionScale(scale: Float) {
        sclX = scale
        sclY = scale
    }
    fun mentionScaleX(scaleX: Float) { sclX = scaleX }
    fun mentionScaleY(scaleY: Float) { sclY = scaleY }

    override fun onTouch(view: View?, motion: MotionEvent?): Boolean {
        if (view != null && motion != null) {
            val previousEventAction = lastEventAction
            lastEventAction = motion.action

            when(motion.action) {
                MotionEvent.ACTION_DOWN -> {
                    prepareToMove(this@Movement)
                    lastPosX = motion.rawX
                    lastPosY = motion.rawY
                }
                MotionEvent.ACTION_UP -> if (previousEventAction == MotionEvent.ACTION_DOWN) view.performClick()
                MotionEvent.ACTION_MOVE -> {
                    val dx = (motion.rawX - lastPosX) / sclX
                    val dy = (motion.rawY - lastPosY) / sclY
                    lastPosX = motion.rawX
                    lastPosY = motion.rawY
                    view.x = (view.x + dx).coerceIn(bdsX)
                    view.y = (view.y + dy).coerceIn(bdsY)
                }
                else -> return false
            }
            return true
        }
        return false
    }
}