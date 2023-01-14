package dev.sora.relay.game.entity

import com.nukkitx.math.vector.Vector3f
import com.nukkitx.protocol.bedrock.BedrockPacket
import com.nukkitx.protocol.bedrock.packet.AddEntityPacket
import com.nukkitx.protocol.bedrock.packet.AddPlayerPacket
import com.nukkitx.protocol.bedrock.packet.MoveEntityAbsolutePacket
import dev.sora.relay.game.event.Listen
import dev.sora.relay.game.event.EventPacketInbound
import kotlin.math.sqrt

abstract class Entity(open val entityId: Long) {

    open var posX = 0.0
    open var posY = 0.0
    open var posZ = 0.0
    var identifier = ""
    open var prevPosX = 0.0
    open var prevPosY = 0.0
    open var prevPosZ = 0.0

    open var rotationYaw = 0f
    open var rotationPitch = 0f
    open var rotationYawHead = 0f

    open var motionX = 0.0
    open var motionY = 0.0
    open var motionZ = 0.0
    var uniqueEntityId = 0L

    var entityType = 0

    var tickExists = 0L

//    val attributeList = mutableListOf<AttributeData>()
//    val metadataList = EntityDataMap()

    val vec3Position: Vector3f
        get() = Vector3f.from(posX, posY, posZ)

    val vec3Rotation: Vector3f
        get() = Vector3f.from(rotationPitch, rotationYaw, rotationYawHead)

    // TODO: inventory

    open fun move(x: Double, y: Double, z: Double) {
        this.prevPosX = this.posX
        this.prevPosY = this.posY
        this.prevPosZ = this.posZ
        this.posX = x
        this.posY = y
        this.posZ = z
        this.motionX = x - prevPosX
        this.motionY = y - prevPosY
        this.motionZ = z - prevPosZ
    }

    open fun move(position: Vector3f) {
        move(position.x.toDouble(), position.y.toDouble(), position.z.toDouble())
    }

    open fun rotate(yaw: Float, pitch: Float) {
        this.rotationYaw = yaw
        this.rotationPitch = pitch
    }

    open fun rotate(rotation: Vector3f) {
        rotate(rotation.y, rotation.x)
        rotationYawHead = rotation.z
    }

    fun distanceSq(x: Double, y: Double, z: Double): Double {
        val dx = posX - x
        val dy = posY - y
        val dz = posZ - z
        return dx * dx + dy * dy + dz * dz
    }

    fun distanceSq(entity: Entity)
            = distanceSq(entity.posX, entity.posY, entity.posZ)

    fun distance(x: Double, y: Double, z: Double)
        = sqrt(distanceSq(x, y, z))

    fun distance(entity: Entity)
        = distance(entity.posX, entity.posY, entity.posZ)

    open fun onPacket(packet: BedrockPacket) {
        if (packet is MoveEntityAbsolutePacket && packet.runtimeEntityId == entityId) {
            move(packet.position)
            rotate(packet.rotation)
            tickExists++
        } else if(packet is AddEntityPacket && packet.runtimeEntityId == entityId){
            uniqueEntityId = packet.uniqueEntityId
            entityType = packet.entityType
            identifier = packet.identifier
        } else if(packet is AddPlayerPacket && packet.runtimeEntityId == entityId){
            uniqueEntityId = packet.uniqueEntityId
            entityType = 63
        } /* else if (packet is MoveEntityDeltaPacket && packet.runtimeEntityId == entityId) {
            // TODO
        } */
    }
    @Listen
    fun onPacketInbound(event: EventPacketInbound) {
        val packet = event.packet
        if (packet is MoveEntityAbsolutePacket && packet.runtimeEntityId == entityId) {
            move(packet.position)
            rotate(packet.rotation)
            tickExists++
        }
    }
    open fun reset() {
//        attributeList.clear()
//        metadataList.clear()
    }
}