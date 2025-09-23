package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.GuiEditManager
import at.hannibal2.skyhanni.features.garden.fortuneguide.FFGuideGui
import at.hannibal2.skyhanni.features.misc.visualwords.VisualWordGui
import at.hannibal2.skyhanni.utils.ConfigUtils
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.NeuItems
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiEditSign
import org.lwjgl.input.Keyboard

object ChatPeek {

    @JvmStatic
    fun peek(): Boolean {
        val key = SkyHanniMod.feature.chat.peekChat

        if (!MinecraftCompat.localPlayerExists) return false
        if (key <= Keyboard.KEY_NONE) return false
        if (Minecraft.getMinecraft().currentScreen is GuiEditSign) return false
        if (ConfigUtils.configScreenCurrentlyOpen) return false

        if (NeuItems.neuHasFocus()) return false
        if (GuiEditManager.isInGui() || FFGuideGui.isInGui() || VisualWordGui.isInGui()) return false

        return key.isKeyHeld()
    }
}
