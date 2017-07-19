package org.secondbase.core.moduleconnection;

import java.io.IOException;

/**
 * Implemented by webconsole implementation so the core module can talk to the webconsole without
 * knowing about implementation details and having to include its dependencies.
 */
public interface WebConsole {
    /**
     * Start the webconsole server.
     * @throws IOException If there's trouble starting it.
     */
    void start() throws IOException;

    /**
     * Shutdown the webconsole server.
     * @throws IOException If there's trouble starting it.
     */
    void shutdown() throws IOException;

    /**
     * Get the port the webconsole is running on.
     * @return the webconsole port.
     */
    int getPort();
}
