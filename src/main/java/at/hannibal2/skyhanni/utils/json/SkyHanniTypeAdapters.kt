package at.hannibal2.skyhanni.utils.json

import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.jsonobjects.other.NbtBoolean
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.NEURaritySpecificPetNums
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.NeuPetNums
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.recipe.NeuAbstractRecipe
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.recipe.NeuRecipeComponent
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.recipe.NeuRecipeType
import at.hannibal2.skyhanni.data.model.SkyblockStat
import at.hannibal2.skyhanni.features.fishing.trophy.TrophyRarity
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.pests.PestType
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NeuItems
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.asTimeMark
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.system.ModVersion
import at.hannibal2.skyhanni.utils.tracker.SkyHanniTracker
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import net.minecraft.item.ItemStack
import java.time.LocalDate
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

object SkyHanniTypeAdapters {
    val NEU_ITEMSTACK: TypeAdapter<ItemStack> = SimpleStringTypeAdapter(NeuItems::saveNBTData, NeuItems::loadNBTData)

    val UUID: TypeAdapter<UUID> = SimpleStringTypeAdapter(
        { this.toString() },
        { StringUtils.parseUUID(this) },
    )

    val NBT_BOOLEAN: TypeAdapter<NbtBoolean> = SimpleStringTypeAdapter(
        { this.asString() },
        { NbtBoolean.fromString(this) },
    )

    val INTERNAL_NAME: TypeAdapter<NeuInternalName> = SimpleStringTypeAdapter(
        { this.asString() },
        { this.toInternalName() },
    )

    val VEC_STRING: TypeAdapter<LorenzVec> = SimpleStringTypeAdapter(
        LorenzVec::asStoredString,
        LorenzVec::decodeFromString,
    )

    val TROPHY_RARITY: TypeAdapter<TrophyRarity> = SimpleStringTypeAdapter(
        { name },
        { TrophyRarity.getByName(this) ?: error("Could not parse TrophyRarity from '$this'") },
    )

    val TIME_MARK: TypeAdapter<SimpleTimeMark> = object : TypeAdapter<SimpleTimeMark>() {
        override fun write(out: JsonWriter, value: SimpleTimeMark) {
            out.value(value.toMillis())
        }

        override fun read(reader: JsonReader): SimpleTimeMark {
            return reader.nextLong().asTimeMark()
        }
    }

    val DURATION: TypeAdapter<Duration> = object : TypeAdapter<Duration>() {
        override fun write(out: JsonWriter, value: Duration) {
            out.value(value.inWholeMilliseconds)
        }

        override fun read(reader: JsonReader): Duration {
            return reader.nextLong().milliseconds
        }
    }

    val CROP_TYPE: TypeAdapter<CropType> = SimpleStringTypeAdapter(
        { name },
        { CropType.getByName(this) },
    )

    val PEST_TYPE: TypeAdapter<PestType> = SimpleStringTypeAdapter(
        { name },
        { PestType.getByName(this) },
    )

    val SKYBLOCK_STAT: TypeAdapter<SkyblockStat> = SimpleStringTypeAdapter(
        { name.lowercase() },
        { SkyblockStat.getValue(this.uppercase()) },
    )

    val NEU_RECIPE_COMPONENT: TypeAdapter<NeuRecipeComponent> = SimpleStringTypeAdapter(
        { this.toJsonString() },
        { NeuRecipeComponent.fromJsonString(this) }
    )

    val NEU_RECIPE_TYPE: TypeAdapter<NeuRecipeType> = SimpleStringTypeAdapter(
        { neuRepoId.orEmpty() },
        { NeuRecipeType.fromNeuId(this) },
    )

    val NEU_ABSTRACT_RECIPE = object : TypeAdapter<NeuAbstractRecipe>() {
        override fun write(writer: JsonWriter, value: NeuAbstractRecipe) {
            writer.value(value.toString())
        }

        override fun read(reader: JsonReader): NeuAbstractRecipe {
            val obj = JsonParser().parse(reader).asJsonObject
            val typeId = obj.get("type").asString
            val recipeType = NeuRecipeType.fromNeuIdOrNull(typeId)
                ?: throw IllegalArgumentException("Unknown recipe type: $typeId")
            return ConfigManager.gson.fromJson(obj, recipeType.castClazz)
        }
    }

    val NEU_RARITY_SPECIFIC_PET_NUMS = object : TypeAdapter<NEURaritySpecificPetNums>() {
        override fun write(writer: JsonWriter, value: NEURaritySpecificPetNums) {
            writer.value(value.toString())
        }

        override fun read(reader: JsonReader?): NEURaritySpecificPetNums {
            val obj = JsonParser().parse(reader).asJsonObject
            val neuPetNumsAdapter = ConfigManager.gson.getAdapter(NeuPetNums::class.java)
            val min = neuPetNumsAdapter.fromJsonTree(obj.getAsJsonObject("1"))
            val max = neuPetNumsAdapter.fromJsonTree(obj.getAsJsonObject("100"))
            val curve = obj.get("stats_levelling_curve")?.asString
            return NEURaritySpecificPetNums(min, max, curve)
        }

    }

    val MOD_VERSION: TypeAdapter<ModVersion> = SimpleStringTypeAdapter(ModVersion::asString, ModVersion::fromString)

    val TRACKER_DISPLAY_MODE = SimpleStringTypeAdapter.forEnum<SkyHanniTracker.DefaultDisplayMode>()
    val ISLAND_TYPE = SimpleStringTypeAdapter.forEnum<IslandType>(IslandType.UNKNOWN)
    val RARITY = SimpleStringTypeAdapter.forEnum<LorenzRarity>()

    val LOCALE_DATE = object : TypeAdapter<LocalDate>() {
        override fun write(out: JsonWriter, value: LocalDate) {
            out.value(value.toString())
        }

        override fun read(reader: JsonReader): LocalDate {
            return LocalDate.parse(reader.nextString())
        }
    }

    inline fun <reified T> GsonBuilder.registerTypeAdapter(
        crossinline write: (JsonWriter, T) -> Unit,
        crossinline read: (JsonReader) -> T,
    ): GsonBuilder {
        this.registerTypeAdapter(
            T::class.java,
            object : TypeAdapter<T>() {
                override fun write(out: JsonWriter, value: T) = write(out, value)
                override fun read(reader: JsonReader) = read(reader)
            }.nullSafe(),
        )
        return this
    }
}
