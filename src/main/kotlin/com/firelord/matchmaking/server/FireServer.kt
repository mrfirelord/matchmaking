package com.firelord.matchmaking.server

import com.firelord.matchmaking.server.service.MatchmakingService
import com.firelord.matchmaking.server.service.RoomEvaluator
import com.firelord.matchmaking.server.service.TeamBuilder
import com.firelord.matchmaking.shared.Conf.TCP_PORT
import com.firelord.matchmaking.shared.FireMessageDecoder
import com.firelord.matchmaking.shared.GameMessageEncoder
import com.firelord.matchmaking.shared.ProtoToFireMessageEncoder
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel

object FireServer {
    @JvmStatic
    fun main(args: Array<String>) {
        val bossGroup = NioEventLoopGroup(1)
        val workerGroup = NioEventLoopGroup()

        val roomEvaluator = RoomEvaluator(10, 2)
        val teamBuilder = TeamBuilder(roomEvaluator)
        val gameService = MatchmakingService(teamBuilder)

        try {
            val bootstrap = ServerBootstrap()
            bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel::class.java)
                .childHandler(object : ChannelInitializer<SocketChannel>() {
                    override fun initChannel(ch: SocketChannel) {
                        val pipeline = ch.pipeline()
                        pipeline.addLast(FireMessageDecoder())
                        pipeline.addLast(FromClientMessageHandler(gameService))
                        pipeline.addLast(GameMessageEncoder())
                        pipeline.addLast(ProtoToFireMessageEncoder())
                    }
                })

            val channelFuture = bootstrap.bind(TCP_PORT).sync()
            println("Server started on port $TCP_PORT")
            readLine()

            channelFuture.channel().closeFuture().sync()
        } finally {
            bossGroup.shutdownGracefully()
            workerGroup.shutdownGracefully()
        }
    }
}
