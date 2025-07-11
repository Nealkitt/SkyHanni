package at.hannibal2.skyhanni.config.features.garden

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.config.features.garden.composter.ComposterConfig
import at.hannibal2.skyhanni.config.features.garden.cropmilestones.CropMilestonesConfig
import at.hannibal2.skyhanni.config.features.garden.laneswitch.FarmingLaneConfig
import at.hannibal2.skyhanni.config.features.garden.optimalAngles.OptimalAnglesConfig
import at.hannibal2.skyhanni.config.features.garden.optimalspeed.OptimalSpeedConfig
import at.hannibal2.skyhanni.config.features.garden.pests.PestsConfig
import at.hannibal2.skyhanni.config.features.garden.visitor.VisitorConfig
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.Category
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.SearchTag

class GardenConfig {
    @Expose
    @ConfigOption(name = "SkyMart", desc = "")
    @Accordion
    val skyMart: SkyMartConfig = SkyMartConfig()

    @Expose
    @Category(name = "Visitor", desc = "Visitor Settings")
    val visitors: VisitorConfig = VisitorConfig()

    @Expose
    @ConfigOption(name = "Numbers", desc = "")
    @Accordion
    val number: NumbersConfig = NumbersConfig()

    @Expose
    @Category(name = "Crop Milestones", desc = "Crop Milestones Settings")
    val cropMilestones: CropMilestonesConfig = CropMilestonesConfig()

    // TODO Write ConditionalUtils.onToggle()-s for these values in their feature classes
    @Expose
    @ConfigOption(name = "Custom Keybinds", desc = "")
    @Accordion
    val keyBind: KeyBindConfig = KeyBindConfig()

    @Expose
    @Category(name = "Optimal Speed", desc = "Optimal Speed Settings")
    val optimalSpeeds: OptimalSpeedConfig = OptimalSpeedConfig()

    @Expose
    @Category(name = "Optimal Angles", desc = "Optimal Angles Settings")
    val optimalAngles: OptimalAnglesConfig = OptimalAnglesConfig()

    @Expose
    @ConfigOption(name = "Farming Lane", desc = "")
    @Accordion
    val farmingLane: FarmingLaneConfig = FarmingLaneConfig()

    @Expose
    @ConfigOption(name = "Garden Level", desc = "")
    @Accordion
    val gardenLevels: GardenLevelConfig = GardenLevelConfig()

    @Expose
    @ConfigOption(name = "Farming Weight", desc = "")
    @Accordion
    val eliteFarmingWeights: EliteFarmingWeightConfig = EliteFarmingWeightConfig()

    @Expose
    @ConfigOption(name = "Dicer RNG Drop Tracker", desc = "")
    @Accordion
    val dicerRngDropTracker: DicerRngDropTrackerConfig = DicerRngDropTrackerConfig()

    @Expose
    @ConfigOption(name = "Money per Hour", desc = "")
    @Accordion
    val moneyPerHours: MoneyPerHourConfig = MoneyPerHourConfig()

    @Expose
    @ConfigOption(name = "Next Jacob's Contest", desc = "")
    @Accordion
    val nextJacobContests: NextJacobContestConfig = NextJacobContestConfig()

    @Expose
    @ConfigOption(name = "Armor Drop Tracker", desc = "")
    @Accordion
    val armorDropTracker: ArmorDropTrackerConfig = ArmorDropTrackerConfig()

    @Expose
    @ConfigOption(name = "Anita Shop", desc = "")
    @Accordion
    val anitaShop: AnitaShopConfig = AnitaShopConfig()

    @Expose
    @Category(name = "Composter", desc = "Composter Settings")
    val composters: ComposterConfig = ComposterConfig()

    @Expose
    @Category(name = "Pests", desc = "Pests Settings")
    val pests: PestsConfig = PestsConfig()

    @Expose
    @ConfigOption(name = "Farming Fortune Display", desc = "")
    @Accordion
    val farmingFortunes: FarmingFortuneConfig = FarmingFortuneConfig()

    @Expose
    @ConfigOption(name = "Tooltip Tweaks", desc = "")
    @Accordion
    val tooltipTweak: TooltipTweaksConfig = TooltipTweaksConfig()

    @Expose
    @ConfigOption(name = "Yaw and Pitch", desc = "")
    @Accordion
    val yawPitchDisplay: YawPitchDisplayConfig = YawPitchDisplayConfig()

    @Expose
    @ConfigOption(name = "Sensitivity Reducer", desc = "")
    @Accordion
    val sensitivityReducer: SensitivityReducerConfig = SensitivityReducerConfig()

    @Expose
    @ConfigOption(name = "Crop Start Location", desc = "")
    @Accordion
    val cropStartLocation: CropStartLocationConfig = CropStartLocationConfig()

    @Expose
    @ConfigOption(name = "Plot Menu Highlighting", desc = "")
    @Accordion
    val plotMenuHighlighting: PlotMenuHighlightingConfig = PlotMenuHighlightingConfig()

    @Expose
    @ConfigOption(name = "Garden Plot Icon", desc = "")
    @Accordion
    val plotIcon: PlotIconConfig = PlotIconConfig()

    @Expose
    @ConfigOption(name = "Garden Commands", desc = "")
    @Accordion
    val gardenCommands: GardenCommandsConfig = GardenCommandsConfig()

    @Expose
    @ConfigOption(name = "Atmospheric Filter Display", desc = "")
    @Accordion
    val atmosphericFilterDisplay: AtmosphericFilterDisplayConfig = AtmosphericFilterDisplayConfig()

    @Expose
    @ConfigOption(name = "Personal Bests", desc = "")
    @Accordion
    val personalBests: PersonalBestsConfig = PersonalBestsConfig()

    @Expose
    @ConfigOption(
        name = "Plot Price",
        desc = "Show the price of the plot in coins when inside the Configure Plots inventory.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var plotPrice: Boolean = true

    @Expose
    @ConfigOption(name = "Fungi Cutter Warning", desc = "Warn when breaking mushroom with the wrong Fungi Cutter mode.")
    @ConfigEditorBoolean
    @FeatureToggle
    var fungiCutterWarn: Boolean = true

    @Expose
    @ConfigOption(
        name = "Burrowing Spores",
        desc = "Show a notification when a Burrowing Spores spawns while farming mushrooms.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var burrowingSporesNotification: Boolean = true

    @Expose
    @ConfigOption(
        name = "FF for Contest",
        desc = "Show the minimum needed Farming Fortune for reaching each medal in Jacob's Farming Contest inventory.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var farmingFortuneForContest: Boolean = true

    @Expose
    @ConfigLink(owner = GardenConfig::class, field = "farmingFortuneForContest")
    val farmingFortuneForContestPos: Position = Position(180, 156)

    @Expose
    @ConfigOption(
        name = "Contest Time Needed",
        desc = "Show the time and missing FF for every crop inside Jacob's Farming Contest inventory.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var jacobContestTimes: Boolean = true

    @Expose
    @ConfigOption(
        name = "Custom BPS",
        desc = "Use custom Blocks per Second value in some GUIs instead of the real one.",
    )
    @ConfigEditorBoolean
    var jacobContestCustomBps: Boolean = true

    // TODO Write ConditionalUtils.onToggle()-s for these values in their feature classes
    @Expose
    @ConfigOption(name = "Custom BPS Value", desc = "Set a custom Blocks per Second value.")
    @ConfigEditorSlider(minValue = 15f, maxValue = 20f, minStep = 0.1f)
    var jacobContestCustomBpsValue: Double = 19.9

    @Expose
    @ConfigLink(owner = GardenConfig::class, field = "jacobContestTimes")
    val jacobContestTimesPosition: Position = Position(-359, 149)

    @Expose
    @ConfigOption(
        name = "Contest Summary",
        desc = "Show the average Blocks Per Second and blocks clicked at the end of a Jacob Farming Contest in chat.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var jacobContestSummary: Boolean = true

    // Does not have a config element!
    @Expose
    val cropSpeedMeterPos: Position = Position(278, -236)

    @Expose
    @ConfigOption(
        name = "Enable Plot Borders",
        desc = "Enable the use of F3 + G hotkey to show Garden plot borders. " +
            "Similar to how later Minecraft version render chunk borders.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var plotBorders: Boolean = true

    @Expose
    @ConfigOption(
        name = "Copy Milestone Data",
        desc = "Copy wrong crop milestone data in clipboard when opening the crop milestone menu. " +
            "Please share this data in SkyHanni discord.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var copyMilestoneData: Boolean = true

    @Expose
    @ConfigOption(name = "Log Book Stats", desc = "Show total visited/accepted/denied visitors stats.")
    @ConfigEditorBoolean
    @FeatureToggle
    var showLogBookStats: Boolean = true

    @Expose
    @ConfigLink(owner = GardenConfig::class, field = "showLogBookStats")
    val logBookStatsPos: Position = Position(427, 92)

    @Expose
    @ConfigOption(name = "Carrolyn Fetch Helper", desc = "Helps to fetch items to Carrolyn for permanent buffs.")
    @SearchTag("Expired Pumpkin, Exportable Carrots, Supreme Chocolate Bar, Fine Flour")
    @ConfigEditorBoolean
    @FeatureToggle
    var helpCarrolyn: Boolean = true
}
