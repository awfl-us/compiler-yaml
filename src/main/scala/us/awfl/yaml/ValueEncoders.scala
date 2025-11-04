package us.awfl.yaml

import us.awfl.dsl._
import io.circe.Json
import scala.annotation.tailrec
import io.circe.JsonObject

sealed trait EncodedValue {
  val cel: String
  val json: Json
}

case class ResolvedValue(cel: String) extends EncodedValue {
  val ref = s"$${${cel}}" //.replace('\n', '\r')
  val json = Json.fromString(ref)
}
case class ObjValue(json: Json) extends EncodedValue {
  val cel = json.toString // Doesn't work
}

// @tailrec
def encode(value: BaseValue[_]): EncodedValue = value match {
  case resolved: Resolved[_] => ResolvedValue(encodeCel(Constants.replace(resolved).resolver.path))

  case Obj(value) => value match {
    case list: List[_] =>
      ObjValue(Json.arr(list.map {
        case v: BaseValue[_] => encode(v).json
        case other => encode(Obj(other)).json
      } *))

    case Some(value) => encode(obj(value))
    case None => ResolvedValue("null")

    case opt: OptValue[_] =>
      encode(opt.getOrElse(Value.nil(opt.spec)))
    case opt: OptList[_] =>
      encode(opt.getOrElse(ListValue.nil(opt.spec)))

    case p: Product =>
      val names = p.productElementNames.toList
      val values = p.productIterator.toList
      val jsonFields = encodePairs(names.zip(values))
      ObjValue(Json.fromJsonObject(JsonObject.fromIterable(jsonFields)))

    case m: Map[_, _] =>
      val jsonFields = encodePairs(m.asInstanceOf[Map[String, Any]].toList)
      ObjValue(Json.fromJsonObject(JsonObject.fromIterable(jsonFields)))

    case s: String => ObjValue(Json.fromString(s))
    case d: Double => ObjValue(Json.fromDouble(d).get)
    case i: Int => ObjValue(Json.fromInt(i))
    case b: Boolean => ObjValue(Json.fromBoolean(b))

    case other =>
      throw new Exception(s"Provided Spec did not produce a Product type or Map[String, _]: $other")
  }
}

def encodePairs(pairs: List[(String, _)]): List[(String, Json)] = pairs.map { case (name, field) =>
  val json = field match {
    case value: BaseValue[_] => encode(value).json
    case other => encode(obj(field)).json
  }
  name -> json
}

def encodeCel(cel: Cel): String = cel match {
  case CelConst(value) => value
  case CelStr(content) => s"\"$content\""
  case CelValue(value) => encode(value).cel
  case CelOp(left, op, right) => s"(${encodeCel(left)} $op ${encodeCel(right)})"
  case CelFunc(name, params) => s"${name}(${params.map(encodeCel).mkString(", ")})"
  case CelAt(list, i) => s"${encodeCel(CelValue(list))}[${encodeCel(i)}]"
  case CelPath(path) => path.map(encodeCel).mkString(".")
}
