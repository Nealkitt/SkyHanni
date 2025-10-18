package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.compat.ReiCompat;
import at.hannibal2.skyhanni.events.minecraft.CharEvent;
import at.hannibal2.skyhanni.events.minecraft.KeyDownEvent;
import at.hannibal2.skyhanni.events.minecraft.KeyUpEvent;
import at.hannibal2.skyhanni.events.minecraft.KeyPressEvent;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//#if MC > 1.21.8
//$$ import net.minecraft.client.input.CharInput;
//$$ import net.minecraft.client.input.KeyInput;
//#endif

@Mixin(Keyboard.class)
public class MixinKeyboard {

    @Inject(method = "onKey", at = @At("HEAD"))
    //#if MC < 1.21.9
    private void onKey(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        //#else
        //$$ private void onKey(long window, int action, KeyInput input, CallbackInfo ci) {
        //$$     int key = input.key();
        //#endif
        if (MinecraftClient.getInstance().player == null) return;
        if (key == GLFW.GLFW_KEY_UNKNOWN) return;
        //System.out.println("Key: " + key + " Scancode: " + scancode + " Action: " + action + " Modifiers: " + modifiers);

        // don't send key events if Rei search bar is selected
        if (ReiCompat.searchHasFocus()) return;

        /*
            * action = 0: Key released
            * action = 1: Key pressed
            * action = 2: Key held
            * key = keycode
            * not entirely sure what scancode means
            * modifiers = 0: No modifier
            * modifiers = 1: Shift
            * modifiers = 2: Control
            * modifiers = 4: Alt
         */
        // todo on 1.8 it first checks TextInput.isActive() before posting, however im not sure if this is needed
        // and as of now that file would need to be recoded to work with 1.21 so it hasn't been put here
        // there is also an onChar method we could mixin to and use for typing fields and replace TextInput.isActive() with that somehow
        // the extension functions such as isActive() and isKeyHeld() still work from keyboard manager
        // this only replaces the posting of events
        if (action == 0) new KeyUpEvent(key).post();
        if (action == 1) {
            new KeyDownEvent(key).post();
            // on 1.21 it takes like 1 full second before the key press event will get posted so im doing it here
            new KeyPressEvent(key).post();
        }
        if (action == 2) new KeyPressEvent(key).post();
    }

    @Inject(method = "onChar", at = @At("HEAD"))
    //#if MC < 1.21.9
    private void onChar(long window, int codePoint, int modifiers, CallbackInfo ci) {
        if (MinecraftClient.getInstance().player == null) return;
        new CharEvent(codePoint).post();
    }
    //#else
    //$$ private void onChar(long window, CharInput input, CallbackInfo ci) {
    //$$     if (MinecraftClient.getInstance().player == null) return;
    //$$     new CharEvent(input.codepoint()).post();
    //$$ }
    //#endif
}
