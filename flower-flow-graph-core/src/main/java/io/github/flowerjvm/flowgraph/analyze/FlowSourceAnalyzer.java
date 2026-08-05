package io.github.flowerjvm.flowgraph.analyze;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.SwitchExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import io.github.flowerjvm.flowgraph.model.FlowGraphDocument;
import io.github.flowerjvm.flowgraph.model.FlowGraphDocument.AnalysisNotice;
import io.github.flowerjvm.flowgraph.model.FlowGraphDocument.FlowCompleteness;
import io.github.flowerjvm.flowgraph.model.FlowGraphDocument.FlowDefinition;
import io.github.flowerjvm.flowgraph.model.FlowGraphDocument.FlowKind;
import io.github.flowerjvm.flowgraph.model.FlowGraphDocument.FlowRelation;
import io.github.flowerjvm.flowgraph.model.FlowGraphDocument.FlowRelationCardinality;
import io.github.flowerjvm.flowgraph.model.FlowGraphDocument.FlowRelationCertainty;
import io.github.flowerjvm.flowgraph.model.FlowGraphDocument.FlowRelationKind;
import io.github.flowerjvm.flowgraph.model.FlowGraphDocument.EventSubscription;
import io.github.flowerjvm.flowgraph.model.FlowGraphDocument.EventSubscriptionKind;
import io.github.flowerjvm.flowgraph.model.FlowGraphDocument.InternalTransitionTrigger;
import io.github.flowerjvm.flowgraph.model.FlowGraphDocument.NoticeSeverity;
import io.github.flowerjvm.flowgraph.model.FlowGraphDocument.ProjectInfo;
import io.github.flowerjvm.flowgraph.model.FlowGraphDocument.SourceRef;
import io.github.flowerjvm.flowgraph.model.FlowGraphDocument.SignalOperation;
import io.github.flowerjvm.flowgraph.model.FlowGraphDocument.SignalUse;
import io.github.flowerjvm.flowgraph.model.FlowGraphDocument.StepBehavior;
import io.github.flowerjvm.flowgraph.model.FlowGraphDocument.StepNode;
import io.github.flowerjvm.flowgraph.model.FlowGraphDocument.StepPhase;
import io.github.flowerjvm.flowgraph.model.FlowGraphDocument.StepPhaseTransition;
import io.github.flowerjvm.flowgraph.model.FlowGraphDocument.Transition;
import io.github.flowerjvm.flowgraph.model.FlowGraphDocument.TransitionCertainty;
import io.github.flowerjvm.flowgraph.model.FlowGraphDocument.TransitionEvidence;
import io.github.flowerjvm.flowgraph.model.FlowGraphDocument.TransitionKind;
import io.github.flowerjvm.flowgraph.model.FlowGraphDocument.WorkerDefinition;
import io.github.flowerjvm.flowgraph.model.FlowGraphDocument.WorkerDefinitionSource;
import io.github.flowerjvm.flowgraph.model.FlowGraphDocument.WorkerFlowRelation;
import io.github.flowerjvm.flowgraph.model.FlowGraphDocument.WorkerFlowRelationCertainty;
import io.github.flowerjvm.flowgraph.model.FlowGraphDocument.WorkerKind;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Best-effort source analyzer. It reports unknown dynamic structure instead of
 * evaluating application code or pretending one static graph is authoritative.
 */
public final class FlowSourceAnalyzer {

    private static final Set<String> EXCLUDED_DIRECTORIES = Set.of(
            ".git", ".gradle", ".idea", "build", "node_modules", "target");
    private static final Set<String> TEST_SOURCE_SETS = Set.of(
            "test", "testfixtures", "integrationtest", "functionaltest");
    private static final Pattern SPRING_WORKER_PROPERTY = Pattern.compile(
            "^\\s*flower\\.workers\\[(\\d+)]\\.name\\s*[:=]\\s*(.*?)\\s*$");

    private final JavaParser parser;
    private final GraphHasher graphHasher = new GraphHasher();
    private final GitRevisionReader gitRevisionReader = new GitRevisionReader();

    public FlowSourceAnalyzer() {
        ParserConfiguration configuration = new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.BLEEDING_EDGE);
        this.parser = new JavaParser(configuration);
    }

    public FlowGraphDocument analyze(Path projectRoot) throws IOException {
        Objects.requireNonNull(projectRoot, "projectRoot");
        Path root = projectRoot.toAbsolutePath().normalize();
        if (!Files.isDirectory(root)) {
            throw new IllegalArgumentException("Project directory does not exist: " + root);
        }

        List<AnalysisNotice> documentNotices = new ArrayList<>();
        List<SourceUnit> units = resolveStaticImports(loadSourceUnits(root, documentNotices));
        Map<String, List<TypeSite>> typesBySimpleName = indexTypes(units);
        List<DefinitionSite> definitionSites = new ArrayList<>();

        for (SourceUnit unit : units) {
            List<MethodCallExpr> builderCalls = unit.compilationUnit()
                    .findAll(MethodCallExpr.class, this::isFlowerBuilderCall);
            builderCalls.sort(NODE_ORDER);
            for (MethodCallExpr builderCall : builderCalls) {
                definitionSites.add(analyzeDefinition(unit, builderCall, typesBySimpleName));
            }
        }

        List<FlowDefinition> definitions = definitionSites.stream()
                .map(DefinitionSite::definition)
                .sorted(Comparator
                        .comparing((FlowDefinition definition) -> definition.source().file())
                        .thenComparingInt(definition -> definition.source().line())
                        .thenComparingInt(definition -> definition.source().column()))
                .toList();
        List<WorkerDefinition> workers = analyzeWorkerDefinitions(
                root,
                units,
                documentNotices);
        workers.sort(Comparator
                .comparing((WorkerDefinition worker) -> worker.name() == null
                        ? worker.nameExpression()
                        : worker.name())
                .thenComparing(worker -> worker.source().file())
                .thenComparingInt(worker -> worker.source().line()));
        List<WorkerFlowRelation> workerRelations = new ArrayList<>(
                analyzeWorkerFlowRelations(
                        workers,
                        definitionSites,
                        typesBySimpleName));
        workerRelations.sort(Comparator
                .comparing(WorkerFlowRelation::workerName)
                .thenComparing(WorkerFlowRelation::targetLabel)
                .thenComparing(relation -> relation.source().file())
                .thenComparingInt(relation -> relation.source().line()));
        List<FlowRelation> relations = analyzeFlowRelations(
                definitionSites,
                typesBySimpleName);
        relations.sort(Comparator
                .comparing(FlowRelation::fromDefinitionId)
                .thenComparing(FlowRelation::fromStepId)
                .thenComparing(relation -> relation.source().file())
                .thenComparingInt(relation -> relation.source().line())
                .thenComparing(FlowRelation::targetLabel));
        addVariantNotices(definitions, documentNotices);
        if (definitions.isEmpty()) {
            documentNotices.add(new AnalysisNotice(
                    "NO_FLOW_BUILDERS",
                    NoticeSeverity.INFO,
                    "No Flow.builder(...) or EventFlow.builder(...) call was found.",
                    null));
        }

        String projectName = root.getFileName() == null ? root.toString() : root.getFileName().toString();
        ProjectInfo project = new ProjectInfo(
                projectName,
                root.toString(),
                gitRevisionReader.read(root).orElse(null));
        return new FlowGraphDocument(
                FlowGraphDocument.SCHEMA_VERSION,
                project,
                workers,
                workerRelations,
                definitions,
                relations,
                documentNotices);
    }

    private List<SourceUnit> loadSourceUnits(Path root, List<AnalysisNotice> notices) throws IOException {
        List<Path> files;
        try (Stream<Path> paths = Files.walk(root)) {
            files = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .filter(Predicate.not(path -> isExcluded(root.relativize(path))))
                    .sorted()
                    .toList();
        }

        List<SourceUnit> units = new ArrayList<>();
        for (Path file : files) {
            String relativePath = normalizePath(root.relativize(file));
            try {
                ParseResult<CompilationUnit> result = parser.parse(file);
                if (result.getResult().isPresent()) {
                    CompilationUnit compilationUnit = result.getResult().orElseThrow();
                    SourceUnit unit = new SourceUnit(
                            relativePath,
                            compilationUnit,
                            collectStringConstants(compilationUnit),
                            collectIntegerConstants(compilationUnit),
                            collectVariableTypes(compilationUnit));
                    units.add(unit);
                }
                if (!result.getProblems().isEmpty()) {
                    notices.add(new AnalysisNotice(
                            "JAVA_PARSE_PARTIAL",
                            NoticeSeverity.WARNING,
                            "JavaParser reported " + result.getProblems().size()
                                    + " problem(s); results for this file may be partial.",
                            new SourceRef(relativePath, 0, 0)));
                }
            } catch (IOException | RuntimeException exception) {
                notices.add(new AnalysisNotice(
                        "JAVA_PARSE_FAILED",
                        NoticeSeverity.WARNING,
                        "Could not parse source: " + exception.getMessage(),
                        new SourceRef(relativePath, 0, 0)));
            }
        }
        return units;
    }

    private boolean isExcluded(Path relativePath) {
        String previous = null;
        for (Path segment : relativePath) {
            String current = segment.toString().toLowerCase(Locale.ROOT);
            if (EXCLUDED_DIRECTORIES.contains(current)) {
                return true;
            }
            if ("src".equals(previous) && TEST_SOURCE_SETS.contains(current)) {
                return true;
            }
            previous = current;
        }
        return false;
    }

    private Map<String, List<TypeSite>> indexTypes(List<SourceUnit> units) {
        Map<String, List<TypeSite>> index = new HashMap<>();
        for (SourceUnit unit : units) {
            for (ClassOrInterfaceDeclaration declaration
                    : unit.compilationUnit().findAll(ClassOrInterfaceDeclaration.class)) {
                index.computeIfAbsent(declaration.getNameAsString(), ignored -> new ArrayList<>())
                        .add(new TypeSite(unit, declaration));
            }
        }
        return index;
    }

    private List<WorkerDefinition> analyzeWorkerDefinitions(
            Path root,
            List<SourceUnit> units,
            List<AnalysisNotice> notices
    ) {
        List<WorkerDefinition> workers = new ArrayList<>();
        for (SourceUnit unit : units) {
            List<MethodCallExpr> builders = unit.compilationUnit().findAll(
                            MethodCallExpr.class,
                            this::isWorkerBuilderCall)
                    .stream()
                    .sorted(NODE_ORDER)
                    .toList();
            for (MethodCallExpr builder : builders) {
                Expression nameExpression = builder.getArguments().isEmpty()
                        ? null
                        : builder.getArgument(0);
                String expressionText = nameExpression == null
                        ? "<missing>"
                        : nameExpression.toString();
                String name = nameExpression == null
                        ? null
                        : resolveString(nameExpression, unit.constants());
                SourceRef source = sourceRef(unit, builder);
                workers.add(new WorkerDefinition(
                        "worker:" + source.file() + "#" + source.line() + ":" + source.column(),
                        name,
                        expressionText,
                        isScopeNamed(builder, "EventWorker")
                                ? WorkerKind.EVENT_WORKER
                                : WorkerKind.WORKER,
                        WorkerDefinitionSource.JAVA_BUILDER,
                        name == null,
                        source));
                if (name == null) {
                    notices.add(new AnalysisNotice(
                            "DYNAMIC_WORKER_NAME",
                            NoticeSeverity.INFO,
                            "Worker name is computed at runtime: " + expressionText,
                            source));
                }
            }
        }
        workers.addAll(analyzeSpringWorkerDefinitions(root, notices));
        return workers;
    }

    private List<WorkerDefinition> analyzeSpringWorkerDefinitions(
            Path root,
            List<AnalysisNotice> notices
    ) {
        List<Path> files;
        try (Stream<Path> paths = Files.walk(root)) {
            files = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> isSpringConfigurationFile(path.getFileName().toString()))
                    .filter(Predicate.not(path -> isExcluded(root.relativize(path))))
                    .sorted()
                    .toList();
        } catch (IOException exception) {
            notices.add(new AnalysisNotice(
                    "SPRING_CONFIG_SCAN_FAILED",
                    NoticeSeverity.INFO,
                    "Could not scan Spring configuration for Workers: " + exception.getMessage(),
                    null));
            return List.of();
        }

        List<WorkerDefinition> workers = new ArrayList<>();
        for (Path file : files) {
            String relativePath = normalizePath(root.relativize(file));
            try {
                List<String> lines = Files.readAllLines(file);
                String fileName = file.getFileName().toString().toLowerCase(Locale.ROOT);
                if (fileName.endsWith(".properties")) {
                    parseSpringWorkerProperties(relativePath, lines, workers);
                } else {
                    parseSpringWorkerYaml(relativePath, lines, workers);
                }
            } catch (IOException | RuntimeException exception) {
                notices.add(new AnalysisNotice(
                        "SPRING_CONFIG_PARSE_FAILED",
                        NoticeSeverity.INFO,
                        "Could not read Spring Worker configuration: " + exception.getMessage(),
                        new SourceRef(relativePath, 0, 0)));
            }
        }
        return workers;
    }

    private boolean isSpringConfigurationFile(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        return lower.startsWith("application")
                && (lower.endsWith(".yml")
                || lower.endsWith(".yaml")
                || lower.endsWith(".properties"));
    }

    private void parseSpringWorkerProperties(
            String relativePath,
            List<String> lines,
            List<WorkerDefinition> workers
    ) {
        for (int index = 0; index < lines.size(); index++) {
            Matcher matcher = SPRING_WORKER_PROPERTY.matcher(lines.get(index));
            if (!matcher.matches()) {
                continue;
            }
            appendSpringWorkerDefinition(
                    relativePath,
                    index + 1,
                    lines.get(index).indexOf(matcher.group(2)) + 1,
                    matcher.group(2),
                    workers);
        }
    }

    private void parseSpringWorkerYaml(
            String relativePath,
            List<String> lines,
            List<WorkerDefinition> workers
    ) {
        int flowerIndent = -1;
        int workersIndent = -1;
        for (int index = 0; index < lines.size(); index++) {
            String withoutComment = stripYamlComment(lines.get(index));
            String content = withoutComment.trim();
            if (content.isEmpty()) {
                continue;
            }
            if (content.equals("---")) {
                flowerIndent = -1;
                workersIndent = -1;
                continue;
            }
            int indent = leadingWhitespace(withoutComment);
            if (content.equals("flower:") || content.equals("flower: {}")) {
                flowerIndent = indent;
                workersIndent = -1;
                continue;
            }
            if (content.equals("flower.workers:")) {
                flowerIndent = indent - 1;
                workersIndent = indent;
                continue;
            }
            if (flowerIndent >= 0 && indent <= flowerIndent) {
                flowerIndent = -1;
                workersIndent = -1;
            }
            if (flowerIndent >= 0
                    && indent > flowerIndent
                    && content.equals("workers:")) {
                workersIndent = indent;
                continue;
            }
            if (workersIndent < 0) {
                continue;
            }
            if (indent <= workersIndent) {
                workersIndent = -1;
                continue;
            }
            String item = content.startsWith("-")
                    ? content.substring(1).trim()
                    : content;
            if (!item.startsWith("name:")) {
                continue;
            }
            String value = item.substring("name:".length()).trim();
            appendSpringWorkerDefinition(
                    relativePath,
                    index + 1,
                    Math.max(1, lines.get(index).indexOf(value) + 1),
                    value,
                    workers);
        }
    }

    private void appendSpringWorkerDefinition(
            String relativePath,
            int line,
            int column,
            String rawValue,
            List<WorkerDefinition> workers
    ) {
        String expression = rawValue.trim();
        String name = springLiteral(expression);
        SourceRef source = new SourceRef(relativePath, line, column);
        workers.add(new WorkerDefinition(
                "worker:" + relativePath + "#" + line + ":" + column,
                name,
                expression.isEmpty() ? "<missing>" : expression,
                WorkerKind.WORKER,
                WorkerDefinitionSource.SPRING_CONFIGURATION,
                name == null,
                source));
    }

    private String springLiteral(String value) {
        String trimmed = value.trim();
        if (trimmed.isEmpty()
                || trimmed.equals("null")
                || trimmed.contains("${")
                || trimmed.contains("#{")) {
            return null;
        }
        if (trimmed.length() >= 2
                && (trimmed.startsWith("\"") && trimmed.endsWith("\"")
                || trimmed.startsWith("'") && trimmed.endsWith("'"))) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }

    private String stripYamlComment(String line) {
        boolean singleQuoted = false;
        boolean doubleQuoted = false;
        for (int index = 0; index < line.length(); index++) {
            char current = line.charAt(index);
            if (current == '\'' && !doubleQuoted) {
                singleQuoted = !singleQuoted;
            } else if (current == '"' && !singleQuoted
                    && (index == 0 || line.charAt(index - 1) != '\\')) {
                doubleQuoted = !doubleQuoted;
            } else if (current == '#' && !singleQuoted && !doubleQuoted) {
                return line.substring(0, index);
            }
        }
        return line;
    }

    private int leadingWhitespace(String value) {
        int count = 0;
        while (count < value.length() && Character.isWhitespace(value.charAt(count))) {
            count++;
        }
        return count;
    }

    private List<WorkerFlowRelation> analyzeWorkerFlowRelations(
            List<WorkerDefinition> workers,
            List<DefinitionSite> definitionSites,
            Map<String, List<TypeSite>> typesBySimpleName
    ) {
        Set<String> knownWorkerNames = workers.stream()
                .map(WorkerDefinition::name)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (knownWorkerNames.isEmpty()) {
            return List.of();
        }
        Map<String, List<WorkerDefinition>> workersByName = new LinkedHashMap<>();
        for (WorkerDefinition worker : workers) {
            if (worker.name() != null) {
                workersByName.computeIfAbsent(worker.name(), ignored -> new ArrayList<>())
                        .add(worker);
            }
        }
        Map<String, Set<String>> wrapperBindings = analyzeWorkerWrapperBindings(
                knownWorkerNames,
                typesBySimpleName);
        List<WorkerFlowRelation> relations = new ArrayList<>();
        Set<String> relationKeys = new LinkedHashSet<>();
        for (TypeSite typeSite : allTypeSites(typesBySimpleName)) {
            List<MethodCallExpr> submitCalls = typeSite.declaration().findAll(
                            MethodCallExpr.class,
                            call -> belongsToType(call, typeSite.declaration())
                                    && call.getNameAsString().equals("submit")
                                    && call.getScope().isPresent()
                                    && !call.getArguments().isEmpty())
                    .stream()
                    .sorted(NODE_ORDER)
                    .toList();
            for (MethodCallExpr submitCall : submitCalls) {
                WorkerSubmitSite submitSite = resolveWorkerSubmitSite(
                        submitCall,
                        typeSite,
                        knownWorkerNames,
                        wrapperBindings,
                        typesBySimpleName);
                if (submitSite == null) {
                    continue;
                }
                List<SubmittedTarget> targets = resolveSubmittedTargets(
                                submitSite.flowExpression(),
                                typeSite,
                                definitionSites,
                                typesBySimpleName,
                                new HashSet<>()).stream()
                        .filter(target -> target.resolved()
                                || !target.label().equals("Flow")
                                && !target.label().equals("EventFlow"))
                        .toList();
                for (String workerName : submitSite.workerNames()) {
                    List<WorkerDefinition> matchingWorkers = workersByName.getOrDefault(
                            workerName,
                            List.of());
                    String workerDefinitionId = matchingWorkers.size() == 1
                            ? matchingWorkers.get(0).id()
                            : null;
                    for (SubmittedTarget target : targets) {
                        SourceRef source = sourceRef(typeSite.sourceUnit(), submitCall);
                        String targetKey = target.definitionId() == null
                                ? "label:" + target.label()
                                : "definition:" + target.definitionId();
                        String relationKey = workerName
                                + "|" + source.file() + ":" + source.line() + ":" + source.column()
                                + "|" + targetKey;
                        if (!relationKeys.add(relationKey)) {
                            continue;
                        }
                        relations.add(new WorkerFlowRelation(
                                "worker-relation:" + relationKey,
                                workerDefinitionId,
                                workerName,
                                target.definitionId(),
                                target.label(),
                                submitSite.flowExpression().toString(),
                                relationCardinality(submitCall),
                                workerDefinitionId != null && target.resolved()
                                        ? WorkerFlowRelationCertainty.SOURCE_CONFIRMED
                                        : WorkerFlowRelationCertainty.PARTIAL,
                                source));
                    }
                }
            }
        }
        return relations;
    }

    private Map<String, Set<String>> analyzeWorkerWrapperBindings(
            Set<String> knownWorkerNames,
            Map<String, List<TypeSite>> typesBySimpleName
    ) {
        Map<String, Set<String>> bindings = new LinkedHashMap<>();
        for (TypeSite typeSite : allTypeSites(typesBySimpleName)) {
            for (MethodDeclaration method : typeSite.declaration().getMethodsByName("submit")) {
                for (MethodCallExpr call : method.findAll(MethodCallExpr.class)) {
                    if (!call.getNameAsString().equals("worker")
                            || call.getArguments().isEmpty()) {
                        continue;
                    }
                    String name = resolveString(
                            call.getArgument(0),
                            typeSite.sourceUnit().constants());
                    if (knownWorkerNames.contains(name)) {
                        bindings.computeIfAbsent(
                                        typeSite.declaration().getNameAsString(),
                                        ignored -> new LinkedHashSet<>())
                                .add(name);
                    }
                }
            }
        }
        return bindings;
    }

    private WorkerSubmitSite resolveWorkerSubmitSite(
            MethodCallExpr submitCall,
            TypeSite sourceType,
            Set<String> knownWorkerNames,
            Map<String, Set<String>> wrapperBindings,
            Map<String, List<TypeSite>> typesBySimpleName
    ) {
        Expression scope = submitCall.getScope().orElseThrow();
        if (scope instanceof MethodCallExpr workerLookup
                && workerLookup.getNameAsString().equals("worker")
                && !workerLookup.getArguments().isEmpty()) {
            String name = resolveString(
                    workerLookup.getArgument(0),
                    sourceType.sourceUnit().constants());
            if (knownWorkerNames.contains(name)) {
                return new WorkerSubmitSite(Set.of(name), submitCall.getArgument(0));
            }
        }

        String receiverType = receiverType(scope, sourceType, typesBySimpleName);
        Set<String> boundNames = receiverType == null
                ? Set.of()
                : wrapperBindings.getOrDefault(simpleTypeName(receiverType), Set.of());
        if (!boundNames.isEmpty()) {
            return new WorkerSubmitSite(boundNames, submitCall.getArgument(0));
        }

        if (submitCall.getArguments().size() >= 2) {
            String name = resolveString(
                    submitCall.getArgument(0),
                    sourceType.sourceUnit().constants());
            if (knownWorkerNames.contains(name)) {
                return new WorkerSubmitSite(Set.of(name), submitCall.getArgument(1));
            }
        }
        return null;
    }

    private String receiverType(
            Expression expression,
            TypeSite sourceType,
            Map<String, List<TypeSite>> typesBySimpleName
    ) {
        String type = expressionType(expression, sourceType.sourceUnit(), typesBySimpleName);
        if (type != null) {
            return type;
        }
        if (expression instanceof MethodCallExpr call
                && (call.getNameAsString().equals("getObject")
                || call.getNameAsString().equals("getIfAvailable")
                || call.getNameAsString().equals("get"))
                && call.getScope().isPresent()) {
            String containerVariable = variableName(call.getScope().orElseThrow());
            String containerType = containerVariable == null
                    ? null
                    : sourceType.sourceUnit().variableTypes().get(containerVariable);
            return genericArgument(containerType);
        }
        return null;
    }

    private List<TypeSite> allTypeSites(Map<String, List<TypeSite>> typesBySimpleName) {
        Map<String, TypeSite> unique = new LinkedHashMap<>();
        typesBySimpleName.values().stream()
                .flatMap(List::stream)
                .forEach(site -> unique.putIfAbsent(
                        site.sourceUnit().relativePath() + ":" + line(site.declaration()),
                        site));
        return new ArrayList<>(unique.values());
    }

    private DefinitionSite analyzeDefinition(
            SourceUnit sourceUnit,
            MethodCallExpr builderCall,
            Map<String, List<TypeSite>> typesBySimpleName
    ) {
        List<AnalysisNotice> notices = new ArrayList<>();
        List<MethodCallExpr> calls = callsForBuilder(builderCall, notices, sourceUnit);
        Map<String, String> constants = sourceUnit.constants();

        String flowType = builderCall.getArguments().isEmpty()
                ? null
                : resolveString(builderCall.getArgument(0), constants);
        String flowTypeExpression = builderCall.getArguments().isEmpty()
                ? "<missing>"
                : builderCall.getArgument(0).toString();
        if (flowType == null) {
            notices.add(new AnalysisNotice(
                    "DYNAMIC_FLOW_TYPE",
                    NoticeSeverity.INFO,
                    "Flow type is computed at runtime: " + flowTypeExpression,
                    sourceRef(sourceUnit, builderCall)));
        }
        FlowKind kind = isScopeNamed(builderCall, "EventFlow") ? FlowKind.EVENT_FLOW : FlowKind.FLOW;
        boolean durable = calls.stream().anyMatch(this::makesDurable);

        List<StepNode> steps = new ArrayList<>();
        Map<String, BehaviorFacts> behaviorsByNode = new LinkedHashMap<>();
        Set<String> usedIds = new HashSet<>();
        int dynamicOrdinal = 0;

        for (MethodCallExpr call : calls) {
            if (!isStepCall(call)) {
                continue;
            }
            Expression stepIdArgument = call.getArguments().isEmpty()
                    ? null
                    : call.getArgument(0);
            String resolvedStepId = stepIdArgument == null
                    ? null
                    : resolveString(stepIdArgument, constants);
            boolean dynamicId = resolvedStepId == null;
            String graphStepId = resolvedStepId;
            if (graphStepId == null || !usedIds.add(graphStepId)) {
                dynamicOrdinal++;
                graphStepId = "__dynamic_step_" + line(call) + "_" + dynamicOrdinal;
                usedIds.add(graphStepId);
                if (resolvedStepId != null) {
                    notices.add(new AnalysisNotice(
                            "DUPLICATE_STEP_ID",
                            NoticeSeverity.WARNING,
                            "Duplicate step id '" + resolvedStepId
                                    + "' was given a synthetic graph id. Flower rejects duplicate ids at build time.",
                            sourceRef(sourceUnit, call)));
                }
            }

            Expression stepExpression = call.getArguments().size() >= 2
                    ? call.getArgument(1)
                    : null;
            String stepType = expressionType(stepExpression, sourceUnit, typesBySimpleName);
            Expression guardExpression = guardExpression(call);
            boolean guarded = guardExpression != null && !(guardExpression instanceof NullLiteralExpr);
            String guardType = expressionType(guardExpression, sourceUnit, typesBySimpleName);
            boolean repeatedDynamically = isInsideLoop(call);
            boolean dynamic = dynamicId || stepType == null || repeatedDynamically;

            if (dynamicId) {
                notices.add(new AnalysisNotice(
                        "DYNAMIC_STEP_ID",
                        NoticeSeverity.WARNING,
                        "Step id is computed at runtime: "
                                + (stepIdArgument == null ? "<missing>" : stepIdArgument),
                        sourceRef(sourceUnit, call)));
            }
            if (stepExpression != null && stepType == null) {
                notices.add(new AnalysisNotice(
                        "DYNAMIC_STEP_IMPLEMENTATION",
                        NoticeSeverity.INFO,
                        "Step implementation is supplied by an expression and was not resolved to one class: "
                                + stepExpression,
                        sourceRef(sourceUnit, call)));
            }
            if (repeatedDynamically) {
                notices.add(new AnalysisNotice(
                        "LOOP_ADDED_STEP",
                        NoticeSeverity.INFO,
                        "This declaration is inside a loop and can represent zero or more runtime Steps.",
                        sourceRef(sourceUnit, call)));
            }

            BehaviorFacts stepFacts = analyzeBehaviorType(
                    stepType,
                    "StepResult",
                    typesBySimpleName,
                    notices,
                    sourceRef(sourceUnit, call));
            BehaviorFacts guardFacts = guarded
                    ? analyzeBehaviorType(
                            guardType,
                            "GuardResult",
                            typesBySimpleName,
                            notices,
                            sourceRef(sourceUnit, call))
                    : BehaviorFacts.empty();
            BehaviorFacts combinedFacts = stepFacts.withGuardTargets(guardFacts.goToTargets());
            behaviorsByNode.put(graphStepId, combinedFacts);
            StepStructureFacts structureFacts = analyzeStepStructure(
                    stepType,
                    typesBySimpleName,
                    notices,
                    sourceRef(sourceUnit, call));

            String idExpression = stepIdArgument == null ? "<missing>" : stepIdArgument.toString();
            String stepExpressionText = stepExpression == null ? "<missing>" : stepExpression.toString();
            String guardExpressionText = guardExpression == null ? "" : guardExpression.toString();
            steps.add(new StepNode(
                    graphStepId,
                    idExpression,
                    stepType,
                    stepExpressionText,
                    dynamic,
                    "durableStep".equals(call.getNameAsString()),
                    guarded,
                    guardType,
                    guardExpressionText,
                    combinedFacts.behaviors().stream().sorted().toList(),
                    structureFacts.eventSubscriptions(),
                    structureFacts.phases(),
                    structureFacts.transitions(),
                    structureFacts.partial(),
                    sourceRef(sourceUnit, call)));
        }

        if (steps.isEmpty()) {
            notices.add(new AnalysisNotice(
                    "NO_STATIC_STEPS",
                    NoticeSeverity.WARNING,
                    "The builder was found, but no Step declaration could be resolved statically.",
                    sourceRef(sourceUnit, builderCall)));
        }

        List<Transition> transitions = buildTransitions(steps, behaviorsByNode);
        boolean partial = flowType == null
                || steps.stream().anyMatch(StepNode::dynamic)
                || notices.stream().anyMatch(notice ->
                notice.code().equals("UNRESOLVED_BUILDER_USAGE")
                        || notice.code().equals("DYNAMIC_STEP_ID")
                        || notice.code().equals("LOOP_ADDED_STEP"));

        SourceRef source = sourceRef(sourceUnit, builderCall);
        String owner = enclosingClass(builderCall)
                .map(ClassOrInterfaceDeclaration::getNameAsString)
                .orElse("Source");
        String method = enclosingCallable(builderCall)
                .map(CallableDeclaration::getNameAsString)
                .orElse("builder");
        String displayName = flowType == null ? owner + "." + method : flowType;
        String definitionId = source.file() + "#" + source.line() + ":" + source.column();

        FlowDefinition unhashed = new FlowDefinition(
                definitionId,
                displayName,
                flowType,
                flowTypeExpression,
                kind,
                durable,
                partial ? FlowCompleteness.PARTIAL_DYNAMIC : FlowCompleteness.STATIC_BEST_EFFORT,
                source,
                steps,
                transitions,
                notices,
                null);
        FlowDefinition definition = new FlowDefinition(
                unhashed.id(),
                unhashed.displayName(),
                unhashed.flowType(),
                unhashed.flowTypeExpression(),
                unhashed.kind(),
                unhashed.durable(),
                unhashed.completeness(),
                unhashed.source(),
                unhashed.steps(),
                unhashed.transitions(),
                unhashed.notices(),
                graphHasher.hash(unhashed));
        int ownerParameterCount = enclosingCallable(builderCall)
                .map(callable -> callable.getParameters().size())
                .orElse(-1);
        return new DefinitionSite(
                definition,
                owner,
                method,
                ownerParameterCount);
    }

    private List<FlowRelation> analyzeFlowRelations(
            List<DefinitionSite> definitionSites,
            Map<String, List<TypeSite>> typesBySimpleName
    ) {
        List<FlowRelation> relations = new ArrayList<>();
        Set<String> relationKeys = new LinkedHashSet<>();
        for (DefinitionSite definitionSite : definitionSites) {
            FlowDefinition definition = definitionSite.definition();
            for (StepNode step : definition.steps()) {
                if (step.stepType() == null) {
                    continue;
                }
                List<TypeSite> stepTypes = typesBySimpleName.getOrDefault(
                        simpleTypeName(step.stepType()),
                        List.of());
                if (stepTypes.size() != 1) {
                    continue;
                }
                TypeSite stepType = stepTypes.get(0);
                List<MethodCallExpr> submitCalls = stepType.declaration()
                        .findAll(MethodCallExpr.class, call ->
                                call.getNameAsString().equals("submit")
                                        && call.getScope().isPresent()
                                        && !call.getArguments().isEmpty());
                submitCalls.sort(NODE_ORDER);
                for (MethodCallExpr submitCall : submitCalls) {
                    boolean workerReceiver = isWorkerReceiver(submitCall, stepType);
                    List<SubmittedTarget> targets = resolveSubmittedTargets(
                            submitCall.getArgument(0),
                            stepType,
                            definitionSites,
                            typesBySimpleName,
                            new HashSet<>());
                    if (targets.isEmpty() || !workerReceiver
                            && targets.stream().noneMatch(SubmittedTarget::resolved)) {
                        continue;
                    }

                    appendFlowRelations(
                            relations,
                            relationKeys,
                            definition,
                            step,
                            targets,
                            submitCall.getArgument(0).toString(),
                            relationCardinality(submitCall),
                            workerReceiver,
                            stepType.sourceUnit(),
                            submitCall);
                }

                List<MethodReferenceExpr> submitReferences = stepType.declaration()
                        .findAll(MethodReferenceExpr.class, reference ->
                                reference.getIdentifier().equals("submit"));
                submitReferences.sort(NODE_ORDER);
                for (MethodReferenceExpr submitReference : submitReferences) {
                    Optional<MethodCallExpr> carrier = enclosingMethodCall(submitReference);
                    if (carrier.isEmpty() || carrier.orElseThrow().getScope().isEmpty()) {
                        continue;
                    }
                    MethodCallExpr carrierCall = carrier.orElseThrow();
                    Expression submittedExpression = carrierCall.getScope().orElseThrow();
                    boolean workerReceiver = isWorkerExpression(
                            submitReference.getScope(),
                            stepType);
                    List<SubmittedTarget> targets = resolveContainedFlowTargets(
                            submittedExpression,
                            stepType,
                            typesBySimpleName);
                    if (!workerReceiver || targets.isEmpty()) {
                        continue;
                    }
                    FlowRelationCardinality cardinality =
                            carrierCall.getNameAsString().equals("forEach")
                                    ? FlowRelationCardinality.ZERO_OR_MANY
                                    : FlowRelationCardinality.CONDITIONAL;
                    appendFlowRelations(
                            relations,
                            relationKeys,
                            definition,
                            step,
                            targets,
                            submittedExpression.toString(),
                            cardinality,
                            true,
                            stepType.sourceUnit(),
                            submitReference);
                }
            }
        }
        return relations;
    }

    private void appendFlowRelations(
            List<FlowRelation> relations,
            Set<String> relationKeys,
            FlowDefinition definition,
            StepNode step,
            List<SubmittedTarget> targets,
            String targetExpression,
            FlowRelationCardinality cardinality,
            boolean workerReceiver,
            SourceUnit sourceUnit,
            Node sourceNode
    ) {
        for (SubmittedTarget target : targets) {
            FlowRelationCertainty certainty = workerReceiver && target.resolved()
                    ? FlowRelationCertainty.SOURCE_CONFIRMED
                    : FlowRelationCertainty.PARTIAL;
            SourceRef source = sourceRef(sourceUnit, sourceNode);
            String targetKey = target.definitionId() == null
                    ? "label:" + target.label()
                    : "definition:" + target.definitionId();
            String relationKey = definition.id()
                    + "|" + step.id()
                    + "|" + source.file() + ":" + source.line() + ":" + source.column()
                    + "|" + targetKey;
            if (!relationKeys.add(relationKey)) {
                continue;
            }
            relations.add(new FlowRelation(
                    "relation:" + relationKey,
                    definition.id(),
                    step.id(),
                    target.definitionId(),
                    target.label(),
                    targetExpression,
                    FlowRelationKind.SUBMITS,
                    cardinality,
                    certainty,
                    source));
        }
    }

    private List<SubmittedTarget> resolveSubmittedTargets(
            Expression expression,
            TypeSite sourceType,
            List<DefinitionSite> definitionSites,
            Map<String, List<TypeSite>> typesBySimpleName,
            Set<String> visited
    ) {
        String visitKey = sourceType.sourceUnit().relativePath() + ":" + nodeKey(expression);
        if (!visited.add(visitKey)) {
            return List.of();
        }

        List<SubmittedTarget> targets = new ArrayList<>();
        if (expression instanceof EnclosedExpr enclosed) {
            targets.addAll(resolveSubmittedTargets(
                    enclosed.getInner(),
                    sourceType,
                    definitionSites,
                    typesBySimpleName,
                    visited));
        } else if (expression instanceof ConditionalExpr conditional) {
            targets.addAll(resolveSubmittedTargets(
                    conditional.getThenExpr(),
                    sourceType,
                    definitionSites,
                    typesBySimpleName,
                    visited));
            targets.addAll(resolveSubmittedTargets(
                    conditional.getElseExpr(),
                    sourceType,
                    definitionSites,
                    typesBySimpleName,
                    visited));
        } else if (expression instanceof NameExpr name) {
            List<VariableDeclarator> variables = sourceType.declaration()
                    .findAll(VariableDeclarator.class, variable ->
                            variable.getNameAsString().equals(name.getNameAsString()));
            for (VariableDeclarator variable : variables) {
                variable.getInitializer().ifPresent(initializer ->
                        targets.addAll(resolveSubmittedTargets(
                                initializer,
                                sourceType,
                                definitionSites,
                                typesBySimpleName,
                                visited)));
            }
        } else if (expression instanceof ObjectCreationExpr creation) {
            for (Expression argument : creation.getArguments()) {
                targets.addAll(resolveSubmittedTargets(
                        argument,
                        sourceType,
                        definitionSites,
                        typesBySimpleName,
                        visited));
            }
        } else if (expression instanceof MethodCallExpr call) {
            targets.addAll(definitionsReturnedByFactoryCall(
                    call,
                    sourceType,
                    definitionSites,
                    typesBySimpleName));
            if (targets.isEmpty()
                    && call.getScope().isPresent()
                    && (call.getNameAsString().equals("flow")
                    || call.getNameAsString().equals("getFlow"))) {
                targets.addAll(resolveSubmittedTargets(
                        call.getScope().orElseThrow(),
                        sourceType,
                        definitionSites,
                        typesBySimpleName,
                        visited));
            }
            if (targets.isEmpty()) {
                targets.addAll(definitionsReturnedThroughSourceMethod(
                        call,
                        sourceType,
                        definitionSites,
                        typesBySimpleName,
                        visited));
            }
        }

        if (targets.isEmpty()) {
            String flowType = flowLikeExpressionType(expression, sourceType, typesBySimpleName);
            if (flowType != null) {
                targets.add(new SubmittedTarget(
                        null,
                        simpleTypeName(flowType),
                        false));
            }
        }

        Map<String, SubmittedTarget> unique = new LinkedHashMap<>();
        for (SubmittedTarget target : targets) {
            String key = target.definitionId() == null
                    ? "label:" + target.label()
                    : "definition:" + target.definitionId();
            unique.putIfAbsent(key, target);
        }
        return new ArrayList<>(unique.values());
    }

    private List<SubmittedTarget> definitionsReturnedThroughSourceMethod(
            MethodCallExpr call,
            TypeSite sourceType,
            List<DefinitionSite> definitionSites,
            Map<String, List<TypeSite>> typesBySimpleName,
            Set<String> visited
    ) {
        if (call.getScope().isEmpty()) {
            return List.of();
        }
        String ownerType = receiverType(
                call.getScope().orElseThrow(),
                sourceType,
                typesBySimpleName);
        if (ownerType == null) {
            return List.of();
        }
        List<TypeSite> candidates = typesBySimpleName.getOrDefault(
                simpleTypeName(ownerType),
                List.of());
        if (candidates.size() != 1) {
            return List.of();
        }
        TypeSite candidate = candidates.get(0);
        List<SubmittedTarget> targets = new ArrayList<>();
        for (MethodDeclaration method
                : candidate.declaration().getMethodsByName(call.getNameAsString())) {
            if (method.getParameters().size() != call.getArguments().size()) {
                continue;
            }
            for (ReturnStmt returnStmt : method.findAll(ReturnStmt.class)) {
                if (returnStmt.getExpression().isPresent()) {
                    targets.addAll(resolveSubmittedTargets(
                            returnStmt.getExpression().orElseThrow(),
                            candidate,
                            definitionSites,
                            typesBySimpleName,
                            visited));
                }
            }
        }
        return targets;
    }

    private List<SubmittedTarget> definitionsReturnedByFactoryCall(
            MethodCallExpr call,
            TypeSite sourceType,
            List<DefinitionSite> definitionSites,
            Map<String, List<TypeSite>> typesBySimpleName
    ) {
        String scopeType = call.getScope().isEmpty()
                ? sourceType.declaration().getNameAsString()
                : receiverType(
                        call.getScope().orElseThrow(),
                        sourceType,
                        typesBySimpleName);
        if (scopeType == null) {
            return List.of();
        }
        String ownerType = simpleTypeName(scopeType);
        List<DefinitionSite> matches = definitionSites.stream()
                .filter(site -> site.ownerType().equals(ownerType))
                .filter(site -> site.ownerMethod().equals(call.getNameAsString()))
                .filter(site -> site.ownerParameterCount() < 0
                        || site.ownerParameterCount() == call.getArguments().size())
                .toList();
        return matches.stream()
                .map(site -> new SubmittedTarget(
                        site.definition().id(),
                        site.definition().displayName(),
                        true))
                .toList();
    }

    private String flowLikeExpressionType(
            Expression expression,
            TypeSite sourceType,
            Map<String, List<TypeSite>> typesBySimpleName
    ) {
        String type = expressionType(expression, sourceType.sourceUnit(), typesBySimpleName);
        if (type == null && expression instanceof MethodCallExpr call
                && call.getNameAsString().equals("get")
                && call.getScope().isPresent()) {
            String scopeVariable = variableName(call.getScope().orElseThrow());
            String containerType = scopeVariable == null
                    ? null
                    : sourceType.sourceUnit().variableTypes().get(scopeVariable);
            if (containerType == null && scopeVariable != null) {
                containerType = variableInitializerType(
                        scopeVariable,
                        sourceType,
                        typesBySimpleName);
            }
            type = genericArgument(containerType);
        }
        return isFlowLikeType(type) ? type : null;
    }

    private List<SubmittedTarget> resolveContainedFlowTargets(
            Expression containerExpression,
            TypeSite sourceType,
            Map<String, List<TypeSite>> typesBySimpleName
    ) {
        String containerType = expressionType(
                containerExpression,
                sourceType.sourceUnit(),
                typesBySimpleName);
        if (containerType == null && containerExpression instanceof NameExpr name) {
            containerType = sourceType.sourceUnit().variableTypes().get(name.getNameAsString());
            if (containerType == null) {
                containerType = variableInitializerType(
                        name.getNameAsString(),
                        sourceType,
                        typesBySimpleName);
            }
        }
        String flowType = genericArgument(containerType);
        if (!isFlowLikeType(flowType)) {
            return List.of();
        }
        return List.of(new SubmittedTarget(
                null,
                simpleTypeName(flowType),
                false));
    }

    private String variableInitializerType(
            String variableName,
            TypeSite sourceType,
            Map<String, List<TypeSite>> typesBySimpleName
    ) {
        Set<String> types = new LinkedHashSet<>();
        for (VariableDeclarator variable : sourceType.declaration()
                .findAll(VariableDeclarator.class, candidate ->
                        candidate.getNameAsString().equals(variableName))) {
            variable.getInitializer()
                    .map(initializer -> expressionType(
                            initializer,
                            sourceType.sourceUnit(),
                            typesBySimpleName))
                    .filter(Objects::nonNull)
                    .ifPresent(types::add);
        }
        return types.size() == 1 ? types.iterator().next() : null;
    }

    private String genericArgument(String typeName) {
        if (typeName == null) {
            return null;
        }
        int start = typeName.indexOf('<');
        int end = typeName.lastIndexOf('>');
        return start >= 0 && end > start ? typeName.substring(start + 1, end).trim() : null;
    }

    private boolean isFlowLikeType(String typeName) {
        if (typeName == null) {
            return false;
        }
        String simpleName = simpleTypeName(typeName);
        return simpleName.equals("Flow")
                || simpleName.equals("EventFlow")
                || simpleName.endsWith("Flow");
    }

    private boolean isWorkerReceiver(MethodCallExpr submitCall, TypeSite sourceType) {
        return isWorkerExpression(submitCall.getScope().orElseThrow(), sourceType);
    }

    private boolean isWorkerExpression(Expression scope, TypeSite sourceType) {
        String scopeVariable = variableName(scope);
        String scopeType = scopeVariable == null
                ? null
                : sourceType.sourceUnit().variableTypes().get(scopeVariable);
        if (scopeType != null && simpleTypeName(scopeType).endsWith("Worker")) {
            return true;
        }
        return scope.toString().toLowerCase(Locale.ROOT).contains("worker");
    }

    private Optional<MethodCallExpr> enclosingMethodCall(Node node) {
        Node cursor = node;
        while (cursor.getParentNode().isPresent()) {
            cursor = cursor.getParentNode().orElseThrow();
            if (cursor instanceof MethodCallExpr call) {
                return Optional.of(call);
            }
            if (cursor instanceof CallableDeclaration<?>) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private FlowRelationCardinality relationCardinality(MethodCallExpr submitCall) {
        if (isInsideLoop(submitCall) || isInsideForEach(submitCall)) {
            return FlowRelationCardinality.ZERO_OR_MANY;
        }
        if (isInsideConditional(submitCall)) {
            return FlowRelationCardinality.CONDITIONAL;
        }
        return FlowRelationCardinality.ONE_PER_CALL;
    }

    private boolean isInsideForEach(Node node) {
        Optional<CallableDeclaration<?>> callable = enclosingCallable(node);
        Node cursor = node;
        while (cursor.getParentNode().isPresent()) {
            cursor = cursor.getParentNode().orElseThrow();
            if (cursor instanceof MethodCallExpr call
                    && call.getNameAsString().equals("forEach")) {
                return true;
            }
            if (callable.isPresent() && cursor == callable.orElseThrow()) {
                return false;
            }
        }
        return false;
    }

    private boolean isInsideConditional(Node node) {
        Optional<CallableDeclaration<?>> callable = enclosingCallable(node);
        Node cursor = node;
        while (cursor.getParentNode().isPresent()) {
            cursor = cursor.getParentNode().orElseThrow();
            if (cursor instanceof IfStmt
                    || cursor instanceof ConditionalExpr
                    || cursor instanceof SwitchEntry) {
                return true;
            }
            if (callable.isPresent() && cursor == callable.orElseThrow()) {
                return false;
            }
        }
        return false;
    }

    private List<MethodCallExpr> callsForBuilder(
            MethodCallExpr builderCall,
            List<AnalysisNotice> notices,
            SourceUnit sourceUnit
    ) {
        List<MethodCallExpr> directCalls = directCallChain(builderCall);
        boolean hasBuild = directCalls.stream().anyMatch(call -> call.getNameAsString().equals("build"));
        if (hasBuild) {
            return directCalls;
        }

        MethodCallExpr top = directCalls.get(directCalls.size() - 1);
        Optional<VariableDeclarator> variable = top.getParentNode()
                .filter(VariableDeclarator.class::isInstance)
                .map(VariableDeclarator.class::cast);
        if (variable.isEmpty()) {
            Optional<AssignExpr> assignment = top.getParentNode()
                    .filter(AssignExpr.class::isInstance)
                    .map(AssignExpr.class::cast);
            if (assignment.isPresent() && assignment.orElseThrow().getTarget().isNameExpr()) {
                String variableName = assignment.orElseThrow().getTarget().asNameExpr().getNameAsString();
                return mergeCalls(
                        directCalls,
                        variableRootedCalls(builderCall, variableName));
            }
            notices.add(new AnalysisNotice(
                    "UNRESOLVED_BUILDER_USAGE",
                    NoticeSeverity.WARNING,
                    "The Flow builder escapes the direct call chain; later Step additions may be missing.",
                    sourceRef(sourceUnit, builderCall)));
            return directCalls;
        }

        String variableName = variable.orElseThrow().getNameAsString();
        return mergeCalls(directCalls, variableRootedCalls(builderCall, variableName));
    }

    private List<MethodCallExpr> directCallChain(MethodCallExpr builderCall) {
        List<MethodCallExpr> result = new ArrayList<>();
        MethodCallExpr current = builderCall;
        result.add(current);
        while (true) {
            Node parentNode = current.getParentNode().orElse(null);
            if (!(parentNode instanceof MethodCallExpr parent)
                    || parent.getScope().orElse(null) != current) {
                break;
            }
            current = parent;
            result.add(current);
        }
        return result;
    }

    private List<MethodCallExpr> variableRootedCalls(MethodCallExpr builderCall, String variableName) {
        Node scope = enclosingBlock(builderCall)
                .map(Node.class::cast)
                .orElseGet(() -> enclosingCallable(builderCall)
                        .map(Node.class::cast)
                        .orElse(builderCall));
        int builderLine = line(builderCall);
        return scope.findAll(MethodCallExpr.class, call ->
                        line(call) >= builderLine && isRootedAt(call, variableName))
                .stream()
                .toList();
    }

    private boolean isRootedAt(MethodCallExpr call, String variableName) {
        Expression cursor = call;
        while (cursor instanceof MethodCallExpr methodCall) {
            if (methodCall.getScope().isEmpty()) {
                return false;
            }
            cursor = methodCall.getScope().orElseThrow();
        }
        return cursor instanceof NameExpr nameExpr
                && nameExpr.getNameAsString().equals(variableName);
    }

    private List<MethodCallExpr> mergeCalls(
            List<MethodCallExpr> first,
            List<MethodCallExpr> second
    ) {
        Map<String, MethodCallExpr> unique = new LinkedHashMap<>();
        Stream.concat(first.stream(), second.stream())
                .sorted(NODE_ORDER)
                .forEach(call -> unique.putIfAbsent(nodeKey(call), call));
        return new ArrayList<>(unique.values());
    }

    private List<Transition> buildTransitions(
            List<StepNode> steps,
            Map<String, BehaviorFacts> behaviorsByNode
    ) {
        List<Transition> transitions = new ArrayList<>();
        int ordinal = 0;
        for (int index = 0; index + 1 < steps.size(); index++) {
            StepNode from = steps.get(index);
            StepNode to = steps.get(index + 1);
            TransitionCertainty certainty = from.dynamic() || to.dynamic()
                    ? TransitionCertainty.PARTIAL
                    : TransitionCertainty.DECLARED;
            transitions.add(new Transition(
                    "transition-" + (++ordinal),
                    from.id(),
                    to.id(),
                    TransitionKind.DONE_NEXT,
                    TransitionEvidence.FLOW_BUILDER,
                    certainty,
                    "done",
                    from.source()));
        }

        for (StepNode step : steps) {
            BehaviorFacts facts = behaviorsByNode.getOrDefault(step.id(), BehaviorFacts.empty());
            for (ResolvedTarget target : facts.goToTargets()) {
                transitions.add(new Transition(
                        "transition-" + (++ordinal),
                        step.id(),
                        target.target(),
                        target.guard() ? TransitionKind.GUARD_GO_TO : TransitionKind.GO_TO,
                        TransitionEvidence.STEP_SOURCE,
                        TransitionCertainty.SOURCE_LITERAL,
                        target.guard() ? "guard goTo" : "goTo",
                        target.source()));
            }
            if (facts.behaviors().contains(StepBehavior.FINISH)) {
                transitions.add(new Transition(
                        "transition-" + (++ordinal),
                        step.id(),
                        null,
                        TransitionKind.FINISH,
                        TransitionEvidence.STEP_SOURCE,
                        TransitionCertainty.SOURCE_LITERAL,
                        "finish",
                        step.source()));
            }
            if (facts.behaviors().contains(StepBehavior.FAIL)) {
                transitions.add(new Transition(
                        "transition-" + (++ordinal),
                        step.id(),
                        null,
                        TransitionKind.FAIL,
                        TransitionEvidence.STEP_SOURCE,
                        TransitionCertainty.SOURCE_LITERAL,
                        "fail",
                        step.source()));
            }
        }
        return transitions;
    }

    private StepStructureFacts analyzeStepStructure(
            String typeName,
            Map<String, List<TypeSite>> typesBySimpleName,
            List<AnalysisNotice> notices,
            SourceRef declarationSource
    ) {
        if (typeName == null) {
            return StepStructureFacts.empty();
        }
        List<TypeSite> candidates = typesBySimpleName.getOrDefault(
                simpleTypeName(typeName),
                List.of());
        if (candidates.size() != 1) {
            return StepStructureFacts.empty();
        }

        TypeSite typeSite = candidates.get(0);
        ClassOrInterfaceDeclaration declaration = typeSite.declaration();
        SourceUnit sourceUnit = typeSite.sourceUnit();
        List<EventSubscription> subscriptions = declaration.findAll(
                        MethodCallExpr.class,
                        call -> belongsToType(call, declaration))
                .stream()
                .sorted(NODE_ORDER)
                .map(call -> eventSubscription(call, sourceUnit))
                .flatMap(Optional::stream)
                .toList();

        List<SwitchEntry> entries = declaration.findAll(
                        SwitchEntry.class,
                        entry -> belongsToStepNoSwitch(entry))
                .stream()
                .sorted(NODE_ORDER)
                .toList();
        List<StepPhase> phases = new ArrayList<>();
        List<StepPhaseTransition> transitions = new ArrayList<>();
        Set<String> structuredSetStepNoCalls = new HashSet<>();
        boolean partial = false;
        for (SwitchEntry entry : entries) {
            if (entry.getLabels().isEmpty()) {
                continue;
            }
            for (Expression labelExpression : entry.getLabels()) {
                Integer stepNo = resolveInteger(labelExpression, sourceUnit.integerConstants());
                if (stepNo == null) {
                    partial = true;
                    continue;
                }
                partial |= addStepNoPhase(
                        stepNo,
                        labelExpression,
                        entry,
                        declaration,
                        sourceUnit,
                        phases,
                        transitions,
                        structuredSetStepNoCalls);
            }
        }

        List<IfStmt> stepNoIfStatements = declaration.findAll(
                        IfStmt.class,
                        statement -> belongsToType(statement, declaration))
                .stream()
                .sorted(NODE_ORDER)
                .toList();
        for (IfStmt ifStmt : stepNoIfStatements) {
            for (StepNoPhaseCondition condition : literalStepNoConditions(
                    ifStmt.getCondition(),
                    sourceUnit)) {
                partial |= addStepNoPhase(
                        condition.stepNo(),
                        condition.expression(),
                        ifStmt.getThenStmt(),
                        declaration,
                        sourceUnit,
                        phases,
                        transitions,
                        structuredSetStepNoCalls);
            }
        }

        List<ConditionalExpr> stepNoConditionals = declaration.findAll(
                        ConditionalExpr.class,
                        expression -> belongsToType(expression, declaration))
                .stream()
                .sorted(NODE_ORDER)
                .toList();
        for (ConditionalExpr conditional : stepNoConditionals) {
            for (StepNoPhaseCondition condition : literalStepNoConditions(
                    conditional.getCondition(),
                    sourceUnit)) {
                partial |= addStepNoPhase(
                        condition.stepNo(),
                        condition.expression(),
                        conditional.getThenExpr(),
                        declaration,
                        sourceUnit,
                        phases,
                        transitions,
                        structuredSetStepNoCalls);
            }
        }

        phases = phases.stream()
                .collect(LinkedHashMap<Integer, StepPhase>::new,
                        (unique, phase) -> unique.putIfAbsent(phase.stepNo(), phase),
                        LinkedHashMap::putAll)
                .values().stream()
                .sorted(Comparator.comparingInt(StepPhase::stepNo))
                .toList();
        transitions = transitions.stream()
                .collect(LinkedHashMap<String, StepPhaseTransition>::new,
                        (unique, transition) -> unique.putIfAbsent(
                                transition.fromStepNo() + ":" + transition.toExpression()
                                        + ":" + transition.source().file()
                                        + ":" + transition.source().line(),
                                transition),
                        LinkedHashMap::putAll)
                .values().stream()
                .toList();

        Set<Integer> phaseNumbers = phases.stream()
                .map(StepPhase::stepNo)
                .collect(java.util.stream.Collectors.toSet());
        if (transitions.stream().anyMatch(transition ->
                transition.toStepNo() != null
                        && !phaseNumbers.contains(transition.toStepNo()))) {
            partial = true;
        }
        List<MethodCallExpr> setStepNoCalls = declaration.findAll(MethodCallExpr.class).stream()
                .filter(call -> belongsToType(call, declaration))
                .filter(call -> isStepContextCall(call, "setStepNo"))
                .toList();
        if (setStepNoCalls.stream().anyMatch(call ->
                !structuredSetStepNoCalls.contains(nodeKey(call)))) {
            partial = true;
        }
        boolean usesStepNoForControlFlow = declaration.findAll(MethodCallExpr.class).stream()
                .anyMatch(call -> belongsToType(call, declaration)
                        && isStepContextCall(call, "stepNo")
                        && isControlFlowUse(call));
        boolean hasStepNoStructure = usesStepNoForControlFlow || !setStepNoCalls.isEmpty();
        if (hasStepNoStructure && phases.isEmpty()) {
            partial = true;
            notices.add(new AnalysisNotice(
                    "STEP_NO_STRUCTURE_PARTIAL",
                    NoticeSeverity.INFO,
                    "Step source uses or updates stepNo for control flow, but no literal internal phases were resolved.",
                    declarationSource));
        }
        if (partial && !phases.isEmpty()) {
            notices.add(new AnalysisNotice(
                    "STEP_NO_STRUCTURE_PARTIAL",
                    NoticeSeverity.INFO,
                    "Some internal stepNo cases or targets are computed and were not guessed.",
                    declarationSource));
        }
        return new StepStructureFacts(subscriptions, phases, transitions, partial);
    }

    private boolean addStepNoPhase(
            int stepNo,
            Expression phaseExpression,
            Node phaseBody,
            ClassOrInterfaceDeclaration declaration,
            SourceUnit sourceUnit,
            List<StepPhase> phases,
            List<StepPhaseTransition> transitions,
            Set<String> structuredSetStepNoCalls
    ) {
        List<MethodCallExpr> calls = reachableCalls(phaseBody, declaration);
        List<SignalUse> signalUses = calls.stream()
                .map(call -> signalUse(call, sourceUnit))
                .flatMap(Optional::stream)
                .distinct()
                .toList();
        boolean startsTimeout = calls.stream()
                .anyMatch(call -> isStepContextCall(call, "startTimeout"));
        boolean checksTimeout = calls.stream()
                .anyMatch(call -> isStepContextCall(call, "timedOut"));
        phases.add(new StepPhase(
                stepNo,
                phaseLabel(phaseExpression, stepNo),
                signalUses,
                startsTimeout,
                checksTimeout,
                sourceRef(sourceUnit, phaseExpression)));

        boolean partial = false;
        for (MethodCallExpr call : calls) {
            if (!isStepContextCall(call, "setStepNo") || call.getArguments().isEmpty()) {
                continue;
            }
            structuredSetStepNoCalls.add(nodeKey(call));
            Expression targetExpression = call.getArgument(0);
            Integer target = resolveInteger(targetExpression, sourceUnit.integerConstants());
            if (target == null) {
                partial = true;
            }
            transitions.add(new StepPhaseTransition(
                    stepNo,
                    target,
                    targetExpression.toString(),
                    isGuardedByTimeout(call)
                            ? InternalTransitionTrigger.TIMEOUT
                            : InternalTransitionTrigger.SET_STEP_NO,
                    sourceRef(sourceUnit, call)));
        }
        return partial;
    }

    private List<StepNoPhaseCondition> literalStepNoConditions(
            Expression condition,
            SourceUnit sourceUnit
    ) {
        Map<String, BinaryExpr> comparisons = new LinkedHashMap<>();
        if (condition instanceof BinaryExpr binary) {
            comparisons.put(nodeKey(binary), binary);
        }
        condition.findAll(BinaryExpr.class).forEach(binary ->
                comparisons.putIfAbsent(nodeKey(binary), binary));

        List<StepNoPhaseCondition> conditions = new ArrayList<>();
        for (BinaryExpr comparison : comparisons.values()) {
            if (comparison.getOperator() != BinaryExpr.Operator.EQUALS) {
                continue;
            }
            Expression valueExpression = null;
            if (isStepNoExpression(comparison.getLeft())) {
                valueExpression = comparison.getRight();
            } else if (isStepNoExpression(comparison.getRight())) {
                valueExpression = comparison.getLeft();
            }
            if (valueExpression == null) {
                continue;
            }
            Integer stepNo = resolveInteger(valueExpression, sourceUnit.integerConstants());
            if (stepNo != null) {
                conditions.add(new StepNoPhaseCondition(stepNo, valueExpression));
            }
        }
        return conditions;
    }

    private boolean isStepNoExpression(Expression expression) {
        Expression candidate = expression;
        while (candidate instanceof EnclosedExpr enclosed) {
            candidate = enclosed.getInner();
        }
        return candidate instanceof MethodCallExpr call
                && isStepContextCall(call, "stepNo");
    }

    private boolean isControlFlowUse(MethodCallExpr stepNoCall) {
        Optional<CallableDeclaration<?>> callable = enclosingCallable(stepNoCall);
        Node cursor = stepNoCall;
        while (cursor.getParentNode().isPresent()) {
            cursor = cursor.getParentNode().orElseThrow();
            if (cursor instanceof SwitchExpr switchExpr
                    && isWithin(stepNoCall, switchExpr.getSelector())) {
                return true;
            }
            if (cursor instanceof SwitchStmt switchStmt
                    && isWithin(stepNoCall, switchStmt.getSelector())) {
                return true;
            }
            if (cursor instanceof IfStmt ifStmt
                    && isWithin(stepNoCall, ifStmt.getCondition())) {
                return true;
            }
            if (cursor instanceof ConditionalExpr conditional
                    && isWithin(stepNoCall, conditional.getCondition())) {
                return true;
            }
            if (cursor instanceof WhileStmt whileStmt
                    && isWithin(stepNoCall, whileStmt.getCondition())) {
                return true;
            }
            if (cursor instanceof DoStmt doStmt
                    && isWithin(stepNoCall, doStmt.getCondition())) {
                return true;
            }
            if (cursor instanceof ForStmt forStmt
                    && forStmt.getCompare().stream()
                    .anyMatch(compare -> isWithin(stepNoCall, compare))) {
                return true;
            }
            if (callable.isPresent() && cursor == callable.orElseThrow()) {
                return false;
            }
        }
        return false;
    }

    private boolean isWithin(Node node, Node possibleAncestor) {
        Node cursor = node;
        while (true) {
            if (cursor == possibleAncestor) {
                return true;
            }
            Optional<Node> parent = cursor.getParentNode();
            if (parent.isEmpty()) {
                return false;
            }
            cursor = parent.orElseThrow();
        }
    }

    private Optional<EventSubscription> eventSubscription(
            MethodCallExpr call,
            SourceUnit sourceUnit
    ) {
        EventSubscriptionKind kind;
        Expression eventExpression;
        List<SignalUse> emittedSignals = List.of();
        boolean filtered = false;
        if (isStepContextCall(call, "subscribe") && call.getArguments().size() >= 2) {
            kind = EventSubscriptionKind.SUBSCRIBE;
            eventExpression = call.getArgument(0);
            Expression handler = call.getArgument(1);
            emittedSignals = handler.findAll(MethodCallExpr.class).stream()
                    .map(signalCall -> signalUse(signalCall, sourceUnit))
                    .flatMap(Optional::stream)
                    .filter(signal -> signal.operation() == SignalOperation.EMIT)
                    .toList();
            filtered = !handler.findAll(IfStmt.class).isEmpty()
                    || !handler.findAll(ConditionalExpr.class).isEmpty();
        } else if (call.getNameAsString().equals("event")
                && isScopeNamed(call, "AwaitCondition")
                && !call.getArguments().isEmpty()) {
            kind = EventSubscriptionKind.AWAIT;
            eventExpression = call.getArgument(0);
        } else {
            return Optional.empty();
        }

        String eventType = eventExpression instanceof ClassExpr classExpr
                ? classExpr.getType().asString()
                : null;
        String lifecycleMethod = enclosingCallable(call)
                .map(CallableDeclaration::getNameAsString)
                .orElse("<source>");
        return Optional.of(new EventSubscription(
                kind,
                eventType,
                eventExpression.toString(),
                lifecycleMethod,
                emittedSignals,
                filtered,
                sourceRef(sourceUnit, call)));
    }

    private Optional<SignalUse> signalUse(MethodCallExpr call, SourceUnit sourceUnit) {
        SignalOperation operation = switch (call.getNameAsString()) {
            case "signal" -> SignalOperation.EMIT;
            case "hasSignal" -> SignalOperation.CHECK;
            case "signalPayload" -> SignalOperation.READ;
            case "consumeSignal" -> SignalOperation.CONSUME;
            case "clearSignal" -> SignalOperation.CLEAR;
            default -> null;
        };
        if (operation == null
                || !isStepContextCall(call, call.getNameAsString())
                || call.getArguments().isEmpty()) {
            return Optional.empty();
        }
        Expression signalExpression = call.getArgument(0);
        return Optional.of(new SignalUse(
                resolveString(signalExpression, sourceUnit.constants()),
                signalExpression.toString(),
                operation,
                sourceRef(sourceUnit, call)));
    }

    private List<MethodCallExpr> reachableCalls(
            Node root,
            ClassOrInterfaceDeclaration declaration
    ) {
        Map<String, MethodCallExpr> calls = new LinkedHashMap<>();
        Set<String> visitedMethods = new LinkedHashSet<>();
        collectReachableCalls(root, declaration, calls, visitedMethods);
        return new ArrayList<>(calls.values());
    }

    private void collectReachableCalls(
            Node root,
            ClassOrInterfaceDeclaration declaration,
            Map<String, MethodCallExpr> calls,
            Set<String> visitedMethods
    ) {
        if (visitedMethods.size() > 64) {
            return;
        }
        List<MethodCallExpr> localCalls = root.findAll(MethodCallExpr.class).stream()
                .filter(call -> belongsToType(call, declaration))
                .sorted(NODE_ORDER)
                .toList();
        for (MethodCallExpr call : localCalls) {
            calls.putIfAbsent(nodeKey(call), call);
            if (!isLocalMethodCall(call)) {
                continue;
            }
            for (MethodDeclaration method : declaration.getMethodsByName(call.getNameAsString())) {
                if (method.getParameters().size() != call.getArguments().size()) {
                    continue;
                }
                String methodKey = nodeKey(method);
                if (visitedMethods.add(methodKey)) {
                    collectReachableCalls(method, declaration, calls, visitedMethods);
                }
            }
        }
    }

    private boolean isLocalMethodCall(MethodCallExpr call) {
        return call.getScope().isEmpty()
                || call.getScope().orElseThrow() instanceof ThisExpr;
    }

    private boolean belongsToType(Node node, ClassOrInterfaceDeclaration declaration) {
        return enclosingClass(node).orElse(null) == declaration;
    }

    private boolean belongsToStepNoSwitch(SwitchEntry entry) {
        Node cursor = entry;
        while (cursor.getParentNode().isPresent()) {
            cursor = cursor.getParentNode().orElseThrow();
            Expression selector = null;
            if (cursor instanceof SwitchExpr switchExpr) {
                selector = switchExpr.getSelector();
            } else if (cursor instanceof SwitchStmt switchStmt) {
                selector = switchStmt.getSelector();
            }
            if (selector != null) {
                return selector instanceof MethodCallExpr call
                        && isStepContextCall(call, "stepNo");
            }
        }
        return false;
    }

    private boolean isStepContextCall(MethodCallExpr call, String methodName) {
        if (!call.getNameAsString().equals(methodName) || call.getScope().isEmpty()) {
            return false;
        }
        Expression scope = call.getScope().orElseThrow();
        if (!(scope instanceof NameExpr name)) {
            return false;
        }
        String variable = name.getNameAsString();
        return enclosingCallable(call).stream()
                .flatMap(callable -> callable.getParameters().stream())
                .anyMatch(parameter -> parameter.getNameAsString().equals(variable)
                        && simpleTypeName(parameter.getTypeAsString()).equals("StepContext"));
    }

    private String phaseLabel(Expression expression, int stepNo) {
        if (expression instanceof NameExpr name) {
            return name.getNameAsString();
        }
        if (expression instanceof FieldAccessExpr fieldAccess) {
            return fieldAccess.getNameAsString();
        }
        return "stepNo " + stepNo;
    }

    private boolean isGuardedByTimeout(MethodCallExpr call) {
        Optional<CallableDeclaration<?>> callable = enclosingCallable(call);
        Node cursor = call;
        while (cursor.getParentNode().isPresent()) {
            cursor = cursor.getParentNode().orElseThrow();
            if (cursor instanceof IfStmt ifStmt
                    && ifStmt.getCondition().findAll(MethodCallExpr.class).stream()
                    .anyMatch(candidate -> isStepContextCall(candidate, "timedOut"))) {
                return true;
            }
            if (callable.isPresent() && cursor == callable.orElseThrow()) {
                return false;
            }
        }
        return false;
    }

    private BehaviorFacts analyzeBehaviorType(
            String typeName,
            String resultType,
            Map<String, List<TypeSite>> typesBySimpleName,
            List<AnalysisNotice> notices,
            SourceRef declarationSource
    ) {
        if (typeName == null) {
            return BehaviorFacts.empty();
        }
        String simpleName = simpleTypeName(typeName);
        List<TypeSite> candidates = typesBySimpleName.getOrDefault(simpleName, List.of());
        if (candidates.size() > 1) {
            notices.add(new AnalysisNotice(
                    "AMBIGUOUS_STEP_TYPE",
                    NoticeSeverity.INFO,
                    "Multiple source types named '" + simpleName
                            + "' were found; behavior transitions were not guessed.",
                    declarationSource));
            return BehaviorFacts.empty();
        }
        if (candidates.isEmpty()) {
            return BehaviorFacts.empty();
        }

        TypeSite typeSite = candidates.get(0);
        EnumSet<StepBehavior> behaviors = EnumSet.noneOf(StepBehavior.class);
        List<ResolvedTarget> goToTargets = new ArrayList<>();
        for (MethodCallExpr call : typeSite.declaration().findAll(MethodCallExpr.class)) {
            if (!isScopeNamed(call, resultType)) {
                continue;
            }
            StepBehavior behavior = behaviorForMethod(call.getNameAsString());
            if (behavior == null) {
                continue;
            }
            behaviors.add(behavior);
            if (behavior == StepBehavior.GO_TO && !call.getArguments().isEmpty()) {
                String target = resolveString(call.getArgument(0), typeSite.sourceUnit().constants());
                if (target != null) {
                    goToTargets.add(new ResolvedTarget(
                            target,
                            resultType.equals("GuardResult"),
                            sourceRef(typeSite.sourceUnit(), call)));
                } else {
                    notices.add(new AnalysisNotice(
                            "DYNAMIC_GOTO_TARGET",
                            NoticeSeverity.INFO,
                            resultType + ".goTo(...) target is computed at runtime: "
                                    + call.getArgument(0),
                            sourceRef(typeSite.sourceUnit(), call)));
                }
            }
        }
        return new BehaviorFacts(behaviors, goToTargets);
    }

    private StepBehavior behaviorForMethod(String name) {
        return switch (name) {
            case "stay" -> StepBehavior.STAY;
            case "done" -> StepBehavior.DONE;
            case "repeat" -> StepBehavior.REPEAT;
            case "goTo" -> StepBehavior.GO_TO;
            case "finish" -> StepBehavior.FINISH;
            case "fail" -> StepBehavior.FAIL;
            default -> null;
        };
    }

    private boolean isFlowerBuilderCall(MethodCallExpr call) {
        return call.getNameAsString().equals("builder")
                && (isScopeNamed(call, "Flow") || isScopeNamed(call, "EventFlow"));
    }

    private boolean isWorkerBuilderCall(MethodCallExpr call) {
        return call.getNameAsString().equals("builder")
                && (isScopeNamed(call, "Worker") || isScopeNamed(call, "EventWorker"));
    }

    private boolean isScopeNamed(MethodCallExpr call, String expectedName) {
        if (call.getScope().isEmpty()) {
            return false;
        }
        String scope = call.getScope().orElseThrow().toString();
        return scope.equals(expectedName) || scope.endsWith("." + expectedName);
    }

    private boolean isStepCall(MethodCallExpr call) {
        String name = call.getNameAsString();
        return name.equals("step") || name.equals("durableStep");
    }

    private Expression guardExpression(MethodCallExpr call) {
        String name = call.getNameAsString();
        int argumentCount = call.getArguments().size();
        if (name.equals("step") && argumentCount == 3) {
            return call.getArgument(2);
        }
        if (name.equals("durableStep") && argumentCount == 4) {
            return call.getArgument(2);
        }
        return null;
    }

    private boolean makesDurable(MethodCallExpr call) {
        if (call.getNameAsString().equals("durable")) {
            return true;
        }
        return call.getNameAsString().equals("persistence")
                && !call.getArguments().isEmpty()
                && call.getArgument(0).toString().endsWith("DURABLE");
    }

    private String expressionType(
            Expression expression,
            SourceUnit sourceUnit,
            Map<String, List<TypeSite>> typesBySimpleName
    ) {
        if (expression instanceof ObjectCreationExpr creation) {
            return creation.getTypeAsString();
        }
        if (expression instanceof NameExpr name) {
            return sourceUnit.variableTypes().get(name.getNameAsString());
        }
        if (expression instanceof FieldAccessExpr fieldAccess) {
            return sourceUnit.variableTypes().get(fieldAccess.getNameAsString());
        }
        if (expression instanceof MethodCallExpr call && call.getScope().isPresent()) {
            String scopeVariable = variableName(call.getScope().orElseThrow());
            String scopeType = scopeVariable == null
                    ? null
                    : sourceUnit.variableTypes().get(scopeVariable);
            if (scopeType == null) {
                return null;
            }
            List<TypeSite> candidates = typesBySimpleName.getOrDefault(
                    simpleTypeName(scopeType),
                    List.of());
            Set<String> returnTypes = new LinkedHashSet<>();
            for (TypeSite candidate : candidates) {
                candidate.declaration().getMethodsByName(call.getNameAsString()).stream()
                        .filter(method -> method.getParameters().size() == call.getArguments().size()
                                || method.getParameters().stream().anyMatch(parameter -> parameter.isVarArgs()))
                        .map(method -> method.getType().asString())
                        .filter(type -> !type.equals("void") && !type.equals("var"))
                        .forEach(returnTypes::add);
            }
            return returnTypes.size() == 1 ? returnTypes.iterator().next() : null;
        }
        return null;
    }

    private String variableName(Expression expression) {
        if (expression instanceof NameExpr name) {
            return name.getNameAsString();
        }
        if (expression instanceof FieldAccessExpr fieldAccess) {
            return fieldAccess.getNameAsString();
        }
        return null;
    }

    private boolean isInsideLoop(Node node) {
        Optional<CallableDeclaration<?>> callable = enclosingCallable(node);
        Node cursor = node;
        while (cursor.getParentNode().isPresent()) {
            cursor = cursor.getParentNode().orElseThrow();
            if (cursor instanceof ForStmt
                    || cursor instanceof ForEachStmt
                    || cursor instanceof WhileStmt
                    || cursor instanceof DoStmt) {
                return true;
            }
            if (callable.isPresent() && cursor == callable.orElseThrow()) {
                return false;
            }
        }
        return false;
    }

    private Map<String, String> collectStringConstants(CompilationUnit compilationUnit) {
        Map<String, String> constants = new LinkedHashMap<>();
        List<VariableDeclarator> variables = compilationUnit.findAll(VariableDeclarator.class);
        boolean changed;
        do {
            changed = false;
            for (VariableDeclarator variable : variables) {
                if (constants.containsKey(variable.getNameAsString())
                        || variable.getInitializer().isEmpty()
                        || !isFinalVariable(variable)
                        || !isStringType(variable.getTypeAsString())) {
                    continue;
                }
                String value = resolveString(variable.getInitializer().orElseThrow(), constants);
                if (value != null) {
                    constants.put(variable.getNameAsString(), value);
                    changed = true;
                }
            }
        } while (changed);
        return Map.copyOf(constants);
    }

    private Map<String, Integer> collectIntegerConstants(CompilationUnit compilationUnit) {
        Map<String, Integer> constants = new LinkedHashMap<>();
        List<VariableDeclarator> variables = compilationUnit.findAll(VariableDeclarator.class);
        boolean changed;
        do {
            changed = false;
            for (VariableDeclarator variable : variables) {
                if (constants.containsKey(variable.getNameAsString())
                        || variable.getInitializer().isEmpty()
                        || !isFinalVariable(variable)
                        || !isIntegerType(variable.getTypeAsString())) {
                    continue;
                }
                Integer value = resolveInteger(variable.getInitializer().orElseThrow(), constants);
                if (value != null) {
                    constants.put(variable.getNameAsString(), value);
                    changed = true;
                }
            }
        } while (changed);
        return Map.copyOf(constants);
    }

    private boolean isStringType(String typeName) {
        return typeName.equals("String") || typeName.equals("java.lang.String");
    }

    private boolean isIntegerType(String typeName) {
        return typeName.equals("int")
                || typeName.equals("Integer")
                || typeName.equals("java.lang.Integer");
    }

    private List<SourceUnit> resolveStaticImports(List<SourceUnit> units) {
        Map<String, String> qualifiedConstants = new LinkedHashMap<>();
        Map<String, Integer> qualifiedIntegerConstants = new LinkedHashMap<>();
        for (SourceUnit unit : units) {
            String packageName = unit.compilationUnit().getPackageDeclaration()
                    .map(declaration -> declaration.getNameAsString() + ".")
                    .orElse("");
            for (FieldDeclaration field
                    : unit.compilationUnit().findAll(FieldDeclaration.class)) {
                if (!field.isStatic() || !field.isFinal()) {
                    continue;
                }
                for (VariableDeclarator variable : field.getVariables()) {
                    String owner = enclosingClass(variable)
                            .map(ClassOrInterfaceDeclaration::getNameAsString)
                            .orElse(null);
                    if (owner == null) {
                        continue;
                    }
                    String qualifiedName = packageName + owner + "." + variable.getNameAsString();
                    String value = unit.constants().get(variable.getNameAsString());
                    if (value != null) {
                        qualifiedConstants.put(
                                qualifiedName,
                                value);
                    }
                    Integer integerValue = unit.integerConstants().get(variable.getNameAsString());
                    if (integerValue != null) {
                        qualifiedIntegerConstants.put(qualifiedName, integerValue);
                    }
                }
            }
        }

        List<SourceUnit> resolved = new ArrayList<>();
        for (SourceUnit unit : units) {
            Map<String, String> imported = new LinkedHashMap<>();
            Map<String, Integer> importedIntegers = new LinkedHashMap<>();
            for (ImportDeclaration declaration : unit.compilationUnit().getImports()) {
                String importedName = declaration.getNameAsString();
                if (!declaration.isStatic()) {
                    if (!declaration.isAsterisk()) {
                        String prefix = importedName + ".";
                        qualifiedConstants.forEach((qualifiedName, value) -> {
                            if (qualifiedName.startsWith(prefix)) {
                                imported.putIfAbsent(
                                        qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1),
                                        value);
                            }
                        });
                        qualifiedIntegerConstants.forEach((qualifiedName, value) -> {
                            if (qualifiedName.startsWith(prefix)) {
                                importedIntegers.putIfAbsent(
                                        qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1),
                                        value);
                            }
                        });
                    }
                    continue;
                }
                if (declaration.isAsterisk()) {
                    String prefix = importedName + ".";
                    qualifiedConstants.forEach((qualifiedName, value) -> {
                        if (qualifiedName.startsWith(prefix)) {
                            imported.put(
                                    qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1),
                                    value);
                        }
                    });
                    qualifiedIntegerConstants.forEach((qualifiedName, value) -> {
                        if (qualifiedName.startsWith(prefix)) {
                            importedIntegers.put(
                                    qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1),
                                    value);
                        }
                    });
                } else {
                    String value = qualifiedConstants.get(importedName);
                    if (value != null) {
                        imported.put(
                                importedName.substring(importedName.lastIndexOf('.') + 1),
                                value);
                    }
                    Integer integerValue = qualifiedIntegerConstants.get(importedName);
                    if (integerValue != null) {
                        importedIntegers.put(
                                importedName.substring(importedName.lastIndexOf('.') + 1),
                                integerValue);
                    }
                }
            }
            imported.putAll(unit.constants());
            importedIntegers.putAll(unit.integerConstants());
            resolved.add(new SourceUnit(
                    unit.relativePath(),
                    unit.compilationUnit(),
                    Map.copyOf(imported),
                    Map.copyOf(importedIntegers),
                    unit.variableTypes()));
        }
        return resolved;
    }

    private boolean isFinalVariable(VariableDeclarator variable) {
        Node parent = variable.getParentNode().orElse(null);
        return parent instanceof FieldDeclaration field && field.isFinal()
                || parent instanceof VariableDeclarationExpr local && local.isFinal();
    }

    private Map<String, String> collectVariableTypes(CompilationUnit compilationUnit) {
        Map<String, Set<String>> candidates = new LinkedHashMap<>();
        for (VariableDeclarator variable : compilationUnit.findAll(VariableDeclarator.class)) {
            String type = variable.getTypeAsString();
            if (type.equals("var")) {
                continue;
            }
            candidates.computeIfAbsent(variable.getNameAsString(), ignored -> new LinkedHashSet<>())
                    .add(type);
        }
        Map<String, String> resolved = new LinkedHashMap<>();
        for (Map.Entry<String, Set<String>> entry : candidates.entrySet()) {
            if (entry.getValue().size() == 1) {
                resolved.put(entry.getKey(), entry.getValue().iterator().next());
            }
        }
        return Map.copyOf(resolved);
    }

    private String resolveString(Expression expression, Map<String, String> constants) {
        if (expression instanceof StringLiteralExpr literal) {
            return literal.asString();
        }
        if (expression instanceof NameExpr name) {
            return constants.get(name.getNameAsString());
        }
        if (expression instanceof FieldAccessExpr fieldAccess) {
            return constants.get(fieldAccess.getNameAsString());
        }
        if (expression instanceof EnclosedExpr enclosed) {
            return resolveString(enclosed.getInner(), constants);
        }
        if (expression instanceof BinaryExpr binary
                && binary.getOperator() == BinaryExpr.Operator.PLUS) {
            String left = resolveString(binary.getLeft(), constants);
            String right = resolveString(binary.getRight(), constants);
            return left == null || right == null ? null : left + right;
        }
        return null;
    }

    private Integer resolveInteger(Expression expression, Map<String, Integer> constants) {
        if (expression instanceof IntegerLiteralExpr literal) {
            try {
                return literal.asInt();
            } catch (RuntimeException ignored) {
                return null;
            }
        }
        if (expression instanceof NameExpr name) {
            return constants.get(name.getNameAsString());
        }
        if (expression instanceof FieldAccessExpr fieldAccess) {
            return constants.get(fieldAccess.getNameAsString());
        }
        if (expression instanceof EnclosedExpr enclosed) {
            return resolveInteger(enclosed.getInner(), constants);
        }
        if (expression instanceof UnaryExpr unary) {
            Integer value = resolveInteger(unary.getExpression(), constants);
            if (value == null) {
                return null;
            }
            return switch (unary.getOperator()) {
                case MINUS -> -value;
                case PLUS -> value;
                default -> null;
            };
        }
        return null;
    }

    private void addVariantNotices(
            List<FlowDefinition> definitions,
            List<AnalysisNotice> documentNotices
    ) {
        Map<String, List<FlowDefinition>> byFlowType = new LinkedHashMap<>();
        for (FlowDefinition definition : definitions) {
            String identity = definition.flowType() != null
                    ? "literal:" + definition.flowType()
                    : "expression:" + definition.flowTypeExpression();
            byFlowType.computeIfAbsent(identity, ignored -> new ArrayList<>()).add(definition);
        }
        for (Map.Entry<String, List<FlowDefinition>> entry : byFlowType.entrySet()) {
            if (entry.getValue().size() <= 1) {
                continue;
            }
            FlowDefinition first = entry.getValue().get(0);
            String identityLabel = first.flowType() != null
                    ? "Flow type '" + first.flowType() + "'"
                    : "Flow type expression '" + first.flowTypeExpression() + "'";
            documentNotices.add(new AnalysisNotice(
                    "MULTIPLE_FLOW_VARIANTS",
                    NoticeSeverity.INFO,
                    identityLabel + " has " + entry.getValue().size()
                            + " source definitions. They are shown as separate variants.",
                    first.source()));
        }
    }

    private String simpleTypeName(String typeName) {
        String withoutGenerics = typeName.replaceAll("<.*>", "");
        int dot = withoutGenerics.lastIndexOf('.');
        return dot < 0 ? withoutGenerics : withoutGenerics.substring(dot + 1);
    }

    private Optional<ClassOrInterfaceDeclaration> enclosingClass(Node node) {
        Node cursor = node;
        while (cursor.getParentNode().isPresent()) {
            cursor = cursor.getParentNode().orElseThrow();
            if (cursor instanceof ClassOrInterfaceDeclaration declaration) {
                return Optional.of(declaration);
            }
        }
        return Optional.empty();
    }

    private Optional<CallableDeclaration<?>> enclosingCallable(Node node) {
        Node cursor = node;
        while (cursor.getParentNode().isPresent()) {
            cursor = cursor.getParentNode().orElseThrow();
            if (cursor instanceof CallableDeclaration<?> callable) {
                return Optional.of(callable);
            }
        }
        return Optional.empty();
    }

    private Optional<BlockStmt> enclosingBlock(Node node) {
        Node cursor = node;
        while (cursor.getParentNode().isPresent()) {
            cursor = cursor.getParentNode().orElseThrow();
            if (cursor instanceof BlockStmt block) {
                return Optional.of(block);
            }
        }
        return Optional.empty();
    }

    private SourceRef sourceRef(SourceUnit sourceUnit, Node node) {
        return new SourceRef(sourceUnit.relativePath(), line(node), column(node));
    }

    private int line(Node node) {
        return node.getRange().map(range -> range.begin.line).orElse(0);
    }

    private int column(Node node) {
        return node.getRange().map(range -> range.begin.column).orElse(0);
    }

    private String nodeKey(Node node) {
        return line(node) + ":" + column(node) + ":" + node;
    }

    private String normalizePath(Path path) {
        return path.toString().replace('\\', '/');
    }

    private static final Comparator<Node> NODE_ORDER = Comparator
            .comparingInt((Node node) -> node.getRange().map(range -> range.begin.line).orElse(0))
            .thenComparingInt(node -> node.getRange().map(range -> range.begin.column).orElse(0));

    private record SourceUnit(
            String relativePath,
            CompilationUnit compilationUnit,
            Map<String, String> constants,
            Map<String, Integer> integerConstants,
            Map<String, String> variableTypes
    ) {
    }

    private record TypeSite(
            SourceUnit sourceUnit,
            ClassOrInterfaceDeclaration declaration
    ) {
    }

    private record DefinitionSite(
            FlowDefinition definition,
            String ownerType,
            String ownerMethod,
            int ownerParameterCount
    ) {
    }

    private record SubmittedTarget(
            String definitionId,
            String label,
            boolean resolved
    ) {
    }

    private record WorkerSubmitSite(
            Set<String> workerNames,
            Expression flowExpression
    ) {
        private WorkerSubmitSite {
            workerNames = Set.copyOf(workerNames);
            flowExpression = Objects.requireNonNull(flowExpression, "flowExpression");
        }
    }

    private record ResolvedTarget(
            String target,
            boolean guard,
            SourceRef source
    ) {
    }

    private record BehaviorFacts(
            Set<StepBehavior> behaviors,
            List<ResolvedTarget> goToTargets
    ) {
        private BehaviorFacts {
            behaviors = Set.copyOf(behaviors);
            goToTargets = List.copyOf(goToTargets);
        }

        private static BehaviorFacts empty() {
            return new BehaviorFacts(Set.of(), List.of());
        }

        private BehaviorFacts withGuardTargets(List<ResolvedTarget> guardTargets) {
            Set<StepBehavior> combinedBehaviors = new LinkedHashSet<>(behaviors);
            if (!guardTargets.isEmpty()) {
                combinedBehaviors.add(StepBehavior.GO_TO);
            }
            List<ResolvedTarget> combinedTargets = new ArrayList<>(goToTargets);
            combinedTargets.addAll(guardTargets);
            return new BehaviorFacts(combinedBehaviors, combinedTargets);
        }
    }

    private record StepStructureFacts(
            List<EventSubscription> eventSubscriptions,
            List<StepPhase> phases,
            List<StepPhaseTransition> transitions,
            boolean partial
    ) {
        private StepStructureFacts {
            eventSubscriptions = List.copyOf(eventSubscriptions);
            phases = List.copyOf(phases);
            transitions = List.copyOf(transitions);
        }

        private static StepStructureFacts empty() {
            return new StepStructureFacts(List.of(), List.of(), List.of(), false);
        }
    }

    private record StepNoPhaseCondition(
            int stepNo,
            Expression expression
    ) {
    }
}
