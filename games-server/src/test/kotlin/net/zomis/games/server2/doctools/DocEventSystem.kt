package net.zomis.games.server2.doctools

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import net.zomis.core.events.EventSystem
import net.zomis.games.server2.ClientJsonMessage
import net.zomis.games.server2.clients.FakeClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.extension.*
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.util.*
import kotlin.concurrent.getOrSet
import kotlin.reflect.KClass

class DocBlock(val events: EventSystem, val printer: PrintWriter) {

    private val mapper = ObjectMapper()

    fun send(client: FakeClient, documentation: String, message: String) {
        val data = mapper.readTree(message)
        printer.append("$documentation\n\n    $message\n\n")
        events.execute(ClientJsonMessage(client, data))
    }

    fun send(client: FakeClient, message: String) {
        this.send(client, "${client.name} sends:", message)
    }

    fun receive(client: FakeClient, expected: String) {
        assertEquals(expected, client.nextMessage())
        printer.append("${client.name} will receive:\n\n    $expected\n\n")
    }

    fun receive(client: FakeClient, expected: Map<String, Any?>) {
        val actual = client.nextMessage()
        val documentationExcpected = expected.plus("..." to "...")
        val actualMap: Map<String, Any?> = mapper.readValue(actual)
        val match = expected.all { actualMap[it.key] == it.value }

        assertTrue(match) { "Expected\n$documentationExcpected\nbut was\n$actualMap" }
        printer.append("${client.name} will receive:\n\n    ${mapper.writeValueAsString(documentationExcpected)}\n\n")
    }

    fun text(text: String) {
        printer.append("$text\n\n")
    }

}

class DocWriter(val filename: String, val docFile: String = "UNDEFINED") : AfterEachCallback, BeforeEachCallback {

    override fun afterEach(context: ExtensionContext?) {
        file.close()
    }

    override fun beforeEach(context: ExtensionContext?) {
        file = PrintWriter(FileWriter(File(filename), true))
    }

    private lateinit var file : PrintWriter

    fun writeConnection(cause: KClass<*>, effect: KClass<*>) {
        file.append("- ${cause.qualifiedName} --> ${effect.qualifiedName}\n")
    }

    fun document(events: EventSystem, header: String, block: DocBlock.() -> Unit) {
        val printer = PrintWriter(FileWriter(File("../documentation/$docFile.md"), true))
        printer.use {
            it.append("### $header\n\n")

            val docBlock = DocBlock(events, printer)
            block.invoke(docBlock)
        }
    }

}

class DocEventThread(val writer: DocWriter) {

    val events = LinkedList<KClass<*>>()

    fun beginExecuting(clazz: KClass<*>) {
        if (events.isEmpty()) {
            events.addLast(clazz)
            return
        }
        val mostRecent = events.peekLast()
        writer.writeConnection(mostRecent, clazz)
        events.addLast(clazz)
    }

    fun finishExecuting() {
        events.removeLast()
    }

}

class DocEventSystem(val docWriter: DocWriter) : EventSystem() {

    private val eventsOnThreads = ThreadLocal<DocEventThread>()

    override fun <E : Any> execute(event: E): E {
        beforeExecute(event)
        super.execute(event)
        afterExecute(event)
        return event
    }

    private fun <E> afterExecute(event: E) {
        current().finishExecuting()
    }

    private fun current(): DocEventThread {
        return eventsOnThreads.getOrSet { DocEventThread(docWriter) }
    }

    private fun <E : Any> beforeExecute(event: E) {
        current().beginExecuting(event::class)
    }

    // Use a ThreadLocal
    // Before event: Check number of occurrences of "execute" in stacktrace
    // If there are more than 1, then this event got triggered by the previous event
    // Write "StartupEvent->GameTypeRegisteredEvent" to a file
    // TODO: Also write the description of the listener that caused the event

    // At the end, parse this file and create a graph of how events are connected


    // Before event: Check Client messages?
    // After event: Check Client messages?

}
