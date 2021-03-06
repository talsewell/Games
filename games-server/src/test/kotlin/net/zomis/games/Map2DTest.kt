package net.zomis.games

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import kotlin.random.Random

class Map2DTest {

    @Test
    fun testFixed() {
        val map = Map2DX(3, 3) {x, y -> y * 3 + x}
        Assertions.assertEquals(Transformation.ROTATE_180, map.asMap2D().standardizedTransformation { it })
        assertMapValues(map.asMap2D()) {
            row(0, 1, 2)
            row(3, 4, 5)
            row(6, 7, 8)
        }

        map.asMap2D().transform(Transformation.ROTATE_90)
        assertMapValues(map.asMap2D()) {
            row(6, 3, 0)
            row(7, 4, 1)
            row(8, 5, 2)
        }
    }

    class MapAssertion<T>(val map: Map2D<T>) {
        var position: Position? = Position(0, 0, map.sizeX, map.sizeY)
        fun row(vararg values: T) {
            Assertions.assertEquals(map.sizeX, values.size)
            values.forEach {
                Assertions.assertNotNull(position)
                Assertions.assertEquals(it, map.getter(position!!.x, position!!.y))
                position = position?.next()
            }
        }
    }
    private fun <T> assertMapValues(map: Map2D<T>, assertions: MapAssertion<T>.() -> Unit) {
        val assertion = MapAssertion(map)
        assertions(assertion)
        Assertions.assertNull(assertion.position)
    }

    @Test
    fun transformationMapping() {
        val transformations = Transformation.values().toList()
        val rand = Random(43)
        val map = Map2DX(5, 5) {_, _ ->
            rand.nextInt(1000)
        }

        val combinations = transformations.flatMap { a -> transformations.map { b -> a to b } }
        combinations.forEach {
            (0 until map.sizeY).forEach {y ->
                (0 until map.sizeX).forEach {x ->
                    val pos = Position(x, y, map.sizeX, map.sizeY)
                    Assertions.assertEquals(it.second.transform(it.first.transform(pos)), it.first.apply(it.second).transform(pos))
                }
            }
        }
    }

}