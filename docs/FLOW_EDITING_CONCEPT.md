# Future Flow Editing Concept

This document records a possible future direction. Flow editing is not part of
the current Flower Flow Graph product.

## Current decision

Flower Flow Graph is a read-only source explorer. It reads Flower Java source
and shows possible Worker, Flow, Step, transition, event, and signal structure.
Moving a node, panning, zooming, or saving a workspace changes only the saved
view. It does not change the Flower structure.

The current product intentionally does not provide:

- Step or transition editing;
- Java source generation;
- coding-agent request export;
- changes to a running Engine;
- retry, resume, signal, event, or tick controls.

## Possible future workflow

If repeated real-world use shows that graph-based change planning is useful, a
future version could support this sequence:

1. Read the current graph from Java source.
2. Let a developer describe a proposed Step or transition change separately
   from the source graph.
3. Export a structured, coding-agent-neutral change request.
4. Let a coding agent inspect and modify the real Java project.
5. Run focused tests and Flower Check.
6. Re-scan the changed source and compare the resulting graph.

The edited graph would be a proposal, never the source of truth. Java code
would remain the execution definition.

## Required boundaries

Any future editing feature should preserve these boundaries:

- It must not mutate a running Flower Engine.
- It must not write application source directly from the browser.
- It must not depend on Codex or any other specific coding agent.
- Static source facts, runtime observations, and proposed changes must remain
  visually and structurally separate.
- Dynamic or unresolved source structure must remain unknown rather than being
  converted into a guessed edge.
- Step ID removal or rename must warn about durable checkpoint compatibility.
- A proposed change is not complete until code review and project tests pass.

## Reconsider only after validation

Editing should be reconsidered only after users repeatedly encounter a problem
that cannot be handled well by the read-only graph, source details, Flower
Testkit, Flower Check, or coding-agent guidance. Until then, keeping the graph
small and read-only is the preferred direction.
