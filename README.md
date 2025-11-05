compiler-yaml — Compile the AWFL DSL to Google Cloud Workflows YAML

Overview
This library provides a compiler from the us.awfl.dsl workflow DSL to a data model that matches Google Cloud Workflows. You can then emit YAML (or JSON) for deployment using circe-yaml or other encoders.

Key points
- Language: Scala 3.3.1
- Artifact name: compiler-yaml
- Purpose: Turn a dsl.Workflow[_] into a serializable Workflow AST that mirrors Cloud Workflows constructs (call, return, for, switch, try/except, raise, assign, block, etc.).
- Output: A case-class model (us.awfl.yaml.Workflow) that can be encoded to JSON/YAML via Circe.

Status
- Version: 0.1.0-SNAPSHOT
- Intended for use alongside the us.awfl:dsl library.

Getting started
1) Add dependencies (sbt)
- If you published locally (see Build locally below):
  libraryDependencies ++= Seq(
    "us.awfl" %% "compiler-yaml" % "0.1.0-SNAPSHOT",
    // Only needed if you want to render YAML directly
    "io.circe" %% "circe-yaml" % "0.14.2"
  )

Note: compiler-yaml depends on us.awfl:dsl and will bring it in transitively when published. If you work from source, ensure the dsl module is available (e.g., published locally).

2) Compile a DSL workflow to YAML
Assuming you already build a workflow with us.awfl.dsl:

import us.awfl.dsl._
import us.awfl.yaml
import io.circe.syntax._

// Optionally, to print YAML
import io.circe.yaml.Printer

// myWorkflow: dsl.Workflow[_] is constructed using the AWFL DSL in your app
val wfAst: yaml.Workflow = yaml.compile(myWorkflow)

// Encode to JSON (for APIs or inspection)
val json = wfAst.asJson

// Pretty-print YAML for Cloud Workflows deployment
val printer = Printer(preserveOrder = true, mappingStyle = Printer.FlowStyle.Block)
val yamlString: String = printer.pretty(json)
println(yamlString)

What the compiler generates
- main.params: The compiled workflow has a single parameter named "input". Your DSL workflow should expect its input via this parameter.
- Steps: DSL steps (Call, Return, For, ForRange, Fold, Switch, Try, Raise, Assign, Block) are mapped to the equivalent Cloud Workflows fields.
- Unique step names: If duplicate step names occur, an incrementing suffix (_1, _2, …) is added to keep names unique across the entire workflow.
- Try semantics: Calls are emitted within a try block and default to retry: http.default_retry (aligning with common Cloud Workflows retry semantics).
- Constants mapping: Special DSL constants are replaced with Cloud-provided environment values:
  - WORKFLOW_ID -> ${sys.get_env("GOOGLE_CLOUD_WORKFLOW_ID")}
  - WORKFLOW_EXECUTION_ID -> ${sys.get_env("GOOGLE_CLOUD_WORKFLOW_EXECUTION_ID")}
  - PROJECT_ID -> ${sys.get_env("GOOGLE_CLOUD_PROJECT_ID")}
  - LOCATION -> ${sys.get_env("GOOGLE_CLOUD_LOCATION")}

Minimal example shape
Below is the rough shape; the exact DSL construction happens in us.awfl.dsl. Once you have a dsl.Workflow[_], this library turns it into the YAML structure shown.

// val myWorkflow: dsl.Workflow[_] = ... build using us.awfl.dsl ...
val wfAst = us.awfl.yaml.compile(myWorkflow)
val yamlStr = io.circe.yaml.Printer().pretty(wfAst.asJson)

Building locally
Requirements
- JDK 17+
- sbt 1.9+

Commands
- Compile: sbt compile
- Package: sbt package
- Publish locally (for use in other projects): sbt publishLocal

After publishLocal, you should see artifacts under target/scala-3.3.1, and you can depend on this module from another sbt project via:
  libraryDependencies += "us.awfl" %% "compiler-yaml" % "0.1.0-SNAPSHOT"

Project structure
- build.sbt — module definition (name: compiler-yaml, Scala 3.3.1)
- src/main/scala — sources (encoded in the published -sources jar)
  - us.awfl.yaml.Compiler — compile(dsl.Workflow[_]) -> us.awfl.yaml.Workflow
  - us.awfl.yaml.ValueEncoders — encodes DSL values and CEL expressions to JSON-compatible structures
  - us.awfl.yaml.Workflow — case-class model of Cloud Workflows YAML/JSON
  - us.awfl.yaml.Constants — maps DSL constants to Cloud Workflow environment variables

License
This project is open source under the LICENSE file in the repository.
