package io.github.flowerjvm.flowgraph.spring;

import io.github.flowerjvm.flowgraph.server.LocalGraphServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class FlowerFlowGraphAutoConfigurationTest {

    @TempDir
    Path projectRoot;

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(FlowerFlowGraphAutoConfiguration.class));

    @Test
    void registersAutoConfigurationForSpringBootDiscovery() throws IOException {
        String resource = "META-INF/spring/"
                + "org.springframework.boot.autoconfigure.AutoConfiguration.imports";
        try (var input = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertThat(input).isNotNull();
            assertThat(new String(input.readAllBytes(), StandardCharsets.UTF_8).trim())
                    .isEqualTo(FlowerFlowGraphAutoConfiguration.class.getName());
        }
    }

    @Test
    void remainsDisabledByDefault() {
        contextRunner.run(context -> assertThat(context)
                .doesNotHaveBean(LocalGraphServer.class));
    }

    @Test
    void startsLoopbackServerWhenExplicitlyEnabledAndStopsWithContext()
            throws IOException {
        AtomicInteger selectedPort = new AtomicInteger();

        contextRunner
                .withPropertyValues(
                        "flower.flow-graph.enabled=true",
                        "flower.flow-graph.project-root=" + projectRoot,
                        "flower.flow-graph.port=0")
                .run(context -> {
                    assertThat(context).hasSingleBean(LocalGraphServer.class);
                    LocalGraphServer server = context.getBean(LocalGraphServer.class);
                    selectedPort.set(server.port());

                    try {
                        HttpResponse<String> response = HttpClient.newHttpClient().send(
                                HttpRequest.newBuilder(URI.create(
                                                "http://localhost:" + server.port() + "/api/graph"))
                                        .GET()
                                        .build(),
                                HttpResponse.BodyHandlers.ofString());

                        assertThat(response.statusCode()).isEqualTo(200);
                        assertThat(response.body()).contains("\"schemaVersion\"");
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        throw new AssertionError("Could not call the local graph server", exception);
                    } catch (IOException exception) {
                        throw new AssertionError("Could not call the local graph server", exception);
                    }
                });

        assertThat(selectedPort.get()).isPositive();
        try (ServerSocket socket = new ServerSocket()) {
            socket.bind(new InetSocketAddress(
                    InetAddress.getLoopbackAddress(), selectedPort.get()));
        }
    }

    @Test
    void rejectsProjectRootThatIsNotADirectory() {
        Path missing = projectRoot.resolve("missing");

        contextRunner
                .withPropertyValues(
                        "flower.flow-graph.enabled=true",
                        "flower.flow-graph.project-root=" + missing,
                        "flower.flow-graph.port=0")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseMessage(
                                    "flower.flow-graph.project-root is not a directory: "
                                            + missing.toAbsolutePath().normalize());
                });
    }
}
