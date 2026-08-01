package io.github.flowerjvm.flowgraph.model;

import java.util.List;
import java.util.Objects;

/**
 * Agent-neutral, read-only representation of Flower definitions found in source.
 */
public record FlowGraphDocument(
        String schemaVersion,
        ProjectInfo project,
        List<WorkerDefinition> workers,
        List<WorkerFlowRelation> workerRelations,
        List<FlowDefinition> definitions,
        List<FlowRelation> relations,
        List<AnalysisNotice> notices
) {
    public static final String SCHEMA_VERSION = "flower.flow-graph/v4";

    public FlowGraphDocument {
        schemaVersion = Objects.requireNonNull(schemaVersion, "schemaVersion");
        project = Objects.requireNonNull(project, "project");
        workers = List.copyOf(workers);
        workerRelations = List.copyOf(workerRelations);
        definitions = List.copyOf(definitions);
        relations = List.copyOf(relations);
        notices = List.copyOf(notices);
    }

    public record ProjectInfo(
            String name,
            String root,
            String revision
    ) {
        public ProjectInfo {
            name = Objects.requireNonNull(name, "name");
            root = Objects.requireNonNull(root, "root");
        }
    }

    /** A Worker declaration found in Java source or Spring configuration. */
    public record WorkerDefinition(
            String id,
            String name,
            String nameExpression,
            WorkerKind kind,
            WorkerDefinitionSource definitionSource,
            boolean dynamic,
            SourceRef source
    ) {
        public WorkerDefinition {
            id = Objects.requireNonNull(id, "id");
            nameExpression = Objects.requireNonNull(nameExpression, "nameExpression");
            kind = Objects.requireNonNull(kind, "kind");
            definitionSource = Objects.requireNonNull(definitionSource, "definitionSource");
            source = Objects.requireNonNull(source, "source");
        }
    }

    /**
     * A source-level submission connecting a Worker name to a possible Flow
     * definition. It is not a snapshot of the contents of a running Worker.
     */
    public record WorkerFlowRelation(
            String id,
            String workerDefinitionId,
            String workerName,
            String toDefinitionId,
            String targetLabel,
            String targetExpression,
            FlowRelationCardinality cardinality,
            WorkerFlowRelationCertainty certainty,
            SourceRef source
    ) {
        public WorkerFlowRelation {
            id = Objects.requireNonNull(id, "id");
            workerName = Objects.requireNonNull(workerName, "workerName");
            targetLabel = Objects.requireNonNull(targetLabel, "targetLabel");
            targetExpression = Objects.requireNonNull(targetExpression, "targetExpression");
            cardinality = Objects.requireNonNull(cardinality, "cardinality");
            certainty = Objects.requireNonNull(certainty, "certainty");
            source = Objects.requireNonNull(source, "source");
        }
    }

    public record FlowDefinition(
            String id,
            String displayName,
            String flowType,
            String flowTypeExpression,
            FlowKind kind,
            boolean durable,
            FlowCompleteness completeness,
            SourceRef source,
            List<StepNode> steps,
            List<Transition> transitions,
            List<AnalysisNotice> notices,
            String graphHash
    ) {
        public FlowDefinition {
            id = Objects.requireNonNull(id, "id");
            displayName = Objects.requireNonNull(displayName, "displayName");
            flowTypeExpression = Objects.requireNonNull(flowTypeExpression, "flowTypeExpression");
            kind = Objects.requireNonNull(kind, "kind");
            completeness = Objects.requireNonNull(completeness, "completeness");
            source = Objects.requireNonNull(source, "source");
            steps = List.copyOf(steps);
            transitions = List.copyOf(transitions);
            notices = List.copyOf(notices);
        }
    }

    public record StepNode(
            String id,
            String idExpression,
            String stepType,
            String stepExpression,
            boolean dynamic,
            boolean durableStep,
            boolean guarded,
            String guardType,
            String guardExpression,
            List<StepBehavior> behaviors,
            List<EventSubscription> eventSubscriptions,
            List<StepPhase> internalPhases,
            List<StepPhaseTransition> internalTransitions,
            boolean internalStructurePartial,
            SourceRef source
    ) {
        public StepNode {
            id = Objects.requireNonNull(id, "id");
            idExpression = Objects.requireNonNull(idExpression, "idExpression");
            stepExpression = Objects.requireNonNull(stepExpression, "stepExpression");
            guardExpression = Objects.requireNonNull(guardExpression, "guardExpression");
            behaviors = List.copyOf(behaviors);
            eventSubscriptions = List.copyOf(eventSubscriptions);
            internalPhases = List.copyOf(internalPhases);
            internalTransitions = List.copyOf(internalTransitions);
            source = Objects.requireNonNull(source, "source");
        }
    }

    /**
     * An event registration visible in Step source. This does not assert that
     * the registration is active in a particular runtime instance.
     */
    public record EventSubscription(
            EventSubscriptionKind kind,
            String eventType,
            String eventExpression,
            String lifecycleMethod,
            List<SignalUse> emittedSignals,
            boolean filtered,
            SourceRef source
    ) {
        public EventSubscription {
            kind = Objects.requireNonNull(kind, "kind");
            eventExpression = Objects.requireNonNull(eventExpression, "eventExpression");
            lifecycleMethod = Objects.requireNonNull(lifecycleMethod, "lifecycleMethod");
            emittedSignals = List.copyOf(emittedSignals);
            source = Objects.requireNonNull(source, "source");
        }
    }

    /** A source-confirmed use of a Flower Step signal. */
    public record SignalUse(
            String name,
            String expression,
            SignalOperation operation,
            SourceRef source
    ) {
        public SignalUse {
            expression = Objects.requireNonNull(expression, "expression");
            operation = Objects.requireNonNull(operation, "operation");
            source = Objects.requireNonNull(source, "source");
        }
    }

    /**
     * One source case of a {@code switch (ctx.stepNo())}. These phases remain
     * internal to one Flower Step and are not additional Flow Steps.
     */
    public record StepPhase(
            int stepNo,
            String label,
            List<SignalUse> signalUses,
            boolean startsTimeout,
            boolean checksTimeout,
            SourceRef source
    ) {
        public StepPhase {
            label = Objects.requireNonNull(label, "label");
            signalUses = List.copyOf(signalUses);
            source = Objects.requireNonNull(source, "source");
        }
    }

    /** A literal {@code ctx.setStepNo(...)} found from one internal phase. */
    public record StepPhaseTransition(
            int fromStepNo,
            Integer toStepNo,
            String toExpression,
            InternalTransitionTrigger trigger,
            SourceRef source
    ) {
        public StepPhaseTransition {
            toExpression = Objects.requireNonNull(toExpression, "toExpression");
            trigger = Objects.requireNonNull(trigger, "trigger");
            source = Objects.requireNonNull(source, "source");
        }
    }

    public record Transition(
            String id,
            String fromStepId,
            String toStepId,
            TransitionKind kind,
            TransitionEvidence evidence,
            TransitionCertainty certainty,
            String label,
            SourceRef source
    ) {
        public Transition {
            id = Objects.requireNonNull(id, "id");
            fromStepId = Objects.requireNonNull(fromStepId, "fromStepId");
            kind = Objects.requireNonNull(kind, "kind");
            evidence = Objects.requireNonNull(evidence, "evidence");
            certainty = Objects.requireNonNull(certainty, "certainty");
            source = Objects.requireNonNull(source, "source");
        }
    }

    /**
     * A source-level handoff from one Step to a separately submitted Flow.
     * This is not a nested lifecycle or runtime parent/child assertion.
     */
    public record FlowRelation(
            String id,
            String fromDefinitionId,
            String fromStepId,
            String toDefinitionId,
            String targetLabel,
            String targetExpression,
            FlowRelationKind kind,
            FlowRelationCardinality cardinality,
            FlowRelationCertainty certainty,
            SourceRef source
    ) {
        public FlowRelation {
            id = Objects.requireNonNull(id, "id");
            fromDefinitionId = Objects.requireNonNull(fromDefinitionId, "fromDefinitionId");
            fromStepId = Objects.requireNonNull(fromStepId, "fromStepId");
            targetLabel = Objects.requireNonNull(targetLabel, "targetLabel");
            targetExpression = Objects.requireNonNull(targetExpression, "targetExpression");
            kind = Objects.requireNonNull(kind, "kind");
            cardinality = Objects.requireNonNull(cardinality, "cardinality");
            certainty = Objects.requireNonNull(certainty, "certainty");
            source = Objects.requireNonNull(source, "source");
        }
    }

    public record AnalysisNotice(
            String code,
            NoticeSeverity severity,
            String message,
            SourceRef source
    ) {
        public AnalysisNotice {
            code = Objects.requireNonNull(code, "code");
            severity = Objects.requireNonNull(severity, "severity");
            message = Objects.requireNonNull(message, "message");
        }
    }

    public record SourceRef(
            String file,
            int line,
            int column
    ) {
        public SourceRef {
            file = Objects.requireNonNull(file, "file");
        }
    }

    public enum FlowKind {
        FLOW,
        EVENT_FLOW
    }

    public enum WorkerKind {
        WORKER,
        EVENT_WORKER
    }

    public enum WorkerDefinitionSource {
        JAVA_BUILDER,
        SPRING_CONFIGURATION
    }

    public enum WorkerFlowRelationCertainty {
        SOURCE_CONFIRMED,
        PARTIAL
    }

    public enum FlowCompleteness {
        STATIC_BEST_EFFORT,
        PARTIAL_DYNAMIC
    }

    public enum StepBehavior {
        STAY,
        DONE,
        REPEAT,
        GO_TO,
        FINISH,
        FAIL
    }

    public enum EventSubscriptionKind {
        SUBSCRIBE,
        AWAIT
    }

    public enum SignalOperation {
        EMIT,
        CHECK,
        READ,
        CONSUME,
        CLEAR
    }

    public enum InternalTransitionTrigger {
        SET_STEP_NO,
        TIMEOUT
    }

    public enum TransitionKind {
        DONE_NEXT,
        GO_TO,
        GUARD_GO_TO,
        FINISH,
        FAIL
    }

    public enum TransitionEvidence {
        FLOW_BUILDER,
        STEP_SOURCE,
        RUNTIME_OBSERVED
    }

    public enum TransitionCertainty {
        DECLARED,
        SOURCE_LITERAL,
        PARTIAL
    }

    public enum FlowRelationKind {
        SUBMITS
    }

    /**
     * Static call-site multiplicity, not the number of runtime child instances.
     */
    public enum FlowRelationCardinality {
        ONE_PER_CALL,
        CONDITIONAL,
        ZERO_OR_MANY
    }

    public enum FlowRelationCertainty {
        SOURCE_CONFIRMED,
        PARTIAL
    }

    public enum NoticeSeverity {
        INFO,
        WARNING
    }
}
