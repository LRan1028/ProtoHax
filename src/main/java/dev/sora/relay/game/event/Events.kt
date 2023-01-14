package dev.sora.relay.game.event

import com.nukkitx.network.util.DisconnectReason
import com.nukkitx.protocol.bedrock.BedrockPacket
import dev.sora.relay.game.GameSession

abstract class GameEvent(val session: GameSession)

abstract class GameEventCancelable(session: GameSession) : GameEvent(session) {

    private var canceled = false

    open fun cancel() {
        canceled = true
    }

    open fun isCanceled() = canceled

}

class EventTick(session: GameSession) : GameEvent(session)

class EventDisconnect(session: GameSession, val client: Boolean, val reason: DisconnectReason) : GameEvent(session)

class EventPacketInbound(session: GameSession, val packet: BedrockPacket) : GameEventCancelable(session)

class EventPacketOutbound(session: GameSession, val packet: BedrockPacket) : GameEventCancelable(session)