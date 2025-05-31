package com.firelord.matchmaking.server.service

import com.firelord.matchmaking.proto.Role
import com.firelord.matchmaking.server.model.QueuedUser
import com.firelord.matchmaking.server.model.Room

class TeamBuilder(val roomEvaluator: RoomEvaluator) {
    var iteration = 0
    fun buildBestConfiguration(users: List<QueuedUser>, currentTime: Long = System.currentTimeMillis()): Room {
        val allRoles = Role.entries.filter { it != Role.UNRECOGNIZED }.toList()
        val userById: Map<Int, QueuedUser> = users.associateBy { it.userId }

        // Group players by userId to handle role assignment more efficiently
        val playersByUserId = users.associateBy { it.userId }
        val allUserIds = playersByUserId.keys.toList()

        val bestRoom = Room(2)

        buildBestConfiguration(
            userById = userById,
            allUserIds = allUserIds,
            playersByUserId = playersByUserId,
            allRoles = allRoles,
            userIndex = 0,
            room = Room(2),
            bestRoom = bestRoom,
            roomEvaluator = roomEvaluator,
            currentTime = currentTime
        )

        return bestRoom
    }

    private fun buildBestConfiguration(
        userById: Map<Int, QueuedUser>,
        allUserIds: List<Int>,
        playersByUserId: Map<Int, QueuedUser>,
        allRoles: List<Role>,
        userIndex: Int,
        room: Room,
        bestRoom: Room,
        roomEvaluator: RoomEvaluator,
        currentTime: Long
    ) {
        iteration++
        if (iteration % 100 == 0)
            println(iteration)

        // Base case: Room is full, evaluate it
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

        // If we've considered all users, but the room isn't full, this is an invalid path
        if (userIndex >= allUserIds.size)
            return

        // Calculate how many more players we need
        val playersNeeded = (room.teamSize * 2) - (room.getAlUsers().size)
        // Calculate how many users are remaining to consider
        val usersRemaining = allUserIds.size - userIndex

        // Pruning: If we don't have enough users remaining to fill the room, stop this branch
        if (usersRemaining < playersNeeded)
            return

        val userId = allUserIds[userIndex]
        val user = playersByUserId[userId]!!

        // Only consider this user if they're not already in a team
        if (!room.containsPlayer(userId)) {
            // Try each role for this user in first team
            for (role in allRoles) {
                if (!room.firstTeamIsFull()) {
                    room.addToFirstTeam(user.withRole(role))
                    buildBestConfiguration(userById, allUserIds, playersByUserId, allRoles, userIndex + 1, room, bestRoom, roomEvaluator, currentTime)
                    room.removeLastPlayerFromFirstTeam()
                }
            }

            // Try each role for this user in second team
            for (role in allRoles) {
                if (!room.secondTeamIsFull()) {
                    room.addToSecondTeam(user.withRole(role))
                    buildBestConfiguration(userById, allUserIds, playersByUserId, allRoles, userIndex + 1, room, bestRoom, roomEvaluator, currentTime)
                    room.removeLastPlayerFromSecondTeam()
                }
            }
        }

        // Only try skipping this user if we have enough users remaining to fill the teams
        if (usersRemaining > playersNeeded) {
            buildBestConfiguration(userById, allUserIds, playersByUserId, allRoles, userIndex + 1, room, bestRoom, roomEvaluator, currentTime)
        }
    }
}
