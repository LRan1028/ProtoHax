package dev.sora.relay.cheat.module.impl.misc

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.nukkitx.protocol.bedrock.data.InputMode
import com.nukkitx.protocol.bedrock.packet.LoginPacket
import com.nukkitx.protocol.bedrock.packet.PlayerAuthInputPacket
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.cheat.value.BoolValue
import dev.sora.relay.game.event.Listen
import dev.sora.relay.game.event.EventPacketOutbound
import dev.sora.relay.utils.toHexString
import io.netty.util.AsciiString
import java.util.Base64
import kotlin.random.Random

class ModuleDeviceSpoof : CheatModule("DeviceSpoof") {

    private val deviceIdValue = BoolValue("DeviceId", true)
    private val platformValue = BoolValue("Platform", true)

    @Listen
    fun onPacketOutbound(event: EventPacketOutbound) {
        val packet = event.packet

        if (packet is LoginPacket) {
            val body = JsonParser.parseString(Base64.getDecoder().decode(packet.skinData.toString().split(".")[1]).toString(Charsets.UTF_8)).asJsonObject
            if (deviceIdValue.get()) {
                body.addProperty("ClientRandomId", Random.nextLong())
                body.addProperty("DeviceId", Random.nextBytes(ByteArray(16)).toHexString())
            }
            if (platformValue.get()) {
                body.addProperty("DeviceModel","iPhone14 Plus")
                body.addProperty("DeviceOS",2) //iOS
                body.addProperty("CurrentInputMode",2) //Touch
            }
            packet.skinData = AsciiString(".${Base64.getEncoder().encodeToString(Gson().toJson(body).toByteArray(Charsets.UTF_8))}.")
        } else if (platformValue.get() && packet is PlayerAuthInputPacket) {
            packet.inputMode = InputMode.TOUCH
        }
    }
}