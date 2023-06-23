package at.hannibal2.skyhanni.features.rift

import at.hannibal2.skyhanni.events.*
import at.hannibal2.skyhanni.test.GriffinUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import io.github.moulberry.notenoughupdates.events.ReplaceItemEvent
import io.github.moulberry.notenoughupdates.events.SlotClickEvent
import io.github.moulberry.notenoughupdates.util.Utils
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.client.player.inventory.ContainerLocalMenu
import net.minecraft.init.Blocks
import net.minecraft.inventory.ContainerChest
import net.minecraft.item.ItemStack
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object EnigmaSoulWaypoints {
    private var inInventory = false
    private val soulLocations = mutableMapOf<String, Pair<LorenzVec, RiftRegion>>()
    private val trackedSouls = mutableListOf<String>()
    private val inventoryUnfound = mutableListOf<String>()
    private var adding = true

    private val item by lazy {
        val neuItem = NEUItems.getItemStackOrNull("SKYBLOCK_ENIGMA_SOUL") ?: ItemStack(Blocks.obsidian) // can prob have a better fallback
        Utils.createItemStack(neuItem.item, "§5Toggle Missing", "§7Click here to toggle", "§7the waypoints for each", "§7missing souls on this page")
    }

    @SubscribeEvent
    fun replaceItem(event: ReplaceItemEvent) {
        if (inventoryUnfound.isEmpty()) return
        if (event.inventory is ContainerLocalMenu && inInventory && event.slotNumber == 31) {
            event.replaceWith(item)
        }
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryOpenEvent) {
        inInventory = false
        if (!event.inventoryName.contains("Enigma Souls")) return
        inInventory = true

        for (stack in event.inventoryItems.values) {
            val split = stack.displayName.split("Enigma: ")
            if (split.size == 2) {
                if (stack.getLore().last() == "§8✖ Not completed yet!") {
                    inventoryUnfound.add(split.last())
                }
            }
        }
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        inInventory = false
        inventoryUnfound.clear()
        adding = true
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onSlotClick(event: SlotClickEvent) {
        if (!inInventory || !RiftAPI.inRift()) return

        if (event.slotId == 31 && inventoryUnfound.isNotEmpty()) {
            event.isCanceled =  true
            if (adding) {
                trackedSouls.addAll(inventoryUnfound)
                adding = false
            } else {
                trackedSouls.removeAll(inventoryUnfound)
                adding = true
            }
        }

        val split = event.slot.stack.displayName.split("Enigma: ")
        if (split.size == 2) {
            event.isCanceled =  true // maybe change to a middle click
            if (soulLocations.contains(split.last())) {
                if (!trackedSouls.contains(split.last())) {
                    LorenzUtils.chat("§5Tracking the ${split.last()} Enigma Soul!")
                    trackedSouls.add(split.last())
                } else {
                    trackedSouls.remove(split.last())
                    LorenzUtils.chat("§5No longer tracking the ${split.last()} Enigma Soul!")
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!RiftAPI.inRift() || !inInventory) return

        if (event.gui !is GuiChest) return
        val guiChest = event.gui
        val chest = guiChest.inventorySlots as ContainerChest

        for (slot in chest.inventorySlots) {
            if (slot == null) continue
            val stack = slot.stack ?: continue

            for (soul in trackedSouls) {
                if (stack.displayName.removeColor().contains(soul)) {
                    slot highlight LorenzColor.DARK_PURPLE
                }
            }
        }
        if (!adding) {
            chest.inventorySlots[31] highlight LorenzColor.DARK_PURPLE
        }
    }

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorldLastEvent) {
        if (!RiftAPI.inRift()) return
        for (soul in trackedSouls) {
            soulLocations[soul]?.let { event.drawWaypointFilled(it.first, LorenzColor.DARK_PURPLE.toColor(), true, true) }
            soulLocations[soul]?.let { event.drawDynamicText(it.first.add(0, 1, 0), "§5$soul Soul", 1.0) }
        }
    }

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val data = event.getConstant("EnigmaSouls") ?: return

        for (area in data.entrySet()) {
            val region = RiftRegion.values().firstOrNull {it.areaName == area.key.toString()} ?: continue
            val element = area.value.asJsonArray

            for (i in 0 until element.size()) {
                val itemObject = element[i].asJsonObject
                val name = itemObject["name"].asString
                val position = itemObject["position"].asString
                val split = position.split(", ")
                if (split.size == 3) {
                    val location = LorenzVec(split[0].toDouble(), split[1].toDouble(), split[2].toDouble())
                    soulLocations[name] = Pair(location, region)
                }
            }
        }
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!RiftAPI.inRift()) return
        val message = event.message.removeColor().trim()
        // not sure how this works for buying the souls
        if (message == "You have already found that Enigma Soul!" || message == "SOUL! You unlocked an Enigma Soul!") {
            hideClosestSoul()
        }
    }

    private fun hideClosestSoul() {
        var closestSoul = ""
        var closestDistance = 8.0

        for (soul in soulLocations) {
            if (soul.value.first.distanceToPlayer() < closestDistance) {
                closestSoul = soul.key
                closestDistance = soul.value.first.distanceToPlayer()
            }
        }
        trackedSouls.remove(closestSoul)
        LorenzUtils.chat("§5Found the $closestSoul Enigma Soul!")
    }
}