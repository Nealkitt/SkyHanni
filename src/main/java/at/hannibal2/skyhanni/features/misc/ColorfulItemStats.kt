package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.model.SkyblockStat
import at.hannibal2.skyhanni.events.item.ItemHoverEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.replace
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object ColorfulItemStats {
    private val config get() = SkyHanniMod.feature.misc.colorfulItemTooltips

    private val group = RepoPattern.group("misc.itemstats")

    /**
     * REGEX-TEST: §7Crit Chance: §c+30%
     * REGEX-TEST: §7Magic Find: §a+54.52
     * REGEX-TEST: §7Rift Time: §a+60s
     * REGEX-TEST: §7Strength: §c+60 §e(+20) §9(+40) §8(+199.2)
     * REGEX-FAIL: §7Health: §c+1000❤
     */
    private val genericStat by group.pattern(
        "generic-stats",
        "§7(?<stat>[a-zA-Z ]+): (?<oldColor>§[0-9a-f])(?<bonus>[-+]?[\\d.,%s]+)(?:\\s|$)",
    )

    @HandleEvent(onlyOnSkyblock = true)
    fun onTooltipEvent(event: ItemHoverEvent) {
        if (!config.enabled) return

        for ((index, line) in event.toolTip.withIndex()) {
            event.toolTip[index] = genericStat.replace(line) {

                val stat = group("stat")
                val oldColor = group("oldColor")

                val statId = stat.uppercase().replace(" ", "_")

                val skyblockStatIcon = SkyblockStat.getIconOrNull(statId) ?: return@replace this.group()

                val bonusGroup = group("bonus")
                val bonus = when {
                    config.replacePercentages && config.statIcons && bonusGroup.endsWith("%") -> bonusGroup.removeSuffix("%")
                    config.replaceRiftSeconds && config.statIcons && bonusGroup.endsWith("s") -> bonusGroup.removeSuffix("s")
                    else -> bonusGroup
                }

                buildString {
                    append("§7$stat: ")
                    append(skyblockStatIcon.take(2))
                    append(bonus)
                    if (config.statIcons) {
                        append(skyblockStatIcon.drop(2))
                    }
                    append(oldColor)
                    append(" ")
                }
            }
        }
    }
}
