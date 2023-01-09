package dev.sora.relay.cheat.module.impl.movement

import com.nukkitx.math.vector.Vector3f
import com.nukkitx.protocol.bedrock.data.Ability
import com.nukkitx.protocol.bedrock.data.AbilityLayer
import com.nukkitx.protocol.bedrock.data.PlayerAuthInputData
import com.nukkitx.protocol.bedrock.data.PlayerPermission
import com.nukkitx.protocol.bedrock.data.command.CommandPermission
import com.nukkitx.protocol.bedrock.data.entity.EntityEventType
import com.nukkitx.protocol.bedrock.packet.*
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.cheat.value.FloatValue
import dev.sora.relay.cheat.value.ListValue
import dev.sora.relay.game.event.Listen
import dev.sora.relay.game.event.impl.EventPacketInbound
import dev.sora.relay.game.event.impl.EventPacketOutbound
import dev.sora.relay.game.event.impl.EventTick
import dev.sora.relay.game.utils.movement.MovementUtils.isMoving
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

class ModuleFly : CheatModule("Fly") {

    private val modeValue = ListValue("Mode", arrayOf("Motion","Vanilla", "Mineplex"), "Vanilla")
    private val motionYValue = FloatValue("MotionY" ,0.4849f,0f,2f)
    private val motionXZValue = FloatValue("MotionXZ" ,0.4849f,0f,2f)

    private var launchY = 0.0
    private var canFly = false

    private val abilityPacket = UpdateAbilitiesPacket().apply {
        playerPermission = PlayerPermission.OPERATOR
        commandPermission = CommandPermission.OPERATOR
        abilityLayers.add(AbilityLayer().apply {
            layerType = AbilityLayer.Type.BASE
            abilitiesSet.addAll(Ability.values())
            abilityValues.addAll(arrayOf(Ability.BUILD, Ability.MINE, Ability.DOORS_AND_SWITCHES, Ability.OPEN_CONTAINERS, Ability.ATTACK_PLAYERS, Ability.ATTACK_MOBS, Ability.OPERATOR_COMMANDS, Ability.MAY_FLY, Ability.FLY_SPEED, Ability.WALK_SPEED))
            walkSpeed = 0.1f
            flySpeed = 0.15f
        })
    }

    override fun onEnable() {
        canFly = false
        launchY = session.thePlayer.posY
    }

    @Listen
    fun onTick(event: EventTick) {
        when {
            modeValue.get() == "Mineplex" -> {
                event.session.netSession.inboundPacket(abilityPacket.apply {
                    uniqueEntityId = event.session.thePlayer.entityId
                })
                if (!canFly) return
                val player = event.session.thePlayer
                val yaw = Math.toRadians(player.rotationYaw.toDouble())
                val value = 2.2f
                player.teleport(player.posX - sin(yaw) * value, launchY, player.posZ + cos(yaw) * value, event.session.netSession)
            }
            modeValue.get() == "Vanilla" && !canFly -> {
                canFly = true
                event.session.netSession.inboundPacket(abilityPacket.apply {
                    uniqueEntityId = event.session.thePlayer.entityId
                })
            }
        }
    }

    @Listen
    fun onPacketInbound(event: EventPacketInbound) {
        if (event.packet is UpdateAbilitiesPacket) {
            event.cancel()
            event.session.netSession.inboundPacket(abilityPacket.apply {
                uniqueEntityId = event.session.thePlayer.entityId
            })
        } else if (event.packet is StartGamePacket) {
            event.session.netSession.inboundPacket(abilityPacket.apply {
                uniqueEntityId = event.session.thePlayer.entityId
            })
        }
    }

    @Listen
    fun onPacketOutbound(event: EventPacketOutbound) {
        when {
            modeValue.get() == "Mineplex" -> {
                if (event.packet is RequestAbilityPacket && event.packet.ability == Ability.FLYING) {
                    canFly = !canFly
                    if (canFly) {
                        launchY = floor(session.thePlayer.posY) - 0.38
                        event.session.sendPacketToClient(EntityEventPacket().apply {
                            runtimeEntityId = event.session.thePlayer.entityId
                            type = EntityEventType.HURT
                            data = 0
                        })
                        val player = event.session.thePlayer
                        repeat(5) {
                            event.session.sendPacket(MovePlayerPacket().apply {
                                runtimeEntityId = player.entityId
                                position = Vector3f.from(player.posX, launchY, player.posZ)
                                rotation = Vector3f.from(player.rotationPitch, player.rotationYaw, 0f)
                                mode = MovePlayerPacket.Mode.NORMAL
                            })
                        }
                    }
                    event.session.netSession.inboundPacket(abilityPacket.apply {
                        uniqueEntityId = session.thePlayer.entityId
                    })
                    event.cancel()
                } else if (event.packet is MovePlayerPacket && canFly) {
                    event.packet.isOnGround = true
                    event.packet.position = event.packet.position.let {
                        Vector3f.from(it.x, launchY.toFloat(), it.z)
                    }
                }
            }
            modeValue.get() == "Motion" -> {
                if(event.packet is PlayerAuthInputPacket && isMoving(mc)) strafe(motionXZValue.get(),if(mc.thePlayer.inputData.contains(PlayerAuthInputData.JUMPING)) motionYValue.get() else if(mc.thePlayer.inputData.contains(PlayerAuthInputData.SNEAKING)) -motionYValue.get() else 0.01f)
            }
            else -> {
                if (event.packet is RequestAbilityPacket && event.packet.ability == Ability.FLYING) {
                    event.cancel()
                }
            }
        }
    }
    private val direction: Double
        get() {
            var rotationYaw = mc.thePlayer.rotationYaw
            if (mc.thePlayer.moveForward < 0f) rotationYaw += 180f
            var forward = 1f
            if (mc.thePlayer.moveForward < 0f) forward = -0.5f else if (mc.thePlayer.moveForward > 0f) forward = 0.5f
            if (mc.thePlayer.moveStrafing > 0f) rotationYaw -= 90f * forward
            if (mc.thePlayer.moveStrafing < 0f) rotationYaw += 90f * forward
            return Math.toRadians(rotationYaw.toDouble())
        }
    private fun strafe(speed: Float,motionY:Float) {
        val yaw = direction
        session.netSession.inboundPacket(SetEntityMotionPacket().apply {
            runtimeEntityId = mc.thePlayer.entityId
            motion = Vector3f.from(-sin(yaw) * speed, motionY.toDouble(), cos(yaw) * speed)
        })
    }
}