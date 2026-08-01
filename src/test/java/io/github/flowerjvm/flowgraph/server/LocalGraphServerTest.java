package io.github.flowerjvm.flowgraph.server;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalGraphServerTest {

    @TempDir
    Path project;

    @Test
    void servesUiAndReadOnlyGraphApiOnLoopback() throws IOException, InterruptedException {
        Path source = project.resolve("src/main/java/example/Sample.java");
        Files.createDirectories(source.getParent());
        Files.writeString(source, """
                package example;
                final class Sample {
                    Flow create() {
                        return Flow.builder("sample", "1")
                                .step("one", new OneStep())
                                .build();
                    }
                }
                final class OneStep {}
                """);

        try (LocalGraphServer server = new LocalGraphServer(project, 0)) {
            server.start();
            HttpClient client = HttpClient.newHttpClient();

            HttpResponse<String> page = client.send(
                    HttpRequest.newBuilder(URI.create(
                                    "http://localhost:" + server.port() + "/"))
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(200, page.statusCode());
            assertTrue(page.body().contains("Flower Flow Graph"));
            assertFalse(page.body().contains("Change draft"));
            assertFalse(page.body().contains("draft-panel"));
            assertEquals("DENY", page.headers().firstValue("X-Frame-Options").orElseThrow());

            HttpResponse<String> graph = client.send(
                    HttpRequest.newBuilder(URI.create(
                                    "http://localhost:" + server.port() + "/api/graph"))
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(200, graph.statusCode());
            assertTrue(graph.body().contains("\"schemaVersion\" : \"flower.flow-graph/v4\""));
            assertTrue(graph.body().contains("\"flowType\" : \"sample\""));

            HttpResponse<String> rejectedMutation = client.send(
                    HttpRequest.newBuilder(URI.create(
                                    "http://localhost:" + server.port() + "/api/graph"))
                            .POST(HttpRequest.BodyPublishers.noBody())
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(405, rejectedMutation.statusCode());
        }
    }
}
