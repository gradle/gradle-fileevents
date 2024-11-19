package org.gradle.fileevents.internal

import org.gradle.fileevents.testfixtures.TestFileEventFunctions
import spock.lang.Specification

import java.util.concurrent.LinkedBlockingDeque

import static java.util.concurrent.TimeUnit.MILLISECONDS
import static java.util.concurrent.TimeUnit.SECONDS

class SyntheticFileEventFunctionsTest extends Specification {
    def eventQueue = new LinkedBlockingDeque()

    def "normal termination produces termination event"() {
        def service = new TestFileEventFunctions()
        def watcher = service
            .newWatcher(eventQueue)
            .start()

        when:
        watcher.shutdown()
        watcher.awaitTermination(1, SECONDS)

        then:
        eventQueue*.toString() == ["TERMINATE"]
    }

    def "failure in run loop produces failure event followed by termination events"() {
        def service = new TestFileEventFunctions()
        def watcher = service
            .newWatcher(eventQueue)
            .start()

        when:
        watcher.injectFailureIntoRunLoop()
        watcher.awaitTermination(1, SECONDS)

        then:
        eventQueue*.toString() == ["FAILURE Error", "TERMINATE"]
    }

    def "can handle watcher start timing out"() {
        def service = new TestFileEventFunctions({
            Thread.sleep(200)
        })

        when:
        service.newWatcher(eventQueue).start(100, MILLISECONDS)

        then:
        def ex = thrown AbstractFileEventFunctions.FileWatcherTimeoutException
        ex.message == "Starting the watcher timed out"
    }
}
