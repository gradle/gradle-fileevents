package org.gradle.fileevents.internal;

import org.gradle.fileevents.FileWatcher;

import java.util.Collection;

/**
 * A {@link InotifyInstanceLimitTooLowException} is thrown by {@link FileWatcher#startWatching(Collection)}
 * when the inotify watches count is too low.
 */
@SuppressWarnings("unused") // Thrown from the native side
public class InotifyWatchesLimitTooLowException extends InsufficientResourcesForWatchingException {
    public InotifyWatchesLimitTooLowException(String message) {
        super(message);
    }
}
