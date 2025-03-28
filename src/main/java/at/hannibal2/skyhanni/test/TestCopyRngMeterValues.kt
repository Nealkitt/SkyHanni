package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.formatLong
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object TestCopyRngMeterValues {

    private val patternGroup = RepoPattern.group("test.dev.copyrng")

    /**
     * REGEX-TEST: §7§7Slayer XP: §d20,625§5/§d7,917
     */
    private val slayerPattern by patternGroup.pattern(
        "slayer",
        "§7§7Slayer XP: §d.*§5/§d(?<xp>.*)"
    )

    /**
     * REGEX-TEST: §7§7Dungeon Score: §d1,237§5/§d40,620
     */
    private val dungeonPattern by patternGroup.pattern(
        "dungeon",
        "§7§7Dungeon Score: §d.*§5/§d(?<xp>.*)"
    )

    @HandleEvent
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (!SkyHanniMod.feature.dev.debug.copyRngMeter) return

        val map = mutableMapOf<NeuInternalName, Long>()
        for (item in event.inventoryItems.values) {
            for (line in item.getLore()) {
                slayerPattern.matchMatcher(line) {
                    map[item.getInternalName()] = group("xp").formatLong()
                }
                dungeonPattern.matchMatcher(line) {
                    map[item.getInternalName()] = group("xp").formatLong()
                }
            }
        }
        if (map.isEmpty()) return

        OSUtils.copyToClipboard(ConfigManager.gson.toJson(map))
        ChatUtils.debug("${map.size} items saved to clipboard.")
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "dev.copyRngMeter", "dev.debug.copyRngMeter")
    }
}
