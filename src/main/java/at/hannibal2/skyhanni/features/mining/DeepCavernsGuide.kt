package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.jsonobjects.repo.ParkourJson
import at.hannibal2.skyhanni.data.repo.RepoManager
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.events.render.gui.ReplaceItemEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.ParkourHelper
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SpecialColor.toSpecialColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.player.inventory.ContainerLocalMenu
import net.minecraft.init.Items

@SkyHanniModule
object DeepCavernsGuide {

    private val config get() = SkyHanniMod.feature.mining.deepCavernsGuide

    private var parkourHelper: ParkourHelper? = null
    private var show = false
    private var showStartIcon = false

    private val startIcon by lazy {
        ItemUtils.createItemStack(
            Items.map,
            "§bDeep Caverns Guide",
            "§8(From SkyHanni)",
            "",
            "§7Manually enable the ",
            "§7guide to the bottom",
            "§7of the Deep Caverns.",
        )
    }

    private val patternGroup = RepoPattern.group("features.mining.deepcavernsguide")
    private val notUnlockedPattern by patternGroup.pattern(
        "notunlocked",
        "§e\\[NPC] §bLift Operator§f: §rVenture down into the Lapis Quarry to unlock my Lift Menu!",
    )

    @HandleEvent
    fun onIslandChange(event: IslandChangeEvent) {
        parkourHelper?.reset()
        show = false
    }

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val data = event.getConstant<ParkourJson>("DeepCavernsParkour")
        parkourHelper = ParkourHelper(
            data.locations,
            data.shortCuts,
            platformSize = 1.0,
            detectionRange = 3.5,
            depth = false,
            onEndReach = {
                show = false
            },
        )
        updateConfig()
    }

    @HandleEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        ConditionalUtils.onToggle(config.rainbowColor, config.monochromeColor, config.lookAhead) {
            updateConfig()
        }
    }

    private fun updateConfig() {
        parkourHelper?.run {
            rainbowColor = config.rainbowColor.get()
            monochromeColor = config.monochromeColor.get().toSpecialColor()
            lookAhead = config.lookAhead.get() + 1
        }
    }

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent) {
        if (!isEnabled()) return
        if (LorenzUtils.skyBlockArea != "Gunpowder Mines") return
        if (notUnlockedPattern.matches(event.message)) {
            DelayedRun.runNextTick {
                start()
            }
        }
    }

    @HandleEvent
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        showStartIcon = false
        if (!isEnabled()) return
        if (event.inventoryName != "Lift") return
        if (LorenzUtils.skyBlockArea != "Gunpowder Mines") return
        showStartIcon = true

        event.inventoryItems[31]?.let {
            if (it.displayName != "§aObsidian Sanctuary") {
                start()
            }
        }
    }

    private fun start() {
        if (show) return
        show = true
        parkourHelper?.reset()
        if (parkourHelper == null) {
            ChatUtils.clickableChat(
                "DeepCavernsParkour missing in SkyHanni Repo! Try /shupdaterepo to fix it!",
                onClick = {
                    RepoManager.updateRepo()
                },
                "§eClick to update the repo!",
                prefixColor = "§c",
            )
        }
        @Suppress("MaxLineLength")
        ChatUtils.chat(
            "Automatically enabling the Deep Caverns Guide, helping you find the way to the bottom of the Deep Caverns and the path to Rhys."
        )
    }

    @HandleEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        showStartIcon = false
    }

    @HandleEvent
    fun replaceItem(event: ReplaceItemEvent) {
        if (show) return
        if (event.inventory is ContainerLocalMenu && showStartIcon && event.slot == 49) {
            event.replace(startIcon)
        }
    }

    @HandleEvent(priority = HandleEvent.HIGH)
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (showStartIcon && event.slotId == 49) {
            event.cancel()
            ChatUtils.chat("Manually enabled Deep Caverns Guide.")
            start()
        }
    }

    @HandleEvent
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!isEnabled()) return
        if (!show) return

        parkourHelper?.render(event)
    }

    fun isEnabled() = IslandType.DEEP_CAVERNS.isInIsland() && config.enabled

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(38, "mining.deepCavernsParkour", "mining.deepCavernsGuide")
    }
}
