package com.firelord.matchmaking.server

import com.firelord.matchmaking.proto.Role
import com.firelord.matchmaking.server.model.QueuedUser
import com.firelord.matchmaking.server.model.Room
import com.firelord.matchmaking.server.service.RoomEvaluator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RoomEvaluatorTest {
    private lateinit var roomEvaluator: RoomEvaluator
    private lateinit var queuedUsers: Map<Int, QueuedUser>

    @BeforeEach
    fun setUp() {
        // Configure evaluator with reasonable weights
        roomEvaluator = RoomEvaluator(oneSecondWeight = 5, countOfDesiredRolesPerUser = 3)

        // Create test data for queued users
        queuedUsers = mapOf(
            1 to QueuedUser(1, listOf(Role.Top, Role.Mid, Role.Jungle), 1000),
            2 to QueuedUser(2, listOf(Role.Mid, Role.Top, Role.Jungle), 1200),
            3 to QueuedUser(3, listOf(Role.Jungle, Role.Support, Role.Carry), 1100),
            4 to QueuedUser(4, listOf(Role.Support, Role.Jungle, Role.Carry), 900),
            5 to QueuedUser(5, listOf(Role.Carry, Role.Mid, Role.Top), 1300),
            6 to QueuedUser(6, listOf(Role.Top, Role.Jungle, Role.Mid), 1050),
            7 to QueuedUser(7, listOf(Role.Mid, Role.Carry, Role.Top), 1150),
            8 to QueuedUser(8, listOf(Role.Jungle, Role.Top, Role.Mid), 1250),
            9 to QueuedUser(9, listOf(Role.Support, Role.Carry, Role.Jungle), 950),
            10 to QueuedUser(10, listOf(Role.Carry, Role.Support, Role.Mid), 1350)
        )
    }

    @Test
    fun `evaluate with perfect balance and all first preferences`() {
        // Create a room with perfect MMR balance and all players getting their first choice
        val room = Room().apply {
            // First team - Total MMR: 5500
            addToFirstTeam(Player(1, Role.Top))     // 1000 MMR, 1st preference
            addToFirstTeam(Player(2, Role.Mid))     // 1200 MMR, 1st preference
            addToFirstTeam(Player(3, Role.Jungle))  // 1100 MMR, 1st preference
            addToFirstTeam(Player(4, Role.Support)) // 900 MMR, 1st preference
            addToFirstTeam(Player(5, Role.Carry))     // 1300 MMR, 1st preference

            // Second team - Total MMR: 5500
            addToSecondTeam(Player(6, Role.Top))     // 1050 MMR, 1st preference
            addToSecondTeam(Player(7, Role.Mid))     // 1150 MMR, 1st preference
            addToSecondTeam(Player(8, Role.Jungle))  // 1250 MMR, 1st preference
            addToSecondTeam(Player(9, Role.Support)) // 950 MMR, 1st preference
            addToSecondTeam(Player(10, Role.Carry))    // 1350 MMR, 1st preference
        }

        val totalQueueTime = 60 // 1 minute
        val mmrDiff = room.averageMmrDiff(queuedUsers) // Should be 0

        // Expected: (queue time * weight) - mmrDiff + role preference score
        // Role preference score: 10 players * (3 - 0) = 30 (all got 1st preference)
        val expected = (totalQueueTime * 5) - mmrDiff + 30

        val actual = roomEvaluator.evaluate(mmrDiff, totalQueueTime, room, queuedUsers)
        assertEquals(expected, actual)
    }

    @Test
    fun `evaluate with MMR difference and some second preferences`() {
        // Create a room with some MMR difference and mixed preferences
        val room = Room().apply {
            // First team - Higher MMR
            addToFirstTeam(Player(1, Role.Top))     // 1000 MMR, 1st preference
            addToFirstTeam(Player(2, Role.Top))     // 1200 MMR, 2nd preference (wanted MID)
            addToFirstTeam(Player(3, Role.Jungle))  // 1100 MMR, 1st preference
            addToFirstTeam(Player(4, Role.Support)) // 900 MMR, 1st preference
            addToFirstTeam(Player(5, Role.Mid))     // 1300 MMR, 2nd preference (wanted BOT)

            // Second team - Lower MMR
            addToSecondTeam(Player(6, Role.Mid))     // 1050 MMR, 3rd preference (wanted TOP)
            addToSecondTeam(Player(7, Role.Carry))     // 1150 MMR, 2nd preference (wanted MID)
            addToSecondTeam(Player(8, Role.Jungle))  // 1250 MMR, 1st preference
            addToSecondTeam(Player(9, Role.Support)) // 950 MMR, 1st preference
            addToSecondTeam(Player(10, Role.Carry))    // 1350 MMR, 1st preference
        }

        val totalQueueTime = 120 // 2 minutes
        val mmrDiff = room.averageMmrDiff(queuedUsers)

        // Calculate role preference score manually
        val roleScore = (3 - 0) + // Player 1: 1st preference
                (3 - 1) + // Player 2: 2nd preference
                (3 - 0) + // Player 3: 1st preference
                (3 - 0) + // Player 4: 1st preference
                (3 - 1) + // Player 5: 2nd preference
                (3 - 2) + // Player 6: 3rd preference
                (3 - 1) + // Player 7: 2nd preference
                (3 - 0) + // Player 8: 1st preference
                (3 - 0) + // Player 9: 1st preference
                (3 - 0)   // Player 10: 1st preference

        val expected = (totalQueueTime * 5) - mmrDiff + roleScore

        val actual = roomEvaluator.evaluate(mmrDiff, totalQueueTime, room, queuedUsers)
        assertEquals(expected, actual)
    }

    @Test
    fun `evaluate with long queue time compensating for imperfect match`() {
        // Create a room with significant MMR difference but long queue time
        val room = Room().apply {
            // First team - Much higher MMR
            addToFirstTeam(Player(2, Role.Mid))     // 1200 MMR, 1st preference
            addToFirstTeam(Player(5, Role.Carry))     // 1300 MMR, 1st preference
            addToFirstTeam(Player(7, Role.Top))     // 1150 MMR, 3rd preference (wanted MID)
            addToFirstTeam(Player(8, Role.Jungle))  // 1250 MMR, 1st preference
            addToFirstTeam(Player(10, Role.Support)) // 1350 MMR, 2nd preference (wanted BOT)

            // Second team - Much lower MMR
            addToSecondTeam(Player(1, Role.Top))     // 1000 MMR, 1st preference
            addToSecondTeam(Player(3, Role.Mid))     // 1100 MMR, 3rd preference (wanted JUNGLE)
            addToSecondTeam(Player(4, Role.Support)) // 900 MMR, 1st preference
            addToSecondTeam(Player(6, Role.Jungle))  // 1050 MMR, 2nd preference (wanted TOP)
            addToSecondTeam(Player(9, Role.Carry))     // 950 MMR, 2nd preference (wanted SUPPORT)
        }

        val totalQueueTime = 300 // 5 minutes
        val mmrDiff = room.averageMmrDiff(queuedUsers)

        // Calculate role preference score manually
        val roleScore = (3 - 0) + // Player 2: 1st preference
                (3 - 0) + // Player 5: 1st preference
                (3 - 2) + // Player 7: 3rd preference
                (3 - 0) + // Player 8: 1st preference
                (3 - 1) + // Player 10: 2nd preference
                (3 - 0) + // Player 1: 1st preference
                (3 - 3) + // Player 3: Not in preferences
                (3 - 0) + // Player 4: 1st preference
                (3 - 1) + // Player 6: 2nd preference
                (3 - 1)   // Player 9: 2nd preference

        val expected = (totalQueueTime * 5) - mmrDiff + roleScore

        val actual = roomEvaluator.evaluate(mmrDiff, totalQueueTime, room, queuedUsers)
        assertEquals(expected, actual)
    }

    @Test
    fun `evaluate with some players getting roles not in their preferences`() {
        // Create a room where some players get roles they didn't ask for
        val room = Room().apply {
            // First team
            addToFirstTeam(Player(1, Role.Carry))     // Not in preferences
            addToFirstTeam(Player(2, Role.Mid))     // 1st preference
            addToFirstTeam(Player(3, Role.Jungle))  // 1st preference
            addToFirstTeam(Player(4, Role.Support)) // 1st preference
            addToFirstTeam(Player(5, Role.Top))     // Not in preferences

            // Second team
            addToSecondTeam(Player(6, Role.Top))     // 1st preference
            addToSecondTeam(Player(7, Role.Mid))     // 1st preference
            addToSecondTeam(Player(8, Role.Support)) // Not in preferences
            addToSecondTeam(Player(9, Role.Jungle))  // 3rd preference
            addToSecondTeam(Player(10, Role.Carry))    // 1st preference
        }

        val totalQueueTime = 180 // 3 minutes
        val mmrDiff = room.averageMmrDiff(queuedUsers)

        // Calculate role preference score manually
        val roleScore = 0 + // Player 1: Role not in preferences
                (3 - 0) + // Player 2: 1st preference
                (3 - 0) + // Player 3: 1st preference
                (3 - 0) + // Player 4: 1st preference
                (3 - 2) + // Player 5: 3rd preference
                (3 - 0) + // Player 6: 1st preference
                (3 - 0) + // Player 7: 1st preference
                0 + // Player 8: Role not in preferences
                (3 - 2) + // Player 9: 3rd preference
                (3 - 0)   // Player 10: 1st preference

        val expected = (totalQueueTime * 5) - mmrDiff + roleScore

        val actual = roomEvaluator.evaluate(mmrDiff, totalQueueTime, room, queuedUsers)
        assertEquals(expected, actual)
    }

    @Test
    fun `evaluate with extreme values`() {
        // Test with very high MMR difference and long queue time
        val room = Room().apply {
            // First team - Very high MMR
            addToFirstTeam(Player(2, Role.Top))     // 1200 MMR
            addToFirstTeam(Player(5, Role.Mid))     // 1300 MMR
            addToFirstTeam(Player(7, Role.Jungle))  // 1150 MMR
            addToFirstTeam(Player(8, Role.Support)) // 1250 MMR
            addToFirstTeam(Player(10, Role.Carry))    // 1350 MMR

            // Second team - Very low MMR
            addToSecondTeam(Player(1, Role.Top))     // 1000 MMR
            addToSecondTeam(Player(3, Role.Mid))     // 1100 MMR
            addToSecondTeam(Player(4, Role.Jungle))  // 900 MMR
            addToSecondTeam(Player(6, Role.Support)) // 1050 MMR
            addToSecondTeam(Player(9, Role.Carry))     // 950 MMR
        }

        val totalQueueTime = 600 // 10 minutes
        val mmrDiff = 300 // Manually setting a high MMR difference

        // All players get roles in their preferences, calculate score
        val roleScore = (3 - 1) + // Player 2: 2nd preference
                (3 - 1) + // Player 5: 2nd preference
                0 +        // Player 7: Role not in preferences
                0 +        // Player 8: Role not in preferences
                (3 - 0) +  // Player 10: 1st preference
                (3 - 0) +  // Player 1: 1st preference
                0 +        // Player 3: Role not in preferences
                (3 - 1) +  // Player 4: 2nd preference
                0 +        // Player 6: Role not in preferences
                (3 - 1)    // Player 9: 2nd preference

        val expected = (totalQueueTime * 5) - mmrDiff + roleScore

        val actual = roomEvaluator.evaluate(mmrDiff, totalQueueTime, room, queuedUsers)
        assertEquals(expected, actual)
    }
}