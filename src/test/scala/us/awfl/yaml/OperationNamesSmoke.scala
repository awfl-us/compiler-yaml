package us.awfl.yaml

import us.awfl.dsl.*

/** Backend-neutral names render identically to the existing Google strings. */
object OperationNamesSmoke {
  def main(args: Array[String]): Unit = {
    val input = CelConst("1")
    val functionCases = List(
      CelFunctions.Default -> "default",
      CelFunctions.TypeOf -> "get_type",
      CelFunctions.ToInt -> "int",
      CelFunctions.ToString -> "string",
      CelFunctions.Length -> "len",
      CelFunctions.MapGet -> "map.get",
      CelFunctions.MapMerge -> "map.merge",
      CelFunctions.ListConcat -> "list.concat",
      CelFunctions.JsonDecode -> "json.decode",
      CelFunctions.JsonEncodeBytes -> "json.encode",
      CelFunctions.JsonEncodeString -> "json.encode_to_string",
      CelFunctions.Base64Encode -> "base64.encode",
      CelFunctions.TextEncode -> "text.encode",
      CelFunctions.TextDecode -> "text.decode",
      CelFunctions.TextSplit -> "text.split",
      CelFunctions.TextSubstring -> "text.substring",
      CelFunctions.TextLowercase -> "text.to_lower",
      CelFunctions.TextUrlEncode -> "text.url_encode",
      CelFunctions.TimeFormat -> "time.format",
      CelFunctions.UuidGenerate -> "uuid.generate",
      CelFunctions.ClockNow -> "sys.now",
      CelFunctions.EnvironmentGet -> "sys.get_env"
    )
    assert(functionCases.size == 22)
    for ((semantic, google) <- functionCases) {
      assert(encodeCel(CelFunc(semantic, input)) == s"$google(1)")
      assert(encodeCel(CelFunc(google, input)) == s"$google(1)")
    }
    assert(encodeCel(CelOp(CelConst("1"), "+", CelConst("2"))) == "(1 + 2)")
    assert(encodeCel(CelOp(CelConst("not"), "", CelConst("true"))).contains("not"))

    val callCases = List(
      CallOperations.HttpGet -> "http.get",
      CallOperations.HttpPost -> "http.post",
      CallOperations.HttpPatch -> "http.patch",
      CallOperations.HttpDelete -> "http.delete",
      CallOperations.LogWrite -> "sys.log",
      CallOperations.CallbackCreate -> "events.create_callback_endpoint",
      CallOperations.CallbackAwait -> "events.await_callback",
      CallOperations.WorkflowRun -> "googleapis.workflowexecutions.v1.projects.locations.workflows.executions.run",
      CallOperations.StorageGet -> "googleapis.storage.v1.objects.get",
      CallOperations.MessagingPublish -> "googleapis.pubsub.v1.projects.topics.publish"
    )
    assert(callCases.size == 10)
    for ((semantic, google) <- callCases) {
      val canonical = Call[Map[String, String], String]("request", semantic,
        obj(Map("workflow" -> "example")))
      val old = Call[Map[String, String], String]("request", google,
        obj(Map("workflow" -> "example")))
      // The original Step.resultValue uses a hash-derived slot name, so
      // substituting a name can alter *temporary YAML variable IDs* while
      // preserving the actual Google connector target and arguments.
      val canonicalYaml = compile(Workflow((List(canonical), canonical.resultValue))).toString
      val legacyYaml = compile(Workflow((List(old), old.resultValue))).toString
      assert(canonicalYaml.contains(google), s"missing mapped target: $semantic")
      assert(legacyYaml.contains(google), s"missing old target: $google")
      assert(!canonicalYaml.contains(s"CallStep($semantic,") || semantic == google)
    }
    println("Existing YAML call/CEL shapes render backend-neutral constants")
  }
}
