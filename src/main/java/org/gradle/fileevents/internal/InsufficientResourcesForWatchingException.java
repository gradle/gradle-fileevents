package org.gradle.fileevents.internal;

import net.rubygrapefruit.platform.NativeException;

/**
 * Thrown when the native file events integration is not available on the current platform.
 *
 * <p>This can typically happen on Linux when we run out of inotify watches.</p>
 */
public class InsufficientResourcesForWatchingException extends NativeException {
    public InsufficientResourcesForWatchingException(String message) {
        super(message);
    }
}
