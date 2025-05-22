package com.firelord.matchmaking.server.service

import com.firelord.matchmaking.proto.GameInvitation
import com.firelord.matchmaking.proto.GameInvitationResponse
import com.firelord.matchmaking.proto.JoinQueueRequest
import com.firelord.matchmaking.proto.LeaveQueueRequest
import com.firelord.matchmaking.server.model.QueuedUser
import com.firelord.matchmaking.server.model.QueuedUserFactory
import com.firelord.matchmaking.server.model.Room
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.time.Duration

class MatchmakingService(val teamBuilder: TeamBuilder) : GameService {
    private val incomingQueue = ConcurrentLinkedQueue<QueuedUser>()

    private val userChannels = ConcurrentHashMap<Int, Channel>()

    private val roomsLock = ReentrantReadWriteLock()
    private val rooms = LinkedHashMap<String, Room>()

    private val runMatchmakingScheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    private val removeExpiredRoomScheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

    private val batchSize = 20

    init {
        runMatchmakingScheduler.scheduleAtFixedRate(::runMatchmaking, 0, 1, TimeUnit.SECONDS)
        removeExpiredRoomScheduler.scheduleAtFixedRate(::removeExpiredRooms, 0, 100, TimeUnit.MILLISECONDS)
    }

    private fun runMatchmaking() {
        // todo: when we don't have batchSize people in the queue we might want to build a room from what we have
        if (incomingQueue.size >= batchSize) {
            val players = mutableListOf<QueuedUser>()
            for (i in 0 until batchSize) {
                val user = incomingQueue.poll() ?: break
                players.add(user)
            }

            if (players.size >= batchSize) {
                val room: Room = teamBuilder.buildBestConfiguration(players)
                val inviteId = System.currentTimeMillis().toString()

                roomsLock.write {
                    room.sendInvite(inviteId, Duration.parse("5s"))
                    rooms[inviteId] = room
                }

                sendInvitations(room, inviteId)

                val unmatched = players.filter { !room.containsPlayer(it.userId) }
                unmatched.reversed().forEach { incomingQueue.add(it) }
            }
        }
    }

    private fun sendInvitations(room: Room, inviteId: String) {
        room.getAlUsers().forEach { queuedUser ->
            val channel = userChannels[queuedUser.userId]
            if (channel != null && channel.isActive) {
                val invitation = GameInvitation.newBuilder()
                    .setInviteId(inviteId)
                    .setUserId(queuedUser.userId)
                    .build()

                channel.writeAndFlush(invitation)
                println("Sent invite to user ${queuedUser.userId} for room $inviteId")
            } else {
                println("Warning: Cannot send invite to user ${queuedUser.userId}, channel not found or inactive")
                if (channel != null && !channel.isActive) {
                    userChannels.remove(queuedUser.userId)
                }
            }
        }
    }

    private fun removeExpiredRooms() {
        val expiredRooms = findExpiredRooms()
        if (expiredRooms.isNotEmpty())
            roomsLock.write { removeRooms(expiredRooms) }
    }

    private fun findExpiredRooms(): MutableList<Pair<String, Room>> {
        val expiredRooms = mutableListOf<Pair<String, Room>>()

        roomsLock.read {
            val iterator = rooms.entries.iterator()
            var foundNonExpired = false

            while (iterator.hasNext() && !foundNonExpired) {
                val (inviteId, room) = iterator.next()

                if (room.inviteIsExpired())
                    expiredRooms.add(inviteId to room)
                else
                    foundNonExpired = true
            }
        }
        return expiredRooms
    }

    private fun removeRooms(expiredRooms: MutableList<Pair<String, Room>>) {
        expiredRooms.forEach { (inviteId, room) ->
            rooms.remove(inviteId)

            // Re-queue users
            room.getAlUsers().forEach { queuedUser ->
                if (room.userAcceptedInvite(queuedUser.userId)) {
                    incomingQueue.add(queuedUser)
                    println("User ${queuedUser.userId} from expired room $inviteId returned to queue")
                }
            }
        }
    }

    override fun joinQueue(ctx: ChannelHandlerContext, payload: JoinQueueRequest) {
        userChannels[payload.userId] = ctx.channel()
        incomingQueue.add(QueuedUserFactory.create(payload))
    }

    override fun leaveQueue(ctx: ChannelHandlerContext, payload: LeaveQueueRequest) {
        userChannels.remove(payload.userId)
        incomingQueue.removeIf { it.userId == payload.userId }
    }

    override fun receiveAcceptResponse(ctx: ChannelHandlerContext, response: GameInvitationResponse) {
        var inviteIdToRemove: String? = null

        roomsLock.read {
            rooms[response.inviteId]?.let { room ->
                if (response.accepted && room.addAcceptedUser(response.userId)) {
                    inviteIdToRemove = response.inviteId
                    val gameId = UUID.randomUUID().toString()
                    room.startGame(gameId)
                    println("Game $gameId is started. Room => $room")
                    room.getAlUsers().map { it.userId }.forEach { userChannels.remove(it) }
                }
            }
        }

        if (inviteIdToRemove != null) {
            roomsLock.write { rooms.remove(inviteIdToRemove) }
        }
    }

    /**
     * Shuts down the schedulers, allowing the service to be cleanly terminated.
     */
    fun shutdown() {
        runMatchmakingScheduler.shutdown()
        removeExpiredRoomScheduler.shutdown()

        try {
            if (!runMatchmakingScheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                runMatchmakingScheduler.shutdownNow()
            }
            if (!removeExpiredRoomScheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                removeExpiredRoomScheduler.shutdownNow()
            }
        } catch (_: InterruptedException) {
            runMatchmakingScheduler.shutdownNow()
            removeExpiredRoomScheduler.shutdownNow()
            Thread.currentThread().interrupt()
        }
    }
}