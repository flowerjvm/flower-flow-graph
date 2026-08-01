package io.github.flowerjvm.flowgraph.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.github.flowerjvm.flowgraph.analyze.FlowSourceAnalyzer;
import io.github.flowerjvm.flowgraph.server.LocalGraphServer;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public final class FlowerFlowGraphCli {

    private static final int DEFAULT_PORT = 8790;

    private FlowerFlowGraphCli() {
    }

    public static void main(String[] args) {
        int exitCode = run(args, System.out, System.err);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    static int run(String[] args, PrintStream out, PrintStream err) {
        if (args.length == 0 || isHelp(args[0])) {
            printHelp(out);
            return 0;
        }

        String command = args[0];
        Map<String, String> options;
        try {
            options = parseOptions(args);
        } catch (IllegalArgumentException exception) {
            err.println("Error: " + exception.getMessage());
            printHelp(err);
            return 2;
        }

        String projectValue = options.getOrDefault("--project", ".");
        Path project = Path.of(projectValue);

        try {
            return switch (command) {
                case "inspect" -> inspect(project, options, out);
                case "serve" -> serve(project, options, out);
                default -> {
                    err.println("Error: unknown command '" + command + "'.");
                    printHelp(err);
                    yield 2;
                }
            };
        } catch (IllegalArgumentException exception) {
            err.println("Error: " + exception.getMessage());
            return 2;
        } catch (Exception exception) {
            err.println("Error: " + exception.getMessage());
            return 1;
        }
    }

    private static int inspect(Path project, Map<String, String> options, PrintStream out)
            throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        if (!options.containsKey("--compact")) {
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        }
        out.println(objectMapper.writeValueAsString(new FlowSourceAnalyzer().analyze(project)));
        return 0;
    }

    private static int serve(Path project, Map<String, String> options, PrintStream out)
            throws IOException, InterruptedException {
        int port = parsePort(options.getOrDefault("--port", Integer.toString(DEFAULT_PORT)));
        LocalGraphServer server = new LocalGraphServer(project, port);
        Runtime.getRuntime().addShutdownHook(new Thread(server::close, "flower-flow-graph-shutdown"));
        server.start();
        out.println("Flower Flow Graph is running at http://localhost:" + server.port() + "/");
        out.println("Project: " + project.toAbsolutePath().normalize());
        out.println("Read-only source inspection; press Ctrl+C to stop.");
        new CountDownLatch(1).await();
        return 0;
    }

    private static int parsePort(String value) {
        try {
            int port = Integer.parseInt(value);
            if (port < 0 || port > 65_535) {
                throw new IllegalArgumentException("--port must be between 0 and 65535.");
            }
            return port;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("--port must be a number: " + value, exception);
        }
    }

    private static Map<String, String> parseOptions(String[] args) {
        Map<String, String> options = new LinkedHashMap<>();
        for (int index = 1; index < args.length; index++) {
            String argument = args[index];
            if (argument.equals("--compact")) {
                options.put(argument, "true");
                continue;
            }
            if (!argument.startsWith("--")) {
                throw new IllegalArgumentException("Unexpected argument: " + argument);
            }
            if (index + 1 >= args.length || args[index + 1].startsWith("--")) {
                throw new IllegalArgumentException("Missing value for " + argument);
            }
            options.put(argument, args[++index]);
        }
        return options;
    }

    private static boolean isHelp(String value) {
        return value.equals("help") || value.equals("--help") || value.equals("-h");
    }

    private static void printHelp(PrintStream out) {
        out.println("""
                Flower Flow Graph

                Usage:
                  flower-flow-graph inspect [--project <path>] [--compact]
                  flower-flow-graph serve   [--project <path>] [--port <port>]

                Commands:
                  inspect  Print an agent-neutral static Flow graph document as JSON.
                  serve    Start the loopback-only read-only graph UI.

                The current directory is inspected when --project is omitted.
                The tool never writes to the inspected project.
                """);
    }
}
