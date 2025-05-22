package com.firelord.matchmaking.server.service

import com.firelord.matchmaking.proto.GameInvitationResponse
import com.firelord.matchmaking.proto.JoinQueueRequest
import com.firelord.matchmaking.proto.LeaveQueueRequest
import io.netty.channel.ChannelHandlerContext

interface GameService {
    fun joinQueue(ctx: ChannelHandlerContext, payload: JoinQueueRequest)
    fun leaveQueue(ctx: ChannelHandlerContext, payload: LeaveQueueRequest)
    fun receiveAcceptResponse(ctx: ChannelHandlerContext, response: GameInvitationResponse)
}
