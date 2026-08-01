package io.github.flowerjvm.flowgraph.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServeMojoTest {

    @TempDir
    Path project;

    @Test
    void usesMavenExecutionRootByDefault() throws MojoExecutionException {
        Path resolved = ServeMojo.resolveProjectRoot(null, project.toFile());

        assertEquals(project.toAbsolutePath().normalize(), resolved);
    }

    @Test
    void explicitProjectDirectoryOverridesMavenExecutionRoot() throws MojoExecutionException {
        Path override = project.resolve("override");
        override.toFile().mkdirs();

        Path resolved = ServeMojo.resolveProjectRoot(
                override.toFile(),
                new File("unused-maven-root"));

        assertEquals(override.toAbsolutePath().normalize(), resolved);
    }

    @Test
    void rejectsMissingProjectDirectory() {
        File missing = project.resolve("missing").toFile();

        assertThrows(
                MojoExecutionException.class,
                () -> ServeMojo.resolveProjectRoot(missing, project.toFile()));
    }

    @Test
    void acceptsDynamicAndFixedPorts() throws MojoExecutionException {
        ServeMojo.validatePort(0);
        ServeMojo.validatePort(8790);
        ServeMojo.validatePort(65_535);
    }

    @Test
    void rejectsInvalidPorts() {
        assertThrows(MojoExecutionException.class, () -> ServeMojo.validatePort(-1));
        assertThrows(MojoExecutionException.class, () -> ServeMojo.validatePort(65_536));
    }
}
