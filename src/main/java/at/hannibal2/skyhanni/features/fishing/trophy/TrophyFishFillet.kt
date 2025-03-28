package at.hannibal2.skyhanni.features.fishing.trophy

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.minecraft.ToolTipEvent
import at.hannibal2.skyhanni.features.fishing.trophy.TrophyFishManager.getFilletValue
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPrice
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import org.lwjgl.input.Keyboard

@SkyHanniModule
object TrophyFishFillet {

    private val MAGMA_FISH = "MAGMA_FISH".toInternalName()

    @HandleEvent
    fun onToolTip(event: ToolTipEvent) {
        if (!isEnabled()) return
        if (InventoryUtils.openInventoryName().contains("Sack")) return

        val internalName = event.itemStack.getInternalName().asString()
        val trophyFishName = internalName.substringBeforeLast("_")
            .replace("_", "").lowercase()
        val trophyRarityName = internalName.substringAfterLast("_")

        val info = TrophyFishManager.getInfo(trophyFishName) ?: return
        val rarity = TrophyRarity.getByName(trophyRarityName) ?: return

        val multiplier = if (Keyboard.KEY_LSHIFT.isKeyHeld()) event.itemStack.stackSize else 1
        val filletValue = info.getFilletValue(rarity) * multiplier

        val filletPrice = filletValue * MAGMA_FISH.getPrice()
        event.toolTip.add("§7Fillet: §8${filletValue.addSeparators()} Magmafish §7(§6${filletPrice.shortFormat()}§7)")
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "fishing.trophyFilletTooltip", "fishing.trophyFishing.filletTooltip")
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && SkyHanniMod.feature.fishing.trophyFishing.filletTooltip
}
