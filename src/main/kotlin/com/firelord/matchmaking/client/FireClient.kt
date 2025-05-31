package com.firelord.matchmaking.client

import com.firelord.matchmaking.proto.JoinQueueRequest
import com.firelord.matchmaking.proto.LeaveQueueRequest
import com.firelord.matchmaking.proto.Role
import com.firelord.matchmaking.shared.Conf.TCP_PORT
import com.firelord.matchmaking.shared.FireMessageDecoder
import com.firelord.matchmaking.shared.GameMessageEncoder
import com.firelord.matchmaking.shared.ProtoToFireMessageEncoder
import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel

class FireClient(val userId: Int) {
    private val group = NioEventLoopGroup(1)
    private lateinit var channel: Channel

    fun connect(): Channel {
        val b = Bootstrap()
        b.group(group)
            .channel(NioSocketChannel::class.java)
            .handler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(ch: SocketChannel) {
                    val pipeline = ch.pipeline()
                    pipeline.addLast(FireMessageDecoder())
                    pipeline.addLast(FromServerMessageHandler())
                    pipeline.addLast(GameMessageEncoder())
                    pipeline.addLast(ProtoToFireMessageEncoder())
                }
            })

        channel = b.connect("localhost", TCP_PORT).sync().channel()
        println("Client $userId connected to server")
        return channel
    }

    fun disconnect() {
        if (::channel.isInitialized && channel.isActive) {
            channel.close().sync()
        }
        group.shutdownGracefully()
        println("Client $userId disconnected")
    }

    fun joinQueue(mmr: Int, roles: List<Role>) {
        if (!::channel.isInitialized || !channel.isActive)
            throw IllegalStateException("Client is not connected")

        val request = JoinQueueRequest.newBuilder().setUserId(userId).setMmr(mmr)
            .also { builder -> roles.forEach { builder.addRolePriorities(it) } }
            .build()

        channel.writeAndFlush(request)
        println("Client $userId joined queue")
    }

    fun leaveQueue() {
        if (::channel.isInitialized && channel.isActive) {
            val request = LeaveQueueRequest.newBuilder()
                .setUserId(userId)
                .build()
            channel.writeAndFlush(request)
            println("Client $userId left queue")
        }
    }

    // Add other client actions as needed
}