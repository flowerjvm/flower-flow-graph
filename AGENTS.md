# Flower Flow Graph agent rules

Read this file before changing the project.

## Product boundary

- Java source remains the source of truth for a Flower definition.
- A static graph is a best-effort view of possible structure, not a runtime fact.
- Runtime snapshots and static source facts must remain visibly separate.
- The browser UI is read-only with respect to Flower structure and runtime state.
- Moving nodes, panning, zooming, and saving a workspace only customize the view.
- Runtime inspection stays read-only. Do not add retry, resume, signal, tick, or mutation controls here.
- Core models and interchange formats are coding-agent neutral. Product-specific plugins or adapters live outside the core model.
- Prefer an explicit unknown or partial result over a guessed edge.

## Scope

The first useful slice is deliberately small:

1. inspect Flower builders in Java source;
2. show one definition at a time and a project Flow submission map;
3. keep separately submitted Flows collapsed and clickable;
4. let users arrange the read-only graph and save or reopen that view;
5. never modify the inspected repository.

Do not turn this into a hosted workflow platform, BPMN editor, code generator, or operations control plane.

## Verification

Run:

```powershell
.\mvnw.cmd test
.\mvnw.cmd package
```

When parser behavior changes, test direct builder chains, multiple variants with
the same flow type, and variable/dynamic builders.
