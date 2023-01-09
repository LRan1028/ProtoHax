package dev.sora.relay.cheat

import com.nukkitx.protocol.bedrock.packet.TextPacket
import dev.sora.relay.game.GameSession

abstract class BasicThing {

    lateinit var session: GameSession
    lateinit var mc: GameSession

    protected fun chat(msg: String) {
        chat(session, msg)
    }

    companion object {
        fun chat(session: GameSession, msg: String) {
            if (!session.netSessionInitialized) return
            session.netSession.inboundPacket(TextPacket().apply {
                type = TextPacket.Type.RAW
                isNeedsTranslation = false
                message = "[§9§lProtoHax§r] $msg"
                xuid = ""
                sourceName = ""
            })
        }
    }
}