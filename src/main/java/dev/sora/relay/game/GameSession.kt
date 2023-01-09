package dev.sora.relay.game

import com.google.gson.JsonParser
import com.nukkitx.protocol.bedrock.BedrockPacket
import com.nukkitx.protocol.bedrock.data.AuthoritativeMovementMode
import com.nukkitx.protocol.bedrock.data.SyncedPlayerMovementSettings
import com.nukkitx.protocol.bedrock.packet.LoginPacket
import com.nukkitx.protocol.bedrock.packet.RespawnPacket
import com.nukkitx.protocol.bedrock.packet.SetEntityMotionPacket
import com.nukkitx.protocol.bedrock.packet.StartGamePacket
import dev.sora.relay.RakNetRelaySession
import dev.sora.relay.RakNetRelaySessionListener
import dev.sora.relay.cheat.module.ModuleManager
import dev.sora.relay.game.entity.EntityPlayerSP
import dev.sora.relay.game.event.EventManager
import dev.sora.relay.game.event.impl.EventPacketInbound
import dev.sora.relay.game.event.impl.EventPacketOutbound
import dev.sora.relay.game.event.impl.EventTick
import dev.sora.relay.game.world.WorldClient
import dev.sora.relay.utils.base64Decode
import java.util.*

class GameSession : RakNetRelaySessionListener.PacketListener {

    val thePlayer = EntityPlayerSP()
    val theWorld = WorldClient(this)
    lateinit var moduleManager : ModuleManager;
    val eventManager = EventManager()

    lateinit var netSession: RakNetRelaySession

    var xuid = ""
    var identity = UUID.randomUUID().toString()
    var displayName = "Player"

    val netSessionInitialized: Boolean
        get() = this::netSession.isInitialized

    override fun onPacketInbound(packet: BedrockPacket): Boolean {
        val event = EventPacketInbound(this, packet)
        eventManager.emit(event)
        if (event.isCanceled()) {
            return false
        }

        if (packet is StartGamePacket) {
            thePlayer.entityId = packet.runtimeEntityId
            theWorld.entityMap.clear()
        } else if (packet is RespawnPacket) {
            thePlayer.entityId = packet.runtimeEntityId
            theWorld.entityMap.clear()
        }else if (packet is StartGamePacket){
            event.cancel()
            packet.playerMovementSettings= SyncedPlayerMovementSettings().apply {
                movementMode= AuthoritativeMovementMode.SERVER
                rewindHistorySize=0
                isServerAuthoritativeBlockBreaking=true
            }
            netSession.inboundPacket(packet)
        }
        thePlayer.onPacket(packet)
        theWorld.onPacket(packet)

        return true
    }

    override fun onPacketOutbound(packet: BedrockPacket): Boolean {
        val event = EventPacketOutbound(this, packet)
        eventManager.emit(event)
        if (event.isCanceled()) {
            return false
        }

        if (packet is LoginPacket) {
            val body = JsonParser.parseString(packet.chainData.toString()).asJsonObject.getAsJsonArray("chain")
            for (chain in body) {
                val chainBody = JsonParser.parseString(base64Decode(chain.asString.split(".")[1]).toString(Charsets.UTF_8)).asJsonObject
                if (chainBody.has("extraData")) {
                    val xData = chainBody.getAsJsonObject("extraData")
                    xuid = xData.get("XUID").asString
                    identity = xData.get("identity").asString
                    displayName = xData.get("displayName").asString
                }
            }
        } else {
            thePlayer.handleClientPacket(packet, this)
        }

        return true
    }

    fun onTick() {
        eventManager.emit(EventTick(this))
    }

    fun sendPacket(packet: BedrockPacket) {
        val event = EventPacketOutbound(this, packet)
        eventManager.emit(event)
        if (event.isCanceled()) {
            return
        }

        netSession.outboundPacket(packet)
    }

    fun sendPacketToClient(packet: BedrockPacket) {
        val event = EventPacketInbound(this, packet)
        eventManager.emit(event)
        if (event.isCanceled()) {
            return
        }
        netSession.outboundPacket(packet)
    }
}