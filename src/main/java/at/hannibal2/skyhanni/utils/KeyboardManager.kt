package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.model.TextInput
import at.hannibal2.skyhanni.events.minecraft.KeyPressEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.compat.MouseCompat
import io.github.notenoughupdates.moulconfig.gui.GuiScreenElementWrapper
import io.github.notenoughupdates.moulconfig.internal.KeybindHelper
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiChat
import net.minecraft.client.settings.KeyBinding
import org.apache.commons.lang3.SystemUtils
import org.lwjgl.input.Keyboard
import kotlin.time.Duration.Companion.milliseconds
//#if MC > 1.21
//$$ import net.minecraft.client.util.InputUtil
//#endif

@SkyHanniModule
object KeyboardManager {

    const val LEFT_MOUSE = -100
    const val RIGHT_MOUSE = -99
    const val MIDDLE_MOUSE = -98

    private var lastClickedMouseButton = -1

    // A mac-only key, represents Windows key on windows (but different key code)
    private fun isCommandKeyDown() = Keyboard.KEY_LMETA.isKeyHeld() || Keyboard.KEY_RMETA.isKeyHeld()

    // Windows: Alt key Mac: Option key
    private fun isMenuKeyDown() = Keyboard.KEY_LMENU.isKeyHeld() || Keyboard.KEY_RMENU.isKeyHeld()

    private fun isControlKeyDown() = Keyboard.KEY_LCONTROL.isKeyHeld() || Keyboard.KEY_RCONTROL.isKeyHeld()

    fun isDeleteWordDown() =
        Keyboard.KEY_BACK.isKeyHeld() && if (SystemUtils.IS_OS_MAC) isMenuKeyDown() else isControlKeyDown()

    fun isDeleteLineDown() =
        Keyboard.KEY_BACK.isKeyHeld() && if (SystemUtils.IS_OS_MAC) isCommandKeyDown() else isControlKeyDown() && isShiftKeyDown()

    fun isShiftKeyDown() = Keyboard.KEY_LSHIFT.isKeyHeld() || Keyboard.KEY_RSHIFT.isKeyHeld()

    fun isPastingKeysDown() = isModifierKeyDown() && Keyboard.KEY_V.isKeyHeld()

    fun isCopyingKeysDown() = isModifierKeyDown() && Keyboard.KEY_C.isKeyHeld()

    fun isModifierKeyDown() = if (SystemUtils.IS_OS_MAC) isCommandKeyDown() else isControlKeyDown()

    /**
     * TODO make use of this function unnecessary: Try to avoid using `isModifierKeyDown` as the only option,
     * allow the user to set a different option instead and just set the default key to isModifierKeyDown
     */
    fun getModifierKeyName(): String = if (SystemUtils.IS_OS_MAC) "Command" else "Control"

    @HandleEvent(priority = HandleEvent.LOWEST)
    fun onTick(event: SkyHanniTickEvent) {
        //#if MC < 1.16
        val currentScreen = Minecraft.getMinecraft().currentScreen
        val isConfigScreen = currentScreen is GuiScreenElementWrapper
        if (isConfigScreen) return
        if (currentScreen is GuiChat) return


        if (MouseCompat.getEventButtonState() && MouseCompat.getEventButton() != -1) {
            val key = MouseCompat.getEventButton() - 100
            postEvent(key)
            lastClickedMouseButton = key
            return
        }

        if (Keyboard.getEventKeyState() && Keyboard.getEventKey() != 0) {
            postEvent(Keyboard.getEventKey())
            lastClickedMouseButton = -1
            return
        }

        if (MouseCompat.getEventButton() == -1 && lastClickedMouseButton != -1) {
            if (lastClickedMouseButton.isKeyHeld()) {
                postEvent(lastClickedMouseButton)
                return
            }
            lastClickedMouseButton = -1
        }

        // This is needed because of other keyboards that don't have a key code for the key, but is read as a character
        if (Keyboard.getEventKey() == 0) {
            postEvent(Keyboard.getEventCharacter().code + 256)
        }
        //#else
        //$$ // todo use fabric event or whatnot
        //#endif
    }

    private fun postEvent(keyCode: Int) {
        // This cooldown is here to make sure the Text input features in graph editor
        // and in renderable calls have time to react first,
        // and lock this key press event properly
        DelayedRun.runDelayed(50.milliseconds) {
            if (TextInput.isActive()) return@runDelayed
            KeyPressEvent(keyCode).post()
        }
    }

    fun KeyBinding.isActive(): Boolean {
        //#if MC < 1.16
        if (!Keyboard.isCreated()) return false
        try {
            if (keyCode.isKeyHeld()) return true
        } catch (e: IndexOutOfBoundsException) {
            ErrorManager.logErrorWithData(
                e,
                "Error while checking if a key is pressed.",
                "keyCode" to keyCode,
            )
            return false
        }
        //#endif
        return this.isKeyDown || this.isPressed
    }

    fun Int.isKeyHeld(): Boolean = when {
        //#if MC < 1.16
        this == 0 -> false
        this < 0 -> MouseCompat.isButtonDown(this + 100)
        this >= Keyboard.KEYBOARD_SIZE -> {
            val pressedKey = if (Keyboard.getEventKey() == 0) Keyboard.getEventCharacter().code + 256 else Keyboard.getEventKey()
            Keyboard.getEventKeyState() && this == pressedKey
        }

        else -> Keyboard.isKeyDown(this)
        //#else
        //$$ this == -1 || this == 0 -> false
        //$$ else -> InputUtil.isKeyPressed(MinecraftClient.getInstance().window.handle, this)
        //#endif
    }

    private val pressedKeys = mutableMapOf<Int, Boolean>()

    /** Can only be used once per click. Since the function locks itself until the key is no longer held*/
    fun Int.isKeyClicked(): Boolean = if (this.isKeyHeld()) {
        if (pressedKeys[this] != true) {
            pressedKeys[this] = true
            true
        } else {
            false
        }
    } else {
        pressedKeys[this] = false
        false
    }

    fun getKeyName(keyCode: Int): String = KeybindHelper.getKeyName(keyCode)

    object WasdInputMatrix : Iterable<KeyBinding> {
        operator fun contains(keyBinding: KeyBinding) = when (keyBinding) {
            w, a, s, d, up, down -> true
            else -> false
        }

        val w get() = Minecraft.getMinecraft().gameSettings.keyBindForward!!
        val a get() = Minecraft.getMinecraft().gameSettings.keyBindLeft!!
        val s get() = Minecraft.getMinecraft().gameSettings.keyBindBack!!
        val d get() = Minecraft.getMinecraft().gameSettings.keyBindRight!!

        val up get() = Minecraft.getMinecraft().gameSettings.keyBindJump!!
        val down get() = Minecraft.getMinecraft().gameSettings.keyBindSneak!!

        override fun iterator(): Iterator<KeyBinding> =
            object : Iterator<KeyBinding> {

                var current = w
                var finished = false

                override fun hasNext(): Boolean =
                    !finished

                override fun next(): KeyBinding {
                    if (!hasNext()) throw NoSuchElementException()

                    return current.also {
                        current = when (it) {
                            w -> a
                            a -> s
                            s -> d
                            d -> up
                            up -> down
                            else -> {
                                finished = true
                                throw NoSuchElementException()
                            }
                        }
                    }
                }

            }

    }
}
