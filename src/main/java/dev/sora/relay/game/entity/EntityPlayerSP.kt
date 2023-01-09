package dev.sora.relay.game.entity

import com.nukkitx.math.vector.Vector3f
import com.nukkitx.protocol.bedrock.BedrockPacket
import com.nukkitx.protocol.bedrock.data.PlayerAuthInputData
import com.nukkitx.protocol.bedrock.data.SoundEvent
import com.nukkitx.protocol.bedrock.data.inventory.ItemData
import com.nukkitx.protocol.bedrock.data.inventory.TransactionType
import com.nukkitx.protocol.bedrock.packet.*
import dev.sora.relay.RakNetRelaySession
import dev.sora.relay.cheat.BasicThing
import dev.sora.relay.game.GameSession
import java.util.*

class EntityPlayerSP : EntityPlayer(0L, UUID.randomUUID(), "") {

    override var entityId: Long = 0L
    var heldItemSlot = 0
        private set
    var onGround = true
    var inputData = EnumSet.noneOf(PlayerAuthInputData::class.java)
    var currentItem : ItemData? = null
    var moveStrafing = 0.0f
    var moveForward = 0.0f
    fun teleport(x: Double, y: Double, z: Double, netSession: RakNetRelaySession) {
        move(x, y, z)
        netSession.inboundPacket(MovePlayerPacket().apply {
            runtimeEntityId = entityId
            position = Vector3f.from(x, y, z)
            rotation = Vector3f.from(rotationPitch, rotationYaw, 0f)
            mode = MovePlayerPacket.Mode.NORMAL
        })
    }

    fun handleClientPacket(packet: BedrockPacket, session: GameSession) {
        if (packet is MovePlayerPacket) {
            move(packet.position)
            rotate(packet.rotation)
            if (packet.runtimeEntityId != entityId) {
                BasicThing.chat(session, "runtimeEntityId mismatch, desync occur? (client=${packet.runtimeEntityId}, relay=${entityId})")
                entityId = packet.runtimeEntityId
            }
            session.onTick()
            tickExists++
        } else if (packet is PlayerAuthInputPacket) {
            inputData= packet.inputData as EnumSet<PlayerAuthInputData>?
            moveStrafing = packet.motion.x
            moveForward = packet.motion.y
            onGround=packet.motion.y==0f
            move(packet.position)
            rotate(packet.rotation)
            session.onTick()
            tickExists++
        } else if (packet is PlayerHotbarPacket && packet.containerId == 0) {
            heldItemSlot = packet.selectedHotbarSlot
        } else if (packet is MobEquipmentPacket && packet.runtimeEntityId == entityId) {
            heldItemSlot = packet.hotbarSlot
        }
    }

    fun attackEntity(entity: Entity, session: GameSession, swingValue: SwingMode = SwingMode.BOTH) {
        AnimatePacket().apply {
            action = AnimatePacket.Action.SWING_ARM
            runtimeEntityId = session.thePlayer.entityId
        }.also {
            // send the packet back to client in order to display the swing animation
            if (swingValue == SwingMode.BOTH || swingValue == SwingMode.CLIENTSIDE)
                session.netSession.inboundPacket(it)
            if (swingValue == SwingMode.BOTH || swingValue == SwingMode.SERVERSIDE)
                session.sendPacket(it)
        }

        session.sendPacket(LevelSoundEventPacket().apply {
            sound = SoundEvent.ATTACK_STRONG
            position = session.thePlayer.vec3Position
            extraData = -1
            identifier = "minecraft:player"
            isBabySound = false
            isRelativeVolumeDisabled = false
        })

        // attack
        session.sendPacket(InventoryTransactionPacket().apply {
            transactionType = TransactionType.ITEM_USE_ON_ENTITY
            actionType = 1
            runtimeEntityId = entity.entityId
            hotbarSlot = session.thePlayer.heldItemSlot
            itemInHand = ItemData.AIR
            playerPosition = session.thePlayer.vec3Position
            clickPosition = Vector3f.ZERO
        })
    }

    fun jump(session:GameSession) {
        session.netSession.inboundPacket(SetEntityMotionPacket().apply {
            runtimeEntityId=entityId
            motion = Vector3f.from(0.0f,0.42f,0.0f)
        })
    }

    fun getEyeHeight() : Double{
        return posY
    }
    fun getJavaPosY() : Double{
        return posY-1.62
    }

    enum class SwingMode {
        CLIENTSIDE,
        SERVERSIDE,
        BOTH,
        NONE
    }
}