package us.awfl.yaml

import us.awfl.dsl.*

/** Backend-neutral names render identically to the existing Google strings. */
object OperationNamesSmoke {
  def main(args: Array[String]): Unit = {
    val input = CelConst("1")
    assert(encodeCel(CelFunc(CelFunctions.Length, input)) == "len(1)")
    assert(encodeCel(CelFunc(CelFunctions.JsonEncodeString, input)) ==
      "json.encode_to_string(1)")
    assert(encodeCel(CelFunc(CelFunctions.TextLowercase, CelStr("UP"))) ==
      "text.to_lower(\"UP\")")
    assert(encodeCel(CelFunc("len", input)) == "len(1)")
    assert(encodeCel(CelOp(CelConst("1"), "+", CelConst("2"))) == "(1 + 2)")

    val request = Call[Map[String, String], String]("request",
      CallOperations.WorkflowRun, obj(Map("workflow" -> "example")))
    val compiled = compile(Workflow((List(request), request.resultValue)))
    assert(compiled.toString.contains(
      "googleapis.workflowexecutions.v1.projects.locations.workflows.executions.run"))
    assert(!compiled.toString.contains("workflow.run"))
    println("Existing YAML call/CEL shapes render backend-neutral constants")
  }
}
