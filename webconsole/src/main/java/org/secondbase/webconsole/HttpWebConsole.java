package org.secondbase.webconsole;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.secondbase.core.SecondBase;
import org.secondbase.core.config.SecondBaseModule;
import org.secondbase.core.moduleconnection.WebConsole;
import org.secondbase.webconsole.widget.Widget;

/**
 * A webserver for hosting secondbase servlets using Sun's {@link HttpServer}.
 */
public final class HttpWebConsole implements SecondBaseModule, WebConsole {

    private static final Logger LOG = Logger.getLogger(HttpWebConsole.class.getName());
    private final HttpServer server;
    private int port;

    private final ServiceLoader<Widget> widgets = ServiceLoader.load(Widget.class);

    /**
     * Basic /healthz endpoint, returning 200 OK.
     */
    private final class HealthzHandler implements HttpHandler {
        private final String healthyMsg = "Healthy";

        public void handle(final HttpExchange t) throws IOException {
            final byte [] response = healthyMsg.getBytes();
            t.sendResponseHeaders(200, response.length);
            final OutputStream os = t.getResponseBody();
            os.write(response);
            os.close();
        }
    }

    /**
     * Set up the webconsole using port from {@link WebConsoleConfiguration}.
     * @throws IOException if server can't start on a given port
     */
    public HttpWebConsole() throws IOException {
        server = HttpServer.create();
        server.createContext("/healthz", new HealthzHandler());
    }

    /**
     * Load WebConsole Flags and set the secondbase webconsole to "this".
     * @param secondBase module coordinator
     */
    @Override
    public void load(final SecondBase secondBase) {
        secondBase.getFlags().loadOpts(WebConsoleConfiguration.class);
        secondBase.setWebConsole(this);
    }

    @Override
    public void start() throws IOException {
        if (! WebConsoleConfiguration.enableWebConsole) {
            return;
        }
        port = (WebConsoleConfiguration.port == 0) ? getFreePort() : WebConsoleConfiguration.port;
        final int useSystemDefaultBacklog = 0;
        server.bind(
                new InetSocketAddress(port),
                useSystemDefaultBacklog);
        LOG.info("Starting webconsole on port " + port);
        for (final Widget widget : widgets) {
            LOG.info("Adding webconsole widget " + widget.getPath());
            server.createContext(widget.getPath(), widget.getServlet());
        }
        server.start();
    }

    @Override
    public void shutdown() throws IOException {
        LOG.info("Shutting down webconsole.");
        if (! WebConsoleConfiguration.enableWebConsole) {
            return;
        }
        final int USE_SYSTEM_DEFAULT_BACKLOG = 0;
        server.bind(
                new InetSocketAddress(port),
                USE_SYSTEM_DEFAULT_BACKLOG);
        server.start();
    }

    @Override
    public int getPort() {
        return port;
    }

    /**
     * Get the server implementation.
     * @return HttpServer
     */
    public HttpServer getServer() {
        return server;
    }

    /**
     * Find a free local port.
     *
     * @return a free local port number.
     * @throws RuntimeException on error allocating port.
     */
    private int getFreePort() {
        ServerSocket socket = null;
        try {
            socket = new ServerSocket(0);
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        } catch (final IOException e) {
            LOG.log(Level.INFO, "Failed to allocate local port", e);
            throw new RuntimeException("Failed to allocate local port", e);
        } finally {
            try {
                if (null != socket) {
                    socket.close();
                }
            } catch (final IOException e) {
                LOG.log(Level.WARNING, "Failed to close server socket used for finding free port");
            }
        }
    }
}
