package at.hannibal2.skyhanni.config.features.garden.pests

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class PestTimerConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Show the time since the last pest spawned in your garden.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigOption(
        name = "Only When Holding",
        desc = "Only show the time display when holding the specified items.\n" +
            "Leave empty to always show.",
    )
    @ConfigEditorDraggableList
    val onlyWhenHolding: MutableList<HeldItem> = mutableListOf(
        HeldItem.FARMING_TOOL,
    )

    enum class HeldItem(val displayName: String) {
        FARMING_TOOL("Farming Tool"),
        VACUUM("Vacuum"),
        LASSO("Lasso"),
        ;

        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(name = "Pest Timer Text", desc = "Drag text to change the appearance of the overlay.")
    @ConfigEditorDraggableList
    val pestDisplay: MutableList<PestTimerTextEntry> = mutableListOf(
        PestTimerTextEntry.PEST_TIMER,
        PestTimerTextEntry.PEST_COOLDOWN,
    )

    enum class PestTimerTextEntry(private val displayName: String) {
        PEST_TIMER("§eLast pest spawned: §b8s ago"),
        PEST_COOLDOWN("§ePest Cooldown: §b1m 8s"),
        AVERAGE_PEST_SPAWN("§eAverage time to spawn: §b4m 32s"),
        ;

        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(name = "Pest Cooldown Warning", desc = "Warn when pests are eligible to spawn.")
    @ConfigEditorBoolean
    @FeatureToggle
    var cooldownOverWarning: Boolean = false

    @Expose
    @ConfigOption(name = "Repeat Warning", desc = "Repeat the warning sound and title until wardrobe is opened or pest cooldown is over.")
    @ConfigEditorBoolean
    var repeatWarning: Boolean = false

    @Expose
    @ConfigOption(name = "Warn Before Cooldown End", desc = "Warn this many seconds before the cooldown is over.")
    @ConfigEditorSlider(minValue = 1f, maxValue = 30f, minStep = 1f)
    var cooldownWarningTime: Int = 5

    @Expose
    @ConfigOption(
        name = "Custom Pest Cooldown",
        desc = "Set pest cooldown to a custom time after a pest spawns. Useful for equipment swapping."
    )
    @ConfigEditorBoolean
    val customCooldown: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Custom Pest Cooldown Time", desc = "Set pest cooldown to this amount after a pest spawns.")
    @ConfigEditorSlider(minValue = 75f, maxValue = 135f, minStep = 5f)
    val customCooldownTime: Property<Int> = Property.of(135)

    @Expose
    @ConfigOption(
        name = "AFK Timeout",
        desc = "Don't include spawn time in average spawn time display when the player goes AFK for at least this many seconds.",
    )
    @ConfigEditorSlider(minValue = 5f, maxValue = 300f, minStep = 1f)
    var averagePestSpawnTimeout: Int = 30

    @Expose
    @ConfigOption(
        name = "Pest Spawn Time Chat Message",
        desc = "When a pest spawns, send the time it took to spawn it in chat.",
    )
    @ConfigEditorBoolean
    var pestSpawnChatMessage: Boolean = false

    @Expose
    @ConfigOption(name = "Sound Settings", desc = "")
    @Accordion
    val sound: PestTimerSoundSettings = PestTimerSoundSettings()

    @Expose
    @ConfigLink(owner = PestTimerConfig::class, field = "enabled")
    val position: Position = Position(383, 93)
}
