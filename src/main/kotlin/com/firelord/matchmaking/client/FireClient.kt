package com.firelord.matchmaking.client

import com.firelord.matchmaking.model.FireMessage
import com.firelord.matchmaking.model.Opcode
import com.firelord.matchmaking.proto.*
import com.firelord.matchmaking.shared.GameMessageEncoder
import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelInitializer
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel

object FireClient {
    @JvmStatic
    fun main(args: Array<String>) {
        val group = NioEventLoopGroup()

        try {
            val b = Bootstrap()
            b.group(group)
                .channel(NioSocketChannel::class.java)
                .handler(object : ChannelInitializer<SocketChannel>() {
                    override fun initChannel(ch: SocketChannel) {
                        ch.pipeline().addLast(GameMessageEncoder())
                        ch.pipeline().addLast(object : ChannelInboundHandlerAdapter() {
                            override fun channelActive(ctx: ChannelHandlerContext) {
//                                ctx.writeAndFlush(
//                                    FireMessage(
//                                        Opcode.JOIN_QUEUE, JoinQueueRequest.newBuilder()
//                                            .setUserId(1)
//                                            .setUsername("Vasilii")
//                                            .setRegion(Region.US_East)
//                                            .setMmr(2000)
//                                            .addRolePriorities(Role.Support)
//                                            .addRolePriorities(Role.Carry)
//                                            .build().toByteArray()
//                                    )
//                                )
//                                ctx.writeAndFlush(
//                                    FireMessage(
//                                        Opcode.LEAVE_QUEUE,
//                                        LeaveQueueRequest.newBuilder().setUserId(1).build().toByteArray()
//                                    )
//                                )
//                                ctx.writeAndFlush(
//                                    FireMessage(
//                                        Opcode.ACCEPT_GAME,
//                                        Accept.newBuilder().setUserId(1).build().toByteArray()
//                                    )
//                                )
                            }

                            override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
                                println("Server says: $msg")
                            }

                            override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
                                cause.printStackTrace()
                                ctx.close()
                            }
                        })
                    }
                })

            val channel = b.connect("localhost", 9000).sync().channel()
            channel.closeFuture().sync()
        } finally {
            group.shutdownGracefully()
        }
    }

}