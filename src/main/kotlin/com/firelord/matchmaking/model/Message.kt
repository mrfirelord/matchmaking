package com.firelord.matchmaking.model

class FireMessage(val opcode: Byte, val payload: ByteArray)

object Opcode {
    /* ==== From client ==== */
    const val JOIN_QUEUE: Byte = 1
    const val LEAVE_QUEUE: Byte = 2
    const val ACCEPT_GAME: Byte = 3

    /* ==== To client ==== */
    const val ACCEPT_GAME_RESPONSE: Byte = 101
}


