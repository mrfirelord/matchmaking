package com.firelord.matchmaking.client

import com.firelord.matchmaking.model.FireMessage
import com.firelord.matchmaking.model.Opcode
import com.firelord.matchmaking.proto.GameInvitation
import com.firelord.matchmaking.proto.GameInvitationResponse
import com.firelord.matchmaking.proto.JoinQueueRequest
import com.firelord.matchmaking.proto.LeaveQueueRequest
import com.firelord.matchmaking.server.service.GameService
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import kotlin.random.Random

class FromServerMessageHandler : SimpleChannelInboundHandler<FireMessage>() {
    override fun channelRead0(ctx: ChannelHandlerContext, msg: FireMessage) {
        when (msg.opcode) {
            Opcode.ACCEPT_GAME -> {
                val gameInvitation = GameInvitation.parseFrom(msg.payload)
                // Todo imitates delay from client
//                Thread.sleep(Random.nextInt(5) * 1000L)
                println("Client received invitation: $gameInvitation")

                ctx.channel().writeAndFlush(
                    GameInvitationResponse.newBuilder()
                        .setUserId(gameInvitation.userId)
                        .setInviteId(gameInvitation.inviteId)
                        .setAccepted(true)
                        .build()
                )
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
