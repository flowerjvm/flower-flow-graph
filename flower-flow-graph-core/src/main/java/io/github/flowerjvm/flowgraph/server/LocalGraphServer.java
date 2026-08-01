package io.github.flowerjvm.flowgraph.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.flowerjvm.flowgraph.analyze.FlowSourceAnalyzer;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Loopback-only, read-only server for the local graph UI.
 */
public final class LocalGraphServer implements AutoCloseable {

    private static final Map<String, Resource> STATIC_RESOURCES = Map.of(
            "/", new Resource("web/index.html", "text/html; charset=utf-8"),
            "/index.html", new Resource("web/index.html", "text/html; charset=utf-8"),
            "/app.js", new Resource("web/app.js", "text/javascript; charset=utf-8"),
            "/styles.css", new Resource("web/styles.css", "text/css; charset=utf-8"));

    private final Path projectRoot;
    private final FlowSourceAnalyzer analyzer;
    private final ObjectMapper objectMapper;
    private final HttpServer server;
    private final ExecutorService executor;

    public LocalGraphServer(Path projectRoot, int port) throws IOException {
        this.projectRoot = projectRoot.toAbsolutePath().normalize();
        this.analyzer = new FlowSourceAnalyzer();
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        this.server = HttpServer.create(
                new InetSocketAddress(InetAddress.getLoopbackAddress(), port),
                0);
        this.executor = Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable, "flower-flow-graph-http");
            thread.setDaemon(true);
            return thread;
        });
        server.setExecutor(executor);
        server.createContext("/api/graph", this::serveGraph);
        server.createContext("/", this::serveStatic);
    }

    public void start() {
        server.start();
    }

    public int port() {
        return server.getAddress().getPort();
    }

    private void serveGraph(HttpExchange exchange) throws IOException {
        addSecurityHeaders(exchange);
        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            exchange.getResponseHeaders().set("Allow", "GET");
            send(exchange, 405, "application/json; charset=utf-8",
                    objectMapper.writeValueAsBytes(Map.of("error", "method-not-allowed")));
            return;
        }
        try {
            byte[] body = objectMapper.writeValueAsBytes(analyzer.analyze(projectRoot));
            exchange.getResponseHeaders().set("Cache-Control", "no-store");
            send(exchange, 200, "application/json; charset=utf-8", body);
        } catch (RuntimeException exception) {
            byte[] body = objectMapper.writeValueAsBytes(Map.of(
                    "error", "analysis-failed",
                    "message", String.valueOf(exception.getMessage())));
            send(exchange, 500, "application/json; charset=utf-8", body);
        }
    }

    private void serveStatic(HttpExchange exchange) throws IOException {
        addSecurityHeaders(exchange);
        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            exchange.getResponseHeaders().set("Allow", "GET");
            send(exchange, 405, "text/plain; charset=utf-8",
                    "Method not allowed".getBytes(StandardCharsets.UTF_8));
            return;
        }
        Resource resource = STATIC_RESOURCES.get(exchange.getRequestURI().getPath());
        if (resource == null) {
            send(exchange, 404, "text/plain; charset=utf-8",
                    "Not found".getBytes(StandardCharsets.UTF_8));
            return;
        }
        try (InputStream input = LocalGraphServer.class.getClassLoader()
                .getResourceAsStream(resource.classpathLocation())) {
            if (input == null) {
                send(exchange, 404, "text/plain; charset=utf-8",
                        "Resource not found".getBytes(StandardCharsets.UTF_8));
                return;
            }
            exchange.getResponseHeaders().set("Cache-Control", "no-cache");
            send(exchange, 200, resource.contentType(), input.readAllBytes());
        }
    }

    private void addSecurityHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set(
                "Content-Security-Policy",
                "default-src 'self'; script-src 'self'; style-src 'self'; "
                        + "img-src 'self' data:; connect-src 'self'; frame-ancestors 'none'");
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().set("Referrer-Policy", "no-referrer");
        exchange.getResponseHeaders().set("X-Frame-Options", "DENY");
    }

    private void send(HttpExchange exchange, int status, String contentType, byte[] body)
            throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, body.length);
        try (var output = exchange.getResponseBody()) {
            output.write(body);
        }
    }

    @Override
    public void close() {
        server.stop(0);
        executor.shutdownNow();
    }

    private record Resource(String classpathLocation, String contentType) {
    }
}
