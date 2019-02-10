package net.zomis.games.server2.games

import klogging.KLoggers
import net.zomis.core.events.EventSystem
import net.zomis.games.Features
import net.zomis.games.server2.*

class SimpleMatchMakingSystem {

    private val logger = KLoggers.logger(this)
    private val waiting: MutableMap<String, Client> = mutableMapOf()

    fun setup(features: Features, events: EventSystem) {
        val gameTypes = features[GameSystem.GameTypes::class]!!.gameTypes
        events.listen("Simple matchmaking", ClientJsonMessage::class, {
            it.data.getTextOrDefault("type", "") == "matchMake"
        }, {
                val gameType = it.data.get("game").asText()
                if (gameTypes[gameType] == null) {
                    logger.warn { "Received unknown gametype: $gameType" }
                    return@listen
                }

                synchronized(waiting, {
                if (waiting.containsKey(gameType)) {
                    val opponent = waiting[gameType]!!
                    logger.info { "Pair up $gameType: Waiting $opponent now joining ${it.client}" }

                    val game = gameTypes[gameType]!!.createGame()
                    game.players.addAll(listOf(opponent, it.client))
                    events.execute(GameStartedEvent(game))
                    waiting.remove(gameType)
                } else {
                    waiting[gameType] = it.client
                    logger.info { "Now waiting for a match to play $gameType: ${it.client}" }
                }
                })
        })
    }

}