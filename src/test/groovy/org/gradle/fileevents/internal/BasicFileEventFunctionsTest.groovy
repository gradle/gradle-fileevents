/*
 * Copyright 2012 Adam Murdoch
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.gradle.fileevents.internal

import net.rubygrapefruit.platform.NativeException
import net.rubygrapefruit.platform.internal.Platform
import org.gradle.fileevents.FileWatchEvent
import org.junit.jupiter.api.Assumptions
import org.slf4j.LoggerFactory
import spock.lang.IgnoreIf
import spock.lang.Requires
import spock.lang.Unroll

import java.util.concurrent.BlockingQueue
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

import static org.gradle.fileevents.FileWatchEvent.ChangeType.CREATED
import static org.gradle.fileevents.FileWatchEvent.ChangeType.MODIFIED
import static org.gradle.fileevents.FileWatchEvent.ChangeType.REMOVED
import static org.gradle.fileevents.internal.AbstractFileEventFunctionsTest.PlatformType.OTHERWISE
import static org.gradle.fileevents.internal.AbstractFileEventFunctionsTest.PlatformType.WINDOWS
import static org.slf4j.event.Level.ERROR
import static org.slf4j.event.Level.INFO
import static org.slf4j.event.Level.TRACE

@Unroll
@Requires({ Platform.current().macOs || Platform.current().linux || Platform.current().windows })
class BasicFileEventFunctionsTest extends AbstractFileEventFunctionsTest {
    def "can start and shutdown watcher without watching any paths"() {
        when:
        def watcher = startNewWatcher()

        then:
        noExceptionThrown()

        when:
        shutdownWatcher(watcher)

        then:
        expectEvents termination()
    }

    def "can start and shutdown watcher on a directory without receiving any events"() {
        when:
        def watcher = startNewWatcher(rootDir)

        then:
        noExceptionThrown()

        when:
        shutdownWatcher(watcher)

        then:
        expectEvents termination()
    }

    def "can detect file created"() {
        given:
        def createdFile = new File(rootDir, "created.txt")
        startWatcher(rootDir)

        when:
        createNewFile(createdFile)

        then:
        expectEvents change(CREATED, createdFile)
    }

    @IgnoreIf({ Platform.current().linux })
    def "can detect file created in subdirectory"() {
        given:
        def subDir = new File(rootDir, "sub-dir")
        subDir.mkdirs()
        def createdFile = new File(subDir, "created.txt")
        startWatcher(rootDir)

        when:
        createNewFile(createdFile)

        then:
        expectEvents change(CREATED, createdFile)
    }

    def "can detect directory created"() {
        given:
        def createdDir = new File(rootDir, "created")
        startWatcher(rootDir)

        when:
        assert createdDir.mkdirs()

        then:
        expectEvents change(CREATED, createdDir)
    }

    @IgnoreIf({ Platform.current().linux })
    def "can detect directory created in subdirectory"() {
        given:
        def subDir = new File(rootDir, "sub-dir")
        subDir.mkdirs()
        def createdDir = new File(subDir, "created")
        startWatcher(rootDir)

        when:
        assert createdDir.mkdirs()

        then:
        expectEvents change(CREATED, createdDir)
    }

    def "can detect file removed"() {
        given:
        def removedFile = new File(rootDir, "removed.txt")
        createNewFile(removedFile)
        startWatcher(rootDir)

        when:
        removedFile.delete()

        then:
        // Windows reports the file as modified before removing it
        expectEvents byPlatform(
            (WINDOWS): [change(MODIFIED, removedFile), change(REMOVED, removedFile)],
            (OTHERWISE): [change(REMOVED, removedFile)]
        )
    }

    @IgnoreIf({ Platform.current().linux })
    def "can detect file removed in subdirectory"() {
        given:
        def subDir = new File(rootDir, "sub-dir")
        subDir.mkdirs()
        def removedFile = new File(subDir, "removed.txt")
        createNewFile(removedFile)
        startWatcher(rootDir)

        when:
        removedFile.delete()

        then:
        // Windows reports the file as modified before removing it
        expectEvents byPlatform(
            (WINDOWS): [change(MODIFIED, removedFile), change(REMOVED, removedFile)],
            (OTHERWISE): [change(REMOVED, removedFile)]
        )
    }

    def "can detect directory removed"() {
        given:
        def removedDir = new File(rootDir, "removed")
        assert removedDir.mkdirs()
        startWatcher(rootDir)

        when:
        removedDir.deleteDir()

        then:
        expectEvents change(REMOVED, removedDir)
    }

    @IgnoreIf({ Platform.current().linux })
    def "can detect directory removed in subdirectory"() {
        given:
        def subDir = new File(rootDir, "sub-dir")
        subDir.mkdirs()
        def removedDir = new File(subDir, "removed")
        assert removedDir.mkdirs()
        startWatcher(rootDir)

        when:
        removedDir.deleteDir()

        then:
        expectEvents change(REMOVED, removedDir)
    }

    @IgnoreIf({ Platform.current().linux })
    def "can detect hierarchy removed"() {
        given:
        def removedDir = new File(rootDir, "removed")
        assert removedDir.mkdirs()
        def removedSubDir = new File(removedDir, "sub-dir")
        assert removedSubDir.mkdirs()
        def removedSubSubDir = new File(removedSubDir, "sub-sub-dir")
        assert removedSubSubDir.mkdirs()
        def removedFile = new File(removedSubSubDir, "file.txt")
        createNewFile(removedFile)
        startWatcher(rootDir)

        when:
        removedDir.deleteDir()

        then:
        expectEvents byPlatform(
            (WINDOWS): [change(MODIFIED, removedFile), change(REMOVED, removedFile), change(REMOVED, removedSubSubDir), change(REMOVED, removedSubDir), change(REMOVED, removedDir)],
            (OTHERWISE): [change(REMOVED, removedFile), change(REMOVED, removedSubSubDir), change(REMOVED, removedSubDir), change(REMOVED, removedDir)]
        )
    }

    def "can detect file modified"() {
        given:
        def modifiedFile = new File(rootDir, "modified.txt")
        createNewFile(modifiedFile)
        startWatcher(rootDir)

        when:
        modifiedFile << "change"

        then:
        expectEvents change(MODIFIED, modifiedFile)
    }

    @IgnoreIf({ Platform.current().linux })
    def "can detect file modified in subdirectory"() {
        given:
        def subDir = new File(rootDir, "sub-dir")
        subDir.mkdirs()
        def modifiedFile = new File(subDir, "modified.txt")
        createNewFile(modifiedFile)
        startWatcher(rootDir)

        when:
        modifiedFile << "change"

        then:
        expectEvents change(MODIFIED, modifiedFile)
    }

    @Requires({ Platform.current().macOs })
    def "can detect file metadata modified"() {
        given:
        def modifiedFile = new File(rootDir, "modified.txt")
        createNewFile(modifiedFile)
        startWatcher(rootDir)

        when:
        modifiedFile.setReadable(false)

        then:
        expectEvents change(MODIFIED, modifiedFile)

        when:
        modifiedFile.setReadable(true)

        then:
        expectEvents change(MODIFIED, modifiedFile)
    }

    @Requires({ Platform.current().macOs })
    def "changing metadata immediately after creation is reported as modified"() {
        given:
        def createdFile = new File(rootDir, "file.txt")
        startWatcher(rootDir)

        when:
        createNewFile(createdFile)
        createdFile.setReadable(false)

        then:
        expectEvents change(MODIFIED, createdFile)
    }

    @Requires({ Platform.current().macOs })
    def "changing metadata doesn't mask content change"() {
        given:
        def modifiedFile = new File(rootDir, "modified.txt")
        modifiedFile.createNewFile()
        startWatcher(rootDir)

        when:
        modifiedFile.setReadable(false)
        modifiedFile << "change"

        then:
        expectEvents change(MODIFIED, modifiedFile)
    }

    @Requires({ Platform.current().macOs })
    def "changing metadata doesn't mask removal"() {
        given:
        def removedFile = new File(rootDir, "removed.txt")
        createNewFile(removedFile)
        startWatcher(rootDir)

        when:
        removedFile.setReadable(false)
        assert removedFile.delete()

        then:
        expectEvents change(REMOVED, removedFile)
    }

    def "can detect file renamed"() {
        given:
        def sourceFile = new File(rootDir, "source.txt")
        def targetFile = new File(rootDir, "target.txt")
        createNewFile(sourceFile)
        startWatcher(rootDir)

        when:
        sourceFile.renameTo(targetFile)

        then:
        expectEvents change(REMOVED, sourceFile), change(CREATED, targetFile)
    }

    @IgnoreIf({ Platform.current().linux })
    def "can detect file renamed in subdirectory"() {
        given:
        def subDir = new File(rootDir, "sub-dir")
        subDir.mkdirs()
        def sourceFile = new File(subDir, "source.txt")
        def targetFile = new File(subDir, "target.txt")
        createNewFile(sourceFile)
        startWatcher(rootDir)

        when:
        sourceFile.renameTo(targetFile)

        then:
        expectEvents change(REMOVED, sourceFile), change(CREATED, targetFile)
    }

    def "can detect file moved out"() {
        given:
        def outsideDir = new File(testDir, "outside")
        assert outsideDir.mkdirs()
        def sourceFileInside = new File(rootDir, "source-inside.txt")
        def targetFileOutside = new File(outsideDir, "target-outside.txt")
        createNewFile(sourceFileInside)
        startWatcher(rootDir)

        when:
        sourceFileInside.renameTo(targetFileOutside)

        then:
        expectEvents change(REMOVED, sourceFileInside)
    }

    def "can detect file moved in"() {
        given:
        def outsideDir = new File(testDir, "outside")
        assert outsideDir.mkdirs()
        def sourceFileOutside = new File(outsideDir, "source-outside.txt")
        def targetFileInside = new File(rootDir, "target-inside.txt")
        createNewFile(sourceFileOutside)
        startWatcher(rootDir)

        when:
        sourceFileOutside.renameTo(targetFileInside)

        then:
        expectEvents change(CREATED, targetFileInside)
    }

    def "can receive multiple events from the same directory"() {
        given:
        def firstFile = new File(rootDir, "first.txt")
        def secondFile = new File(rootDir, "second.txt")
        startWatcher(rootDir)

        when:
        createNewFile(firstFile)

        then:
        expectEvents change(CREATED, firstFile)

        when:
        waitForChangeEventLatency()
        createNewFile(secondFile)

        then:
        expectEvents change(CREATED, secondFile)
    }

    def "does not receive events from unwatched directory"() {
        given:
        def watchedFile = new File(rootDir, "watched.txt")
        def unwatchedDir = new File(testDir, "unwatched")
        assert unwatchedDir.mkdirs()
        def unwatchedFile = new File(unwatchedDir, "unwatched.txt")
        startWatcher(rootDir)

        when:
        createNewFile(unwatchedFile)
        createNewFile(watchedFile)
        // Let's make sure there are no events for the unwatched file,
        // and we don't just miss them because of timing
        waitForChangeEventLatency()

        then:
        expectEvents change(CREATED, watchedFile)
    }

    // Apparently on macOS we can watch non-existent directories
    // TODO Should we fail for this?
    @IgnoreIf({ Platform.current().macOs })
    def "fails when watching non-existent directory"() {
        given:
        def missingDirectory = new File(rootDir, "missing")

        when:
        startWatcher(missingDirectory)

        then:
        def ex = thrown NativeException
        ex.message ==~ /Couldn't add watch.*: ${Pattern.quote(missingDirectory.absolutePath)}/

        expectLogMessage(ERROR, Pattern.compile("Caught exception: Couldn't add watch.*: ${Pattern.quote(missingDirectory.absolutePath)}"))
    }

    // Apparently on macOS and Windows we can watch files
    @Requires({ Platform.current().linux })
    def "fails when watching file"() {
        given:
        def file = new File(rootDir, "file.txt")
        assert file.createNewFile()

        when:
        startWatcher(file)

        then:
        def ex = thrown NativeException
        ex.message ==~ /Couldn't add watch.*: ${Pattern.quote(file.absolutePath)}/

        expectLogMessage(ERROR, Pattern.compile("Caught exception: Couldn't add watch.*: ${Pattern.quote(file.absolutePath)}"))
    }

    def "fails when watching directory twice"() {
        given:
        startWatcher(rootDir)

        when:
        startWatching(rootDir)

        then:
        def ex = thrown NativeException
        ex.message == "Already watching path: ${rootDir.absolutePath}"

        expectLogMessage(ERROR, "Caught exception: Already watching path: ${rootDir.absolutePath}")
    }

    def "can un-watch path that was not watched"() {
        given:
        startWatcher()

        expect:
        !stopWatching(rootDir)

        expectLogMessage(INFO, "Path is not watched: ${rootDir.absolutePath}")
    }

    def "can un-watch watched directory twice"() {
        given:
        startWatcher(rootDir)

        expect:
        stopWatching(rootDir)
        !stopWatching(rootDir)

        expectLogMessage(INFO, "Path is not watched: ${rootDir.absolutePath}")
    }

    def "does not receive events after directory is unwatched"() {
        given:
        def file = new File(rootDir, "first.txt")
        startWatcher(rootDir)

        expect:
        stopWatching(rootDir)

        when:
        createNewFile(file)

        then:
        expectNoEvents()
    }

    def "can receive multiple events from multiple watched directories"() {
        given:
        def firstWatchedDir = new File(testDir, "first")
        assert firstWatchedDir.mkdirs()
        def firstFileInFirstWatchedDir = new File(firstWatchedDir, "first-watched.txt")
        def secondWatchedDir = new File(testDir, "second")
        assert secondWatchedDir.mkdirs()
        def secondFileInSecondWatchedDir = new File(secondWatchedDir, "sibling-watched.txt")
        startWatcher(firstWatchedDir, secondWatchedDir)

        when:
        createNewFile(firstFileInFirstWatchedDir)

        then:
        expectEvents change(CREATED, firstFileInFirstWatchedDir)

        when:
        createNewFile(secondFileInSecondWatchedDir)

        then:
        expectEvents change(CREATED, secondFileInSecondWatchedDir)
    }

    @Requires({ !Platform.current().linux })
    def "can receive events from directory with different casing"() {
        given:
        def lowercaseDir = new File(rootDir, "watch-this")
        def uppercaseDir = new File(rootDir, "WATCH-THIS")
        def fileInLowercaseDir = new File(lowercaseDir, "lowercase.txt")
        def fileInUppercaseDir = new File(uppercaseDir, "UPPERCASE.TXT")
        uppercaseDir.mkdirs()

        def reportedDir = Platform.current().macOs
            ? uppercaseDir
            : lowercaseDir

        startWatcher(lowercaseDir)

        when:
        createNewFile(fileInLowercaseDir)

        then:
        expectEvents change(CREATED, new File(reportedDir, fileInLowercaseDir.name))

        when:
        createNewFile(fileInUppercaseDir)

        then:
        expectEvents change(CREATED, new File(reportedDir, fileInUppercaseDir.name))
    }

    def "fails when stopped multiple times"() {
        given:
        def watcher = startNewWatcher()
        shutdownWatcher(watcher)

        when:
        watcher.shutdown()

        then:
        def ex = thrown IllegalStateException
        ex.message == "Watcher already closed"
    }

    def "can be used multiple times"() {
        given:
        def firstFile = new File(rootDir, "first.txt")
        def secondFile = new File(rootDir, "second.txt")
        startWatcher(rootDir)

        when:
        createNewFile(firstFile)

        then:
        expectEvents change(CREATED, firstFile)

        when:
        shutdownWatcher()

        then:
        expectEvents termination()

        when:
        startWatcher(rootDir)
        createNewFile(secondFile)

        then:
        expectEvents change(CREATED, secondFile)
    }

    def "can start multiple watchers"() {
        given:
        def firstRoot = new File(rootDir, "first")
        firstRoot.mkdirs()
        def secondRoot = new File(rootDir, "second")
        secondRoot.mkdirs()
        def firstFile = new File(firstRoot, "file.txt")
        def secondFile = new File(secondRoot, "file.txt")
        def firstQueue = newEventQueue()
        def secondQueue = newEventQueue()

        LOGGER.info("> Starting first watcher")
        def firstWatcher = startNewWatcher(firstQueue)
        firstWatcher.startWatching([firstRoot])
        LOGGER.info("> Starting second watcher")
        def secondWatcher = startNewWatcher(secondQueue)
        secondWatcher.startWatching([secondRoot])
        LOGGER.info("> Watchers started")

        when:
        createNewFile(firstFile)

        then:
        expectEvents firstQueue, change(CREATED, firstFile)

        when:
        createNewFile(secondFile)

        then:
        expectEvents secondQueue, change(CREATED, secondFile)

        cleanup:
        shutdownWatcher(firstWatcher)
        shutdownWatcher(secondWatcher)
    }

    @Requires({ !Platform.current().linux })
    def "can receive event about a non-direct descendant change"() {
        given:
        def subDir = new File(rootDir, "sub-dir")
        subDir.mkdirs()
        def fileInSubDir = new File(subDir, "watched-descendant.txt")
        startWatcher(rootDir)

        when:
        createNewFile(fileInSubDir)

        then:
        expectEvents change(CREATED, fileInSubDir)
    }

    @Requires({ Platform.current().linux })
    def "does not receive event about a non-direct descendant change"() {
        given:
        def subDir = new File(rootDir, "sub-dir")
        subDir.mkdirs()
        def fileInSubDir = new File(subDir, "unwatched-descendant.txt")

        when:
        createNewFile(fileInSubDir)

        then:
        expectNoEvents()
    }

    def "can watch directory with long path"() {
        given:
        def subDir = new File(rootDir, "long-path")
        4.times {
            subDir = new File(subDir, "X" * 200)
        }
        subDir.mkdirs()
        def fileInSubDir = new File(subDir, "watched-descendant.txt")
        startWatcher(subDir)

        when:
        createNewFile(fileInSubDir)

        then:
        expectEvents change(CREATED, fileInSubDir)
    }

    def "can watch directory with #type characters"() {
        Assumptions.assumeTrue(supported as boolean)

        given:
        def subDir = new File(rootDir, path)
        subDir.mkdirs()
        def fileInSubDir = new File(subDir, path)
        startWatcher(subDir)

        when:
        createNewFile(fileInSubDir)

        then:
        expectEvents change(CREATED, fileInSubDir)

        where:
        type             | path                     | supported
        "ASCII-only"     | "directory"              | true
        "Chinese"        | "输入文件"               | true
        "four-byte UTF8" | "𠜎𠜱𠝹𠱓"                   | true
        "Hungarian"      | "Dezső"                  | true
        "space"          | "test directory"         | true
        "zwnj"           | "test\u200cdirectory"    | true
        "newline"        | "test\ndirectory"        | Platform.current().macOs
        "URL-quoted"     | "test%<directory>#2.txt" | !Platform.current().windows
    }

    @SuppressWarnings('UnnecessaryQualifiedReference')
    def "can set log level"() {
        given:
        def nativeLogger = LoggerFactory.getLogger(NativeLogger) as ch.qos.logback.classic.Logger
        def originalLevel = nativeLogger.level
        def fileChanged = new File(rootDir, "changed.txt")
        fileChanged.createNewFile()

        when:
        logging.clear()
        nativeLogger.level = ch.qos.logback.classic.Level.TRACE
        startWatcher(rootDir)
        fileChanged << "changed"
        waitForChangeEventLatency()

        then:
        logging.messages.values().any { it == TRACE }

        when:
        shutdownWatcher()
        logging.clear()
        nativeLogger.level = ch.qos.logback.classic.Level.WARN
        startWatcher()
        fileChanged << "changed again"
        waitForChangeEventLatency()

        then:
        !logging.messages.values().any { it == TRACE }

        cleanup:
        nativeLogger.level = originalLevel
    }

    def "handles queue not able to take any events"() {
        given:
        def notAcceptingQueue = Stub(BlockingQueue) {
            _ * offer(_ as FileWatchEvent, _ as long, _ as TimeUnit) >> false
            _ * offer(_ as FileWatchEvent) >> false
        }
        def fileCreated = new File(rootDir, "changed.txt")
        def watcher = startNewWatcher(notAcceptingQueue, rootDir)

        when:
        fileCreated.createNewFile()
        waitForChangeEventLatency()

        then:
        expectLogMessage(INFO, "Event queue overflow, dropping all events")
        expectLogMessage(ERROR, "Couldn't queue event: OVERFLOW (EVENT_QUEUE) at null")

        when:
        shutdownWatcher(watcher)

        then:
        expectLogMessage(INFO, "Event queue overflow, dropping all events")
        expectLogMessage(ERROR, "Couldn't queue event: OVERFLOW (EVENT_QUEUE) at null")
        expectLogMessage(ERROR, "Couldn't queue event: TERMINATE")
    }

    @IgnoreIf(value = { Platform.current().windows }, reason = "Windows 2019 Server does not handle this")
    def "can detect events in directory removed then re-added"() {
        given:
        def watchedDir = new File(rootDir, "watched")
        assert watchedDir.mkdirs()
        def createdFile = new File(watchedDir, "created.txt")
        startWatcher(watchedDir)

        def directoryRemoved = watchedDir.delete()
        def directoryRecreated = watchedDir.mkdirs()
        // On Windows we don't always manage to remove the watched directory, it's unreliable
        if (!Platform.current().windows) {
            assert directoryRemoved
            assert directoryRecreated
        }
        waitForChangeEventLatency()

        // Restart watching freshly recreated directory on platforms that auto-unregister on deletion
        if (!Platform.current().macOs) {
            watcher.startWatching([watchedDir])
        }
        // Ignore events received during setup
        waitForChangeEventLatency()
        eventQueue.clear()

        when:
        createdFile.createNewFile()

        then:
        expectEvents change(CREATED, createdFile)
    }
}
