package at.hannibal2.skyhanni.features.gifting

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.WinterApi
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.entity.EntityCustomNameUpdateEvent
import at.hannibal2.skyhanni.events.entity.EntityEnterWorldEvent
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ColorUtils.addAlpha
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.EntityUtils.isNpc
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.player.EntityPlayer

@SkyHanniModule
object UniqueGiftingOpportunitiesFeatures {

    private val playerList: MutableSet<String>?
        get() = ProfileStorageData.playerSpecific?.winter?.playersThatHaveBeenGifted

    private val patternGroup = RepoPattern.group("event.winter.uniquegifts")

    /**
     * REGEX-TEST: §6+1 Unique Gift given! To oBlazin§r§6!
     */
    private val giftedPattern by patternGroup.pattern(
        "gifted",
        "§6\\+1 Unique Gift given! To (?<player>[^§]+)§r§6!",
    )

    private fun hasGiftedPlayer(player: EntityPlayer) = playerList?.contains(player.name) == true

    private fun addGiftedPlayer(playerName: String) {
        playerList?.add(playerName)
    }

    private val config get() = SkyHanniMod.feature.event.gifting.giftingOpportunities

    private fun isHoldingGift() = !config.highlighWithGiftOnly || GiftApi.isHoldingGift()
    private fun isEnabled() = isHoldingGift() && config.enabled && WinterApi.isDecember()

    @Suppress("UnusedPrivateProperty")
    private const val HAS_NOT_GIFTED_NAMETAG = "§a§lꤥ"
    private const val HAS_GIFTED_NAMETAG = "§c§lꤥ"

    private fun analyzeArmorStand(entity: EntityArmorStand) {
        if (!config.useArmorStandDetection) return
        if (entity.name != HAS_GIFTED_NAMETAG) return

        val matchedPlayer = EntityUtils.getEntitiesNearby<EntityPlayer>(entity.getLorenzVec(), 2.0)
            .singleOrNull { !it.isNpc() } ?: return
        addGiftedPlayer(matchedPlayer.name)
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onEntityChangeName(event: EntityCustomNameUpdateEvent<EntityArmorStand>) {
        analyzeArmorStand(event.entity)
    }

    @HandleEvent
    fun onEntityJoinWorld(event: EntityEnterWorldEvent<Entity>) {
        playerColor(event)
        val entity = event.entity as? EntityArmorStand ?: return
        analyzeArmorStand(entity)
    }

    private fun playerColor(event: EntityEnterWorldEvent<Entity>) {
        if (event.entity is EntityOtherPlayerMP) {
            val entity = event.entity
            if (entity.isNpc() || isIronman(entity) || isBingo(entity)) return

            RenderLivingEntityHelper.setEntityColor(
                entity,
                LorenzColor.DARK_GREEN.toColor().addAlpha(127),
            ) { isEnabled() && !hasGiftedPlayer(entity) }
        }
    }

    private fun isBingo(entity: EntityLivingBase) =
        !LorenzUtils.isBingoProfile && entity.displayName.formattedText.endsWith("Ⓑ§r")

    private fun isIronman(entity: EntityLivingBase) =
        !LorenzUtils.noTradeMode && entity.displayName.formattedText.endsWith("♲§r")

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent) {
        giftedPattern.matchMatcher(event.message) {
            addGiftedPlayer(group("player"))
            UniqueGiftCounter.addUniqueGift()
        }
    }
}
