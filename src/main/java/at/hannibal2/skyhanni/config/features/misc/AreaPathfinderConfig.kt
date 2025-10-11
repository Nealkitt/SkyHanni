package at.hannibal2.skyhanni.config.features.misc

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class AreaPathfinderConfig {
    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "While in your inventory, show all areas of the island. Click on an area to display the path to this area."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    val enabled: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Show Always", desc = "Show the list always, also while outside of an inventory.")
    @ConfigEditorBoolean
    var showAlways: Boolean = false

    @Expose
    @ConfigOption(name = "Current Area", desc = "Show the name of the current area at the top of the list")
    @ConfigEditorBoolean
    val includeCurrentArea: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Path Color", desc = "Change the color of the path.")
    @ConfigEditorColour
    val color: Property<ChromaColour> = Property.of(ChromaColour.fromStaticRGB(85, 255, 85, 245))

    @Expose
    @ConfigLink(owner = AreaPathfinderConfig::class, field = "enabled")
    val position: Position = Position(-350, 100)
}
