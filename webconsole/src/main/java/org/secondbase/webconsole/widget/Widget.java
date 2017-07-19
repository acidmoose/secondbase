package org.secondbase.webconsole.widget;

import com.sun.net.httpserver.HttpHandler;

/**
 * Implemented by WebConsole widgets.
 */
public interface Widget {
    String getPath();
    HttpHandler getServlet();
}
