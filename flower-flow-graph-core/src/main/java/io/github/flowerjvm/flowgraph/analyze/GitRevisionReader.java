package io.github.flowerjvm.flowgraph.analyze;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

final class GitRevisionReader {

    Optional<String> read(Path projectRoot) {
        Path dotGit = projectRoot.resolve(".git");
        if (Files.isDirectory(dotGit)) {
            return readFromGitDirectory(dotGit);
        }
        if (Files.isRegularFile(dotGit)) {
            return readWorktreeGitDirectory(dotGit);
        }
        return Optional.empty();
    }

    private Optional<String> readWorktreeGitDirectory(Path dotGitFile) {
        try {
            String content = Files.readString(dotGitFile, StandardCharsets.UTF_8).trim();
            if (!content.startsWith("gitdir:")) {
                return Optional.empty();
            }
            Path configured = Path.of(content.substring("gitdir:".length()).trim());
            Path gitDirectory = configured.isAbsolute()
                    ? configured.normalize()
                    : dotGitFile.getParent().resolve(configured).normalize();
            return readFromGitDirectory(gitDirectory);
        } catch (IOException | RuntimeException ignored) {
            return Optional.empty();
        }
    }

    private Optional<String> readFromGitDirectory(Path gitDirectory) {
        Path head = gitDirectory.resolve("HEAD");
        try {
            String value = Files.readString(head, StandardCharsets.UTF_8).trim();
            if (!value.startsWith("ref:")) {
                return value.isEmpty() ? Optional.empty() : Optional.of(value);
            }
            String refName = value.substring("ref:".length()).trim();
            Path looseRef = gitDirectory.resolve(refName);
            if (Files.isRegularFile(looseRef)) {
                String revision = Files.readString(looseRef, StandardCharsets.UTF_8).trim();
                return revision.isEmpty() ? Optional.empty() : Optional.of(revision);
            }
            return readPackedRef(gitDirectory.resolve("packed-refs"), refName);
        } catch (IOException ignored) {
            return Optional.empty();
        }
    }

    private Optional<String> readPackedRef(Path packedRefs, String refName) {
        if (!Files.isRegularFile(packedRefs)) {
            return Optional.empty();
        }
        try {
            List<String> lines = Files.readAllLines(packedRefs, StandardCharsets.UTF_8);
            for (String line : lines) {
                if (line.isBlank() || line.startsWith("#") || line.startsWith("^")) {
                    continue;
                }
                int separator = line.indexOf(' ');
                if (separator > 0 && line.substring(separator + 1).equals(refName)) {
                    return Optional.of(line.substring(0, separator));
                }
            }
        } catch (IOException ignored) {
            return Optional.empty();
        }
        return Optional.empty();
    }
}
