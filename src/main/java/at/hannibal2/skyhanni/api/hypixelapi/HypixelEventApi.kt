package at.hannibal2.skyhanni.api.hypixelapi

import at.hannibal2.skyhanni.events.hypixel.modapi.HypixelApiJoinEvent
import at.hannibal2.skyhanni.events.hypixel.modapi.HypixelApiServerChangeEvent
import at.hannibal2.skyhanni.features.misc.CurrentPing
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import net.hypixel.data.region.Environment
import net.hypixel.modapi.HypixelModAPI
import net.hypixel.modapi.packet.impl.clientbound.ClientboundHelloPacket
import net.hypixel.modapi.packet.impl.clientbound.ClientboundPingPacket
import net.hypixel.modapi.packet.impl.clientbound.event.ClientboundLocationPacket
import net.hypixel.modapi.packet.impl.serverbound.ServerboundVersionedPacket
import kotlin.jvm.optionals.getOrNull

@SkyHanniModule
object HypixelEventApi {

    private val modApi: HypixelModAPI = HypixelModAPI.getInstance()

    init {
        modApi.subscribeToEventPacket(ClientboundLocationPacket::class.java)
        modApi.createHandler(ClientboundHelloPacket::class.java, ::onHelloPacket)
        modApi.createHandler(ClientboundLocationPacket::class.java, ::onLocationPacket)
        modApi.createHandler(ClientboundPingPacket::class.java, CurrentPing::onPingPacket)
    }

    private fun onHelloPacket(packet: ClientboundHelloPacket) {
        if (!HypixelLocationApi.config) return
        val isAlpha = packet.environment != Environment.PRODUCTION
        HypixelApiJoinEvent(isAlpha).post()
    }

    private fun onLocationPacket(packet: ClientboundLocationPacket) {
        if (!HypixelLocationApi.config) return
        HypixelApiServerChangeEvent(
            packet.serverName,
            packet.serverType.getOrNull(),
            packet.lobbyName.getOrNull(),
            packet.mode.getOrNull(),
            packet.map.getOrNull(),
        ).post()
    }

    fun sendPacket(packet: ServerboundVersionedPacket) {
        modApi.sendPacket(packet)
    }
}
