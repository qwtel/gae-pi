package pi.json

import net.minidev.json.JSONValue
import net.minidev.json.JSONArray
import net.minidev.json.JSONObject

import scala.language.dynamics
import scala.collection.JavaConversions._

/**
 * http://www.furida.mu/blog/2012/09/18/beautiful-json-parsing-in-scala/
 */
object JSON {
  def parseJSON(s: String) = new ScalaJSON(JSONValue.parse(s))

  def makeJSON(a: Any): String = a match {
    case m: Map[String, Any] => m.map {
      case (name, content) => "\"" + name + "\":" + makeJSON(content)
    }.mkString("{", ",", "}")
    case l: List[Any] => l.map(makeJSON).mkString("[", ",", "]")
    case l: java.util.List[Any] => l.map(makeJSON).mkString("[", ",", "]")
    case s: String => "\"" + s + "\""
    case i: Int => i.toString
    case l: Long => l.toString
    case b: Boolean => b.toString
    case d: Double => d.toString
  }

  implicit def ScalaJSONToString(s: ScalaJSON) = s.toString

  implicit def ScalaJSONToInt(s: ScalaJSON) = s.toInt

  implicit def ScalaJSONToLong(s: ScalaJSON) = s.toLong

  implicit def ScalaJSONToDouble(s: ScalaJSON) = s.toDouble

  implicit def ScalaJSONToBoolean(s: ScalaJSON) = s.toBoolean
}

class JSONException(s: String) extends Exception(s) {
  def this() = this("")
}

class ScalaJSONIterator(i: java.util.Iterator[java.lang.Object]) extends Iterator[ScalaJSON] {
  def hasNext = i.hasNext

  def next() = new ScalaJSON(i.next())
}


class ScalaJSON(o: java.lang.Object) extends Seq[ScalaJSON] with Dynamic {
  override def toString(): String = o.toString

  def toInt: Int = o match {
    case i: java.lang.Number => i.intValue()
    case _ => throw new JSONException
  }

  def toLong: Long = o match {
    case i: java.lang.Number => i.longValue()
    case _ => throw new JSONException
  }

  def toBoolean: Boolean = o match {
    case b: java.lang.Boolean => b
    case _ => throw new JSONException
  }

  def toDouble: Double = o match {
    case n: java.lang.Number => n.doubleValue()
    case e: Any => throw new JSONException("Not a number: " + e.getClass.toString)
  }

  def apply(key: String): ScalaJSON = o match {
    case m: JSONObject => new ScalaJSON(m.get(key))
    case _ => throw new JSONException
  }

  def apply(idx: Int): ScalaJSON = o match {
    case a: JSONArray => new ScalaJSON(a.get(idx))
    case _ => throw new JSONException
  }

  def length: Int = o match {
    case a: JSONArray => a.size()
    case m: JSONObject => m.size()
    case _ => throw new JSONException
  }

  def iterator: Iterator[ScalaJSON] = o match {
    case a: JSONArray => new ScalaJSONIterator(a.iterator())
    case _ => throw new JSONException
  }

  def selectDynamic(name: String): ScalaJSON = apply(name)

  def applyDynamic(name: String)(arg: Any) = {
    arg match {
      case s: String => apply(name)(s)
      case n: Int => apply(name)(n)
      case u: Unit => apply(name)
    }
  }
}
