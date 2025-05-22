package com.firelord.matchmaking.server.model

import kotlin.math.absoluteValue
import kotlin.time.Duration

class Room(val teamSize: Int) {
    // Filled when all users accepted the game and it successfully started
    var gameId: String = ""

    // Filled when the invite was sent to every player
    private var inviteId: String = ""
    private var inviteExpireTime: Long = 0

    private var inviteSentTimestamp: Long = 0
    private var gameStartedTimestamp: Long = 0
    private val userIdsAcceptedTheGame = hashSetOf<Int>()

    private val firstTeam: MutableList<QueuedUser> = mutableListOf()
    private val secondTeam: MutableList<QueuedUser> = mutableListOf()
    private val userIds: HashSet<Int> = hashSetOf()

    fun sendInvite(inviteId: String, roomLifetime: Duration) {
        this.inviteId = inviteId
        inviteSentTimestamp = System.currentTimeMillis()
        this.inviteExpireTime = inviteSentTimestamp + roomLifetime.inWholeMilliseconds
    }

    fun startGame(gameId: String) {
        this.gameId = gameId
        this.gameStartedTimestamp = System.currentTimeMillis()
    }

    /**
     * Adds the specified user to the list of users who have accepted the game.
     *
     * @param userId The unique identifier of the user who has accepted the game.
     * @return A boolean indicating whether the number of accepted users has reached the maximum required for the game
     * to start.
     */
    fun addAcceptedUser(userId: Int): Boolean {
        userIdsAcceptedTheGame.add(userId)
        return userIdsAcceptedTheGame.size == teamSize * 2
    }

    fun userAcceptedInvite(userId: Int): Boolean = userIdsAcceptedTheGame.contains(userId)

    fun inviteIsExpired(): Boolean = System.currentTimeMillis() > inviteExpireTime

    fun isFull() = firstTeam.size == teamSize && secondTeam.size == teamSize

    fun addToFirstTeam(player: QueuedUser) {
        firstTeam += player
        userIds.add(player.userId)
    }

    fun removeLastPlayerFromFirstTeam() {
        val removed = firstTeam.removeLast()
        userIds.remove(removed.userId)
    }

    fun addToSecondTeam(player: QueuedUser) {
        secondTeam += player
        userIds.add(player.userId)
    }

    fun removeLastPlayerFromSecondTeam() {
        val removed = secondTeam.removeLast()
        userIds.remove(removed.userId)
    }

    fun containsPlayer(userId: Int): Boolean = userIds.contains(userId)

    fun getAlUsers(): List<QueuedUser> = firstTeam + secondTeam

    fun averageMmrDiff(queuedUserById: Map<Int, QueuedUser>): Int {
        val firstAverage = firstTeam.map { player -> queuedUserById[player.userId]!!.mmr }.average()
        val secondAverage = secondTeam.map { player -> queuedUserById[player.userId]!!.mmr }.average()
        return (firstAverage - secondAverage).toInt().absoluteValue
    }

    fun getFirstTeamTotalTimeInQueue(queuedUserById: Map<Int, QueuedUser>, currentTime: Long): Long {
        return firstTeam.map { player -> queuedUserById[player.userId]!!.inQueueSince }.sumOf { currentTime - it }
    }

    fun getSecondTeamTotalTimeInQueue(queuedUserById: Map<Int, QueuedUser>, currentTime: Long): Long {
        return secondTeam.map { player -> queuedUserById[player.userId]!!.inQueueSince }.sumOf { currentTime - it }
    }

    fun copyFrom(room: Room) {
        this.firstTeam += room.firstTeam
        this.secondTeam += room.secondTeam
    }
}