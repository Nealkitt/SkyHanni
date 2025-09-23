package at.hannibal2.skyhanni.config.features.slayer.blaze

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class BlazeHellionConfig {


    // TODO rename to highlight phase or similar
    @Expose
    @ConfigOption(
        name = "Colored Mobs",
        desc = "Color the Blaze Slayer boss and the demons in the right hellion shield color and show the name."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var coloredMobs: Boolean = false

    @Expose
    @ConfigOption(name = "Blaze Daggers", desc = "Faster and permanent display for the Blaze Slayer daggers.")
    @ConfigEditorBoolean
    @FeatureToggle
    var daggers: Boolean = false

    @Expose
    @ConfigOption(name = "Right Dagger", desc = "Mark the right dagger to use for Blaze Slayer in the dagger overlay.")
    @ConfigEditorBoolean
    @FeatureToggle
    var markRightHellionShield: Boolean = false

    @Expose
    @ConfigOption(name = "First Dagger", desc = "Select the first, left sided dagger for the display.")
    @ConfigEditorDropdown
    var firstDagger: FirstDaggerEntry = FirstDaggerEntry.SPIRIT_OR_CRYSTAL

    enum class FirstDaggerEntry(private val displayName: String) {
        SPIRIT_OR_CRYSTAL("Spirit/Crystal"),
        ASHEN_OR_AURIC("Ashen/Auric"),
        ;

        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(name = "Hide Chat", desc = "Remove the wrong Blaze Slayer dagger messages from chat.")
    @ConfigEditorBoolean
    @FeatureToggle
    var hideDaggerWarning: Boolean = false

    @Expose
    @ConfigLink(owner = BlazeHellionConfig::class, field = "daggers")
    val positionTop: Position = Position(-475, 173, 4.4f, true)

    @Expose
    @ConfigLink(owner = BlazeHellionConfig::class, field = "daggers")
    val positionBottom: Position = Position(-475, 230, 3.2f, true)
}
