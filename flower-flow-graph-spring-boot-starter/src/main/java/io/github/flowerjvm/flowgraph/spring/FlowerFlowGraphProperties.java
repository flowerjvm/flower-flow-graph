package io.github.flowerjvm.flowgraph.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

/**
 * Development-only settings for the local Flower Flow Graph server.
 */
@ConfigurationProperties("flower.flow-graph")
public final class FlowerFlowGraphProperties {

    /**
     * Enables the graph server. Disabled by default so adding the dependency
     * cannot expose application source accidentally.
     */
    private boolean enabled;

    /**
     * Source checkout to inspect. Defaults to the application working directory.
     */
    private Path projectRoot = Path.of(System.getProperty("user.dir", "."));

    /**
     * Loopback HTTP port. Use zero to select an available port.
     */
    private int port = 8790;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Path getProjectRoot() {
        return projectRoot;
    }

    public void setProjectRoot(Path projectRoot) {
        this.projectRoot = projectRoot;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
