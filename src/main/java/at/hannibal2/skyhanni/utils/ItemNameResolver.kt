package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.api.enoughupdates.ItemResolutionQuery
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.NeuRepositoryReloadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NeuItems.getItemStackOrNull
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimalIfNecessary
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.allLettersFirstUppercase
import at.hannibal2.skyhanni.utils.StringUtils.removeColor

@SkyHanniModule
object ItemNameResolver {
    private val itemNameCache = mutableMapOf<String, NeuInternalName>() // item name -> internal name
    private val HAY_BALE = "HAY_BALE".toInternalName()
    private val HAY_BLOCK = "HAY_BLOCK".toInternalName()

    @Suppress("ReturnCount", "CyclomaticComplexMethod")
    internal fun getInternalNameOrNull(itemName: String): NeuInternalName? {
        val lowercase = itemName.lowercase()
        itemNameCache[lowercase]?.let {
            return it
        }

        getInternalNameOrNullIgnoreCase(lowercase)?.let {
            return itemNameCache.getOrPut(lowercase) { it }
        }

        if (itemName == "§cmissing repo item") {
            return itemNameCache.getOrPut(lowercase) { NeuInternalName.MISSING_ITEM }
        }

        ItemResolutionQuery.resolveEnchantmentByName(itemName)?.let {
            return itemNameCache.getOrPut(lowercase) { fixEnchantmentName(it.asString()) }
        }

        resolveEnchantmentByCleanName(itemName)?.let {
            return itemNameCache.getOrPut(lowercase) { it }
        }

        resolvePetWithRarity(itemName)?.let {
            return itemNameCache.getOrPut(lowercase) { it }
        }

        if (itemName.endsWith("gemstone", ignoreCase = true)) {
            val split = lowercase.split(" ")
            if (split.size == 3) {
                val gemstoneQuery = "${
                    when (split[1]) {
                        "jade", "peridot", "citrine" -> '☘'
                        "amethyst" -> '❈'
                        "ruby" -> '❤'
                        "amber" -> '⸕'
                        "opal" -> '❂'
                        "topaz" -> '✧'
                        "onyx" -> '☠'
                        "sapphire" -> '✎'
                        "aquamarine" -> '☂'
                        "jasper" -> '❁'
                        else -> ' '
                    }
                } ${split.joinToString("_").allLettersFirstUppercase()}"
                ItemResolutionQuery.findInternalNameByDisplayName(gemstoneQuery, true)?.let {
                    return itemNameCache.getOrPut(lowercase) { it }
                }
            }
        }

        val internalName = when (itemName) {
            "SUPERBOOM TNT" -> "SUPERBOOM_TNT".toInternalName()
            else -> {
                ItemResolutionQuery.findInternalNameByDisplayName(itemName, true)?.let {
                    // This fixes a NEU bug with §9Hay Bale (cosmetic item)
                    if (it == HAY_BALE) HAY_BLOCK else it
                } ?: return null
            }
        }

        itemNameCache[lowercase] = internalName
        return internalName
    }

    private fun resolvePetWithRarity(itemName: String): NeuInternalName? {
        val splits = itemName.split(" ").takeIf { it.size > 1 } ?: return null
        val rarityLocation = splits.indexOfFirst { LorenzRarity.getByName(it) != null }
        val expectedRarityLocations = listOf(0, splits.size - 1)
        if (rarityLocation !in expectedRarityLocations) return null
        val petName = splits.filterIndexed { index, _ -> index != rarityLocation }.joinToString("_").uppercase()
        val petRarity = LorenzRarity.getByName(splits[rarityLocation]) ?: return null
        val internalName = "$petName;${petRarity.id}".toInternalName()
        return internalName.takeIf { it.getItemStackOrNull() != null }
    }

    private fun resolveEnchantmentByCleanName(itemName: String): NeuInternalName? {
        UtilsPatterns.cleanEnchantedNamePattern.matchMatcher(itemName) {
            val name = group("name").replace("'", "")
            val level = group("level").romanToDecimalIfNecessary()
            val rawInternalName = "$name;$level".uppercase()

            var internalName = fixEnchantmentName(rawInternalName)
            internalName.getItemStackOrNull()?.let {
                return internalName
            }

            internalName = fixEnchantmentName("ULTIMATE_$rawInternalName")
            internalName.getItemStackOrNull()?.let {
                return internalName
            }

            return null
        }
        return null
    }

    // Todo use repo for this
    // Workaround for duplex
    private val duplexPattern = "ULTIMATE_DUPLEX;(?<tier>.*)".toPattern()

    fun fixEnchantmentName(originalName: String): NeuInternalName {
        duplexPattern.matchMatcher(originalName) {
            val tier = group("tier")
            return "ULTIMATE_REITERATE;$tier".toInternalName()
        }
        return originalName.toInternalName()
    }

    private fun getInternalNameOrNullIgnoreCase(itemName: String): NeuInternalName? {
        // supports colored names, rarities
        NeuItems.allItemsCache[itemName]?.let {
            itemNameCache[itemName] = it
            return it
        }

        // if nothing got found with colors, try without colors
        val removeColor = itemName.removeColor()
        return NeuItems.allItemsCache.filter { it.key.removeColor() == removeColor }.values.firstOrNull()
    }

    @HandleEvent(NeuRepositoryReloadEvent::class)
    fun onNeuRepoReload() {
        itemNameCache.clear()
    }
}
