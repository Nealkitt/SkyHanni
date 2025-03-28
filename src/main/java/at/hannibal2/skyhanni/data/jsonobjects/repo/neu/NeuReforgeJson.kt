package at.hannibal2.skyhanni.data.jsonobjects.repo.neu

import at.hannibal2.skyhanni.data.model.SkyblockStatList
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalNames
import at.hannibal2.skyhanni.utils.NeuItems
import at.hannibal2.skyhanni.utils.compat.getVanillaItem
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class NeuReforgeJson(
    @Expose val internalName: NeuInternalName?,
    @Expose val reforgeName: String,
    @Expose @SerializedName("itemTypes") val rawItemTypes: Any,
    @Expose val requiredRarities: List<LorenzRarity>,
    @Expose val reforgeCosts: Map<LorenzRarity, Long>?,
    @Expose val reforgeStats: Map<LorenzRarity, SkyblockStatList>?,
    @Expose @SerializedName("reforgeAbility") val rawReforgeAbility: Any?,
) {

    private lateinit var reforgeAbilityField: Map<LorenzRarity, String>
    private lateinit var itemTypeField: Pair<String, List<NeuInternalName>>

    val reforgeAbility
        get() = if (this::reforgeAbilityField.isInitialized) reforgeAbilityField
        else {
            reforgeAbilityField = when (this.rawReforgeAbility) {
                is String -> {
                    this.requiredRarities.associateWith { this.rawReforgeAbility }
                }

                is Map<*, *> -> (this.rawReforgeAbility as? Map<String, String>)?.mapKeys {
                    LorenzRarity.valueOf(
                        it.key.uppercase().replace(" ", "_"),
                    )
                }.orEmpty()

                else -> emptyMap()
            }
            reforgeAbilityField
        }

    val itemType: Pair<String, List<NeuInternalName>>
        get() = if (this::itemTypeField.isInitialized) itemTypeField
        else run {
            return when (val any = this.rawItemTypes) {
                is String -> {
                    any.replace("/", "_AND_").uppercase() to emptyList()
                }

                is Map<*, *> -> {
                    val type = "SPECIAL_ITEMS"
                    val map = any as? Map<String, List<String>> ?: return type to emptyList()
                    val internalNames = map["internalName"]?.toInternalNames().orEmpty()
                    val itemType = map["itemid"]?.map {
                        NeuItems.getInternalNamesForItemId(it.getVanillaItem() ?: return@map emptyList())
                    }?.flatten().orEmpty()
                    type to (internalNames + itemType)
                }

                else -> throw IllegalStateException()
            }
        }
}

