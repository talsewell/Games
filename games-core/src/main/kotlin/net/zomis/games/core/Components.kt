package net.zomis.games.core

open class Component
open class FixedComponent: Component()
open class DataComponent: Component()
open class ContainerComponent: Component()
open class DynamicComponent: Component()

open class LogicComponent: Component()
class Targetable: LogicComponent()
class Actionable: LogicComponent()
data class ActionEvent(val actionable: Entity, val initiatedBy: Entity)


data class EntityComponent<out T: Component>(val entity: Entity, val component: T)


data class Tile(val x: Int, val y: Int): FixedComponent()
data class Container2D(val container: List<List<Entity>>): ContainerComponent()

data class Player(val index: Int, var winner: Boolean?, var resultPosition: Int?): DataComponent()
data class OwnedByPlayer(var owner: Player?): DataComponent()
data class Players(val players: List<Entity>): ContainerComponent()
data class PlayerTurn(var currentPlayer: Player): DataComponent()
