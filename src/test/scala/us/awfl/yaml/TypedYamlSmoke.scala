package us.awfl.yaml

import us.awfl.dsl.*

/** Backwards-compatibility checks for typed operations and legacy CEL rendering.
  * Run with: sbt 'Test / runMain us.awfl.yaml.TypedYamlSmoke'
  */
object TypedYamlSmoke {
  def main(args: Array[String]): Unit = {
    val legacy = CelOp(CelConst("1"), "+", CelConst("2"))
    val native = CelBinary(CelConst("1"), OperatorId.Add, CelConst("2"))
    assert(encodeCel(native) == encodeCel(legacy))
    assert(encodeCel(CelUnary(OperatorId.Not, CelBoolean(true))) == "(not true)")
    assert(encodeCel(CelGlobal(GlobalId.WorkflowId)) ==
      "sys.get_env(\"GOOGLE_CLOUD_WORKFLOW_ID\")")
    assert(encodeCel(CelGlobal(GlobalId.CloudProjectId)) ==
      "sys.get_env(\"GOOGLE_CLOUD_PROJECT_ID\")")
    val legacyFunction = CelFunc("json.encode_to_string", CelStr("hello"))
    val typedFunction = CelFunction(FunctionId.JsonEncodeString, CelStr("hello"))
    assert(encodeCel(typedFunction) == encodeCel(legacyFunction))
    assert(encodeCel(CelString("a\"\nb")) == "\"a\\\"\\nb\"")
    assert(encodeCel(CelList(List(CelInteger(1), CelBoolean(true), CelNull))) ==
      "[1, true, null]")
    println("Typed YAML compatibility smoke checks passed")
  }
}
