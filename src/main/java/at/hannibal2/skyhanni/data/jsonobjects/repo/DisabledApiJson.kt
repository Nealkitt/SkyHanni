package at.hannibal2.skyhanni.data.jsonobjects.repo

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class DisabledApiJson(
    @Expose @SerializedName("disabled_bazaar") val disabledBazaar: Boolean,
    @Expose @SerializedName("disabled_moulberry_lowest_bin") val disabledMoulberryLowestBin: Boolean,
    @Expose @SerializedName("disabled_hypixel_items") val disableHypixelItems: Boolean,
    @Expose @SerializedName("disabled_elite_ah") val disabledEliteAh: Boolean,
    @Expose @SerializedName("disabled_elite_bz") val disabledEliteBz: Boolean,
    @Expose @SerializedName("disabled_elite_items") val disabledEliteItems: Boolean,
)
