package us.awfl.yaml

import us.awfl.dsl.*
import us.awfl.dsl.CelOps.*

/** Verify backend-neutral Calls become Google connectors and original CEL
  * functions remain standard without adding new DSL AST cases.
  */
object GenericCallNamesSmoke {
  def main(args: Array[String]): Unit = {
    val cases = Seq(
      CallOperations.HttpGet -> "http.get",
      CallOperations.HttpPost -> "http.post",
      CallOperations.HttpPatch -> "http.patch",
      CallOperations.HttpDelete -> "http.delete",
      CallOperations.LogWrite -> "sys.log",
      CallOperations.CallbackCreate -> "events.create_callback_endpoint",
      CallOperations.CallbackAwait -> "events.await_callback",
      CallOperations.WorkflowRun ->
        "googleapis.workflowexecutions.v1.projects.locations.workflows.executions.run",
      CallOperations.StorageGet -> "googleapis.storage.v1.objects.get",
      CallOperations.MessagingPublish ->
        "googleapis.pubsub.v1.projects.topics.publish"
    )

    for ((id, expected) <- cases) {
      assert(googleCallName(id) == expected)
      val call = Call[Map[String, String], String]("connector", id,
        obj(Map("url" -> "https://example.org")))
      val compiled = compile(Workflow((List(call), call.resultValue)))
      val step = compiled.main.steps.head("connector").asInstanceOf[TryStep]
      val emitted = step.`try`.steps.head("connector_call").asInstanceOf[CallStep]
      assert(emitted.call == expected, s"$id emitted ${emitted.call}")
    }
    val legacy = "googleapis.workflowexecutions.v1.projects.locations.workflows.executions.run"
    assert(googleCallName(legacy) == legacy)
    assert(googleCallName("sys.log") == "sys.log")
    assert(encodeCel(time.now()) == "sys.now()")
    assert(encodeCel(map.get(CelConst("input"), "foo")) == "map.get(input, \"foo\")")
    assert(encodeCel(text.to_lower("HELLO")) == "text.to_lower(\"HELLO\")")
    assert(encodeCel(uuid.generate()) == "uuid.generate()")
    println("GENERIC_CALL_NAMES_AND_STANDARD_CEL_PASS")
  }
}
