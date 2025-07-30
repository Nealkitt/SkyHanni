package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.PurseChangeEvent
import at.hannibal2.skyhanni.events.SackChangeEvent
import at.hannibal2.skyhanni.events.item.ShardGainEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemCategory
import at.hannibal2.skyhanni.utils.ItemNameResolver
import at.hannibal2.skyhanni.utils.ItemPriceUtils.formatCoin
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPriceOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getItemCategoryOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getItemRarityOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NeuItems.getItemStack
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.compat.getItemOnCursor
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.primitives.ItemStackRenderable.Companion.item
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.item.ItemStack
import java.util.Objects
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object ItemPickupLog {
    enum class DisplayLayout(private val display: String, val renderable: (PickupEntry, String) -> Renderable) {
        CHANGE_AMOUNT(
            "§a+256",
            { entry, prefix ->
                val formattedAmount = if (config.shorten) entry.amount.shortFormat() else entry.amount.addSeparators()
                Renderable.text("$prefix$formattedAmount")
            },
        ),
        ICON(
            "§e✎",
            { entry, _ ->
                val entryInternalName = entry.neuInternalName ?: ItemNameResolver.getInternalNameOrNull(entry.name)
                if (entryInternalName != null) Renderable.item(entryInternalName)
                else Renderable.text("§c?")
            },
        ),
        ITEM_NAME(
            "§d[:3] TransRights's Cake Soul",
            { entry, _ ->
                var name = entry.name
                if (entry.name == "Air") {
                    name = entry.neuInternalName?.repoItemName ?: "?"
                }
                Renderable.text(name)
            },
        ),
        ;

        override fun toString() = display
    }

    data class PickupEntry(val name: String, var amount: Long, val neuInternalName: NeuInternalName?) {
        var timeUntilExpiry = SimpleTimeMark.now()

        fun updateAmount(change: Long) {
            amount += change
            timeUntilExpiry = SimpleTimeMark.now()
        }

        fun isExpired() = timeUntilExpiry.passedSince() > config.expireAfter.seconds

        fun getPlusRenderable(hash: Int) = renderableCache.getOrPut(hash to "§a+") {
            renderList("§a+", this)
        }
        fun getMinusRenderable(hash: Int) = renderableCache.getOrPut(hash to "§c-") {
            renderList("§c-", this)
        }
    }

    private val renderableCache = mutableMapOf<Pair<Int, String>, Renderable>()
    private val config get() = SkyHanniMod.feature.inventory.itemPickupLog
    private val coinConfig get() = config.coinValue
    private val coinIcon = "COIN_TALISMAN".toInternalName()

    private val itemList = mutableMapOf<Int, Pair<ItemStack, Int>>()
    private val itemsAddedToInventory = mutableMapOf<Int, PickupEntry>()
    private val itemsRemovedFromInventory = mutableMapOf<Int, PickupEntry>()
    private var display: Renderable? = null
    private var dirty = false

    private val patternGroup = RepoPattern.group("itempickuplog")

    /**
     * REGEX-TEST: Mite Gel x33
     * REGEX-TEST: Sludge Juice
     */
    private val shopPattern by patternGroup.pattern(
        "shoppattern",
        "^(?<itemName>.+?)(?: x\\d+)?\$",
    )

    @HandleEvent
    fun onRenderOverlay(event: GuiRenderEvent) {
        if (!isEnabled()) return
        display?.let { config.position.renderRenderable(it, posLabel = "Item Pickup Log Display") }
    }

    @HandleEvent
    fun onWorldChange() {
        if (!isEnabled()) return
        itemList.clear()
        itemsAddedToInventory.clear()
        itemsRemovedFromInventory.clear()
    }

    @HandleEvent
    fun onSackChange(event: SackChangeEvent) {
        if (!isEnabled() || !config.sack) return

        event.sackChanges.forEach {
            val itemStack = (it.internalName.getItemStack())
            val item = PickupEntry(itemStack.dynamicName(), it.delta.absoluteValue.toLong(), it.internalName)

            updateItem(itemStack.hash(), item, it.delta < 0)
        }
    }

    @HandleEvent
    fun onShardGain(event: ShardGainEvent) {
        if (!isEnabled() || !config.shards) return

        val itemStack = event.shardInternalName.getItemStack()
        val item = PickupEntry(itemStack.dynamicName(), event.amount.absoluteValue.toLong(), event.shardInternalName)

        updateItem(itemStack.hash(), item, event.amount < 0)
    }

    @HandleEvent
    fun onPurseChange(event: PurseChangeEvent) {
        if (!isEnabled() || !config.coins || !worldChangeCooldown()) return

        updateItem(0, PickupEntry("§6Coins", event.coins.absoluteValue.toLong(), coinIcon), event.coins < 0)
    }

    @HandleEvent
    fun onTick() {
        if (!isEnabled()) return
        val oldItemList = mutableMapOf<Int, Pair<ItemStack, Int>>()

        oldItemList.putAll(itemList)

        if (!InventoryUtils.inInventory()) handleNotInInventory(oldItemList)
        if (!worldChangeCooldown()) return

        checkForDuplicateItems(itemList, oldItemList, false)
        checkForDuplicateItems(oldItemList, itemList, true)

        val itemsRemovedUpdated = itemsRemovedFromInventory.values.removeIf { it.isExpired() }
        val itemsAddedUpdated = itemsAddedToInventory.values.removeIf { it.isExpired() }

        if (itemsRemovedUpdated || itemsAddedUpdated || itemList != oldItemList || dirty) {
            dirty = false
            updateDisplay()
        }
    }

    private fun handleNotInInventory(oldItemList: MutableMap<Int, Pair<ItemStack, Int>>) {
        itemList.clear()

        val inventoryItems = InventoryUtils.getItemsInOwnInventoryWithNull()?.filterIndexed { i, _ -> i != 8 }
            ?.filterNotNull().orEmpty().toMutableList()
        val cursorItem = MinecraftCompat.localPlayer.getItemOnCursor()

        if (cursorItem != null) {
            val hash = cursorItem.hash()
            // this prevents items inside hypixel guis counting when picked up
            if (oldItemList.contains(hash)) {
                inventoryItems.add(cursorItem)
            }
        }

        for (itemStack in inventoryItems) {
            val hash = itemStack.hash()
            val old = itemList[hash]
            if (old != null) {
                itemList[hash] = old.copy(second = old.second + itemStack.stackSize)
            } else {
                itemList[hash] = itemStack to itemStack.stackSize
            }
        }
    }

    // TODO merge with ItemAddInInventoryEvent
    private fun updateItem(hash: Int, itemInfo: PickupEntry, removed: Boolean) {
        val targetInventory = if (removed) itemsRemovedFromInventory else itemsAddedToInventory
        val oppositeInventory = if (removed) itemsAddedToInventory else itemsRemovedFromInventory

        oppositeInventory[hash]?.let { existingItem ->
            existingItem.timeUntilExpiry = SimpleTimeMark.now()
            renderableCache.keys.removeIf { it.first == hash }
        }

        targetInventory[hash]?.let { existingItem ->
            existingItem.updateAmount(itemInfo.amount)
            renderableCache.keys.removeIf { it.first == hash }
            return
        }

        targetInventory[hash] = itemInfo
        dirty = true
    }

    private fun renderList(prefix: String, entry: PickupEntry) = Renderable.horizontal {
        val displayLayout: List<DisplayLayout> = config.displayLayout
        for (item in displayLayout) {
            add(item.renderable(entry, prefix))
        }
    }

    private fun checkForDuplicateItems(
        list: MutableMap<Int, Pair<ItemStack, Int>>,
        listToCheckAgainst: MutableMap<Int, Pair<ItemStack, Int>>,
        add: Boolean,
    ) {
        for ((key, value) in list) {
            val stack = value.first
            val oldAmount = value.second

            if (!listToCheckAgainst.containsKey(key)) {
                val item = PickupEntry(stack.dynamicName(), oldAmount.toLong(), stack.getInternalNameOrNull())
                updateItem(key, item, add)
            } else if (oldAmount > listToCheckAgainst[key]!!.second) {
                val amount = (oldAmount - listToCheckAgainst[key]?.second!!)
                val item = PickupEntry(stack.dynamicName(), amount.toLong(), stack.getInternalNameOrNull())
                updateItem(key, item, add)
            }
        }
    }

    private fun ItemStack.dynamicName(): String {
        val compact = when (getItemCategoryOrNull()) {
            ItemCategory.ENCHANTED_BOOK -> true
            ItemCategory.PET -> true
            else -> false
        }
        return if (compact) getInternalName().repoItemName else displayName
    }

    private fun ItemStack.hash(): Int {
        var displayName = this.displayName.removeColor()
        shopPattern.matchMatcher(displayName) {
            displayName = group("itemName")
        }
        return Objects.hash(
            this.getInternalNameOrNull(),
            displayName,
            this.getItemRarityOrNull(),
        )
    }

    private data class ItemPickupLogSnapshot(
        val removedItems: MutableMap<Int, PickupEntry>,
        val addedItems: MutableMap<Int, PickupEntry>,
    ) {
        val display: MutableList<Renderable> = mutableListOf()
    }

    private fun updateDisplay() {
        if (!isEnabled()) return

        val currentSnapshot = ItemPickupLogSnapshot(itemsRemovedFromInventory.toMutableMap(), itemsAddedToInventory.toMutableMap())
        with(currentSnapshot) {
            if (config.compactLines) handleCompactLines()
            else handleNormalLines()

            addRemainingRemovedItems(display, removedItems)

            this@ItemPickupLog.display = when {
                display.isEmpty() -> null
                else -> {
                    computeTotalCoinValue(display)
                    Renderable.fixedSizeColumn(
                        Renderable.vertical(display, verticalAlign = config.alignment),
                        30,
                    )
                }
            }
        }
    }

    private fun computeTotalCoinValue(display: MutableList<Renderable>) {
        if (!coinConfig.enabled || !(itemsAddedToInventory.isNotEmpty() || itemsRemovedFromInventory.isNotEmpty())) return
        val valueAdded = itemsAddedToInventory.values.sumOf { it.coinValue() }
        val valueRemoved = itemsRemovedFromInventory.values.sumOf { it.coinValue() }
        val total = valueAdded - valueRemoved
        if (total >= coinConfig.threshold || coinConfig.threshold == 0f) {
            display.add(0, Renderable.text("§eValue: ${total.formatCoin()} coins"))
        }
    }

    private fun PickupEntry.coinValue() = if (name == "§6Coins") {
        // Handle purse coins as a special case
        amount.toDouble()
    } else {
        val pricePer = neuInternalName?.getPriceOrNull(coinConfig.priceSource) ?: 0.0
        pricePer * amount
    }

    private fun ItemPickupLogSnapshot.handleCompactLines() {
        val iterator = addedItems.iterator()
        while (iterator.hasNext()) {
            val (hash, rawEntry) = iterator.next()

            if (removedItems.containsKey(hash)) {
                val currentTotalValue = rawEntry.amount - (removedItems[hash]?.amount ?: 0)
                val entry = PickupEntry(rawEntry.name, currentTotalValue, rawEntry.neuInternalName)

                if (currentTotalValue > 0) display.add(entry.getPlusRenderable(hash))
                else if (currentTotalValue < 0) display.add(entry.getMinusRenderable(hash))
                else {
                    itemsAddedToInventory.remove(hash)
                    itemsRemovedFromInventory.remove(hash)
                }
                removedItems.remove(hash)
                iterator.remove()
            } else display.add(rawEntry.getPlusRenderable(hash))
        }
    }

    private fun ItemPickupLogSnapshot.handleNormalLines() {
        for ((hash, entry) in addedItems) {
            display.add(entry.getPlusRenderable(hash))
            removedItems[hash]?.let { removedEntry ->
                display.add(removedEntry.getMinusRenderable(hash))
                removedItems.remove(hash)
            }
        }
    }

    private fun addRemainingRemovedItems(
        display: MutableList<Renderable>,
        removedItems: MutableMap<Int, PickupEntry>,
    ) = removedItems.onEach { (hash, entry) ->
        display.add(entry.getMinusRenderable(hash))
    }

    private fun worldChangeCooldown(): Boolean = SkyBlockUtils.lastWorldSwitch.passedSince() > 2.seconds

    private fun isEnabled() = SkyBlockUtils.inSkyBlock && config.enabled

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(97, "inventory.itemPickupLogConfig", "inventory.itemPickupLog")
        event.move(97, "inventory.itemPickupLog.pos", "inventory.itemPickupLog.position")
    }
}
