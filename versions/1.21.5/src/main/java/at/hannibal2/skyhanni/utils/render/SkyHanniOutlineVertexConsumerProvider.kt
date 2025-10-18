package at.hannibal2.skyhanni.utils.render

import at.hannibal2.skyhanni.test.command.ErrorManager
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.AddressMode
import com.mojang.blaze3d.textures.FilterMode
import com.mojang.blaze3d.textures.GpuTexture
import com.mojang.blaze3d.textures.TextureFormat
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.OutlineVertexConsumerProvider
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.render.VertexConsumerProvider
//#if MC > 1.21.6
//$$ import com.mojang.blaze3d.textures.GpuTextureView
//#endif

// The idea and implementation for this class was inspired by SkyBlocker.
// This implementation has been modified from the original SkyBlocker code to work across multiple versions.

//#if MC < 1.21.9
class SkyHanniOutlineVertexConsumerProvider(parent: VertexConsumerProvider.Immediate) : OutlineVertexConsumerProvider(parent) {
    //#else
    //$$ class SkyHanniOutlineVertexConsumerProvider(parent: VertexConsumerProvider.Immediate) : OutlineVertexConsumerProvider() {
    //#endif

    override fun draw() {
        beginRendering()
        super.draw()
        finishRendering()
    }

    override fun getBuffer(renderLayer: RenderLayer): VertexConsumer {
        beginRendering()
        val returnVal = super.getBuffer(renderLayer)
        finishRendering()
        return returnVal
    }

    companion object {

        @JvmStatic
        val vertexConsumers by lazy { SkyHanniOutlineVertexConsumerProvider(MinecraftClient.getInstance().bufferBuilders.entityVertexConsumers) }

        private var customDepthAttachment: GpuTexture? = null

        //#if MC < 1.21.6
        @JvmStatic
        fun getOverrideDepthAttachment(): GpuTexture? {
            if (!currentlyActive) return null
            return customDepthAttachment
        }
        //#else
        //$$ private var customDepthAttachmentView: GpuTextureView? = null
        //#endif

        @JvmStatic
        var currentlyActive = false

        private fun beginRendering() {
            currentlyActive = true
            //#if MC > 1.21.6
            //$$ RenderSystem.outputDepthTextureOverride = customDepthAttachmentView
            //#endif
        }

        private fun finishRendering() {
            currentlyActive = false
            //#if MC > 1.21.6
            //$$ RenderSystem.outputDepthTextureOverride = null
            //#endif
        }

        private var lastWidth = 0
        private var lastHeight = 0

        @JvmStatic
        fun checkIfDepthAttachmentNeedsUpdating() {
            val window = MinecraftClient.getInstance().window
            if (customDepthAttachment == null || window.framebufferWidth != lastWidth || window.framebufferHeight != lastHeight) {
                lastWidth = window.framebufferWidth
                lastHeight = window.framebufferHeight
                updateDepthAttachment()
            }
            try {
                RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(
                    MinecraftClient.getInstance().framebuffer.depthAttachment,
                    customDepthAttachment!!,
                    0, 0, 0, 0, 0, lastWidth, lastHeight,
                )
            } catch (e: Exception) {
                ErrorManager.logErrorWithData(e, "Failed to copy depth attachment")
            }
        }

        private fun updateDepthAttachment() {
            try {
                if (customDepthAttachment != null) {
                    customDepthAttachment!!.close()
                    //#if MC > 1.21.6
                    //$$ customDepthAttachmentView!!.close()
                    //#endif
                }
                val device = RenderSystem.getDevice()
                val depthAttachment = device.createTexture(
                    "SkyHanni Custom Depth",
                    //#if MC < 1.21.6
                    TextureFormat.DEPTH32, lastWidth, lastHeight, 1,
                    //#else
                    //$$ GpuTexture.USAGE_RENDER_ATTACHMENT or GpuTexture.USAGE_COPY_DST or GpuTexture.USAGE_TEXTURE_BINDING,
                    //$$ TextureFormat.DEPTH32,
                    //$$ lastWidth, lastHeight, 1, 1,
                    //#endif
                )
                depthAttachment.setTextureFilter(FilterMode.NEAREST, false)
                depthAttachment.setAddressMode(AddressMode.CLAMP_TO_EDGE)
                customDepthAttachment = depthAttachment
                //#if MC > 1.21.6
                //$$ customDepthAttachmentView = device.createTextureView(depthAttachment)
                //#endif
            } catch (e: Exception) {
                ErrorManager.logErrorWithData(e, "Failed to update outline depth attachment")
            }
        }
    }
}
