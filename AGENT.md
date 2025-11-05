# Project Assistant Guide (AGENT.md)

This document defines how the repository assistant operates for this project. It covers available tools, the tasks workflow, coding standards for Scala 3, repository overview, known pitfalls, build instructions, and contribution practices.

## Scope and Goals
- Primary goal: assist with maintaining the compiler-yaml module that compiles us.awfl.dsl workflows into a Cloud Workflows-compatible YAML/JSON AST.
- Keep changes minimal, safe, and reviewable.
- Prefer incremental updates with clear tasks and status changes.

## Tools the assistant can use
- READ_FILE {filepath}: Read file content from the repo.
- UPDATE_FILE {filepath, content}: Create or overwrite a file with the provided content.
- RUN_COMMAND {command}: Execute safe shell commands (e.g., ls, sbt compile, grep). Avoid destructive commands unless explicitly requested and justified.
- CREATE_TASK {title, description, status}: Open a task for substantial work.
- UPDATE_TASK {id, title?, description?, status}: Update a task’s status/details.
- multi_tool_use.parallel: When multiple independent tool calls can run in parallel, batch them (e.g., run ls while reading a file).

Guidelines for tools
- Prefer READ_FILE over assumptions; verify before editing.
- Use UPDATE_FILE for textual assets. Do not write binaries.
- Use RUN_COMMAND for repo introspection (sbt compile, lists). Avoid network access commands unless required for builds and approved.
- Keep commands idempotent; never use sudo or destructive operations like rm -rf unless clearly required.

## Tasks workflow
- Create a task when a change involves more than a handful of file edits or reads, or spans multiple steps.
- Status lifecycle: Queued -> In Progress -> Done or Stuck.
- Avoid duplicate tasks: check conversation context for an existing task; update it rather than creating a new one.
- Keep titles concise and descriptions scoped to the change set.
- Update status as you proceed and mark Done when complete, summarizing what changed.

Standard operating procedure for changes
1) Plan: outline intent and files to touch.
2) Create or update a task (In Progress) if scope is non-trivial.
3) Inspect: READ_FILE and RUN_COMMAND to confirm current state.
4) Implement: UPDATE_FILE changes in small, reviewable commits (single file updates per call are fine).
5) Verify: RUN_COMMAND (e.g., sbt compile) when code is affected.
6) Document: update README/AGENT.md as needed.
7) Close: UPDATE_TASK to Done with a concise summary.

## Repository overview
- Module: compiler-yaml (Scala 3.3.1)
- Organization: us.awfl
- Version: 0.1.0-SNAPSHOT
- Purpose: compile a us.awfl.dsl.Workflow[_] into a serializable AST (us.awfl.yaml.Workflow) that matches Google Cloud Workflows constructs, suitable for JSON/YAML encoding (e.g., Circe YAML).

Key source components (src/main/scala)
- us.awfl.yaml.Compiler: compile(dsl.Workflow[_]) -> us.awfl.yaml.Workflow
- us.awfl.yaml.ValueEncoders: encodes DSL values and CEL expressions to JSON-compatible structures
- us.awfl.yaml.Workflow: case-class model of Cloud Workflows YAML/JSON
- us.awfl.yaml.Constants: maps DSL constants to Cloud Workflows environment variables

Related dependency
- us.awfl:dsl: the DSL that authors use to build workflows. This project consumes a dsl.Workflow[_] and produces an AST for emission.

## Known pitfalls and conventions
- Constants mapping: special DSL constants must map to Cloud Workflows environment variables exactly.
  - WORKFLOW_ID -> ${sys.get_env("GOOGLE_CLOUD_WORKFLOW_ID")}
  - WORKFLOW_EXECUTION_ID -> ${sys.get_env("GOOGLE_CLOUD_WORKFLOW_EXECUTION_ID")}
  - PROJECT_ID -> ${sys.get_env("GOOGLE_CLOUD_PROJECT_ID")}
  - LOCATION -> ${sys.get_env("GOOGLE_CLOUD_LOCATION")}
- Step naming: step names must be unique across the entire workflow. If duplicates occur, append _1, _2, … to disambiguate. Be consistent so references remain valid.
- Try/retry defaults: emitted calls are wrapped in try with a default retry policy of http.default_retry. Allow overrides when the DSL specifies custom behavior.
- Input parameter: the compiled workflow exposes a single parameter named "input"; ensure DSL workflows expect input via this parameter.
- YAML/JSON encoding: preserve field order where appropriate for readability when printing YAML (e.g., circe-yaml Printer with preserveOrder = true).

## Scala 3 coding standards (project-specific)
- Language level: Scala 3.3.1; prefer indentation syntax; avoid unnecessary braces.
- Public APIs: annotate return types on public defs/vals given to maintain clarity and binary compatibility.
- Immutability: prefer vals and case classes; avoid mutable state.
- Error handling: use Either/Option; avoid throwing exceptions in library code. Where Cloud Workflows semantics require raise, model it explicitly in the AST.
- Pattern matching: prefer exhaustive matches; avoid .asInstanceOf and unchecked casts.
- Collections: use standard library collections; avoid null; use Option for absence.
- Encoders/decoders: for Circe models, implement or derive encoders deterministically; avoid non-total encoders.
- Linting/flags: heed -deprecation and -feature warnings; fix or justify suppressions.
- Naming: keep names descriptive but concise; mirror Cloud Workflows terms (call, return, switch, try/except, raise, assign, block).
- Testing: prefer small, focused unit tests where applicable (none may exist yet; add if introducing complex logic).

## Build and local development
Requirements
- JDK 17+
- sbt 1.9+

Common commands
- Compile: sbt compile
- Package: sbt package
- Publish locally: sbt publishLocal

Consumption from another project (after publishLocal)
- libraryDependencies += "us.awfl" %% "compiler-yaml" % "0.1.0-SNAPSHOT"
- For YAML rendering convenience: "io.circe" %% "circe-yaml" % "0.14.2"

## Contribution guidelines
- Discuss scope: open or update a task with a clear title and description.
- Small PR mindset: implement changes in small, reviewable steps. Keep diffs focused.
- Tests/docs: add or update tests and documentation when changing behavior or public APIs.
- Consistency: follow Scala 3 standards and repository conventions outlined here.
- Commit messages: imperative mood, short summary line, body with rationale if needed.
- Versioning: this repo currently uses 0.1.0-SNAPSHOT; follow semantic versioning principles for releases.

## Assistant quick recipes
- Add or update documentation: READ_FILE to inspect, UPDATE_FILE to write, and update the task to Done with a summary.
- Change Scala source: READ_FILE target files, implement minimal changes via UPDATE_FILE, RUN_COMMAND "sbt compile" to validate, and document any new behavior.
- Investigate a bug: replicate with a small example if possible, READ_FILE relevant sources, propose a patch with rationale, and ensure compilation passes.

## What not to do
- Do not leak secrets or embed credentials in code or config.
- Do not run destructive commands (e.g., rm -rf) without explicit justification and approval.
- Do not introduce heavy, unrelated dependencies.
- Do not reformat the entire codebase without cause.
- Do not create duplicate tasks; update existing tasks instead.

## Maintainers and ownership
- Organization: us.awfl
- Module: compiler-yaml
- Contact: update this section with maintainer handles if available.

## Appendix: Minimal usage outline
- Given a us.awfl.dsl.Workflow[_], compile and render YAML:
  - import us.awfl.dsl._
  - import us.awfl.yaml
  - val wfAst: yaml.Workflow = yaml.compile(myWorkflow)
  - Render: io.circe.yaml.Printer(preserveOrder = true).pretty(wfAst.asJson)
