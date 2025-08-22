package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.enoughupdates.EnoughUpdatesManager
import at.hannibal2.skyhanni.api.enoughupdates.EnoughUpdatesRepoManager
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.data.NotificationManager
import at.hannibal2.skyhanni.data.SkyHanniNotification
import at.hannibal2.skyhanni.data.jsonobjects.repo.ItemsJson
import at.hannibal2.skyhanni.data.model.SkyblockStat
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.NeuRepositoryReloadEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.features.misc.ReplaceRomanNumerals
import at.hannibal2.skyhanni.features.misc.items.EstimatedItemValueCalculator.getAttributeName
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.CachedItemData.Companion.cachedData
import at.hannibal2.skyhanni.utils.ItemPriceUtils.formatCoin
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPrice
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NeuItems.getItemStackOrNull
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.PrimitiveIngredient.Companion.toPrimitiveItemStacks
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getAttributes
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getExtraAttributes
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getHypixelEnchantments
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getPetInfo
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.isRecombobulated
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.StringUtils.removeResets
import at.hannibal2.skyhanni.utils.chat.TextHelper
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.chat.TextHelper.onClick
import at.hannibal2.skyhanni.utils.chat.TextHelper.onHover
import at.hannibal2.skyhanni.utils.chat.TextHelper.send
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.removeIfKey
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.sortedDesc
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.compat.NbtCompat
import at.hannibal2.skyhanni.utils.compat.getItemOnCursor
import at.hannibal2.skyhanni.utils.compat.setCustomItemName
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.system.PlatformUtils
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import net.minecraft.init.Items
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList
import net.minecraft.nbt.NBTTagString
import net.minecraft.util.IChatComponent
import java.util.LinkedList
import java.util.regex.Matcher
import kotlin.time.Duration.Companion.INFINITE
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

//#if MC > 1.21
//$$ import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLessResets
//$$ import net.minecraft.component.DataComponentTypes
//$$ import net.minecraft.component.type.LoreComponent
//$$ import net.minecraft.component.type.NbtComponent
//$$ import net.minecraft.component.ComponentMap
//$$ import com.mojang.authlib.GameProfile
//$$ import java.util.UUID
//$$ import com.mojang.authlib.properties.Property
//$$ import net.minecraft.component.type.ItemEnchantmentsComponent
//$$ import net.minecraft.component.type.ProfileComponent
//$$ import net.minecraft.registry.Registries
//#endif

@SkyHanniModule
@Suppress("LargeClass")
object ItemUtils {

    private val itemNameCache = mutableMapOf<NeuInternalName, String>() // internal name -> item name
    private val compactItemNameCache = mutableMapOf<NeuInternalName, String>() // internal name -> compact item name

    // This map might not contain all stats the item has, compare with itemBaseStatsRaw if unclear
    private var itemBaseStats = mapOf<NeuInternalName, Map<SkyblockStat, Int>>()
    private var itemBaseStatsRaw = mapOf<NeuInternalName, Map<String, Int>>()

    private val missingRepoItems = mutableSetOf<String>()
    private var lastRepoWarning = SimpleTimeMark.farPast()

    fun updateBaseStats(rawStats: Map<NeuInternalName, Map<String, Int>>) {
        verifyStats(rawStats)
        itemBaseStatsRaw = rawStats
    }

    private fun verifyStats(allRawStats: Map<NeuInternalName, Map<String, Int>>) {
        val allItems = mutableMapOf<NeuInternalName, Map<SkyblockStat, Int>>()
        val unknownStats = mutableMapOf<String, String>()
        for ((internalName, rawStats) in allRawStats) {
            val stats = mutableMapOf<SkyblockStat, Int>()
            for ((rawStat, value) in rawStats) {
                val stat = SkyblockStat.getValueOrNull(rawStat.uppercase())
                if (stat == null) {
                    unknownStats[rawStat.uppercase()] = "on ${internalName.asString()}"
                } else {
                    stats[stat] = value
                }
            }
            allItems[internalName] = stats
        }

        // TODO maybe create a new enum for item stats?
        unknownStats.remove("WEAPON_ABILITY_DAMAGE") // stat exists only on items, not as player stat
        unknownStats.removeIfKey { it.startsWith("RIFT_") } // rift stats are not in SkyblockStat enum

        if (unknownStats.isNotEmpty()) {
            val name = StringUtils.pluralize(unknownStats.size, "stat", withNumber = true)
            ErrorManager.logErrorStateWithData(
                "Found unknown skyblock stats on items, please report this in discord",
                "found $name via Hypixel Item API that are not in enum SkyblockStat",
                // TODO logErrorStateWithData should accept a map of extra data directly
                extraData = unknownStats.map { it.key to it.value }.toTypedArray(),
                betaOnly = true,
            )
        }
        itemBaseStats = allItems
    }

    // Might not contain all actual item stats, compare with getRawBaseStats()
    fun NeuInternalName.getBaseStats(): Map<SkyblockStat, Int> = itemBaseStats[this].orEmpty()

    fun NeuInternalName.getRawBaseStats(): Map<String, Int> = itemBaseStatsRaw[this].orEmpty()

    @HandleEvent(ConfigLoadEvent::class)
    fun onConfigLoad() {
        ConditionalUtils.onToggle(SkyHanniMod.feature.misc.replaceRomanNumerals) {
            itemNameCache.clear()
            compactItemNameCache.clear()
        }
    }

    private val SKYBLOCK_MENU = "SKYBLOCK_MENU".toInternalName()

    fun ItemStack.cleanName() = displayName.removeColor()

    fun isSack(stack: ItemStack) = stack.getInternalName().endsWith("_SACK") && stack.cleanName().endsWith(" Sack")

    //#if MC < 1.21
    fun ItemStack.getLore(): List<String> = this.tagCompound.getLore()
    //#else
    //$$ fun ItemStack.getLore(): List<String> {
    //$$     val data = cachedData
    //$$     if (data.lastLoreFetchTime.passedSince() < 0.1.seconds) {
    //$$         return data.lastLore
    //$$     }
    //$$     val lore = this.get(DataComponentTypes.LORE)?.lines?.map { it.formattedTextCompatLessResets() } ?: emptyList()
    //$$     data.lastLore = lore
    //$$     data.lastLoreFetchTime = SimpleTimeMark.now()
    //$$     return lore
    //$$ }
    //#endif

    fun ItemStack.getSingleLineLore(): String = getLore().filter { it.isNotEmpty() }.joinToString(" ")

    //#if MC < 1.21
    fun NBTTagCompound?.getLore(): List<String> {
        this ?: return emptyList()
        return this.getCompoundTag("display").getStringList("Lore")
    }
    //#else
    //$$ fun ComponentMap?.getLore(): List<String> {
    //$$     this ?: return emptyList()
    //$$     return this.get(DataComponentTypes.LORE)?.lines?.map { it.formattedTextCompatLessResets() } ?: emptyList()
    //$$ }
    //#endif

    fun NBTTagCompound?.getReadableNBTDump(initSeparator: String = "  ", includeLore: Boolean = false): List<String> {
        this ?: return emptyList()
        val tagList = mutableListOf<String>()
        for (s in this.keySet) {
            if (s == "Lore" && !includeLore) continue
            val tag = this.getTag(s)

            if (tag !is NBTTagCompound) {
                tagList.add("$initSeparator$s: $tag")
            } else {
                val element = this.getCompoundTag(s)
                tagList.add("$initSeparator$s:")
                tagList.addAll(element.getReadableNBTDump("$initSeparator  ", includeLore))
            }
        }
        return tagList
    }

    //#if MC < 1.21
    fun getDisplayName(compound: NBTTagCompound?): String? {
        compound ?: return null
        val name = compound.getCompoundTag("display").getString("Name")
        if (name == null || name.isEmpty()) return null
        return name
    }
    //#else
    //$$ fun getDisplayName(compound: ComponentMap?): String? {
    //$$     compound ?: return null
    //$$     val name = compound.get(DataComponentTypes.CUSTOM_NAME)?.formattedTextCompatLeadingWhiteLessResets()
    //$$     if (name.isNullOrEmpty()) return null
    //$$     return name
    //$$ }
    //#endif

    fun ItemStack.setLore(lore: List<String>): ItemStack {
        //#if MC < 1.21
        val tagCompound = this.tagCompound ?: NBTTagCompound()
        val display = tagCompound.getCompoundTag("display")
        val tagList = NBTTagList()
        for (line in lore) {
            tagList.appendTag(NBTTagString(line))
        }
        display.setTag("Lore", tagList)
        tagCompound.setTag("display", display)
        this.tagCompound = tagCompound
        //#else
        //$$ this.set(DataComponentTypes.LORE, LoreComponent(lore.map { Text.of(it) }))
        //#endif
        return this
    }

    var ItemStack.extraAttributes: NBTTagCompound
        get() = this.getExtraAttributes() ?: NBTTagCompound()
        set(value) {
            //#if MC < 1.21
            val tag = this.tagCompound ?: NBTTagCompound().also { tagCompound = it }
            tag.setTag("ExtraAttributes", value)
            //#else
            //$$ set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(value))
            //#endif
        }

    //#if MC < 1.21
    val NBTTagCompound.extraAttributes: NBTTagCompound get() = this.getCompoundTag("ExtraAttributes")
    //#else
    //$$ val ComponentMap.extraAttributes: NbtCompound get() = this.get(DataComponentTypes.CUSTOM_DATA)?.copyNbt() ?: NbtCompound()
    //#endif

    fun ItemStack.overrideId(id: String): ItemStack {
        extraAttributes = extraAttributes.apply { setString("id", id) }
        return this
    }

    // TODO change else janni is sad
    fun ItemStack.isCoopSoulBound(): Boolean = getLore().any {
        it == "§8§l* §8Co-op Soulbound §8§l*" || it == "§8§l* §8Soulbound §8§l*"
    }

    // TODO change else janni is sad
    fun ItemStack.isSoulBound(): Boolean = getLore().any { it == "§8§l* §8Soulbound §8§l*" }

    fun isRecombobulated(stack: ItemStack) = stack.isRecombobulated()

    fun getItemsInInventory(withCursorItem: Boolean = false): List<ItemStack> {
        val list: LinkedList<ItemStack> = LinkedList()
        val player = MinecraftCompat.localPlayer

        for (slot in player.openContainer.inventorySlots) {
            if (slot.hasStack) {
                list.add(slot.stack)
            }
        }

        val cursorStack = player.getItemOnCursor()
        if (withCursorItem && cursorStack != null) {
            list.add(cursorStack)
        }
        return list
    }

    fun ItemStack.getInternalName() = getInternalNameOrNull() ?: NeuInternalName.NONE

    fun ItemStack.getInternalNameOrNull(): NeuInternalName? {
        val data = cachedData
        if (data.lastInternalNameFetchTime.passedSince() < 1.seconds) {
            return data.lastInternalName
        }
        val internalName = grabInternalNameOrNull()
        data.lastInternalName = internalName
        data.lastInternalNameFetchTime = SimpleTimeMark.now()
        return internalName
    }

    private fun ItemStack.grabInternalNameOrNull(): NeuInternalName? {
        if (displayName == "§fWisp's Ice-Flavored Water I Splash Potion") {
            return NeuInternalName.WISP_POTION
        }
        // This is to prevent an error message whenever coins are traded.
        if (getLore().getOrNull(0) == "§7Lump-sum amount") {
            return NeuInternalName.SKYBLOCK_COIN
        }
        val internalName = NeuItems.getInternalName(this)?.replace("ULTIMATE_ULTIMATE_", "ULTIMATE_")
        return internalName?.let { ItemNameResolver.fixEnchantmentName(it) }
    }

    fun ItemStack.isVanilla() = NeuItems.isVanillaItem(this)

    // Checks for the enchantment glint as part of the minecraft enchantments
    fun ItemStack.isEnchanted(): Boolean =
        //#if MC < 1.21
        isItemEnchanted
    //#else
    //$$ hasEnchantments() || this.get(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE) == true
    //#endif

    // Checks for hypixel enchantments in the attributes
    fun ItemStack.hasHypixelEnchantments() = getHypixelEnchantments()?.isNotEmpty() ?: false

    fun ItemStack.removeEnchants(): ItemStack = apply {
        //#if MC < 1.21
        val tempTag = tagCompound ?: NBTTagCompound()
        tempTag.removeTag("ench")
        tempTag.removeTag("StoredEnchantments")
        tagCompound = tempTag
        //#else
        //$$ this.set(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT)
        //#endif
    }

    fun ItemStack.getSkullTexture(): String? {
        if (item != Items.skull) return null
        //#if MC < 1.21
        val compound = tagCompound ?: return null
        if (!compound.hasKey("SkullOwner")) return null
        return compound.getCompoundTag("SkullOwner").getSkullTexture()
        //#else
        //$$ return this.get(DataComponentTypes.PROFILE)?.properties?.get("textures")?.firstOrNull()?.value
        //#endif

    }

    //#if MC < 1.21
    fun NBTTagCompound.getSkullTexture(): String? =
        getCompoundTag("Properties").getCompoundList("textures").firstOrNull()?.getString("Value")
    //#endif

    fun ItemStack.getSkullOwner(): String? {
        if (item != Items.skull) return null
        //#if MC < 1.21
        val nbt = tagCompound ?: return null

        if (!nbt.hasKey("SkullOwner")) return null
        return nbt.getCompoundTag("SkullOwner").getString("Id")
        //#else
        //$$ return this.get(DataComponentTypes.PROFILE)?.id?.get().toString()
        //#endif
    }

    @Suppress("SpreadOperator")
    fun createSkull(displayName: String, uuid: String, value: String, loreColl: Collection<String>) =
        createSkull(displayName, uuid, value, *loreColl.toTypedArray())

    // Taken from NEU
    fun createSkull(displayName: String, uuid: String, value: String, vararg lore: String): ItemStack {
        //#if MC < 1.21
        val stack = ItemStack(Items.skull, 1, 3)
        val tag = NBTTagCompound()
        val skullOwner = NBTTagCompound()
        val properties = NBTTagCompound()
        val textures = NBTTagList()
        val textures0 = NBTTagCompound()

        skullOwner.setString("Id", uuid)
        skullOwner.setString("Name", uuid)
        textures0.setString("Value", value)

        textures.appendTag(textures0)

        addNameAndLore(tag, displayName, *lore)

        properties.setTag("textures", textures)
        skullOwner.setTag("Properties", properties)
        tag.setTag("SkullOwner", skullOwner)
        stack.tagCompound = tag
        return stack
        //#else
        //$$ val stack = ItemStack(Items.PLAYER_HEAD)
        //$$ val profile = GameProfile(UUID.fromString(uuid), "Throwpo")
        //$$ profile.properties.put("textures", Property("textures", value))
        //$$ stack.set(DataComponentTypes.PROFILE, ProfileComponent(profile))
        //$$ stack.setCustomItemName(displayName)
        //$$ stack.setLore(lore.toList())
        //$$ return stack
        //#endif
    }

    fun createItemStack(item: Item, displayName: String, vararg lore: String): ItemStack {
        return createItemStack(item, displayName, lore.toList())
    }

    // Overload to avoid spread operators
    fun createItemStack(item: Item, displayName: String, loreArray: Array<String>, amount: Int = 1, damage: Int = 0): ItemStack =
        createItemStack(item, displayName, loreArray.toList(), amount, damage)

    // Taken from NEU
    fun createItemStack(item: Item, displayName: String, lore: List<String>, amount: Int = 1, damage: Int = 0): ItemStack {
        //#if MC < 1.16
        val stack = ItemStack(item, amount, damage)
        val tag = NBTTagCompound()
        addNameAndLore(tag, displayName, *lore.toTypedArray())
        tag.setInteger("HideFlags", 254)
        stack.tagCompound = tag
        return stack
        //#else
        //$$ // todo we are ignoring damage for now, idk what to do for this
        //$$ val stack = ItemStack(item, amount)
        //$$ stack.setCustomItemName(displayName)
        //$$ stack.setLore(lore)
        //$$ var tooltipDisplay = net.minecraft.component.type.TooltipDisplayComponent.DEFAULT.with(DataComponentTypes.DAMAGE, true)
        //$$ tooltipDisplay = tooltipDisplay.with(DataComponentTypes.ATTRIBUTE_MODIFIERS, true)
        //$$ tooltipDisplay = tooltipDisplay.with(DataComponentTypes.UNBREAKABLE, true)
        //$$ if (displayName.isBlank() && lore.isEmpty()) {
        //$$     tooltipDisplay = net.minecraft.component.type.TooltipDisplayComponent(true, tooltipDisplay.hiddenComponents)
        //$$ }
        //$$ stack.set(DataComponentTypes.TOOLTIP_DISPLAY, tooltipDisplay)
        //$$ return stack
        //#endif
    }

    // Taken from NEU
    //#if MC < 1.21
    // Doesnt make sense in modern
    // just use itemstack.setCustomItemName and itemstack.setLore
    private fun addNameAndLore(tag: NBTTagCompound, displayName: String, vararg lore: String) {
        val display = NBTTagCompound()
        display.setString("Name", displayName)
        if (lore.isNotEmpty()) {
            val tagLore = NBTTagList()
            for (line in lore) {
                tagLore.appendTag(NBTTagString(line))
            }
            display.setTag("Lore", tagLore)
        }
        tag.setTag("display", display)
    }
    //#endif

    fun ItemStack.getItemRarityOrCommon() = getItemRarityOrNull() ?: LorenzRarity.COMMON

    private val itemCategoryRepoCheckPattern by RepoPattern.pattern(
        "itemcategory.repocheck",
        ItemCategory.entries.joinToString(separator = "|") { it.name },
    )
    private val rarityCategoryRepoCheckPattern by RepoPattern.pattern(
        "rarity.repocheck",
        LorenzRarity.entries.joinToString(separator = "|") { it.name },
    )

    private fun ItemStack.readItemCategoryAndRarity(): Pair<LorenzRarity?, ItemCategory?> {
        if (this.getPetInfo() != null) return getPetRarity(this) to ItemCategory.PET

        val cleanName = this.cleanName()
        for (line in this.getLore().reversed()) {
            val (category, rarity) = UtilsPatterns.rarityLoreLinePattern.matchMatcher(line) {
                group("itemCategory").replace(" ", "_") to group("rarity").replace(" ", "_")
            } ?: continue

            val itemCategory = getItemCategory(category, displayName, cleanName)
            val itemRarity = LorenzRarity.getByName(rarity)

            if (itemCategory == null) {
                ErrorManager.logErrorStateWithData(
                    "Could not read category for item ${this.displayName}",
                    "Failed to read category from item rarity via item lore",
                    "internal name" to getInternalName(),
                    "item name" to displayName,
                    "inventory name" to InventoryUtils.openInventoryName(),
                    "pattern result" to category,
                    "lore" to getLore(),
                    betaOnly = true,
                    condition = { !itemCategoryRepoCheckPattern.matches(category) },
                )
            }
            if (itemRarity == null) {
                ErrorManager.logErrorStateWithData(
                    "Could not read rarity for item $displayName",
                    "Failed to read rarity from item rarity via item lore",
                    "internal name" to getInternalName(),
                    "item name" to displayName,
                    "inventory name" to InventoryUtils.openInventoryName(),
                    "pattern result" to rarity,
                    "lore" to getLore(),
                    betaOnly = true,
                    condition = { !rarityCategoryRepoCheckPattern.matches(rarity) },
                )
            }

            return itemRarity to itemCategory
        }
        return null to null
    }

    private fun getItemCategory(itemCategory: String, name: String, cleanName: String = name.removeColor()) =
        if (itemCategory.isEmpty()) when {
            UtilsPatterns.abiPhonePattern.matches(name) -> ItemCategory.ABIPHONE
            UtilsPatterns.baitPattern.matches(cleanName) -> ItemCategory.FISHING_BAIT
            UtilsPatterns.enchantedBookPattern.matches(name) -> ItemCategory.ENCHANTED_BOOK
            UtilsPatterns.potionPattern.matches(name) -> ItemCategory.POTION
            UtilsPatterns.sackPattern.matches(name) -> ItemCategory.SACK
            else -> ItemCategory.NONE
        } else {
            EnumUtils.enumValueOfOrNull<ItemCategory>(itemCategory)
        }

    private fun ItemStack.updateCategoryAndRarity() {
        val data = cachedData
        if (data.itemRarityLastCheck.passedSince() < 10.seconds) return
        data.itemRarityLastCheck = SimpleTimeMark.now()
        val internalName = getInternalName()
        if (internalName == NeuInternalName.NONE) {
            data.itemRarity = null
            data.itemCategory = null
            return
        }
        val pair = this.readItemCategoryAndRarity()
        data.itemRarity = pair.first
        data.itemCategory = pair.second
    }

    fun ItemStack.getItemCategoryOrNull(): ItemCategory? {
        this.updateCategoryAndRarity()
        return cachedData.itemCategory
    }

    fun ItemStack.getItemRarityOrNull(): LorenzRarity? {
        this.updateCategoryAndRarity()
        return cachedData.itemRarity
    }

    // Taken from NEU
    fun ItemStack.editItemInfo(displayName: String, disableNeuTooltips: Boolean, lore: List<String>): ItemStack {
        this.setCustomItemName(displayName)
        this.setLore(lore)
        //#if MC < 1.21
        val tag = this.tagCompound ?: NBTTagCompound()
        tag.setInteger("HideFlags", 254)
        if (disableNeuTooltips) {
            tag.setBoolean("disableNeuTooltip", true)
        }

        this.tagCompound = tag
        //#endif
        return this
    }

    fun isSkyBlockMenuItem(stack: ItemStack?): Boolean = stack?.getInternalName() == SKYBLOCK_MENU

    private val itemAmountCache = mutableMapOf<String, Pair<String, Int>>()

    private val bookPattern = "(?<name>.* [IVX]+) Book".toPattern()

    fun readItemAmount(originalInput: String): Pair<String, Int>? {
        // This workaround fixes 'Turbo Cacti I Book'
        val input = (bookPattern.matchMatcher(originalInput) { group("name") } ?: originalInput).removeResets()

        if (itemAmountCache.containsKey(input)) {
            return itemAmountCache[input]!!
        }

        UtilsPatterns.readAmountBeforePattern.matchMatcher(input) {
            val itemName = group("name")
            if (!itemName.contains("§8x")) {
                return makePair(input, itemName.trim(), this)
            }
        }

        var string = input.trim()
        val color = string.substring(0, 2)
        string = string.substring(2)
        val matcher = UtilsPatterns.readAmountAfterPattern.matcher(string)
        if (!matcher.matches()) {
            return null
        }

        val itemName = color + matcher.group("name").trim()
        return makePair(input, itemName, matcher)
    }

    /**
     * REGEX-TEST: §fEnchanted Book (Lapidary I)
     * REGEX-TEST: §fEnchanted Book (Ice Cold I§r§f)
     */
    private val enchantedBookPattern by RepoPattern.pattern(
        "item.enchantedbook",
        "§fEnchanted Book \\((?<item>.+)\\)"
    )
    fun readBookType(input: String): String? {
        return enchantedBookPattern.matchMatcher(input) {
            group("item").removeColor()
        }
    }

    private fun makePair(input: String, itemName: String, matcher: Matcher): Pair<String, Int> {
        val matcherAmount = matcher.group("amount")
        val amount = matcherAmount?.formatInt() ?: 1
        val pair = Pair(itemName, amount)
        itemAmountCache[input] = pair
        return pair
    }

    private fun getPetRarity(pet: ItemStack): LorenzRarity? {
        val rarityId = pet.getInternalName().asString().split(";").last().toInt()
        val rarity = LorenzRarity.getById(rarityId)
        val name = pet.displayName
        if (rarity == null) {
            ErrorManager.logErrorStateWithData(
                "Could not read rarity for pet $name",
                "Failed to read rarity from pet item via internal name",
                "internal name" to pet.getInternalName(),
                "item name" to name,
                "rarity id" to rarityId,
                "inventory name" to InventoryUtils.openInventoryName(),
            )
        }
        return rarity
    }

    fun NeuInternalName.isRune(): Boolean = contains("_RUNE;")

    /** Use when showing the item name to the user (in guis, chat message, etc.), not for comparing. */
    val ItemStack.repoItemName: String
        get() {
            getAttributeFromShard()?.let {
                return it.getAttributeName()
            }
            return getInternalNameOrNull()?.repoItemName ?: "<null>"
        }

    /** Use when showing the item name to the user (in guis, chat message, etc.), not for comparing. */
    val ItemStack.repoItemNameCompact: String
        get() {
            getAttributeFromShard()?.let {
                return it.getAttributeName()
            }
            return getInternalNameOrNull()?.repoItemNameCompact ?: "<null>"
        }

    fun ItemStack.getAttributeFromShard(): Pair<String, Int>? {
        if (!(getInternalName().asString().startsWith("ATTRIBUTE_SHARD"))) return null
        val attributes = getAttributes() ?: return null
        return attributes.firstOrNull()
    }

    /** Use when showing the item name to the user (in guis, chat message, etc.), not for comparing. */
    val ItemStack.itemNameWithoutColor: String get() = repoItemName.removeColor()

    /** Use when showing the item name to the user (in guis, chat message, etc.), not for comparing. */
    val NeuInternalName.repoItemName: String
        get() = itemNameCache.getOrPut(this) { grabItemName() }

    val NeuInternalName.repoItemNameCompact get() = compactItemNameCache.getOrPut(this) { getRepoCompactName() }

    private fun NeuInternalName.getRepoCompactName(): String {
        var name = repoItemName
        for ((from, to) in compactNameReplace) {
            name = name.replace(from, to)
        }
        return name
    }

    private var compactNameReplace = mapOf<String, String>()
    var bazaarOverrides = mapOf<String, String>()
        private set

    private data class BazaarOverride(
        @Expose @SerializedName("stock") val bazaarInternalName: String,
        @Expose @SerializedName("id") val neuInternalName: String,
    )

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        compactItemNameCache.clear()
        // if compactNames is null, we want the npe to happen in onRepoReload(), not in getRepoCompactName()
        @Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
        compactNameReplace = event.getConstant<ItemsJson>("Items").compactNames!!
    }

    @HandleEvent
    fun onNeuRepoReload(event: NeuRepositoryReloadEvent) {
        val bazaarOverridesTypeToken = object : TypeToken<List<BazaarOverride>>() {}.type
        val overrides = event.getConstant<List<BazaarOverride>>("bazaarstocks", bazaarOverridesTypeToken)
        bazaarOverrides = overrides.associate { it.bazaarInternalName to it.neuInternalName }

        // clear the item name cache so any potential missing items are reloaded
        itemNameCache.clear()
        missingRepoItems.clear()
    }

    /** Use when showing the item name to the user (in guis, chat message, etc.), not for comparing. */
    val NeuInternalName.itemNameWithoutColor: String get() = repoItemName.removeColor()

    val NeuInternalName.readableInternalName: String
        get() = asString().replace("_", " ").lowercase()

    @Suppress("ReturnCount")
    private fun NeuInternalName.grabItemName(): String {
        if (this.isPet) {
            return PetUtils.getCleanPetName(this, colored = true) + " Pet"
        }
        if (this == NeuInternalName.WISP_POTION) {
            return "§fWisp's Ice-Flavored Water"
        }
        if (this == NeuInternalName.SKYBLOCK_COIN) {
            return "§6Coins"
        }
        if (this == NeuInternalName.NONE) {
            error("NEUInternalName.NONE has no name!")
        }
        if (NeuItems.ignoreItemsFilter.match(this.asString())) {
            return "§cBugged Item"
        }

        // We do not use NeuItems.allItemsCache here since we need itemStack below
        val itemStack = getItemStackOrNull() ?: run {
            val name = toString()
            addMissingRepoItem(name, "Could not find item name for $name")
            return "§c$name"
        }
        val name = itemStack.displayName

        // show enchanted book name
        if (itemStack.getItemCategoryOrNull() == ItemCategory.ENCHANTED_BOOK) {
            return ReplaceRomanNumerals.replaceLine(itemStack.getLore()[0])
        }
        if (name.endsWith("Enchanted Book Bundle")) {
            return name.replace("Enchanted Book", ReplaceRomanNumerals.replaceLine(itemStack.getLore()[0]).removeColor())
        }

        // obfuscated trophy fish
        if (name.contains("§kObfuscated")) {
            return name.replace("§kObfuscated", "Obfuscated")
        }

        // remove roman runic tier
        if (isRune()) {
            return ReplaceRomanNumerals.replaceLine(name)
        }

        return name
    }

    fun ItemStack.loreCosts(): MutableList<NeuInternalName> {
        var found = false
        val list = mutableListOf<NeuInternalName>()
        for (lines in getLore()) {
            if (lines == "§7Cost") {
                found = true
                continue
            }

            if (!found) continue
            if (lines.isEmpty()) return list

            NeuInternalName.fromItemNameOrNull(lines)?.let {
                list.add(it)
            }
        }
        return list
    }

    fun neededItems(recipe: PrimitiveRecipe): Map<NeuInternalName, Int> {
        val neededItems = mutableMapOf<NeuInternalName, Int>()
        for ((material, amount) in recipe.ingredients.toPrimitiveItemStacks()) {
            neededItems.addOrPut(material, amount)
        }
        return neededItems
    }

    fun PrimitiveRecipe.getRecipePrice(
        priceSource: ItemPriceSource = ItemPriceSource.BAZAAR_INSTANT_BUY,
        pastRecipes: List<PrimitiveRecipe> = emptyList(),
    ): Double = neededItems(this).map {
        it.key.getPrice(priceSource, pastRecipes) * it.value
    }.sum()

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shtestitem") {
            description = "test item internal name resolving"
            category = CommandCategory.DEVELOPER_TEST
            arg("item", BrigadierArguments.greedyString()) { item ->
                callback {
                    testItemCommand(getArg(item))
                }
            }
            simpleCallback {
                ChatUtils.userError("Usage: /shtestitem <item name or internal name>")
            }
        }
    }

    private fun testItemCommand(args: String) {
        TextHelper.text("§eProcessing..").send(testItemMessageId)

        // running .getPrice() on thousands of items may take ~500ms
        SkyHanniMod.launchIOCoroutine {
            buildTestItemMessage(args).send(testItemMessageId)
        }
    }

    private val testItemMessageId = ChatUtils.getUniqueMessageId()

    private fun buildTestItemMessage(input: String) = buildList {
        add("".asComponent())
        add("§bSkyHanni Test Item".asComponent())
        add("§eInput: '§f$input§e'".asComponent())

        NeuInternalName.fromItemNameOrNull(input)?.let { internalName ->
            formatTestItem(internalName, internalName.getPrice())
            return@buildList
        }

        input.toInternalName().getItemStackOrNull()?.let { item ->
            val internalName = item.getInternalName()
            formatTestItem(internalName, internalName.getPrice())
            return@buildList
        }

        val matches = mutableSetOf<NeuInternalName>()
        for ((name, internalName) in NeuItems.allItemsCache) {
            if (name.contains(input, ignoreCase = true)) {
                matches.add(internalName)
            } else if (internalName.asString().contains(input.replace(" ", "_"), ignoreCase = true)) {
                matches.add(internalName)
            }
        }
        // TODO add all enchantments to NeuItems.allItemsCache
        // somehow, enchantments arent part of NeuItems.allItemsCache atm
        // itemNameCache contains bazaar enchantments
        // the non bz enchantments are only in the cache after found in game
        for ((internalName, name) in itemNameCache) {
            if (name.contains(input, ignoreCase = true)) {
                matches.add(internalName)
            } else if (internalName.asString().contains(input.replace(" ", "_"), ignoreCase = true)) {
                matches.add(internalName)
            }
        }

        if (matches.isEmpty()) {
            add("§cNothing found!".asComponent())
        } else {
            add("§eNo exact match! Show partial matches:".asComponent())
            val max = 10
            if (matches.size > max) {
                add("§7(Showing only the first $max results of ${matches.size.addSeparators()} total)".asComponent())
            }
            for ((internalName, price) in matches.associateWith { it.getPrice() }.sortedDesc().entries.take(max)) {
                formatTestItem(internalName, price)
            }
        }
    }

    private fun MutableList<IChatComponent>.formatTestItem(internalName: NeuInternalName, price: Double) {
        val priceColor = if (price > 0) "§6" else "§7"
        val name = internalName.repoItemName
        val priceFormat = "$priceColor${price.shortFormat()}"
        val componentText = " §8- §r$name $priceFormat".asComponent()
        componentText.onClick {
            ClipboardUtils.copyToClipboard(internalName.asString())
        }
        componentText.onHover(
            listOf(
                name,
                "",
                "§7Price: $priceFormat",
                "§7Internal name: §8${internalName.asString()}",
                "",
                "§eClick to copy internal name to clipboard!",
            ),
        )
        add(componentText)
    }

    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("Missing Repo Items")

        if (missingRepoItems.isNotEmpty()) {
            event.addData {
                add("Detected ${missingRepoItems.size} missing items:")
                for (itemName in missingRepoItems) {
                    add(" - $itemName")
                }
            }
        } else {
            event.addIrrelevant("No Repo Item fails detected.")
        }
    }

    fun addMissingRepoItem(name: String, message: String) {
        if (!missingRepoItems.add(name)) return
        // If we're currently loading the repo (async in a coroutine), we don't want to show the warning
        if (EnoughUpdatesManager.inLoadingState()) {
            return ChatUtils.debug(
                "Ignoring missing repo item warning, repo is currently loading or fetching",
                replaceSameMessage = true,
            )
        }

        ChatUtils.debug(message)
        if (!SkyBlockUtils.debug && !PlatformUtils.isDevEnvironment) return

        if (lastRepoWarning.passedSince() < 3.minutes) return
        lastRepoWarning = SimpleTimeMark.now()
        showRepoWarning(name)
    }

    // These two are matching right now, but we keep them separate for future-proofing
    val resetCommand
        get() = if (PlatformUtils.isNeuLoaded()) "neuresetrepo"
        else EnoughUpdatesRepoManager.updateCommand

    private fun showRepoWarning(item: String) {
        val text = listOf(
            "§c§lMissing repo data for item: $item",
            "§cData used for some SkyHanni features is not up to date, this should normally not be the case.",
            "§cYou can try §l/$resetCommand§r§c and restart your game to see if that fixes the issue.",
            "§cIf the problem persists please join the SkyHanni Discord and message in §l#support§r§c to get support.",
        )
        NotificationManager.queueNotification(SkyHanniNotification(text, INFINITE, true))
    }

    fun NBTTagCompound.getStringList(key: String): List<String> {
        if (!NbtCompat.containsList(this, key)) return emptyList()

        return NbtCompat.getStringTagList(this, key).let { loreList ->
            List(loreList.tagCount()) { loreList.getStringTagAt(it) }
        }
    }

    fun NBTTagCompound.getCompoundList(key: String): List<NBTTagCompound> =
        NbtCompat.getCompoundTagList(this, key).let { loreList ->
            List(loreList.tagCount()) { loreList.getCompoundTagAt(it) }
        }

    fun NBTTagCompound.containsCompound(key: String): Boolean {
        return NbtCompat.containsCompound(this, key)
    }

    fun NeuInternalName.getNumberedName(amount: Number): String {
        val prefix = if (amount == 1.0) "" else "§8${amount.addSeparators()}x "
        return "$prefix§r$repoItemName"
    }

    // Taken from NEU
    // TODO add cache
    fun getCoinItemStack(coinAmount: Number): ItemStack {
        val amount = coinAmount.toDouble()
        var uuid = "2070f6cb-f5db-367a-acd0-64d39a7e5d1b"
        var texture =
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTM4MDcxNzIxY2M1YjRjZDQwNmNlNDMxYTEzZjg2MDgzYTg5NzNlMTA2NGQyZjg4OTc4Njk5MzBlZTZlNTIzNyJ9fX0="

        if (amount >= 100000) {
            uuid = "94fa2455-2881-31fe-bb4e-e3e24d58dbe3"
            texture =
                "eyJ0aW1lc3RhbXAiOjE2MzU5NTczOTM4MDMsInByb2ZpbGVJZCI6ImJiN2NjYTcxMDQzNDQ0MTI4ZDMwODllMTNiZGZhYjU5IiwicHJvZmlsZU5hbWUiOiJsYXVyZW5jaW8zMDMiLCJzaWduYXR1cmVSZXF1aXJlZCI6dHJ1ZSwidGV4dHVyZXMiOnsiU0tJTiI6eyJ1cmwiOiJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2M5Yjc3OTk5ZmVkM2EyNzU4YmZlYWYwNzkzZTUyMjgzODE3YmVhNjQwNDRiZjQzZWYyOTQzM2Y5NTRiYjUyZjYiLCJtZXRhZGF0YSI6eyJtb2RlbCI6InNsaW0ifX19fQo="
        }

        if (amount >= 10000000) {
            uuid = "0af8df1f-098c-3b72-ac6b-65d65fd0b668"
            texture =
                "ewogICJ0aW1lc3RhbXAiIDogMTYzNTk1NzQ4ODQxNywKICAicHJvZmlsZUlkIiA6ICJmNThkZWJkNTlmNTA0MjIyOGY2MDIyMjExZDRjMTQwYyIsCiAgInByb2ZpbGVOYW1lIiA6ICJ1bnZlbnRpdmV0YWxlbnQiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvN2I5NTFmZWQ2YTdiMmNiYzIwMzY5MTZkZWM3YTQ2YzRhNTY0ODE1NjRkMTRmOTQ1YjZlYmMwMzM4Mjc2NmQzYiIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9"
        }

        val skull = createSkull(
            amount.formatCoin() + " Coins",
            uuid,
            texture,
        )

        skull.extraAttributes = skull.extraAttributes.apply { setString("id", "SKYBLOCK_COIN") }

        return skull
    }

    fun ItemStack.isSkull(): Boolean {
        //#if MC < 1.21
        return item === Items.skull
        //#else
        //$$ val hasItemModel = this.getItemModel() != null
        //$$ return item == Items.PLAYER_HEAD && !hasItemModel
        //#endif
    }

    //#if MC > 1.21
    //$$ fun ItemStack.getItemModel(): Item? {
    //$$     val identifier = this.get(DataComponentTypes.ITEM_MODEL)
    //$$     val itemModel = Registries.ITEM.get(identifier)
    //$$     return if (itemModel == Items.AIR || itemModel == this.item) null else itemModel
    //$$ }
    //#endif
}
