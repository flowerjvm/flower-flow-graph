package io.github.flowerjvm.flowgraph.analyze;

import io.github.flowerjvm.flowgraph.model.FlowGraphDocument;
import io.github.flowerjvm.flowgraph.model.FlowGraphDocument.FlowCompleteness;
import io.github.flowerjvm.flowgraph.model.FlowGraphDocument.FlowRelationCardinality;
import io.github.flowerjvm.flowgraph.model.FlowGraphDocument.FlowRelationCertainty;
import io.github.flowerjvm.flowgraph.model.FlowGraphDocument.StepBehavior;
import io.github.flowerjvm.flowgraph.model.FlowGraphDocument.TransitionKind;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlowSourceAnalyzerTest {

    @TempDir
    Path project;

    private final FlowSourceAnalyzer analyzer = new FlowSourceAnalyzer();

    @Test
    void readsDirectBuilderStepsAndLiteralStepResultTransitions() throws IOException {
        writeSource("src/main/java/example/OrderFlowFactory.java", """
                package example;

                final class OrderFlowFactory {
                    private static final String FLOW_TYPE = "order";

                    Flow create(String key) {
                        return Flow.builder(FLOW_TYPE, key)
                                .step("accept", new AcceptStep())
                                .step("check-stock", new CheckStockStep(), new StockGuard())
                                .step("cancel", new CancelStep())
                                .build();
                    }
                }

                final class AcceptStep {
                    StepResult tick() {
                        return StepResult.done();
                    }
                }

                final class CheckStockStep {
                    StepResult tick(boolean available) {
                        return available
                                ? StepResult.done()
                                : StepResult.goTo("cancel");
                    }
                }

                final class CancelStep {
                    StepResult tick() {
                        return StepResult.finish();
                    }
                }

                final class StockGuard {
                    GuardResult check(boolean cancelled) {
                        return cancelled
                                ? GuardResult.goTo("cancel")
                                : GuardResult.pass();
                    }
                }
                """);

        FlowGraphDocument document = analyzer.analyze(project);

        assertEquals(1, document.definitions().size());
        var definition = document.definitions().get(0);
        assertEquals("order", definition.flowType());
        assertEquals(List.of("accept", "check-stock", "cancel"),
                definition.steps().stream().map(FlowGraphDocument.StepNode::id).toList());
        assertEquals(FlowCompleteness.STATIC_BEST_EFFORT, definition.completeness());
        assertTrue(definition.steps().get(1).behaviors().contains(StepBehavior.GO_TO));
        assertTrue(definition.transitions().stream().anyMatch(transition ->
                transition.kind() == TransitionKind.GO_TO
                        && transition.fromStepId().equals("check-stock")
                        && transition.toStepId().equals("cancel")));
        assertTrue(definition.transitions().stream().anyMatch(transition ->
                transition.kind() == TransitionKind.GUARD_GO_TO
                        && transition.fromStepId().equals("check-stock")
                        && transition.toStepId().equals("cancel")));
        assertTrue(definition.transitions().stream().anyMatch(transition ->
                transition.kind() == TransitionKind.FINISH
                        && transition.fromStepId().equals("cancel")
                        && transition.toStepId() == null));
        assertNotNull(definition.graphHash());
        assertEquals(64, definition.graphHash().length());
    }

    @Test
    void keepsSameFlowTypeSourceVariantsSeparate() throws IOException {
        writeSource("src/main/java/example/RecoveryFactory.java", """
                package example;

                final class RecoveryFactory {
                    Flow normal(String key) {
                        return Flow.builder("ai-run", key)
                                .step("prepare", new PrepareStep())
                                .step("await", new AwaitStep())
                                .build();
                    }

                    Flow recovered(String key) {
                        return Flow.builder("ai-run", key)
                                .step("await", new AwaitStep())
                                .build();
                    }
                }

                final class PrepareStep {}
                final class AwaitStep {}
                """);

        FlowGraphDocument document = analyzer.analyze(project);

        assertEquals(2, document.definitions().size());
        assertTrue(document.definitions().stream()
                .allMatch(definition -> definition.flowType().equals("ai-run")));
        assertTrue(document.notices().stream()
                .anyMatch(notice -> notice.code().equals("MULTIPLE_FLOW_VARIANTS")));
        assertFalse(document.definitions().get(0).id().equals(document.definitions().get(1).id()));
    }

    @Test
    void marksLoopAddedVariableBuilderStepsAsPartial() throws IOException {
        writeSource("src/main/java/example/PipelineFactory.java", """
                package example;

                final class PipelineFactory {
                    private static final String FLOW_TYPE = "action";

                    Flow create(String key, Iterable<Stage> stages) {
                        var builder = Flow.builder(FLOW_TYPE, key);
                        for (Stage stage : stages) {
                            builder = builder.step(stage.id(), stage.create());
                        }
                        builder = builder.step("finalize", new FinalizeStep());
                        return builder.build();
                    }
                }

                interface Stage {
                    String id();
                    Object create();
                }

                final class FinalizeStep {}
                """);

        FlowGraphDocument first = analyzer.analyze(project);
        FlowGraphDocument second = analyzer.analyze(project);

        assertEquals(1, first.definitions().size());
        var definition = first.definitions().get(0);
        assertEquals(FlowCompleteness.PARTIAL_DYNAMIC, definition.completeness());
        assertEquals(2, definition.steps().size());
        assertTrue(definition.steps().get(0).dynamic());
        assertEquals("finalize", definition.steps().get(1).id());
        assertTrue(definition.notices().stream()
                .anyMatch(notice -> notice.code().equals("LOOP_ADDED_STEP")));
        assertEquals(definition.graphHash(), second.definitions().get(0).graphHash());
    }

    @Test
    void resolvesStepTypeFromAnInjectedFieldWithoutGuessingRuntimeBehavior() throws IOException {
        writeSource("src/main/java/example/InjectedFactory.java", """
                package example;

                final class InjectedFactory {
                    private final ValidateStep validateStep;

                    InjectedFactory(ValidateStep validateStep) {
                        this.validateStep = validateStep;
                    }

                    Flow create(String key) {
                        return Flow.builder("injected", key)
                                .step("validate", validateStep)
                                .build();
                    }
                }

                final class ValidateStep {
                    StepResult tick() {
                        return StepResult.done();
                    }
                }
                """);

        FlowGraphDocument document = analyzer.analyze(project);

        var definition = document.definitions().get(0);
        assertEquals(FlowCompleteness.STATIC_BEST_EFFORT, definition.completeness());
        assertEquals("ValidateStep", definition.steps().get(0).stepType());
        assertFalse(definition.steps().get(0).dynamic());
        assertTrue(definition.steps().get(0).behaviors().contains(StepBehavior.DONE));
    }

    @Test
    void resolvesStaticImportedStepIdAndFactoryMethodReturnType() throws IOException {
        writeSource("src/main/java/example/StepCatalog.java", """
                package example;

                final class StepCatalog {
                    static final String VALIDATE = "validate";
                }
                """);
        writeSource("src/main/java/example/StepFactory.java", """
                package example;

                final class StepFactory {
                    ValidateStep validate() {
                        return new ValidateStep();
                    }
                }

                final class ValidateStep {
                    StepResult tick() {
                        return StepResult.done();
                    }
                }
                """);
        writeSource("src/main/java/example/ImportedFactory.java", """
                package example;

                import static example.StepCatalog.VALIDATE;

                final class ImportedFactory {
                    private final StepFactory steps;

                    ImportedFactory(StepFactory steps) {
                        this.steps = steps;
                    }

                    Flow create(String key) {
                        return Flow.builder("imported", key)
                                .step(VALIDATE, steps.validate())
                                .build();
                    }
                }
                """);

        FlowGraphDocument document = analyzer.analyze(project);

        var definition = document.definitions().get(0);
        assertEquals(FlowCompleteness.STATIC_BEST_EFFORT, definition.completeness());
        assertEquals("validate", definition.steps().get(0).id());
        assertEquals("ValidateStep", definition.steps().get(0).stepType());
        assertFalse(definition.steps().get(0).dynamic());
    }

    @Test
    void readsSourceEventSubscriptionsAndLiteralStepNoPhases() throws IOException {
        writeSource("src/main/java/example/MonitorFlow.java", """
                package example;

                final class MonitorFlow {
                    Flow create() {
                        return Flow.builder("monitor", "one")
                                .step("monitor-heartbeats", new MonitorStep())
                                .build();
                    }
                }

                final class MonitorStep {
                    private static final String SIGNAL_PING = "ping";
                    private static final int CHECK = 0;
                    private static final int WAIT = 100;

                    void onEnter(StepContext ctx) {
                        ctx.subscribe(PingEvent.class, event -> {
                            if (event.accepted()) {
                                ctx.signal(SIGNAL_PING, event);
                            }
                        });
                    }

                    StepResult onTick(StepContext ctx) {
                        return switch (ctx.stepNo()) {
                            case CHECK -> check(ctx);
                            case WAIT -> waitForPing(ctx);
                            default -> StepResult.fail(new IllegalStateException());
                        };
                    }

                    private StepResult check(StepContext ctx) {
                        ctx.startTimeout(1_000);
                        ctx.setStepNo(WAIT);
                        return StepResult.stay();
                    }

                    private StepResult waitForPing(StepContext ctx) {
                        ctx.consumeSignal(SIGNAL_PING, PingEvent.class);
                        if (ctx.timedOut()) {
                            ctx.setStepNo(CHECK);
                        }
                        return StepResult.stay();
                    }
                }

                record PingEvent(boolean accepted) {}
                """);

        var step = analyzer.analyze(project).definitions().get(0).steps().get(0);

        assertEquals(1, step.eventSubscriptions().size());
        var subscription = step.eventSubscriptions().get(0);
        assertEquals(FlowGraphDocument.EventSubscriptionKind.SUBSCRIBE, subscription.kind());
        assertEquals("PingEvent", subscription.eventType());
        assertEquals("onEnter", subscription.lifecycleMethod());
        assertTrue(subscription.filtered());
        assertEquals("ping", subscription.emittedSignals().get(0).name());

        assertEquals(List.of(0, 100), step.internalPhases().stream()
                .map(FlowGraphDocument.StepPhase::stepNo)
                .toList());
        assertEquals(List.of("CHECK", "WAIT"), step.internalPhases().stream()
                .map(FlowGraphDocument.StepPhase::label)
                .toList());
        assertTrue(step.internalPhases().get(0).startsTimeout());
        assertTrue(step.internalPhases().get(1).checksTimeout());
        assertEquals("ping", step.internalPhases().get(1).signalUses().get(0).name());
        assertTrue(step.internalTransitions().stream().anyMatch(transition ->
                transition.fromStepNo() == 0 && Integer.valueOf(100).equals(transition.toStepNo())));
        assertTrue(step.internalTransitions().stream().anyMatch(transition ->
                transition.fromStepNo() == 100
                        && Integer.valueOf(0).equals(transition.toStepNo())
                        && transition.trigger()
                        == FlowGraphDocument.InternalTransitionTrigger.TIMEOUT));
        assertFalse(step.internalStructurePartial());
    }

    @Test
    void keepsEventLoopAwaitsAndComputedStepNoTargetsExplicitlyPartial() throws IOException {
        writeSource("src/main/java/example/EventMonitorFlow.java", """
                package example;

                final class EventMonitorFlow {
                    EventFlow create() {
                        return EventFlow.builder("event-monitor", "one")
                                .step("await-job", new AwaitJobStep())
                                .build();
                    }
                }

                final class DynamicMonitorFlow {
                    Flow create() {
                        return Flow.builder("dynamic-monitor", "one")
                                .step("monitor", new DynamicMonitorStep())
                                .build();
                    }
                }

                final class AwaitJobStep {
                    EventStepResult onEvent(EventStepContext ctx) {
                        return EventStepResult.await(AwaitCondition.event(JobCompleted.class));
                    }
                }

                final class DynamicMonitorStep {
                    private static final int WAIT = 0;

                    StepResult onTick(StepContext ctx) {
                        return switch (ctx.stepNo()) {
                            case WAIT -> await(ctx);
                            default -> StepResult.fail(new IllegalStateException());
                        };
                    }

                    private StepResult await(StepContext ctx) {
                        ctx.setStepNo(nextPhase());
                        return StepResult.stay();
                    }

                    private int nextPhase() {
                        return 10;
                    }
                }

                record JobCompleted() {}
                """);

        var document = analyzer.analyze(project);
        var eventStep = document.definitions().stream()
                .filter(definition -> "event-monitor".equals(definition.flowType()))
                .findFirst().orElseThrow().steps().get(0);
        var dynamicStep = document.definitions().stream()
                .filter(definition -> "dynamic-monitor".equals(definition.flowType()))
                .findFirst().orElseThrow().steps().get(0);

        assertEquals(1, eventStep.eventSubscriptions().size());
        assertEquals(FlowGraphDocument.EventSubscriptionKind.AWAIT,
                eventStep.eventSubscriptions().get(0).kind());
        assertEquals("JobCompleted", eventStep.eventSubscriptions().get(0).eventType());
        assertEquals(1, dynamicStep.internalPhases().size());
        assertNull(dynamicStep.internalTransitions().get(0).toStepNo());
        assertEquals("nextPhase()", dynamicStep.internalTransitions().get(0).toExpression());
        assertTrue(dynamicStep.internalStructurePartial());
    }

    @Test
    void connectsJavaWorkerDefinitionsToSubmittedFlowDefinitions() throws IOException {
        writeSource("src/main/java/example/WorkerConfiguration.java", """
                package example;

                final class WorkerConfiguration {
                    static final String ORDERS = "orders";
                    static final String ALERTS = "alerts";

                    Engine engine() {
                        return Engine.builder()
                                .worker(Worker.builder(ORDERS).build())
                                .worker(EventWorker.builder(ALERTS).build())
                                .build();
                    }
                }

                final class OrderFlowFactory {
                    Flow create() {
                        return Flow.builder("order", "one")
                                .step("accept", new AcceptStep())
                                .build();
                    }
                }

                final class AlertFlowFactory {
                    EventFlow create() {
                        return EventFlow.builder("alert", "one")
                                .step("notify", new NotifyStep())
                                .build();
                    }
                }

                final class OrderWorker {
                    private final Engine engine;

                    OrderWorker(Engine engine) {
                        this.engine = engine;
                    }

                    void submit(Flow flow) {
                        engine.worker(WorkerConfiguration.ORDERS).submit(flow);
                    }
                }

                final class SubmissionService {
                    private final Engine engine;
                    private final OrderWorker orderWorker;
                    private final OrderFlowFactory orderFlowFactory;
                    private final AlertFlowFactory alertFlowFactory;

                    SubmissionService(
                            Engine engine,
                            OrderWorker orderWorker,
                            OrderFlowFactory orderFlowFactory,
                            AlertFlowFactory alertFlowFactory
                    ) {
                        this.engine = engine;
                        this.orderWorker = orderWorker;
                        this.orderFlowFactory = orderFlowFactory;
                        this.alertFlowFactory = alertFlowFactory;
                    }

                    void start() {
                        orderWorker.submit(orderFlowFactory.create());
                        engine.submit(WorkerConfiguration.ALERTS, alertFlowFactory.create());
                    }
                }

                final class AcceptStep {}
                final class NotifyStep {}
                """);

        FlowGraphDocument document = analyzer.analyze(project);

        assertEquals(List.of("alerts", "orders"), document.workers().stream()
                .map(FlowGraphDocument.WorkerDefinition::name)
                .sorted()
                .toList());
        assertTrue(document.workers().stream().anyMatch(worker ->
                worker.name().equals("alerts")
                        && worker.kind() == FlowGraphDocument.WorkerKind.EVENT_WORKER));
        assertTrue(document.workerRelations().stream().anyMatch(relation ->
                relation.workerName().equals("orders")
                        && relation.targetLabel().equals("order")
                        && relation.certainty()
                        == FlowGraphDocument.WorkerFlowRelationCertainty.SOURCE_CONFIRMED));
        assertTrue(document.workerRelations().stream().anyMatch(relation ->
                relation.workerName().equals("alerts")
                        && relation.targetLabel().equals("alert")));
    }

    @Test
    void readsSpringWorkerNamesFromYamlAndProperties() throws IOException {
        writeSource("src/main/java/example/ConfiguredWorkerSubmission.java", """
                package example;

                final class ConfiguredFlowFactory {
                    Flow create() {
                        return Flow.builder("configured-flow", "one")
                                .step("run", new ConfiguredStep())
                                .build();
                    }
                }

                final class ConfiguredWorkerSubmission {
                    private final Engine engine;
                    private final ConfiguredFlowFactory flowFactory;

                    ConfiguredWorkerSubmission(Engine engine, ConfiguredFlowFactory flowFactory) {
                        this.engine = engine;
                        this.flowFactory = flowFactory;
                    }

                    void start() {
                        engine.submit("yaml-worker", flowFactory.create());
                    }
                }

                final class ConfiguredStep {}
                """);
        writeSource("src/main/resources/application.yml", """
                flower:
                  workers:
                    - name: yaml-worker
                      interval-ms: 100
                    - name: ${DYNAMIC_WORKER_NAME}
                """);
        writeSource("src/main/resources/application-local.properties", """
                flower.workers[0].name=property-worker
                flower.workers[0].interval-ms=250
                """);

        FlowGraphDocument document = analyzer.analyze(project);

        assertTrue(document.workers().stream().anyMatch(worker ->
                "yaml-worker".equals(worker.name())
                        && worker.definitionSource()
                        == FlowGraphDocument.WorkerDefinitionSource.SPRING_CONFIGURATION));
        assertTrue(document.workers().stream().anyMatch(worker ->
                "property-worker".equals(worker.name())));
        assertTrue(document.workers().stream().anyMatch(worker ->
                worker.name() == null && worker.dynamic()));
        assertTrue(document.workerRelations().stream().anyMatch(relation ->
                relation.workerName().equals("yaml-worker")
                        && relation.targetLabel().equals("configured-flow")));
    }

    @Test
    void excludesTestSourceSetsByDefault() throws IOException {
        writeSource("src/main/java/example/MainFlow.java", """
                package example;
                final class MainFlow {
                    Flow create() {
                        return Flow.builder("main-flow", "1")
                                .step("run", new MainStep())
                                .build();
                    }
                }
                final class MainStep {}
                """);
        writeSource("src/test/java/example/TestFlow.java", """
                package example;
                final class TestFlow {
                    Flow create() {
                        return Flow.builder("test-flow", "1")
                                .step("run", new TestStep())
                                .build();
                    }
                }
                final class TestStep {}
                """);
        writeSource("src/integrationTest/java/example/IntegrationFlow.java", """
                package example;
                final class IntegrationFlow {
                    Flow create() {
                        return Flow.builder("integration-flow", "1")
                                .step("run", new IntegrationStep())
                                .build();
                    }
                }
                final class IntegrationStep {}
                """);

        FlowGraphDocument document = analyzer.analyze(project);

        assertEquals(
                List.of("main-flow"),
                document.definitions().stream()
                        .map(FlowGraphDocument.FlowDefinition::flowType)
                        .toList());
    }

    @Test
    void connectsWorkerSubmissionsToResolvedAndUnresolvedFlows() throws IOException {
        writeSource("src/main/java/example/FlowFactories.java", """
                package example;

                final class ParentFlowFactory {
                    Flow create(String key, ChildWorker worker, ChildFlowFactory childFactory) {
                        return Flow.builder("parent", key)
                                .step("submit-child", new SubmitChildStep(worker, childFactory))
                                .step("fan-out", new FanOutStep(worker, childFactory))
                                .step("submit-harness", new SubmitHarnessStep(new AiHarnessWorker(), new AiHarnessFlow()))
                                .step("submit-optional-harness", new SubmitOptionalHarnessStep(
                                        new AiHarnessWorker(), new HarnessService()))
                                .step("submit-harness-reference", new SubmitHarnessReferenceStep(
                                        new AiHarnessWorker(), new HarnessService()))
                                .build();
                    }
                }

                final class ChildFlowFactory {
                    Flow create(String key) {
                        return Flow.builder("child", key)
                                .step("work", new ChildStep())
                                .build();
                    }
                }

                final class SubmitChildStep {
                    private final ChildWorker worker;
                    private final ChildFlowFactory childFactory;

                    SubmitChildStep(ChildWorker worker, ChildFlowFactory childFactory) {
                        this.worker = worker;
                        this.childFactory = childFactory;
                    }

                    StepResult tick() {
                        worker.submit(childFactory.create("one"));
                        return StepResult.done();
                    }
                }

                final class FanOutStep {
                    private final ChildWorker worker;
                    private final ChildFlowFactory childFactory;

                    FanOutStep(ChildWorker worker, ChildFlowFactory childFactory) {
                        this.worker = worker;
                        this.childFactory = childFactory;
                    }

                    StepResult tick(Iterable<String> keys) {
                        keys.forEach(key -> worker.submit(childFactory.create(key)));
                        return StepResult.done();
                    }
                }

                final class SubmitHarnessStep {
                    private final AiHarnessWorker worker;
                    private final AiHarnessFlow harnessFlow;

                    SubmitHarnessStep(AiHarnessWorker worker, AiHarnessFlow harnessFlow) {
                        this.worker = worker;
                        this.harnessFlow = harnessFlow;
                    }

                    StepResult tick() {
                        worker.submit(harnessFlow);
                        return StepResult.done();
                    }
                }

                final class SubmitOptionalHarnessStep {
                    private final AiHarnessWorker worker;
                    private final HarnessService service;

                    SubmitOptionalHarnessStep(AiHarnessWorker worker, HarnessService service) {
                        this.worker = worker;
                        this.service = service;
                    }

                    StepResult tick() {
                        var flow = service.create();
                        if (flow.isPresent()) {
                            worker.submit(flow.get());
                        }
                        return StepResult.done();
                    }
                }

                final class HarnessService {
                    java.util.Optional<AiHarnessFlow> create() {
                        return java.util.Optional.empty();
                    }
                }

                final class SubmitHarnessReferenceStep {
                    private final AiHarnessWorker worker;
                    private final HarnessService service;

                    SubmitHarnessReferenceStep(AiHarnessWorker worker, HarnessService service) {
                        this.worker = worker;
                        this.service = service;
                    }

                    StepResult tick() {
                        var flow = service.create();
                        flow.ifPresent(worker::submit);
                        return StepResult.done();
                    }
                }

                final class ChildWorker {
                    void submit(Flow flow) {}
                }
                final class AiHarnessWorker {
                    void submit(AiHarnessFlow flow) {}
                }
                final class AiHarnessFlow {}
                final class ChildStep {}
                """);

        FlowGraphDocument document = analyzer.analyze(project);
        var parent = document.definitions().stream()
                .filter(definition -> "parent".equals(definition.flowType()))
                .findFirst()
                .orElseThrow();
        var child = document.definitions().stream()
                .filter(definition -> "child".equals(definition.flowType()))
                .findFirst()
                .orElseThrow();

        var direct = document.relations().stream()
                .filter(relation -> relation.fromDefinitionId().equals(parent.id()))
                .filter(relation -> relation.fromStepId().equals("submit-child"))
                .findFirst()
                .orElseThrow();
        assertEquals(child.id(), direct.toDefinitionId());
        assertEquals(FlowRelationCertainty.SOURCE_CONFIRMED, direct.certainty());
        assertEquals(FlowRelationCardinality.ONE_PER_CALL, direct.cardinality());

        var fanOut = document.relations().stream()
                .filter(relation -> relation.fromStepId().equals("fan-out"))
                .findFirst()
                .orElseThrow();
        assertEquals(child.id(), fanOut.toDefinitionId());
        assertEquals(FlowRelationCardinality.ZERO_OR_MANY, fanOut.cardinality());

        var harness = document.relations().stream()
                .filter(relation -> relation.fromStepId().equals("submit-harness"))
                .findFirst()
                .orElseThrow();
        assertNull(harness.toDefinitionId());
        assertEquals("AiHarnessFlow", harness.targetLabel());
        assertEquals(FlowRelationCertainty.PARTIAL, harness.certainty());

        var optionalHarness = document.relations().stream()
                .filter(relation -> relation.fromStepId().equals("submit-optional-harness"))
                .findFirst()
                .orElseThrow();
        assertEquals("AiHarnessFlow", optionalHarness.targetLabel());
        assertEquals(FlowRelationCardinality.CONDITIONAL, optionalHarness.cardinality());

        var harnessReference = document.relations().stream()
                .filter(relation -> relation.fromStepId().equals("submit-harness-reference"))
                .findFirst()
                .orElseThrow();
        assertEquals("AiHarnessFlow", harnessReference.targetLabel());
        assertEquals(FlowRelationCardinality.CONDITIONAL, harnessReference.cardinality());
    }

    private void writeSource(String relativePath, String source) throws IOException {
        Path file = project.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, source);
    }
}
