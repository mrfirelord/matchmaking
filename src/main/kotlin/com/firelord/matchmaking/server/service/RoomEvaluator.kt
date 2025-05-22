package com.firelord.matchmaking.server.service

import com.firelord.matchmaking.server.model.QueuedUser
import com.firelord.matchmaking.server.model.Room

class RoomEvaluator(val oneSecondWeight: Int, val countOfDesiredRolesPerUser: Int) {
    fun evaluate(
        diffBetweenAverageMmr: Int, totalSecondsInQueue: Int, room: Room, userById: Map<Int, QueuedUser>
    ): Int {
        var result = totalSecondsInQueue * oneSecondWeight - diffBetweenAverageMmr
        result += room.getAlUsers().sumOf { user ->
            val priorityRoles = userById[user.userId]?.rolePriorities ?: throw IllegalArgumentException("something bad")
            val indexOfRole = priorityRoles.indexOf(user.chosenRole)
            if (indexOfRole == -1)
                0
            else
                countOfDesiredRolesPerUser - indexOfRole
        }

        return result
    }
}