package org.gradle.fileevents.internal;

import net.rubygrapefruit.platform.NativeException;

public class InsufficientResourcesForWatchingException extends NativeException {
    public InsufficientResourcesForWatchingException(String message) {
        super(message);
    }
}
