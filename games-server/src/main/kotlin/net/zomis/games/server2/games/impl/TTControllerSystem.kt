package net.zomis.games.server2.games.impl

import com.fasterxml.jackson.databind.node.ObjectNode
import klogging.KLoggers
import net.zomis.core.events.EventSystem
import net.zomis.games.server2.StartupEvent
import net.zomis.games.server2.clients.ur.getInt
import net.zomis.games.server2.games.*
import net.zomis.tttultimate.games.TTController

class TTControllerSystem(val gameType: String, private val controller: () -> TTController) {

    private val logger = KLoggers.logger(this)

    fun register(events: EventSystem) {
        events.listen("move in $gameType", PlayerGameMoveRequest::class, {
            it.game.gameType.type == gameType
        }, {
            if (it.game.obj == null) {
                it.game.obj = controller.invoke()
            }
            val x = (it.move as ObjectNode).getInt("x")
            val y = it.move.getInt("y")

            val controller = it.game.obj as TTController
            if (controller.isGameOver) {
                events.execute(it.illegalMove("Game already won by ${controller.wonBy}"))
                return@listen
            }
            if (controller.currentPlayer.playerIndex() != it.player) {
                events.execute(it.illegalMove("Not your turn"))
                return@listen
            }

            val playAt = controller.game.getSmallestTile(x, y)

            if (playAt != null && controller.play(playAt)) {
                logger.info { "${it.game} Player ${it.player} played at $x ${playAt.y}" }
                events.execute(MoveEvent(it.game, it.player, "move", mapOf("x" to x, "y" to y)))
            } else {
                events.execute(it.illegalMove("Not allowed to play there"))
            }

            if (controller.isGameOver) {
                val winner = controller.wonBy
                it.game.players.indices.forEach({ playerIndex ->
                    val won = winner.playerIndex() == playerIndex
                    val losePositionPenalty = if (won) 0 else 1
                    events.execute(PlayerEliminatedEvent(it.game, playerIndex, won, 1 + losePositionPenalty))
                })
                events.execute(GameEndedEvent(it.game))
            }
        })
        events.listen("register $gameType", StartupEvent::class, {true}, {
            events.execute(GameTypeRegisterEvent(gameType))
        })
    }

}