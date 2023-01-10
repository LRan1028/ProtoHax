package dev.sora.relay.cheat.module.impl.misc

import com.nukkitx.math.vector.Vector2f
import com.nukkitx.protocol.bedrock.data.SoundEvent
import com.nukkitx.protocol.bedrock.packet.LevelSoundEventPacket
import com.nukkitx.protocol.bedrock.packet.MovePlayerPacket
import com.nukkitx.protocol.bedrock.packet.PlayerAuthInputPacket
import com.nukkitx.protocol.bedrock.packet.NetworkStackLatencyPacket
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.cheat.value.ListValue
import dev.sora.relay.game.event.Listen
import dev.sora.relay.game.event.impl.EventPacketInbound
import dev.sora.relay.game.event.impl.EventPacketOutbound
import dev.sora.relay.game.event.impl.EventTick
import dev.sora.relay.game.utils.TimerUtil

class ModuleDisabler : CheatModule("Disabler") {

    private val modeValue = ListValue("Mode", arrayOf("LagDetection","CPSCancel","Lifeboat","Mineplex","CubeCraft"), "LagDetection")
    var timer=TimerUtil()
    private var lagPacketCount=0;
    @Listen
    fun onPacketInbound(event: EventPacketInbound) {
        if(modeValue.get() == "LagDetection") {
            if (event.packet is MovePlayerPacket) {
                lagPacketCount++
                timer.reset()
            }
            if (timer.delay(1000F)) {
                if (lagPacketCount >= 2) {
                    mc.moduleManager.getModuleByName("Speed")!!.state = false
                    mc.moduleManager.getModuleByName("Fly")!!.state = false
                }
                lagPacketCount = 0;
            }
        }
    }
    @Listen
    fun onPacketOutbound(event: EventPacketOutbound) {
        val packet = event.packet

        if(modeValue.get() == "CPSCancel"){
            if (packet is LevelSoundEventPacket){
                if(packet.sound == SoundEvent.ATTACK_STRONG || packet.sound == SoundEvent.ATTACK_NODAMAGE)
                    event.cancel()
            }
        }
        else if(modeValue.get() == "Lifeboat"){
            if (packet is MovePlayerPacket) {
                packet.isOnGround = true
                event.session.netSession.outboundPacket(MovePlayerPacket().apply {
                    runtimeEntityId = packet.runtimeEntityId
                    position = packet.position.add(0f, 0.1f, 0f)
                    rotation = packet.rotation
                    mode = packet.mode
                    isOnGround = false
                })
            }
        }else if(modeValue.get() == "CubeCraft") {
            if (packet is MovePlayerPacket) {
                for(i in 0 until 9){
                    event.session.netSession.outboundPacket(packet)
                }
            }else if (packet is PlayerAuthInputPacket) {
                packet.motion = Vector2f.from(0.01f,0.01f)

                for(i in 0 until 9){
                    event.session.netSession.outboundPacket(packet)
                }
            }else if(packet is NetworkStackLatencyPacket){
                event.cancel()
            }
        }
    }

    @Listen
    fun onTick(event: EventTick){
        val session = event.session

        if(modeValue.get() == "Mineplex" || modeValue.get() == "CubeCraft"){
            session.netSession.outboundPacket(MovePlayerPacket().apply {
                runtimeEntityId = session.thePlayer.entityId
                position = session.thePlayer.vec3Position
                rotation = session.thePlayer.vec3Rotation
                isOnGround = true
            })
        }
    }
}