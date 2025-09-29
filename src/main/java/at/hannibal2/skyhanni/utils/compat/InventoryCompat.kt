package at.hannibal2.skyhanni.utils.compat

import at.hannibal2.skyhanni.mixins.transformers.gui.AccessorGuiContainer
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.inventory.Container
import net.minecraft.inventory.ContainerChest
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

//#if FABRIC
//$$ import net.minecraft.screen.slot.SlotActionType
//$$ import at.hannibal2.skyhanni.compat.ReiCompat
//$$ import net.minecraft.client.gui.screen.ingame.HandledScreen
//#endif

fun EntityPlayerSP.getItemOnCursor(): ItemStack? {
    //#if MC < 1.21
    return this.inventory?.itemStack
    //#else
    //$$ val stack = this.currentScreenHandler?.cursorStack
    //$$ if (stack?.isEmpty == true) return null
    //$$ return stack
    //#endif
}

fun stackUnderCursor(): ItemStack? {
    val screen = Minecraft.getMinecraft().currentScreen as? GuiContainer ?: return null
    //#if FORGE
    return screen.slotUnderMouse?.stack
    //#else
    //$$ var stack = screen.focusedSlot?.stack
    //$$ if (stack != null) return stack
    //$$ stack = ReiCompat.getHoveredStackFromRei()
    //$$ return stack
    //#endif
}

fun slotUnderCursor(): Slot? {
    val screen = Minecraft.getMinecraft().currentScreen as? GuiContainer ?: return null
    //#if FORGE
    return screen.slotUnderMouse
    //#else
    //$$ return screen.focusedSlot
    //#endif
}

val GuiChest.container: Container
    //#if MC < 1.16
    get() = this.inventorySlots
//#else
//$$ get() = this.screenHandler
//#endif

object InventoryCompat {

    // TODO add cache that persists until the next gui/window open/close packet is sent/received
    fun getOpenChestName(): String {
        val currentScreen = Minecraft.getMinecraft().currentScreen
        //#if MC < 1.16
        if (currentScreen !is GuiChest) return ""
        val value = currentScreen.inventorySlots as ContainerChest
        return value.lowerChestInventory?.displayName?.unformattedText.orEmpty()
        //#else
        //$$ return currentScreen?.title.formattedTextCompat()
        //#endif
    }


    fun clickInventorySlot(slot: Int, windowId: Int? = getWindowId(), mouseButton: Int, mode: Int) {
        windowId ?: return
        if (slot < 0) return
        val gui = Minecraft.getMinecraft().currentScreen
        //#if FORGE
        if (gui is GuiContainer) {
            val accessor = gui as AccessorGuiContainer
            val slotObj = gui.inventorySlots.getSlot(slot)
            accessor.handleMouseClick_skyhanni(slotObj, slot, mouseButton, mode)
        }
        //#else
        //$$ if (gui is HandledScreen<*>) {
        //$$ val accessor = gui as AccessorHandledScreen
        //$$ val slotObj = gui.screenHandler.getSlot(slot)
        //$$ val actionType = SlotActionType.entries[mode]
        //$$ accessor.handleMouseClick_skyhanni(slotObj, slot, mouseButton, actionType)
        //$$ }
        //#endif
    }

    fun containerSlots(container: GuiContainer): List<Slot> =
        //#if FORGE
        container.inventorySlots.inventorySlots
//#else
//$$ container.screenHandler.slots
//#endif

    private fun getWindowId(): Int? =
        //#if FORGE
        (Minecraft.getMinecraft().currentScreen as? GuiChest)?.inventorySlots?.windowId
//#else
//$$ (MinecraftClient.getInstance().currentScreen as? GenericContainerScreen)?.screenHandler?.syncId
//#endif

    fun Array<ItemStack?>?.filterNotNullOrEmpty(): List<ItemStack>? {
        return this?.filterNotNull()?.filter { it.isNotEmpty() }
    }

    fun Array<ItemStack?>?.convertEmptyToNull(): Array<ItemStack?>? {
        if (this == null) return null
        if (this.isEmpty()) return this
        val new: MutableList<ItemStack?> = mutableListOf()
        for (stack in this) {
            if (!stack.isNotEmpty()) new.add(null)
            else new.add(stack)
        }
        return new.normalizeAsArray()
    }

    @OptIn(ExperimentalContracts::class)
    fun ItemStack?.isNotEmpty(): Boolean {
        contract {
            returns(true) implies (this@isNotEmpty != null)
        }
        this ?: return false
        //#if MC > 1.21
        //$$ return !this.isEmpty
        //#else
        return true
        //#endif
    }

    fun ItemStack?.orNull(): ItemStack? {
        //#if MC > 1.21
        //$$ return this?.takeUnless { it.isEmpty }
        //#endif
        return this
    }
}
