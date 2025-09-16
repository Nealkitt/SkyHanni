package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.enums.OutsideSBFeature
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.minecraft.packet.PacketReceivedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.TimeUtils
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object TpsCounter {

    private val config get() = SkyHanniMod.feature.gui

    private val ignorePacketDelay = 5.seconds
    private val minimumSecondsDisplayDelay = 10.seconds

    private var packetsFromLastSecond = 0
    private val tpsList = mutableListOf<Int>()
    private var hasRemovedFirstSecond = false

    private var hasReceivedPacket = false

    var tps: Double? = null
        private set

    private var display: Renderable? = null

    private val timeSinceWorldSwitch get() = SkyBlockUtils.lastWorldSwitch.passedSince()
    private var pendingTpsCommand = false

    @HandleEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (shouldIgnore()) {
            updateDisplay()
            return
        }

        if (packetsFromLastSecond != 0) {
            if (hasRemovedFirstSecond) tpsList.add(packetsFromLastSecond)
            hasRemovedFirstSecond = true
        }
        packetsFromLastSecond = 0

        if (tpsList.size > 10) tpsList.removeAt(0)

        updateDisplay()

        if (pendingTpsCommand) {
            pendingTpsCommand = false
            tpsCommand()
        }
    }

    private fun updateDisplay() {
        val timeUntil = minimumSecondsDisplayDelay - timeSinceWorldSwitch
        val text = if (timeUntil.isPositive()) {
            "§f(${timeUntil.inWholeSeconds}s)"
        } else {
            // when in limbo we don't receive any packets
            if (tpsList.isEmpty()) {
                "§70 (Limbo?)"
            } else {
                val newTps = tpsList.average().roundTo(1).coerceIn(0.0..20.0)
                tps = newTps
                val legacyColor = format(newTps)
                "$legacyColor${fixTps(newTps)}"
            }
        }
        display = Renderable.text("§eTPS: $text")
    }

    private fun fixTps(tps: Double): Double {
        return if (TimeUtils.isAprilFoolsDay) tps / 2 else tps
    }

    private fun tpsCommand() {
        val timeUntil = minimumSecondsDisplayDelay - timeSinceWorldSwitch
        if (timeUntil.isPositive()) {
            ChatUtils.chat("§eTPS: §fCalculating... §7(${timeUntil.inWholeSeconds}s)")
            DelayedRun.runDelayed(timeUntil) {
                pendingTpsCommand = true
            }
        } else {
            val tpsMessage = tps?.let { "${format(fixTps(it))}$it" } ?: "§70 (Limbo?)"
            ChatUtils.chat("§eTPS: $tpsMessage")
        }
    }

    @HandleEvent
    fun onTick() {
        if (hasReceivedPacket) {
            packetsFromLastSecond++
            hasReceivedPacket = false
        }
    }

    @HandleEvent
    fun onWorldChange() {
        tpsList.clear()
        tps = null
        packetsFromLastSecond = 0
        display = null
        hasRemovedFirstSecond = false
    }

    @HandleEvent(priority = HandleEvent.HIGHEST, receiveCancelled = true)
    fun onPacketReceive(event: PacketReceivedEvent) {
        hasReceivedPacket = true
    }

    @HandleEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return

        config.tpsDisplayPosition.renderRenderable(display, posLabel = "Tps Display")
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shtps") {
            description = "Informs in chat about the server ticks per second (TPS)."
            category = CommandCategory.USERS_ACTIVE
            simpleCallback { tpsCommand() }
        }
    }

    private fun shouldIgnore() = timeSinceWorldSwitch < ignorePacketDelay

    private fun isEnabled() = SkyBlockUtils.onHypixel && config.tpsDisplay &&
        (SkyBlockUtils.inSkyBlock || OutsideSBFeature.TPS_DISPLAY.isSelected())

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "misc.tpsDisplayEnabled", "gui.tpsDisplay")
        event.move(2, "misc.tpsDisplayPosition", "gui.tpsDisplayPosition")
    }

    private fun format(tps: Double): String {
        if (!tps.isFinite()) printError(tps)
        return getColor(tps)
    }

    private fun getColor(tps: Double) = when {
        tps > 19.8 -> "§2"
        tps > 19 -> "§a"
        tps > 17.5 -> "§6"
        tps > 12 -> "§c"

        else -> "§4"
    }

    private fun printError(tps: Double) {
        ErrorManager.logErrorStateWithData(
            "TPS calculation got an error",
            "tps is $tps",
            "tps" to tps,
            "packetsFromLastSecond" to packetsFromLastSecond,
            "hasRemovedFirstSecond" to hasRemovedFirstSecond,
            "hasReceivedPacket" to hasReceivedPacket,
            "tpsList" to tpsList,
            "timeSinceWorldSwitch" to timeSinceWorldSwitch,
        )
    }
}
