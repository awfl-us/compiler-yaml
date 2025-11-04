package us.awfl.yaml

import io.circe.syntax._
import us.awfl.dsl._
import us.awfl.dsl
import us.awfl.dsl.CelOps._
import us.awfl.yaml
import scala.annotation.tailrec
import io.circe.generic.auto._
import io.circe.Encoder
import io.circe.Json
import scala.collection.mutable

def compile(workflow: dsl.Workflow[_]) = {
  val used = mutable.Set[String]()
  val (steps, result) = workflow.steps
  yaml.Workflow(
    Main(List("input"), compileSteps(used, steps :+ Return("return", result)))
  )
}

// Ensure step names are globally unique across the entire workflow by appending _i to duplicates
private def ensureUniqueStepNames(used: mutable.Set[String], steps: List[Map[String, StepDefinition]]): List[Map[String, StepDefinition]] = {
  steps.map { m =>
    val (name, defn) = m.head
    var unique = name
    if (used.contains(unique)) {
      var i = 1
      var candidate = s"${name}_$i"
      while (used.contains(candidate)) { i += 1; candidate = s"${name}_$i" }
      unique = candidate
    }
    used += unique
    if (unique == name) m else Map(unique -> defn)
  }
}

private def compileSteps(used: mutable.Set[String], steps: List[Step[_, _]]): List[Map[String, StepDefinition]] = {
  val compiled = steps.map(s => 
    try {
      Map(s.name -> compileStep(s, used))
    } catch {
      case e: Exception =>
        println(s"Error compiling step '${s.name}': ${e.getMessage}")
        e.printStackTrace()
        throw e
    }
  )
  ensureUniqueStepNames(used, compiled)
}

case class ResultStep(step: Step[_, _]) {
  import step._

  def initValue[T](used: mutable.Set[String], value: Json, stepWithUpdate: StepDefinition) = {
    val inner = List(
      Map(s"${name}_initResult" -> yaml.AssignStep(Map(encodeCel(resultValue.cel) -> value) :: Nil)),
      Map(s"${name}_steps" -> stepWithUpdate),
    )
    yaml.BlockStep(ensureUniqueStepNames(used, inner))
  }

  def updateValue[T](update: EncodedValue, suffix: String = "") = {
    Map(s"${name}_updateResult$suffix" -> yaml.AssignStep(Map(encodeCel(resultValue.cel) -> update.json) :: Nil))
  }

  def concatNext[T](iterResult: BaseValue[_]) = {
    val (maybeAssignObjSteps, resultForConcat) = encode(iterResult) match {
      case ResolvedValue(cel) => Nil -> cel
      case ObjValue(json) =>
        val iterResultName = s"${name}_assignIterResultVar"
        List(Map(s"${name}_assignIterResult" -> yaml.AssignStep(Map(iterResultName -> json) :: Nil))) ->
          iterResultName
    }

    val newResult = ResolvedValue(s"list.concat(${encode(resultValue).cel}, $resultForConcat)")
    
    maybeAssignObjSteps ++ List(
      Map(s"${name}_updateResult" -> yaml.AssignStep(Map(encodeCel(resultValue.cel) -> newResult.json) :: Nil))
    )
  }
}

@tailrec
private def compileStep(step: Step[_, _], used: mutable.Set[String]): StepDefinition = step match {
  case callStep: Call[_, _] => import callStep._
    val body = ensureUniqueStepNames(used, List(
      Map(s"${step.name}_call" -> CallStep(
        call = call,
        args = encode(args).json,
        result = encodeCel(resultValue.resolver.path)
      ))
    ))
    TryStep(yaml.Try(body), None, Some(ResolvedValue("http.default_retry").ref))

  case returnStep: Return[_] =>
    ReturnStep(encode(returnStep.value).json)

  case forStep: For[_, _] =>
    val resultStep = ResultStep(forStep)
    import resultStep._
    import forStep._

    val (steps, iterResult) = each

    val compiled = compileSteps(used, steps)
    val extras = ensureUniqueStepNames(used, concatNext(iterResult))
    val body = compiled ++ extras

    initValue(used, Json.arr(), yaml.ForStep(yaml.For(
      encodeCel(item),
      ResolvedValue(encodeCel(in.resolver.path)).ref,
      body
    )))

  case forRange: ForRange[_] =>
    val resultStep = ResultStep(forRange)
    import resultStep._
    import forRange._

    val (steps, iterResult) = each

    val compiled = compileSteps(used, steps)
    val extras = ensureUniqueStepNames(used, concatNext(iterResult))
    val body = compiled ++ extras

    initValue(used, Json.arr(), yaml.ForRangeStep(yaml.ForRange(
      idx.value,
      ResolvedValue(s"[${encodeCel(from)}, ${encodeCel(to - 1)}]").ref,
      body
    )))

  case fold: Fold[_, _] =>
    val resultStep = ResultStep(fold)
    import resultStep._
    import fold._

    val (steps, foldResult) = run

    val compiled = compileSteps(used, steps)
    val extras = ensureUniqueStepNames(used, List(updateValue(encode(foldResult))))
    val body = compiled ++ extras

    initValue(used, encode(b).json, yaml.ForStep(yaml.For(
      encodeCel(item),
      ResolvedValue(encodeCel(list.resolver.path)).ref,
      body
    )))

  case switch: Switch[_, _] =>
    val resultStep = ResultStep(switch)
    import resultStep._
    import switch._

    initValue(used, Json.obj(), yaml.SwitchStep(cases.zipWithIndex.map { case ((cond, (steps, out)), i) =>
      val compiled = compileSteps(used, steps)
      val extras = ensureUniqueStepNames(used, List(updateValue(encode(out), s"_$i")))
      val body = compiled ++ extras
      yaml.Case(ResolvedValue(encodeCel(cond)).ref, body)
    }))

  case tryStep: Try[_, _] =>
    val resultStep = ResultStep(tryStep)
    import resultStep._
    import tryStep._

    val errorName = s"${name}Error"

    val (steps, output) = run
    val (exceptSteps, exceptOutput) = except(dsl.init(errorName))

    val tryCompiled = compileSteps(used, steps)
    val tryExtras = ensureUniqueStepNames(used, List(updateValue(encode(output))))
    val tryBody = tryCompiled ++ tryExtras

    val exceptCompiled = compileSteps(used, exceptSteps)
    val exceptExtras = ensureUniqueStepNames(used, List(updateValue(encode(exceptOutput), "Error")))
    val exceptBody = exceptCompiled ++ exceptExtras

    initValue(used, Json.obj(), yaml.TryStep(
      yaml.Try(tryBody),
      Some(yaml.Except(errorName, exceptBody)),
      None
    ))

  case Raise(_, raise) =>
    yaml.RaiseStep(encode(raise).json)

  case block: Block[_, _] =>
    yaml.BlockStep(compileSteps(used, block.steps))

  case flatMap: FlatMap[_, _, _] =>
    compileStep(flatMap.step, used)
}
