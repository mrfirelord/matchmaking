package com.firelord.matchmaking.server

import com.firelord.matchmaking.model.FireMessage
import com.firelord.matchmaking.model.Opcode
import com.firelord.matchmaking.proto.GameInvitationResponse
import com.firelord.matchmaking.proto.JoinQueueRequest
import com.firelord.matchmaking.proto.LeaveQueueRequest
import com.firelord.matchmaking.server.service.GameService
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler

class FromClientMessageHandler(private val service: GameService) : SimpleChannelInboundHandler<FireMessage>() {
    override fun channelRead0(ctx: ChannelHandlerContext, msg: FireMessage) {
        when (msg.opcode) {
            Opcode.JOIN_QUEUE -> {
                val request = JoinQueueRequest.parseFrom(msg.payload)
                service.joinQueue(ctx, request)
            }

            Opcode.LEAVE_QUEUE -> {
                val request = LeaveQueueRequest.parseFrom(msg.payload)
                service.leaveQueue(ctx, request)
            }

            Opcode.ACCEPT_GAME -> {
                val request = GameInvitationResponse.parseFrom(msg.payload)
                service.receiveAcceptResponse(ctx, request)
            }

            else -> {
                println("Unknown opcode: ${msg.opcode}")
                ctx.close()
            }
        }
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        println("Error: ${cause.message}")
        ctx.close()
    }
}
