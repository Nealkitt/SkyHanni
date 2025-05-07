package at.hannibal2.skyhanni.utils.guide

import at.hannibal2.skyhanni.utils.GuiRenderUtils
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.renderXYAligned
import net.minecraft.item.ItemStack

class GuideTab(
    val item: ItemStack,
    val tip: Renderable,
    val isVertical: Boolean = false,
    var lastTab: GuideGui.tabWrapper,
    val onClick: (GuideTab) -> Unit
) {

    fun fakeClick() = click()

    private fun click() {
        onClick.invoke(this)
        this.select()
        if (lastTab.tab != this) {
            lastTab.tab?.unSelect()
            lastTab.tab = this
        }
    }

    fun select() {
        selectColor = GuideGui.SELECTED_COLOR
    }

    fun unSelect() {
        selectColor = GuideGui.NOT_SELECTED_COLOR
    }

    fun isSelected() = selectColor == GuideGui.SELECTED_COLOR

    val width = if (isVertical) GuideGui.TAB_LONG_SIDE else GuideGui.TAB_SHORT_SIDE
    val height = if (isVertical) GuideGui.TAB_SHORT_SIDE else GuideGui.TAB_LONG_SIDE

    private var selectColor = GuideGui.NOT_SELECTED_COLOR

    private val renderable = Renderable.clickable(
        object : Renderable {
            override val width = this@GuideTab.width
            override val height = this@GuideTab.height
            override val horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT
            override val verticalAlign: VerticalAlignment = VerticalAlignment.TOP

            val itemRender = Renderable.itemStack(
                item, 1.0, horizontalAlign = HorizontalAlignment.CENTER, verticalAlign = VerticalAlignment.CENTER
            )

            override fun render(posX: Int, posY: Int) {
                GuiRenderUtils.drawRect(0, 0, width, height, selectColor)
                itemRender.renderXYAligned(posX, posY, width, height)
            }
        },
        tips = listOf(tip),
        onLeftClick = {
            click()
            SoundUtils.playClickSound()
        }
    )

    fun render(posX: Int, posY: Int) {
        renderable.render(posX, posY)
    }
}
