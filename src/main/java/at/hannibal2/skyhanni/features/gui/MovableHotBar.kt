package at.hannibal2.skyhanni.features.gui

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.GuiEditManager
import at.hannibal2.skyhanni.events.render.gui.GameOverlayRenderPostEvent
import at.hannibal2.skyhanni.events.render.gui.GameOverlayRenderPreEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.transform
import at.hannibal2.skyhanni.utils.compat.GuiScreenUtils
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import net.minecraft.client.renderer.GlStateManager
import net.minecraftforge.client.event.RenderGameOverlayEvent

@SkyHanniModule
object MovableHotBar {

    private val config get() = SkyHanniMod.feature.gui.hotbar

    private var post = false

    @HandleEvent(priority = HandleEvent.LOWEST)
    fun onRenderOverlayPre(event: GameOverlayRenderPreEvent) {
        if (event.type != RenderGameOverlayEvent.ElementType.HOTBAR || !isEnabled()) return
        post = true
        GlStateManager.pushMatrix()
        val x = GuiScreenUtils.scaledWindowWidth / 2 - 91
        val y = GuiScreenUtils.scaledWindowHeight - 22
        config.hotbar.transform()
        GlStateManager.translate(-x.toFloat(), -y.toFloat(), 0f) // Must be after transform to work with scaling
        GuiEditManager.add(config.hotbar, "Hotbar", 182 - 1, 22 - 1) // -1 since the editor for some reason add +1
    }

    @HandleEvent(priority = HandleEvent.HIGHEST)
    fun onRenderOverlayPost(event: GameOverlayRenderPostEvent) {
        if (event.type != RenderGameOverlayEvent.ElementType.HOTBAR || !post) return
        GlStateManager.popMatrix()
        post = false
    }

    fun isEnabled(): Boolean =
        (LorenzUtils.inSkyBlock || (MinecraftCompat.localPlayerExists && config.showOutsideSkyblock)) &&
            config.editable
}
