package com.firelord.matchmaking.server.service

import com.firelord.matchmaking.proto.Role
import com.firelord.matchmaking.server.model.QueuedUser
import com.firelord.matchmaking.server.model.Room

class TeamBuilder(val roomEvaluator: RoomEvaluator) {
    fun buildBestConfiguration(users: List<QueuedUser>, currentTime: Long = System.currentTimeMillis()): Room {
        val userById: Map<Int, QueuedUser> = users.associateBy { it.userId }
        val allPlayers: List<QueuedUser> = users.flatMap { user -> Role.entries.map { role -> user.withRole(role) } }

        val bestRoom = Room(10)

        buildBestConfiguration(
            userById,
            allPlayers,
            index = 0,
            room = Room(10),
            bestRoom = bestRoom,
            roomEvaluator = roomEvaluator,
            currentTime
        )

        return bestRoom
    }

    private fun buildBestConfiguration(
        userById: Map<Int, QueuedUser>, allPlayers: List<QueuedUser>, index: Int,
        room: Room, bestRoom: Room, roomEvaluator: RoomEvaluator, currentTime: Long
    ) {
        if (room.isFull()) {
            val currentRoomPoints = roomEvaluator.evaluate(
                diffBetweenAverageMmr = room.averageMmrDiff(userById),
                totalSecondsInQueue = room.getFirstTeamTotalTimeInQueue(userById, currentTime).toInt() / 1000,
                room = room,
                userById = userById
            )
            val bestRoomPoints = roomEvaluator.evaluate(
                diffBetweenAverageMmr = room.averageMmrDiff(userById),
                totalSecondsInQueue = room.getSecondTeamTotalTimeInQueue(userById, currentTime).toInt() / 1000,
                room = room,
                userById = userById
            )

            if (currentRoomPoints > bestRoomPoints)
                bestRoom.copyFrom(room)

            return
        }

        if (allPlayers.size == index)
            return

        val nextPlayer = allPlayers[index]
        if (!room.containsPlayer(nextPlayer.userId)) {
            room.addToFirstTeam(nextPlayer)
            buildBestConfiguration(userById, allPlayers, index + 1, room, bestRoom, roomEvaluator, currentTime)
            room.removeLastPlayerFromFirstTeam()

            room.addToSecondTeam(nextPlayer)
            buildBestConfiguration(userById, allPlayers, index + 1, room, bestRoom, roomEvaluator, currentTime)
            room.removeLastPlayerFromSecondTeam()
        }

        buildBestConfiguration(userById, allPlayers, index + 1, room, bestRoom, roomEvaluator, currentTime)
    }
}
