package at.hannibal2.skyhanni.features.gui.customscoreboard.elements

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.informationFilteringConfig
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.formatStringNum
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getCopper
import at.hannibal2.skyhanni.features.gui.customscoreboard.ScoreboardPattern
import at.hannibal2.skyhanni.utils.LorenzUtils.inAnyIsland

// scoreboard
// scoreboard update event
object ScoreboardElementCopper : ScoreboardElement() {
    override fun getDisplay(): String? {
        val copper = formatStringNum(getCopper())
        if (informationFilteringConfig.hideEmptyLines && copper == "0") return null

        return CustomScoreboardUtils.formatNumberDisplay("Copper", copper, "§c")
    }

    override val configLine = "Copper: §c23,495"

    override val elementPatterns = listOf(ScoreboardPattern.copperPattern)

    override fun showIsland() = inAnyIsland(IslandType.GARDEN, IslandType.GARDEN_GUEST)
}

// click: warp barn?
