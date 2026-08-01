package io.github.flowerjvm.flowgraph.cli;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlowerFlowGraphCliTest {

    @Test
    void helpExplainsThatProjectDefaultsToCurrentDirectory() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        int exitCode = FlowerFlowGraphCli.run(
                new String[] {"--help"},
                new PrintStream(output, true, StandardCharsets.UTF_8),
                System.err);

        assertEquals(0, exitCode);
        String help = output.toString(StandardCharsets.UTF_8);
        assertTrue(help.contains("inspect [--project <path>]"));
        assertTrue(help.contains("current directory"));
    }
}
