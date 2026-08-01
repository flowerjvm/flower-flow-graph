package io.github.flowerjvm.flowgraph.maven;

import io.github.flowerjvm.flowgraph.server.LocalGraphServer;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;

/**
 * Opens the read-only Flower source graph for the current Maven project.
 */
@Mojo(name = "serve", requiresProject = true, aggregator = true)
public final class ServeMojo extends AbstractMojo {

    private static final int DEFAULT_PORT = 8790;

    /**
     * Optional project root override. The Maven execution root is used by default.
     */
    @Parameter(property = "flower.graph.project")
    private File projectDirectory;

    /**
     * Root directory of the current Maven invocation.
     */
    @Parameter(defaultValue = "${session.executionRootDirectory}", readonly = true, required = true)
    private File executionRootDirectory;

    /**
     * Loopback HTTP port. Use 0 to select an available port automatically.
     */
    @Parameter(property = "flower.graph.port", defaultValue = "8790")
    private int port = DEFAULT_PORT;

    /**
     * Whether to open the graph in the system browser after the server starts.
     */
    @Parameter(property = "flower.graph.open", defaultValue = "true")
    private boolean openBrowser = true;

    @Override
    public void execute() throws MojoExecutionException {
        Path projectRoot = resolveProjectRoot(projectDirectory, executionRootDirectory);
        validatePort(port);

        try (LocalGraphServer server = new LocalGraphServer(projectRoot, port)) {
            Thread shutdownHook = new Thread(server::close, "flower-flow-graph-maven-shutdown");
            Runtime.getRuntime().addShutdownHook(shutdownHook);
            try {
                server.start();
                URI graphUri = URI.create("http://localhost:" + server.port() + "/");
                getLog().info("Flower Flow Graph: " + graphUri);
                getLog().info("Project: " + projectRoot);
                getLog().info("Read-only source inspection; press Ctrl+C to stop.");
                if (openBrowser && !openBrowser(graphUri)) {
                    getLog().warn("Could not open a browser automatically. Open " + graphUri + " manually.");
                }
                new CountDownLatch(1).await();
            } finally {
                removeShutdownHook(shutdownHook);
            }
        } catch (IOException exception) {
            throw new MojoExecutionException("Could not start Flower Flow Graph: "
                    + exception.getMessage(), exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            getLog().info("Flower Flow Graph stopped.");
        }
    }

    static Path resolveProjectRoot(File projectDirectory, File executionRootDirectory)
            throws MojoExecutionException {
        File selected = projectDirectory != null ? projectDirectory : executionRootDirectory;
        if (selected == null) {
            throw new MojoExecutionException("Could not determine the Maven project directory. "
                    + "Set -Dflower.graph.project=<path>.");
        }
        Path projectRoot = selected.toPath().toAbsolutePath().normalize();
        if (!Files.isDirectory(projectRoot)) {
            throw new MojoExecutionException("Flower graph project is not a directory: " + projectRoot);
        }
        return projectRoot;
    }

    static void validatePort(int port) throws MojoExecutionException {
        if (port < 0 || port > 65_535) {
            throw new MojoExecutionException("flower.graph.port must be between 0 and 65535.");
        }
    }

    private static boolean openBrowser(URI graphUri) {
        try {
            if (!Desktop.isDesktopSupported()) {
                return false;
            }
            Desktop desktop = Desktop.getDesktop();
            if (!desktop.isSupported(Desktop.Action.BROWSE)) {
                return false;
            }
            desktop.browse(graphUri);
            return true;
        } catch (IOException | RuntimeException exception) {
            return false;
        }
    }

    private static void removeShutdownHook(Thread shutdownHook) {
        try {
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
        } catch (IllegalStateException ignored) {
            // The JVM is already shutting down and will run the hook itself.
        }
    }
}
