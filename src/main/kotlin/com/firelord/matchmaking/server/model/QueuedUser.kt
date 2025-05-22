package com.firelord.matchmaking.server.model

import com.firelord.matchmaking.proto.JoinQueueRequest
import com.firelord.matchmaking.proto.Role

data class QueuedUser(
    val userId: Int, val rolePriorities: List<Role>, val mmr: Int,
    var chosenRole: Role,
    val inQueueSince: Long = System.currentTimeMillis()
) {
    fun withRole(newRole: Role): QueuedUser =
        QueuedUser(userId = userId, rolePriorities = rolePriorities, mmr = mmr, chosenRole = newRole)
}

object QueuedUserFactory {
    fun create(joinQueueRequest: JoinQueueRequest): QueuedUser = QueuedUser(
        userId = joinQueueRequest.userId,
        rolePriorities = joinQueueRequest.rolePrioritiesList,
        chosenRole = Role.UNRECOGNIZED,
        mmr = joinQueueRequest.mmr
    )
}