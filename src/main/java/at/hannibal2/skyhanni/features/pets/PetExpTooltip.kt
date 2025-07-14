package at.hannibal2.skyhanni.features.pets

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.minecraft.ToolTipEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getItemRarityOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatPercentage
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.PetUtils
import at.hannibal2.skyhanni.utils.ReflectionUtils.makeAccessible
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getPetInfo
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.system.PlatformUtils
import io.github.moulberry.notenoughupdates.NotEnoughUpdates

@SkyHanniModule
object PetExpTooltip {

    private val config get() = SkyHanniMod.feature.misc.pets.petExperienceToolTip
    private const val LEVEL_100_COMMON = 5_624_785
    private const val LEVEL_100_LEGENDARY = 25_353_230
    private const val LEVEL_200_LEGENDARY = 210_255_385

    @HandleEvent(priority = HandleEvent.LOWEST, onlyOnSkyblock = true)
    fun onTooltip(event: ToolTipEvent) {
        if (!config.petDisplay) return
        if (!KeyboardManager.isShiftKeyDown() && !config.showAlways) return

        val itemStack = event.itemStack
        val petExperience = itemStack.getPetInfo()?.exp?.roundTo(1) ?: return
        val name = itemStack.displayName
        try {
            val index = findIndex(event.toolTip) ?: return
            val fixedIndex = if (index > event.toolTip.size) {
                ErrorManager.logErrorStateWithData(
                    "Error in Pet Exp Tooltip",
                    "index is out of bounds of item tooltip",
                    "index" to index,
                    "event.toolTip.size" to event.toolTip.size,
                    "name" to name,
                    "event.toolTip" to event.toolTip,
                    betaOnly = true,
                )
                event.toolTip.size
            } else {
                index
            }

            val internalName = itemStack.getInternalNameOrNull() ?: return
            val (maxLevel, maxXP) = getMaxValues(name, petExperience, internalName)

            val percentage = petExperience / maxXP
            val percentageFormat = percentage.formatPercentage()

            if (percentage < 1) {
                val isBelowLegendary = itemStack.getItemRarityOrNull()?.let { it < LorenzRarity.LEGENDARY } ?: false
                val addLegendaryColor = if (isBelowLegendary) "§6" else ""
                val progressTextLine = "§7Progress to ${addLegendaryColor}Level $maxLevel: §e$percentageFormat"

                val progressBar = StringUtils.progressBar(percentage)
                val progressBarLine = "$progressBar §e${petExperience.addSeparators()}§6/§e${maxXP.shortFormat()}"

                event.toolTip.addAll(fixedIndex, listOf(progressTextLine, progressBarLine, " "))

            }
        } catch (e: Exception) {
            ErrorManager.logErrorWithData(
                e, "Could not add pet exp tooltip",
                "itemStack" to itemStack,
                "item name" to name,
                "petExperience" to petExperience,
                "toolTip" to event.toolTip,
                "index" to findIndex(event.toolTip),
                "getLore" to itemStack.getLore(),
            )
        }
    }

    private fun findIndex(toolTip: List<String>): Int? {
        var index = toolTip.indexOfFirst { it.contains("MAX LEVEL") }
        if (index != -1) {
            return index + 2
        }

        index = toolTip.indexOfFirst { it.contains("Progress to Level") }
        if (index != -1) {

            val offset = if (PlatformUtils.isNeuLoaded() && isNeuExtendedExpEnabled) 4 else 3
            return index + offset
        }

        return null
    }

    private val isNeuExtendedExpEnabled get() = fieldPetExtendExp.get(objectNeuTooltipTweaks) as Boolean

    private val objectNeuTooltipTweaks by lazy {
        val field = NotEnoughUpdates.INSTANCE.config.javaClass.getDeclaredField("tooltipTweaks")
        field.makeAccessible().get(NotEnoughUpdates.INSTANCE.config)
    }

    private val fieldPetExtendExp by lazy {
        objectNeuTooltipTweaks.javaClass.getDeclaredField("petExtendExp").makeAccessible()
    }

    private fun getMaxValues(petName: String, petExperience: Double, internalName: NeuInternalName): Pair<Int, Int> {
        val isLevel200Pet = PetUtils.getMaxLevel(internalName) == 200
        val useLevel200PetLevelling = isLevel200Pet && (!config.showDragonEgg || petExperience >= LEVEL_100_LEGENDARY)

        val maxLevel = if (useLevel200PetLevelling) 200 else 100

        val maxXP = when {
            useLevel200PetLevelling -> LEVEL_200_LEGENDARY
            petName.contains("Bingo") -> LEVEL_100_COMMON

            else -> LEVEL_100_LEGENDARY
        }

        return Pair(maxLevel, maxXP)
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "misc.petExperienceToolTip.petDisplay", "misc.pets.petExperienceToolTip.petDisplay")
        event.move(3, "misc.petExperienceToolTip.showAlways", "misc.pets.petExperienceToolTip.showAlways")
        event.move(3, "misc.petExperienceToolTip.showGoldenDragonEgg", "misc.pets.petExperienceToolTip.showGoldenDragonEgg")
        event.move(96, "misc.pets.petExperienceToolTip.showGoldenDragonEgg", "misc.pets.petExperienceToolTip.showDragonEgg")
    }
}
