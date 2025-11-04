package us.awfl.yaml

import io.circe.syntax._
import io.circe.Json
import io.circe.Encoder
import io.circe.generic.auto._

case class Workflow(main: Main)

case class Main(
  params: List[String],
  steps: List[Map[String, StepDefinition]]
)

sealed trait StepDefinition

object StepDefinition {
  implicit val stepDefinitionEncoder: Encoder[StepDefinition] = Encoder.instance {
    case call: CallStep => call.asJson
    case ret: ReturnStep => ret.asJson
    case forr: ForStep => forr.asJson
    case forRange: ForRangeStep => forRange.asJson
    case switch: SwitchStep => switch.asJson
    case tryy: TryStep => tryy.asJson
    case raise: RaiseStep => raise.asJson
    case assign: AssignStep => assign.asJson
    case block: BlockStep => block.asJson
  }
}

case class CallStep(
  call: String,
  args: Json,
  result: String
) extends StepDefinition

case class ReturnStep(
  `return`: Json
) extends StepDefinition

case class For(value: String, in: String, steps: List[Map[String, StepDefinition]])

case class ForStep(`for`: For) extends StepDefinition

case class ForRange(value: String, range: String, steps: List[Map[String, StepDefinition]])

case class ForRangeStep(`for`: ForRange) extends StepDefinition

case class Case(condition: String, steps: List[Map[String, StepDefinition]])

case class SwitchStep(switch: List[Case]) extends StepDefinition

case class Except(as: String, steps: List[Map[String, StepDefinition]])

case class Try(steps: List[Map[String, StepDefinition]])

case class TryStep(`try`: Try, except: Option[Except], retry: Option[String]) extends StepDefinition

case class RaiseStep(raise: Json) extends StepDefinition

case class AssignStep(assign: List[Map[String, Json]]) extends StepDefinition

case class BlockStep(steps: List[Map[String, StepDefinition]]) extends StepDefinition
