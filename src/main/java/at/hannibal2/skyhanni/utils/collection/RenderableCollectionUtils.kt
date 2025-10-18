package at.hannibal2.skyhanni.utils.collection

import at.hannibal2.skyhanni.utils.ItemUtils.addEnchantGlint
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuItems
import at.hannibal2.skyhanni.utils.NeuItems.getItemStack
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.renderables.primitives.ItemStackRenderable.Companion.item
import at.hannibal2.skyhanni.utils.renderables.primitives.placeholder
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.renderables.toSearchable
import net.minecraft.item.ItemStack
import java.util.Collections

// TODO move the type specific into the companion objects, the rest goes back into the RenderableUtils
object RenderableCollectionUtils {

    fun MutableList<Renderable>.addString(
        text: String,
        scale: Double = 1.0,
        horizontalAlign: RenderUtils.HorizontalAlignment = RenderUtils.HorizontalAlignment.LEFT,
        verticalAlign: RenderUtils.VerticalAlignment = RenderUtils.VerticalAlignment.CENTER,
    ) {
        add(Renderable.text(text, scale, horizontalAlign = horizontalAlign, verticalAlign = verticalAlign))
    }

    fun MutableList<Renderable>.addString(
        text: String,
        tips: List<String>,
        horizontalAlign: RenderUtils.HorizontalAlignment = RenderUtils.HorizontalAlignment.LEFT,
        verticalAlign: RenderUtils.VerticalAlignment = RenderUtils.VerticalAlignment.CENTER,
    ) {
        add(Renderable.hoverTips(Renderable.text(text, horizontalAlign = horizontalAlign, verticalAlign = verticalAlign), tips = tips))
    }

    fun MutableList<Searchable>.addSearchString(
        text: String,
        searchText: String? = null,
        horizontalAlign: RenderUtils.HorizontalAlignment = RenderUtils.HorizontalAlignment.LEFT,
        verticalAlign: RenderUtils.VerticalAlignment = RenderUtils.VerticalAlignment.CENTER,
    ) {
        add(Renderable.text(text, horizontalAlign = horizontalAlign, verticalAlign = verticalAlign).toSearchable(searchText))
    }

    fun MutableList<List<Renderable>>.addSingleString(text: String) {
        add(Collections.singletonList(Renderable.text(text)))
    }

    fun MutableList<Renderable>.addItemStack(
        itemStack: ItemStack,
        highlight: Boolean = false,
        scale: Double = NeuItems.ITEM_FONT_SIZE,
    ) {
        if (highlight) {
            itemStack.addEnchantGlint()
        }
        add(Renderable.item(itemStack, scale = scale))
    }

    fun MutableList<Renderable>.addItemStack(internalName: NeuInternalName) {
        addItemStack(internalName.getItemStack())
    }

    fun Collection<Collection<Renderable>>.tableStretchXPadding(xSpace: Int): Int {
        if (this.isEmpty()) return xSpace
        val off = RenderableUtils.calculateTableXOffsets(this, 0)
        val xLength = off.size - 1
        val emptySpace = xSpace - off.last()
        return emptySpace / (xLength - 1)
    }

    fun Collection<Collection<Renderable>>.tableStretchYPadding(ySpace: Int): Int {
        if (this.isEmpty()) return ySpace
        val off = RenderableUtils.calculateTableYOffsets(this, 0)
        val yLength = off.size - 1
        val emptySpace = ySpace - off.last()
        return emptySpace / (yLength - 1)
    }

    fun MutableList<Renderable>.addHorizontalSpacer(width: Int = 3) {
        add(Renderable.placeholder(width, 0))
    }

    fun MutableList<Renderable>.addVerticalSpacer(height: Int = 10) {
        add(Renderable.placeholder(0, height))
    }
}
