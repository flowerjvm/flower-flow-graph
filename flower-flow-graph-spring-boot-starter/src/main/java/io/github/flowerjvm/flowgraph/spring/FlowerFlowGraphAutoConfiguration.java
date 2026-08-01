package io.github.flowerjvm.flowgraph.spring;

import io.github.flowerjvm.flowgraph.server.LocalGraphServer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Starts the read-only graph server with the Spring application lifecycle when
 * explicitly enabled.
 */
@AutoConfiguration
@ConditionalOnClass(LocalGraphServer.class)
@ConditionalOnProperty(prefix = "flower.flow-graph", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(FlowerFlowGraphProperties.class)
public class FlowerFlowGraphAutoConfiguration {

    private static final Log LOGGER = LogFactory.getLog(FlowerFlowGraphAutoConfiguration.class);

    @Bean(name = "flowerFlowGraphServer", destroyMethod = "close")
    @ConditionalOnMissingBean(LocalGraphServer.class)
    LocalGraphServer flowerFlowGraphServer(FlowerFlowGraphProperties properties) {
        Path projectRoot = normalizeProjectRoot(properties.getProjectRoot());
        int port = validatePort(properties.getPort());
        try {
            LocalGraphServer server = new LocalGraphServer(projectRoot, port);
            server.start();
            LOGGER.info("Flower Flow Graph: http://localhost:" + server.port()
                    + "/ (project " + projectRoot + ")");
            return server;
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not start Flower Flow Graph: " + exception.getMessage(), exception);
        }
    }

    private static Path normalizeProjectRoot(Path configuredRoot) {
        if (configuredRoot == null) {
            throw new IllegalStateException("flower.flow-graph.project-root must not be empty");
        }
        Path projectRoot = configuredRoot.toAbsolutePath().normalize();
        if (!Files.isDirectory(projectRoot)) {
            throw new IllegalStateException(
                    "flower.flow-graph.project-root is not a directory: " + projectRoot);
        }
        return projectRoot;
    }

    private static int validatePort(int port) {
        if (port < 0 || port > 65_535) {
            throw new IllegalStateException(
                    "flower.flow-graph.port must be between 0 and 65535");
        }
        return port;
    }
}
