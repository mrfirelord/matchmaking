package com.firelord.matchmaking.shared

import com.firelord.matchmaking.model.FireMessage
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder

class FireMessageDecoder : ByteToMessageDecoder() {
    override fun decode(ctx: ChannelHandlerContext, inBuf: ByteBuf, out: MutableList<Any>) {
        // four bytes (int) for payload size and one byte for opcode
        if (inBuf.readableBytes() < 5) return
        inBuf.markReaderIndex()

        val opcode = inBuf.readByte()
        val length = inBuf.readInt()

        if (inBuf.readableBytes() < length) {
            inBuf.resetReaderIndex()
            return
        }

        val payload = ByteArray(length)
        inBuf.readBytes(payload)
        out.add(FireMessage(opcode, payload))
    }
}
