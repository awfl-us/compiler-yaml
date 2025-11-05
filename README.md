# compiler-yaml — AWFL DSL → Google Cloud Workflows YAML compiler

[![Scala 3.3.1](https://img.shields.io/badge/Scala-3.3.1-red.svg)](https://www.scala-lang.org/)
[![sbt ≥ 1.9](https://img.shields.io/badge/sbt-%E2%89%A5%201.9-blue.svg)](https://www.scala-sbt.org/)
[![Status: Snapshot](https://img.shields.io/badge/status-0.1.0--SNAPSHOT-orange.svg)](#status)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

A small Scala 3 library that compiles workflows authored with `us.awfl.dsl` into a type-safe AST that mirrors Google Cloud Workflows. The resulting model can be encoded to YAML or JSON for deployment.

---

## Table of contents
- Overview
- Features
- Getting started
  - Requirements
  - Install
  - Quick start
- What gets generated
- Conventions and defaults
- Project structure
- Building locally
- Version and status
- Contributing
- License

---

## Overview
`compiler-yaml` turns a `dsl.Workflow[_]` (from `us.awfl:dsl`) into a serializable `us.awfl.yaml.Workflow` data model aligned with Cloud Workflows constructs (call, return, for, switch, try/except, raise, assign, block, …). You can then print YAML via `circe-yaml` or use the JSON form directly.

## Features
- Scala 3.3.1, idiomatic and type-safe
- One-step compilation from AWFL DSL to a Cloud Workflows-compatible AST
- Deterministic JSON/YAML encoding (works well with `circe-yaml`)
- Sensible defaults for try/retry, parameterization, and step naming

## Getting started

### Requirements
- JDK 17+
- sbt 1.9+

### Install
If you published locally (see Building locally):

```scala
libraryDependencies ++= Seq(
  "us.awfl" %% "compiler-yaml" % "0.1.0-SNAPSHOT",
  // Only needed if you want to render YAML directly
  "io.circe" %% "circe-yaml" % "0.14.2"
)
```

Note: `compiler-yaml` depends on `us.awfl:dsl`. When working from source, ensure the DSL module is available (e.g., published locally).

### Quick start
```scala
import us.awfl.dsl._
import us.awfl.yaml
import io.circe.syntax._
import io.circe.yaml.Printer

// Assume you have built a workflow with the AWFL DSL
// val myWorkflow: dsl.Workflow[_] = ...

val wfAst: yaml.Workflow = yaml.compile(myWorkflow)

// JSON (for APIs or inspection)
val json = wfAst.asJson

// Pretty-print YAML for Cloud Workflows deployment
val printer = Printer(preserveOrder = true, mappingStyle = Printer.FlowStyle.Block)
val yamlString: String = printer.pretty(json)
println(yamlString)
```

## What gets generated
- Parameters: the compiled workflow exposes a single parameter named `input`. Your DSL workflow should expect input via this parameter.
- Steps: DSL nodes (Call, Return, For, ForRange, Fold, Switch, Try, Raise, Assign, Block) map to equivalent Cloud Workflows fields.
- Unique step names: duplicate step names are disambiguated with a numeric suffix (`_1`, `_2`, …) to ensure global uniqueness.

## Conventions and defaults
- Try/retry: outbound calls are wrapped in `try` with a default `retry: http.default_retry`. The DSL can override where supported.
- Constants mapping: special DSL constants resolve to Cloud-provided values:
  - `WORKFLOW_ID` → `${sys.get_env("GOOGLE_CLOUD_WORKFLOW_ID")}`
  - `WORKFLOW_EXECUTION_ID` → `${sys.get_env("GOOGLE_CLOUD_WORKFLOW_EXECUTION_ID")}`
  - `PROJECT_ID` → `${sys.get_env("GOOGLE_CLOUD_PROJECT_ID")}`
  - `LOCATION` → `${sys.get_env("GOOGLE_CLOUD_LOCATION")}`
- YAML printing: when using `circe-yaml`, prefer a printer with `preserveOrder = true` for stable, readable output.

## Project structure
- `build.sbt` — module definition (name: `compiler-yaml`, Scala 3.3.1)
- `src/main/scala`
  - `us.awfl.yaml.Compiler` — `compile(dsl.Workflow[_]) -> us.awfl.yaml.Workflow`
  - `us.awfl.yaml.ValueEncoders` — encodes DSL values and CEL expressions
  - `us.awfl.yaml.Workflow` — case-class model of Cloud Workflows YAML/JSON
  - `us.awfl.yaml.Constants` — DSL constants → Cloud Workflows environment variables

## Building locally
Common tasks:
- Compile: `sbt compile`
- Package: `sbt package`
- Publish locally: `sbt publishLocal`

After `publishLocal`, you can depend on this module from another sbt project:
```scala
libraryDependencies += "us.awfl" %% "compiler-yaml" % "0.1.0-SNAPSHOT"
```

## Version and status
- Current version: `0.1.0-SNAPSHOT`
- This module is intended for use alongside `us.awfl:dsl` and may evolve; follow semantic versioning for future releases.

## Contributing
Contributions are welcome. Please keep changes small and focused, follow Scala 3 idioms, and maintain deterministic encoders. If changing behavior or public APIs, add/update docs and tests where applicable.

## License
This project is open source under the MIT License. See [LICENSE](LICENSE) for details.
