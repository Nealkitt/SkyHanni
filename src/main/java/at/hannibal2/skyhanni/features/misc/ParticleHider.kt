package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.features.dungeon.DungeonApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.getLorenzVec
import net.minecraft.entity.projectile.EntitySmallFireball
import net.minecraft.util.EnumParticleTypes

@SkyHanniModule
object ParticleHider {

    private val config get() = SkyHanniMod.feature.misc.particleHiders

    private fun inM7Boss() = DungeonApi.inDungeon() && DungeonApi.dungeonFloor == "M7" && DungeonApi.inBossRoom

    @HandleEvent
    fun onReceiveParticle(event: ReceiveParticleEvent) {
        if (!MinecraftCompat.localPlayerExists) return
        val distanceToPlayer = event.distanceToPlayer
        if (config.hideFarParticles && distanceToPlayer > 40 && !inM7Boss()) {
            event.cancel()
            return
        }

        val type = event.type
        if (config.hideCloseRedstoneParticles &&
            type == EnumParticleTypes.REDSTONE && distanceToPlayer < 2
        ) {
            event.cancel()
            return
        }

        if (config.hideFireballParticles &&
            (type == EnumParticleTypes.SMOKE_NORMAL || type == EnumParticleTypes.SMOKE_LARGE)
        ) {
            for (entity in EntityUtils.getEntities<EntitySmallFireball>()) {
                val distance = entity.getLorenzVec().distance(event.location)
                if (distance < 5) {
                    event.cancel()
                    return
                }
            }
        }
    }

    @JvmStatic
    fun shouldHideBlockParticles(): Boolean {
        val config = config.blockBreakParticle
        return when {
            !config.hide -> false
            config.onlyInGarden -> IslandType.GARDEN.isCurrent()
            else -> true
        }
    }

    @JvmStatic
    fun shouldHideFireParticles() = MinecraftCompat.localWorldExists && config.hideFireBlockParticles

    @JvmStatic
    fun shouldHideBlazeParticles() = MinecraftCompat.localWorldExists && config.hideBlazeParticles

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "misc.hideBlazeParticles", "misc.particleHiders.hideBlazeParticles")
        event.move(3, "misc.hideEndermanParticles", "misc.particleHiders.hideEndermanParticles")
        event.move(3, "misc.hideFarParticles", "misc.particleHiders.hideFarParticles")
        event.move(3, "misc.hideFireballParticles", "misc.particleHiders.hideFireballParticles")
        event.move(3, "misc.hideCloseRedstoneparticles", "misc.particleHiders.hideCloseRedstoneParticles")
        event.move(3, "misc.hideFireBlockParticles", "misc.particleHiders.hideFireBlockParticles")
        event.move(3, "misc.hideSmokeParticles", "misc.particleHiders.hideSmokeParticles")
    }
}
