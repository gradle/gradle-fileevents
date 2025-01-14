package org.gradle.fileevents;

import javax.annotation.Nullable;

/**
 * An event that is triggered when a file system change is detected.
 */
public interface FileWatchEvent {

    /**
     * Handles the event.
     *
     * @param handler the handler to use.
     */
    void handleEvent(Handler handler);

    /**
     * A handler for file system change events.
     */
    interface Handler {
        /**
         * Handle an actual change to a file-system entry.
         * @param type the type of change.
         * @param absolutePath the absolute path of the changed file-system entry.
         */
        void handleChangeEvent(ChangeType type, String absolutePath);

        /**
         * Handle an event that is not recognized.
         * @param absolutePath the absolute path of the referenced file-system entry.
         */
        void handleUnknownEvent(String absolutePath);

        /**
         * Handle an overflow event.
         * Either the operating system or the library could not keep track of the changes.
         *
         * @param type which subsystem suffered the overflow.
         * @param absolutePath absolute path of the part of the file-system that is affected.
         */
        void handleOverflow(OverflowType type, @Nullable String absolutePath);

        /**
         * Handle a failure event in the watching logic.
         * @param failure the failure.
         */
        void handleFailure(Throwable failure);

        /**
         * Handle watching being terminated in an orderly fashion.
         */
        void handleTerminated();
    }

    /**
     * The type of file-system change that happened.
     */
    enum ChangeType {
        /**
         * An item with the given path has been created.
         */
        CREATED,

        /**
         * An item with the given path has been removed.
         */
        REMOVED,

        /**
         * An item with the given path has been modified.
         */
        MODIFIED,

        /**
         * Some undisclosed changes happened under the given path,
         * all information about descendants must be discarded.
         */
        INVALIDATED
    }

    /**
     * The subsystem that suffered the overflow.
     */
    enum OverflowType {
        /**
         * The overflow happened in the operating system's routines.
         */
        OPERATING_SYSTEM,

        /**
         * The overflow happened because the Java event queue has filled up.
         */
        EVENT_QUEUE
    }
}
