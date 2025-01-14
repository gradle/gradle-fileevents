package org.gradle.fileevents;

import org.gradle.fileevents.internal.InsufficientResourcesForWatchingException;

import javax.annotation.CheckReturnValue;
import javax.annotation.concurrent.NotThreadSafe;
import java.io.File;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * A handle for watching file system locations.
 */
@NotThreadSafe
public interface FileWatcher {
    /**
     * Initializes the watcher. This method must be called before any other method.
     * @param startTimeout the maximum time to wait for the watcher to start.
     * @param startTimeoutUnit the time unit of the {@code startTimeout} argument.
     * @throws InterruptedException if interrupted while waiting.
     */
    void initialize(long startTimeout, TimeUnit startTimeoutUnit) throws InterruptedException;

    /**
     * Starts watching the given paths.
     * @param paths the paths to watch.
     * @throws InsufficientResourcesForWatchingException if the system does not have enough resources to watch the given paths.
     */
    void startWatching(Collection<File> paths) throws InsufficientResourcesForWatchingException;

    /**
     * Stops watching the given paths.
     * @param paths the paths to stop watching.
     * @return {@code true} if the paths were being watched and are no longer being watched.
     */
    @CheckReturnValue
    boolean stopWatching(Collection<File> paths);

    /**
     * Initiates an orderly shutdown and release of any native resources.
     * No more events will arrive after this method returns.
     */
    void shutdown();

    /**
     * Blocks until the termination is complete after a {@link #shutdown()}
     * request, or the timeout occurs, or the current thread is interrupted,
     * whichever happens first.
     *
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @return {@code true} if this watcher terminated and
     *         {@code false} if the timeout elapsed before termination
     * @throws InterruptedException if interrupted while waiting
     */
    @CheckReturnValue
    boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException;
}
