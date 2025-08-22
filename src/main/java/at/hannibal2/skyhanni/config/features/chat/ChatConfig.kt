package at.hannibal2.skyhanni.config.features.chat

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.Category
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.SearchTag
import org.lwjgl.input.Keyboard

class ChatConfig {
    @Expose
    @ConfigOption(name = "Peek Chat", desc = "Hold this key to keep the chat open.")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_Z)
    var peekChat: Int = Keyboard.KEY_NONE

    // TODO move into own sub category
    @Expose
    @ConfigOption(name = "Chat Filter Types", desc = "")
    @Accordion
    val filterType: FilterTypesConfig = FilterTypesConfig()

    // TODO move into own sub category
    @Expose
    @ConfigOption(name = "Player Messages", desc = "")
    @Accordion
    val playerMessage: PlayerMessagesConfig = PlayerMessagesConfig()

    @Expose
    @ConfigOption(name = "Sound Responses", desc = "")
    @Accordion
    val soundResponse: ChatSoundResponseConfig = ChatSoundResponseConfig()

    @Expose
    @ConfigOption(name = "Rare Drop Messages", desc = "")
    @Accordion
    val rareDropMessages: RareDropMessagesConfig = RareDropMessagesConfig()

    @Expose
    @ConfigOption(name = "Dungeon Filters", desc = "Hide specific message types in Dungeons.")
    @ConfigEditorDraggableList
    val dungeonFilteredMessageTypes: MutableList<DungeonMessageTypes> = mutableListOf()

    enum class DungeonMessageTypes(private val displayName: String) {
        PREPARE("§bPreparation"),
        START("§aClass Buffs §r/ §cMort Dialogue"),
        AMBIENCE("§bAmbience"),
        PICKUP("§ePickup"),
        REMINDER("§cReminder"),
        BUFF("§dBlessings"),
        NOT_POSSIBLE("§cNot possible"),
        DAMAGE("§cDamage"),
        ABILITY("§dAbilities"),
        PUZZLE("§dPuzzle §r/ §cQuiz"),
        END("§cEnd §a(End of run spam)"),
        ;

        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(
        name = "Copy Chat",
        desc = "Right click a chat message to copy it. Holding Shift will copy the message with " +
            "Shwords applied, and holding Ctrl will copy only one line.\n" +
            "§cNote: Will not work correctly with the Chatting mod.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var copyChat: Boolean = false

    @Expose
    @ConfigOption(name = "Dungeon Boss Messages", desc = "Hide messages from the Watcher and bosses in Dungeons.")
    @ConfigEditorBoolean
    @FeatureToggle
    var dungeonBossMessages: Boolean = false

    @Expose
    @ConfigOption(
        name = "Hide Far Deaths",
        desc = "Hide other players' death messages when they're not nearby (except during Dungeons/Kuudra fights)",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var hideFarDeathMessages: Boolean = false

    @Expose
    @ConfigOption(
        name = "Current Chat Display",
        desc = "Displays a GUI element that indicates what chat you are in (e.g. Party, Guild, Coop, All).",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var currentChatDisplay: Boolean = true

    @Expose
    @ConfigLink(owner = ChatConfig::class, field = "currentChatDisplay")
    val currentChatDisplayPos: Position = Position(3, -21)

    // TODO jawbus + thunder
    @Expose
    @ConfigOption(name = "Compact Potion Messages", desc = "")
    @Accordion
    val compactPotionMessages: CompactPotionConfig = CompactPotionConfig()

    @Expose
    @ConfigOption(
        name = "Compact Bestiary Messages",
        desc = "Compact the Bestiary level up message, only showing additional information when hovering.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var compactBestiaryMessage: Boolean = true

    @Expose
    @ConfigOption(
        name = "Compact Enchanting Rewards",
        desc = "Compact the rewards gained from Add-ons and Experiments in Experimentation Table,\n" +
            "only showing additional information when hovering.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var compactExperimentationTable: Boolean = false

    @Expose
    @ConfigOption(
        name = "Compact Jacob Claim",
        desc = "Compact the Jacob Claim message, only showing full information when hovering.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var compactJacobClaim: Boolean = false

    @Expose
    @ConfigOption(
        name = "Arachne Hider",
        desc = "Hide chat messages about the Arachne Fight while outside of §eArachne's Sanctuary§7.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var hideArachneMessages: Boolean = false

    @Expose
    @ConfigOption(
        name = "Sack Change Hider",
        desc = "Hide the sack change message while allowing mods to continue accessing sack data.\n" +
            "§eUse this instead of the toggle in Hypixel Settings.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var hideSacksChange: Boolean = false

    @Expose
    @ConfigOption(
        name = "Only Hide on Garden",
        desc = "Only hide the sack change message in the Garden.",
    )
    @ConfigEditorBoolean
    var onlyHideSacksChangeOnGarden: Boolean = false

    @Category(name = "Translator", desc = "Chat translator settings.")
    @Expose
    val translator: TranslatorConfig = TranslatorConfig()

    @Expose
    @ConfigOption(name = "SkyBlock XP in Chat", desc = "Send the SkyBlock XP messages into the chat.")
    @ConfigEditorBoolean
    @FeatureToggle
    var skyBlockXPInChat: Boolean = true

    @Expose
    @ConfigOption(
        name = "Anita's Accessories",
        desc = "Hide Anita's Accessories' fortune bonus messages outside the Garden.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var hideJacob: Boolean = true

    @Expose
    @ConfigOption(name = "Sky Mall Messages", desc = "Hide the Sky Mall messages outside of Mining Islands.")
    @ConfigEditorBoolean
    @SearchTag("Skymall")
    @FeatureToggle
    var hideSkyMall: Boolean = true

    @Expose
    @ConfigOption(name = "Hide Lottery Messages", desc = "Hide the Lottery messages outside of Foraging Islands.")
    @ConfigEditorBoolean
    @FeatureToggle
    var hideLottery: Boolean = true

    @Expose
    @ConfigOption(
        name = "Shorten Coin Amounts",
        desc = "Replace coin amounts in chat messages with their shortened version.\n" +
            "e.g. §65,100,000 Coins §7-> §65.1M Coins",
    )
    @ConfigEditorBoolean
    @SearchTag("format")
    @FeatureToggle
    var shortenCoinAmounts: Boolean = false

    @Expose
    @ConfigOption(
        name = "Hide Clickable Hints",
        desc = "Hides the 'Click to x' chat line from SkyHanni messages. " +
            "The message is still clickable and shows infos on hover.",
    )
    @ConfigEditorBoolean
    var hideClickableHint: Boolean = false
}
