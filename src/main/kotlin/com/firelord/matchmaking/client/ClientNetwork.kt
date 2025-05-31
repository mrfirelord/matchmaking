package com.firelord.matchmaking.client

import com.firelord.matchmaking.proto.Role

object ClientNetwork {
    @JvmStatic
    fun main(args: Array<String>) {
//        val allRoles = Role.entries.filter { it != Role.UNRECOGNIZED }.toList()
//
//        var userId = 0
//        val numberOfTeams = 2
//        repeat(numberOfTeams) {
//            for (roleIndex in 0 until allRoles.size) {
//                val client = FireClient(userId++)
//                client.connect()
//
//                val mmr = 2000
//                val firstRole = allRoles[roleIndex]
//                val secondRole = allRoles[(roleIndex + 1) % allRoles.size]
//                client.joinQueue(mmr, listOf(firstRole, secondRole))
//            }
//        }

        println(factorial(20) / (factorial(10) * factorial(10)))

        println("Press enter to terminate")
        readLine()
    }

    fun factorial(n: Int): Long {
        var result = 1L
        for (i in 1..n)
            result *= i

        return result
    }
}