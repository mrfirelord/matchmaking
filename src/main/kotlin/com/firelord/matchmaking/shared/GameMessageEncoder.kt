package com.firelord.matchmaking.shared

import com.firelord.matchmaking.model.FireMessage
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder

class GameMessageEncoder : MessageToByteEncoder<FireMessage>() {
    override fun encode(ctx: ChannelHandlerContext, msg: FireMessage, out: ByteBuf) {
        out.writeByte(msg.opcode.toInt())
        out.writeInt(msg.payload.size)
        out.writeBytes(msg.payload)
    }
}
