package org.secondbase.example.main;

import com.sun.net.httpserver.HttpServer;
import io.prometheus.client.Counter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import org.secondbase.consul.ConsulModule;
import org.secondbase.consul.registration.ConsulRegistrationMetricsWebConsole;
import org.secondbase.core.SecondBase;
import org.secondbase.core.SecondBaseException;
import org.secondbase.core.config.SecondBaseModule;
import org.secondbase.flags.Flag;
import org.secondbase.flags.Flags;
import org.secondbase.logging.JsonLoggerModule;
import org.secondbase.webconsole.HttpWebConsole;
import org.secondbase.webconsole.PrometheusWebConsole;
import org.secondbase.webconsole.widget.Widget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HelloAll {
    @Flag(name="variable")
    private static String var = "default";
    @Flag(name="counter")
    private static int counter = 1;

    private static final Counter mycounter = Counter.build("mycounter", "a counter").register();

    private static final Logger LOG = LoggerFactory.getLogger(HelloAll.class.getName());

    /**
     * Start HelloAll service.
     */
    public HelloAll() throws IOException {
        mycounter.inc(counter);
        LOG.info(var);

        // Start a basic http server with a single endpoint.
        final HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.createContext("/myservice", httpExchange -> {
            final byte[] response = "This is my service response".getBytes();
            httpExchange.sendResponseHeaders(200, response.length);
            final OutputStream os = httpExchange.getResponseBody();
            os.write(response);
            os.close();
        });
        server.start();
    }

    public static void main(final String[] args) throws SecondBaseException, IOException {
        final String[] realArgs = {
                // SecondBase settings
                "--service-name=HelloAll",
                "--service-environment=testing",

                // Consul settings (register HelloAll service)
                "--consul-host=localhost:8500",
                "--service-port=8000",
                "--consul-health-check-path=/myservice",
                "--consul-tags=tagone,tagtwo",

                // Logging settings
                "--datacenter=local",

                // Webconsole settings
                "--webconsole-port=8001"
        };

        final SecondBaseModule jsonLogger = new JsonLoggerModule();

        final Widget prometheusWidget = new PrometheusWebConsole();
        final Widget[] widgets = {prometheusWidget};
        final HttpWebConsole webconsole = new HttpWebConsole(widgets);

        final ConsulModule consul = new ConsulModule(ConsulModule.createLocalhostConsulClient());
        final ConsulRegistrationMetricsWebConsole registerMetrics
                = new ConsulRegistrationMetricsWebConsole(webconsole, consul);

        // Put jsonLogger first, since it can define how the other modules do logging.
        final SecondBaseModule[] modules = {jsonLogger, consul, webconsole, registerMetrics};

        final Flags flags = new Flags().loadOpts(HelloAll.class);

        new SecondBase(realArgs, modules, flags);

        new HelloAll();
    }
}
