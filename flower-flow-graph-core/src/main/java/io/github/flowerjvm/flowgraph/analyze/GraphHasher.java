package io.github.flowerjvm.flowgraph.analyze;

import io.github.flowerjvm.flowgraph.model.FlowGraphDocument.FlowDefinition;
import io.github.flowerjvm.flowgraph.model.FlowGraphDocument.StepNode;
import io.github.flowerjvm.flowgraph.model.FlowGraphDocument.Transition;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

final class GraphHasher {

    String hash(FlowDefinition definition) {
        StringBuilder canonical = new StringBuilder();
        append(canonical, definition.id());
        append(canonical, definition.flowType());
        append(canonical, definition.flowTypeExpression());
        append(canonical, definition.kind().name());
        append(canonical, Boolean.toString(definition.durable()));
        for (StepNode step : definition.steps()) {
            append(canonical, step.id());
            append(canonical, step.idExpression());
            append(canonical, step.stepType());
            append(canonical, step.stepExpression());
            append(canonical, Boolean.toString(step.dynamic()));
            append(canonical, Boolean.toString(step.durableStep()));
            append(canonical, Boolean.toString(step.guarded()));
            append(canonical, step.guardType());
            append(canonical, step.guardExpression());
            step.behaviors().forEach(behavior -> append(canonical, behavior.name()));
            step.eventSubscriptions().forEach(subscription -> {
                append(canonical, subscription.kind().name());
                append(canonical, subscription.eventType());
                append(canonical, subscription.eventExpression());
                append(canonical, subscription.lifecycleMethod());
                append(canonical, Boolean.toString(subscription.filtered()));
                subscription.emittedSignals().forEach(signal -> {
                    append(canonical, signal.name());
                    append(canonical, signal.expression());
                    append(canonical, signal.operation().name());
                });
            });
            step.internalPhases().forEach(phase -> {
                append(canonical, Integer.toString(phase.stepNo()));
                append(canonical, phase.label());
                append(canonical, Boolean.toString(phase.startsTimeout()));
                append(canonical, Boolean.toString(phase.checksTimeout()));
                phase.signalUses().forEach(signal -> {
                    append(canonical, signal.name());
                    append(canonical, signal.expression());
                    append(canonical, signal.operation().name());
                });
            });
            step.internalTransitions().forEach(transition -> {
                append(canonical, Integer.toString(transition.fromStepNo()));
                append(canonical, transition.toStepNo() == null
                        ? null
                        : Integer.toString(transition.toStepNo()));
                append(canonical, transition.toExpression());
                append(canonical, transition.trigger().name());
            });
            append(canonical, Boolean.toString(step.internalStructurePartial()));
        }
        for (Transition transition : definition.transitions()) {
            append(canonical, transition.fromStepId());
            append(canonical, transition.toStepId());
            append(canonical, transition.kind().name());
            append(canonical, transition.evidence().name());
            append(canonical, transition.certainty().name());
        }
        return sha256(canonical.toString());
    }

    private void append(StringBuilder target, String value) {
        String safe = value == null ? "<null>" : value;
        target.append(safe.length()).append(':').append(safe).append('|');
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                hex.append(String.format("%02x", item));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is not available", impossible);
        }
    }
}
