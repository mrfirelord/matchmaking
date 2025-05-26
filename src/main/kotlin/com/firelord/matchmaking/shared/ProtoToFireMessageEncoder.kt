package com.firelord.matchmaking.shared

import com.firelord.matchmaking.model.FireMessage
import com.firelord.matchmaking.model.Opcode
import com.firelord.matchmaking.proto.GameInvitation
import com.firelord.matchmaking.proto.GameInvitationResponse
import com.firelord.matchmaking.proto.JoinQueueRequest
import com.firelord.matchmaking.proto.LeaveQueueRequest
import com.google.protobuf.MessageLite
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToMessageEncoder

/** Encodes the protobuf message into `FireMessage` objects for transmission over the network. */
class ProtoToFireMessageEncoder : MessageToMessageEncoder<MessageLite>() {
    override fun encode(ctx: ChannelHandlerContext, msg: MessageLite, out: MutableList<Any>) {
        val opcode = when (msg) {
            is JoinQueueRequest -> Opcode.JOIN_QUEUE
            is LeaveQueueRequest -> Opcode.LEAVE_QUEUE
            is GameInvitation -> Opcode.ACCEPT_GAME
            is GameInvitationResponse -> Opcode.ACCEPT_GAME_RESPONSE
            else -> throw IllegalArgumentException("Unknown message type: ${msg.javaClass}")
        }

        out.add(FireMessage(opcode, msg.toByteArray()))
    }
}