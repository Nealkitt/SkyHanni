package at.hannibal2.skyhanni.utils.renderables

import at.hannibal2.skyhanni.utils.compat.MouseCompat

abstract class ScrollInput(
    private val scrollValue: ScrollValue,
    protected val minValue: Int,
    protected val maxValue: Int,
    protected val velocity: Double,
    protected val dragScrollMouseButton: Int?,
    startValue: Double?,
) {

    init {
        scrollValue.init(startValue ?: minValue.toDouble())
        coerceInLimit()
    }

    protected var scroll
        set(value) {
            scrollValue.setValue(value)
        }
        get() = scrollValue.getValue()

    fun atMinimum() = asInt() == minValue
    fun atMaximum() = asInt() == maxValue
    fun asInt() = scroll.toInt()
    fun asDouble() = scroll
    fun asDirection() =
        if (scroll == 0.0) ScrollDirection.NONE
        else if (scroll > 0.0) ScrollDirection.DOWN
        else ScrollDirection.UP

    enum class ScrollDirection {
        NONE,
        UP,
        DOWN,
    }

    protected fun coerceInLimit() =
        if (maxValue < minValue) {
            scroll = minValue.toDouble()
        } else {
            scroll = scroll.coerceIn(minValue.toDouble(), maxValue.toDouble())
        }

    protected fun isMouseEventValid(): Boolean = scrollValue.isMouseEventValid()
    protected fun isPureScrollEvent() = scrollValue.isPureScrollEvent()

    abstract fun update(isValid: Boolean)

    companion object {

        open class Vertical(
            scrollValue: ScrollValue,
            minHeight: Int,
            maxHeight: Int,
            velocity: Double,
            dragScrollMouseButton: Int?,
            startValue: Double? = null,
        ) : ScrollInput(scrollValue, minHeight, maxHeight, velocity, dragScrollMouseButton, startValue) {
            override fun update(isValid: Boolean) {
                if (maxValue < minValue) return
                if (!isValid || !isMouseEventValid()) return
                if (dragScrollMouseButton != null && MouseCompat.isButtonDown(dragScrollMouseButton)) {
                    scroll += MouseCompat.getEventDY() * velocity
                }
                val deltaWheel = MouseCompat.getScrollDelta()
                scroll += -deltaWheel.coerceIn(-1, 1) * 2.5 * velocity
                coerceInLimit()
            }
        }

        /**
         * Instead of storing a state here, we are solely storing a -1/1 value for the scroll status.
         * Should not be used to hold actual data, but rather as a tracker for pure scroll events.
         */
        class PureVertical(
            scrollValue: ScrollValue = ScrollValue(),
        ) : Vertical(scrollValue, -1, 1, 1.0, null, 0.0) {
            override fun update(isValid: Boolean) {
                // For pure events, we don't care about tracking state
                // and only care about tracking a 1/-1 for the scroll status.
                // We reset to 0 to avoid repeatedly applying the same scroll value.
                dispose()
                if (!isPureScrollEvent()) return

                // Otherwise we let the parent class handle the rest.
                super.update(isValid)
            }

            fun dispose() {
                scroll = 0.0
            }
        }

    }
}

class ScrollValue {
    private var field: Double? = null
    private var mouseEventTime = 0L
    private var lastMouseX = 0
    private var lastMouseY = 0

    fun getValue(): Double =
        field ?: throw IllegalStateException("ScrollValue should be initialized before get.")

    fun setValue(value: Double) {
        field = value
    }

    fun init(value: Double) {
        if (field != null) return
        field = value
    }

    fun isMouseEventValid(): Boolean {
        val mouseEvent = MouseCompat.getEventNanoseconds()
        val mouseEventsValid = mouseEvent - mouseEventTime > 20L
        mouseEventTime = mouseEvent
        return mouseEventsValid
    }

    fun isPureScrollEvent(): Boolean {
        val mouseX = MouseCompat.getEventX()
        val mouseY = MouseCompat.getEventY()
        val isScrollEvent = MouseCompat.getScrollDelta() != 0
        val hasMouseMoved = mouseX != lastMouseX || mouseY != lastMouseY

        // Update last mouse position
        lastMouseX = mouseX
        lastMouseY = mouseY

        return isScrollEvent && !hasMouseMoved
    }
}
