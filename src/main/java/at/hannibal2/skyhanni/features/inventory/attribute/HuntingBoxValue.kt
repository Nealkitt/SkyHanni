package at.hannibal2.skyhanni.features.inventory.attribute

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.DisplayTableEntry
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemPriceSource
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPrice
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.skyhanni.utils.compat.InventoryCompat.orNull
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack

@SkyHanniModule
object HuntingBoxValue {

    private val config get() = AttributeShardsData.config
    private var display = emptyList<Renderable>()

    private var totalShards = 0
    private var totalInstantSell = 0.0
    private var totalInstantBuy = 0.0

    fun processInventory(slots: List<Slot>) {
        if (!config.huntingBoxValue) return

        totalShards = 0
        totalInstantSell = 0.0
        totalInstantBuy = 0.0

        val table = mutableListOf<DisplayTableEntry>()

        for (slot in slots) {
            val slotNumber = slot.slotNumber
            if (!isValidSlotNumber(slotNumber)) continue
            val stack = slot.stack.orNull() ?: continue
            processAttributeShardSlot(slotNumber, stack, table)
        }

        display = buildList {
            addString("§eHunting Box Value")

            if (table.isNotEmpty()) {
                add(RenderableUtils.fillScrollTable(table, padding = 5, itemScale = 0.7, height = 225, velocity = 5.0))
            } else {
                possiblyAddWarning()
            }

            addString("§7Total Attribute Shards: §a$totalShards")
            addString("§7Total Instant Sell Value: §6${totalInstantSell.toLong().addSeparators()}")
            addString("§7Total Instant Buy Value: §6${totalInstantBuy.toLong().addSeparators()}")
        }
    }

    private fun MutableList<Renderable>.possiblyAddWarning() {
        InventoryUtils.getItemAtSlotIndex(10).orNull() ?: return

        addString("§cError detected!")
        addString("§cPlease run §e/shdebug repo§c to get debug information.")
        addString("§cThen send the data on discord.")
    }

    private fun processAttributeShardSlot(slotNumber: Int, stack: ItemStack, table: MutableList<DisplayTableEntry>) {
        val internalName = stack.getInternalNameOrNull() ?: return

        val amountOwned = AttributeShardsData.amountOwnedPattern.firstMatcher(stack.getLore()) {
            group("amount").formatInt()
        } ?: return
        totalShards += amountOwned

        val pricePerInstantSell = internalName.getPrice(ItemPriceSource.BAZAAR_INSTANT_SELL)
        val totalPriceInstantSell = pricePerInstantSell * amountOwned
        totalInstantSell += totalPriceInstantSell

        val pricePerInstantBuy = internalName.getPrice(ItemPriceSource.BAZAAR_INSTANT_BUY)
        val totalPriceInstantBuy = pricePerInstantBuy * amountOwned
        totalInstantBuy += totalPriceInstantBuy

        val hover = buildList {
            add(internalName.repoItemName)
            add("")
            add("§7Price per Instant Sell: §6${pricePerInstantSell.toInt().addSeparators()}")
            add("§7Price per Instant Buy: §6${pricePerInstantBuy.toInt().addSeparators()}")
            add("")
            add("§7Amount Owned: §a$amountOwned")
            add("§7Total Price Instant Sell: §6${totalPriceInstantSell.toLong().addSeparators()}")
            add("§7Total Price Instant Buy: §6${totalPriceInstantBuy.toLong().addSeparators()}")
        }

        table.add(
            DisplayTableEntry(
                "${internalName.repoItemName} §8x$amountOwned",
                "§6${totalPriceInstantSell.toLong().addSeparators()}",
                totalPriceInstantSell,
                internalName,
                hover,
                highlightsOnHoverSlots = listOf(slotNumber),
            ),
        )
    }

    private fun isValidSlotNumber(slot: Int): Boolean {
        if (slot < 9 || slot > 44) return false
        val modNine = slot % 9
        return modNine != 0 && modNine != 8
    }

    @HandleEvent(GuiRenderEvent.ChestGuiOverlayRenderEvent::class, onlyOnSkyblock = true)
    fun onRenderOverlay() {
        if (!config.huntingBoxValue) return
        if (!AttributeShardsData.huntingBoxInventory.isInside()) return

        config.huntingBoxValuePosition.renderRenderables(display, posLabel = "Hunting Box Value")
    }
}
