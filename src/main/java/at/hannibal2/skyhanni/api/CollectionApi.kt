package at.hannibal2.skyhanni.api

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.ItemAddManager
import at.hannibal2.skyhanni.events.CollectionUpdateEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.ItemAddEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NeuItems
import at.hannibal2.skyhanni.utils.NeuItems.getItemStackOrNull
import at.hannibal2.skyhanni.utils.NumberUtil.formatLong
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object CollectionApi {
    private val patternGroup = RepoPattern.group("data.collection.api")

    /**
     * REGEX-TEST: §2§l§m                      §f§l§m   §r §e43,649§6/§e50k
     */
    private val counterPattern by patternGroup.pattern(
        "counter",
        ".* §e(?<amount>.*)§6/.*",
    )

    /**
     * REGEX-TEST: §7Total collected: §e261,390
     * REGEX-TEST: §7Total Collected: §e2,012,418
     */
    private val singleCounterPattern by patternGroup.pattern(
        "singlecounter",
        "§7Total [c|C]ollected: §e(?<amount>.*)",
    )

    /**
     * REGEX-TEST: §7Progress to Nether Wart I: §e46§6%
     */
    private val collectionTier0Pattern by patternGroup.pattern(
        "tierzero",
        "§7Progress to .* I: .*",
    )

    val collectionValue = mutableMapOf<NeuInternalName, Long>()

    // TODO repo
    private val incorrectCollectionNames = mapOf(
        "Mushroom" to "RED_MUSHROOM".toInternalName(),
    )

    @HandleEvent
    fun onProfileJoin(event: ProfileJoinEvent) {
        collectionValue.clear()
    }

    @HandleEvent
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        val inventoryName = event.inventoryName
        if (inventoryName.endsWith(" Collection")) {
            val stack = event.inventoryItems[4] ?: return
            singleCounterPattern.firstMatcher(stack.getLore()) {
                val counter = group("amount").formatLong()
                val name = inventoryName.split(" ").dropLast(1).joinToString(" ")
                val internalName = incorrectCollectionNames[name] ?: NeuInternalName.fromItemName(name)
                collectionValue[internalName] = counter
            }
            CollectionUpdateEvent.post()
        }

        if (inventoryName.endsWith(" Collections")) {
            if (inventoryName == "Boss Collections") return

            for ((_, stack) in event.inventoryItems) {
                var name = stack.displayName.removeColor()
                if (name.contains("Collections")) continue

                val lore = stack.getLore()
                if (!lore.any { it.contains("Click to view!") }) continue

                if (!isCollectionTier0(lore)) {
                    name = name.split(" ").dropLast(1).joinToString(" ")
                }

                val internalName = incorrectCollectionNames[name] ?: NeuInternalName.fromItemName(name)
                counterPattern.firstMatcher(lore) {
                    val counter = group("amount").formatLong()
                    collectionValue[internalName] = counter
                }
            }
            CollectionUpdateEvent.post()
        }
    }

    @HandleEvent
    fun onItemAdd(event: ItemAddEvent) {
        if (event.source == ItemAddManager.Source.COMMAND) return
        val internalName = event.internalName
        val amount = NeuItems.getPrimitiveMultiplier(internalName).amount
        if (amount > 1) return

        // TODO add support for replenish (higher collection than actual items in inv)
        if (internalName.getItemStackOrNull() == null) {
            ChatUtils.debug("CollectionAPI.addFromInventory: item is null for '$internalName'")
            return
        }
        collectionValue.addOrPut(internalName, event.amount.toLong())
    }

    fun isCollectionTier0(lore: List<String>) = lore.any { collectionTier0Pattern.matches(it) }
    fun getCollectionCounter(internalName: NeuInternalName): Long? = collectionValue[internalName]
}
